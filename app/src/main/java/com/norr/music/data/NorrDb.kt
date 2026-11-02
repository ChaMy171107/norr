package com.norr.music.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Bộ nhớ đệm thư viện nhạc bằng SQLite thuần (không cần thư viện ngoài).
 *
 * Mục đích chính: mở app là thấy nhạc ngay, không phải quét lại toàn bộ bộ nhớ.
 * Mỗi bài lưu kèm (sizeBytes, modifiedAt) để lần quét sau chỉ xử lý file mới hoặc file đã đổi.
 */
class NorrDb(context: Context) : SQLiteOpenHelper(context, NAME, null, VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE tracks(
                path        TEXT PRIMARY KEY NOT NULL,
                title       TEXT,
                artist      TEXT,
                album       TEXT,
                duration_ms INTEGER NOT NULL DEFAULT 0,
                has_art     INTEGER NOT NULL DEFAULT 0,
                size_bytes  INTEGER NOT NULL DEFAULT 0,
                modified_at INTEGER NOT NULL DEFAULT 0,
                album_id    INTEGER NOT NULL DEFAULT 0,
                added_at    INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_tracks_title ON tracks(title)")
        db.execSQL("CREATE INDEX idx_tracks_artist ON tracks(artist)")

        db.execSQL(
            """
            CREATE TABLE playlists(
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                name       TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                sort_order INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE playlist_tracks(
                playlist_id INTEGER NOT NULL,
                path        TEXT NOT NULL,
                position    INTEGER NOT NULL,
                PRIMARY KEY(playlist_id, path),
                FOREIGN KEY(playlist_id) REFERENCES playlists(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_pt ON playlist_tracks(playlist_id, position)")

        db.execSQL(
            """
            CREATE TABLE favorites(
                path    TEXT PRIMARY KEY NOT NULL,
                added_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE recent(
                path      TEXT PRIMARY KEY NOT NULL,
                played_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Bản 2.0 là bản đầu tiên có DB. Nếu nâng cấp sau này, xử lý theo oldVersion.
    }

    // ---------------------------------------------------------------- tracks

    suspend fun loadTracks(): List<Track> = withContext(Dispatchers.IO) {
        val out = ArrayList<Track>(512)
        readableDatabase.rawQuery(
            "SELECT path,title,artist,album,duration_ms,has_art,size_bytes,modified_at,album_id " +
                "FROM tracks",
            null,
        ).use { c ->
            while (c.moveToNext()) {
                val path = c.getString(0) ?: continue
                out.add(
                    Track(
                        file = File(path),
                        title = c.getStringOrNull(1),
                        artist = c.getStringOrNull(2),
                        album = c.getStringOrNull(3),
                        durationMs = c.getLong(4),
                        hasArtwork = c.getInt(5) != 0,
                        sizeBytes = c.getLong(6),
                        modifiedAt = c.getLong(7),
                        albumId = c.getLong(8),
                    ),
                )
            }
        }
        out
    }

    /** Chữ ký để so sánh nhanh: đường dẫn -> (size, modifiedAt). */
    suspend fun loadSignatures(): Map<String, LongArray> = withContext(Dispatchers.IO) {
        val map = HashMap<String, LongArray>(1024)
        readableDatabase.rawQuery("SELECT path,size_bytes,modified_at FROM tracks", null).use { c ->
            while (c.moveToNext()) {
                c.getString(0)?.let { map[it] = longArrayOf(c.getLong(1), c.getLong(2)) }
            }
        }
        map
    }

    suspend fun upsertTracks(tracks: List<Track>): Unit = withContext(Dispatchers.IO) {
        if (tracks.isEmpty()) return@withContext
        val db = writableDatabase
        db.beginTransaction()
        try {
            val stmt = db.compileStatement(
                """
                INSERT INTO tracks(path,title,artist,album,duration_ms,has_art,size_bytes,modified_at,album_id,added_at)
                VALUES(?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(path) DO UPDATE SET
                    title=excluded.title,
                    artist=excluded.artist,
                    album=excluded.album,
                    duration_ms=excluded.duration_ms,
                    has_art=excluded.has_art,
                    size_bytes=excluded.size_bytes,
                    modified_at=excluded.modified_at,
                    album_id=excluded.album_id
                """.trimIndent(),
            )
            val now = System.currentTimeMillis()
            for (t in tracks) {
                stmt.clearBindings()
                stmt.bindString(1, t.file.absolutePath)
                bindNullable(stmt, 2, t.title)
                bindNullable(stmt, 3, t.artist)
                bindNullable(stmt, 4, t.album)
                stmt.bindLong(5, t.durationMs)
                stmt.bindLong(6, if (t.hasArtwork) 1L else 0L)
                stmt.bindLong(7, t.sizeBytes)
                stmt.bindLong(8, t.modifiedAt)
                stmt.bindLong(9, t.albumId)
                stmt.bindLong(10, now)
                stmt.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    suspend fun deleteTracks(paths: Collection<String>): Unit = withContext(Dispatchers.IO) {
        if (paths.isEmpty()) return@withContext
        val db = writableDatabase
        db.beginTransaction()
        try {
            val stmt = db.compileStatement("DELETE FROM tracks WHERE path=?")
            for (p in paths) {
                stmt.clearBindings()
                stmt.bindString(1, p)
                stmt.executeUpdateDelete()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    suspend fun updateTrackArtwork(path: String, hasArt: Boolean): Unit = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply { put("has_art", if (hasArt) 1 else 0) }
        writableDatabase.update("tracks", cv, "path=?", arrayOf(path))
    }

    // ------------------------------------------------------------- playlists

    suspend fun playlists(): List<Playlist> = withContext(Dispatchers.IO) {
        val out = ArrayList<Playlist>()
        readableDatabase.rawQuery(
            """
            SELECT p.id, p.name, p.created_at, COUNT(pt.path)
            FROM playlists p
            LEFT JOIN playlist_tracks pt ON pt.playlist_id = p.id
            GROUP BY p.id
            ORDER BY p.sort_order ASC, p.created_at ASC
            """.trimIndent(),
            null,
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    Playlist(
                        id = c.getLong(0),
                        name = c.getString(1) ?: "Danh sách",
                        createdAt = c.getLong(2),
                        trackCount = c.getInt(3),
                    ),
                )
            }
        }
        out
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("name", name)
            put("created_at", System.currentTimeMillis())
            put("sort_order", System.currentTimeMillis())
        }
        writableDatabase.insert("playlists", null, cv)
    }

    suspend fun renamePlaylist(id: Long, name: String): Unit = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply { put("name", name) }
        writableDatabase.update("playlists", cv, "id=?", arrayOf(id.toString()))
    }

    suspend fun deletePlaylist(id: Long): Unit = withContext(Dispatchers.IO) {
        writableDatabase.delete("playlists", "id=?", arrayOf(id.toString()))
    }

    suspend fun playlistPaths(id: Long): List<String> = withContext(Dispatchers.IO) {
        val out = ArrayList<String>()
        readableDatabase.rawQuery(
            "SELECT path FROM playlist_tracks WHERE playlist_id=? ORDER BY position ASC",
            arrayOf(id.toString()),
        ).use { c ->
            while (c.moveToNext()) c.getString(0)?.let(out::add)
        }
        out
    }

    suspend fun addToPlaylist(id: Long, paths: List<String>): Int = withContext(Dispatchers.IO) {
        if (paths.isEmpty()) return@withContext 0
        val db = writableDatabase
        var nextPos = 0
        db.rawQuery(
            "SELECT IFNULL(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlist_id=?",
            arrayOf(id.toString()),
        ).use { c -> if (c.moveToFirst()) nextPos = c.getInt(0) }

        var inserted = 0
        db.beginTransaction()
        try {
            val stmt = db.compileStatement(
                "INSERT OR IGNORE INTO playlist_tracks(playlist_id,path,position) VALUES(?,?,?)",
            )
            for (p in paths) {
                stmt.clearBindings()
                stmt.bindLong(1, id)
                stmt.bindString(2, p)
                stmt.bindLong(3, nextPos.toLong())
                if (stmt.executeInsert() != -1L) {
                    inserted++
                    nextPos++
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        inserted
    }

    suspend fun removeFromPlaylist(id: Long, path: String): Unit = withContext(Dispatchers.IO) {
        writableDatabase.delete(
            "playlist_tracks",
            "playlist_id=? AND path=?",
            arrayOf(id.toString(), path),
        )
    }

    suspend fun reorderPlaylist(id: Long, orderedPaths: List<String>): Unit =
        withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.beginTransaction()
            try {
                val stmt = db.compileStatement(
                    "UPDATE playlist_tracks SET position=? WHERE playlist_id=? AND path=?",
                )
                orderedPaths.forEachIndexed { index, p ->
                    stmt.clearBindings()
                    stmt.bindLong(1, index.toLong())
                    stmt.bindLong(2, id)
                    stmt.bindString(3, p)
                    stmt.executeUpdateDelete()
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

    // ------------------------------------------------------ favorites / recent

    suspend fun favoritePaths(): Set<String> = withContext(Dispatchers.IO) {
        val out = HashSet<String>()
        readableDatabase.rawQuery("SELECT path FROM favorites", null).use { c ->
            while (c.moveToNext()) c.getString(0)?.let(out::add)
        }
        out
    }

    suspend fun toggleFavorite(path: String): Boolean = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val exists = db.rawQuery("SELECT 1 FROM favorites WHERE path=?", arrayOf(path)).use {
            it.moveToFirst()
        }
        if (exists) {
            db.delete("favorites", "path=?", arrayOf(path))
            false
        } else {
            val cv = ContentValues().apply {
                put("path", path)
                put("added_at", System.currentTimeMillis())
            }
            db.insertWithOnConflict("favorites", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            true
        }
    }

    suspend fun pushRecent(path: String): Unit = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("path", path)
            put("played_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("recent", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        // Giữ tối đa 100 bài gần nhất để bảng không phình ra.
        db.execSQL(
            "DELETE FROM recent WHERE path NOT IN " +
                "(SELECT path FROM recent ORDER BY played_at DESC LIMIT 100)",
        )
    }

    suspend fun recentPaths(): List<String> = withContext(Dispatchers.IO) {
        val out = ArrayList<String>()
        readableDatabase.rawQuery(
            "SELECT path FROM recent ORDER BY played_at DESC LIMIT 100",
            null,
        ).use { c ->
            while (c.moveToNext()) c.getString(0)?.let(out::add)
        }
        out
    }

    suspend fun pruneOrphans(): Unit = withContext(Dispatchers.IO) {
        // Dọn playlist/favorites trỏ tới file không còn trong thư viện.
        //
        // Dùng truy vấn con thay vì "NOT IN (?,?,…)" vì SQLite chỉ cho tối đa
        // 999 tham số mỗi câu lệnh — thư viện 5.000 bài sẽ làm câu lệnh vỡ.
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM playlist_tracks WHERE path NOT IN (SELECT path FROM tracks)")
            db.execSQL("DELETE FROM favorites WHERE path NOT IN (SELECT path FROM tracks)")
            db.execSQL("DELETE FROM recent WHERE path NOT IN (SELECT path FROM tracks)")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun bindNullable(stmt: android.database.sqlite.SQLiteStatement, index: Int, value: String?) {
        if (value == null) stmt.bindNull(index) else stmt.bindString(index, value)
    }

    private fun Cursor.getStringOrNull(index: Int): String? =
        if (isNull(index)) null else getString(index)

    companion object {
        private const val NAME = "norr.db"
        private const val VERSION = 1

        @Volatile
        private var instance: NorrDb? = null

        fun get(context: Context): NorrDb = instance ?: synchronized(this) {
            instance ?: NorrDb(context.applicationContext).also { instance = it }
        }
    }
}
