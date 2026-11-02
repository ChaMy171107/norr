package com.norr.music.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Xuất và nhập danh sách phát dưới dạng JSON.
 *
 * Mục đích: đổi máy hoặc cài lại app không mất công sắp xếp.
 * Chỉ lưu **đường dẫn** file nhạc, không nhúng nội dung — file backup nhỏ gọn.
 * Khi nhập, bài nào không còn trên máy sẽ được bỏ qua và đếm lại.
 */
object PlaylistBackup {

    data class ImportResult(
        val playlists: Int,
        val added: Int,
        val skipped: Int,
    )

    suspend fun export(db: NorrDb): String {
        val root = JSONObject()
        root.put("app", "norr-music")
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())

        val lists = JSONArray()
        for (playlist in db.playlists()) {
            val paths = db.playlistPaths(playlist.id)
            if (paths.isEmpty()) continue
            val item = JSONObject()
            item.put("name", playlist.name)
            item.put("tracks", JSONArray(paths))
            lists.put(item)
        }
        root.put("playlists", lists)
        root.put("favorites", JSONArray(db.favoritePaths().toList()))
        return root.toString(2)
    }

    /**
     * Nhập từ JSON. Nếu đã có danh sách trùng tên thì thêm bài vào danh sách đó
     * thay vì tạo bản trùng.
     */
    suspend fun import(db: NorrDb, json: String, knownPaths: Set<String>): ImportResult {
        val root = runCatching { JSONObject(json) }.getOrNull()
            ?: error("File không đúng định dạng JSON")
        if (root.optString("app") != "norr-music") {
            error("File này không phải bản sao lưu của Norr Music")
        }

        val existing = db.playlists().associateBy { it.name.lowercase() }
        var playlistCount = 0
        var added = 0
        var skipped = 0

        val lists = root.optJSONArray("playlists") ?: JSONArray()
        for (i in 0 until lists.length()) {
            val item = lists.optJSONObject(i) ?: continue
            val name = item.optString("name").trim().ifEmpty { "Danh sách" }
            val tracksArray = item.optJSONArray("tracks") ?: JSONArray()

            val paths = ArrayList<String>(tracksArray.length())
            for (j in 0 until tracksArray.length()) {
                val path = tracksArray.optString(j)
                if (path.isBlank()) continue
                if (path in knownPaths) paths.add(path) else skipped++
            }
            if (paths.isEmpty()) continue

            val id = existing[name.lowercase()]?.id ?: db.createPlaylist(name)
            added += db.addToPlaylist(id, paths)
            playlistCount++
        }

        val favorites = root.optJSONArray("favorites") ?: JSONArray()
        val currentFavorites = db.favoritePaths().toMutableSet()
        for (i in 0 until favorites.length()) {
            val path = favorites.optString(i)
            if (path.isBlank()) continue
            if (path !in knownPaths) {
                skipped++
                continue
            }
            if (path !in currentFavorites) {
                db.toggleFavorite(path)
                currentFavorites.add(path)
            }
        }

        return ImportResult(playlistCount, added, skipped)
    }
}
