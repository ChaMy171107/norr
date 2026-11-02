package com.norr.music.data

data class PlaybackUiState(
    val current: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatOne: Boolean = false,
    val shuffle: Boolean = false,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    /** Tốc độ phát, 1.0 là bình thường. */
    val speed: Float = 1f,
    /** Số phút còn lại của hẹn giờ tắt; null là không hẹn. */
    val sleepMinutesLeft: Int? = null,
    val eqEnabled: Boolean = false,
    val eqPreset: Int = 0,
    val eqPresets: List<String> = emptyList(),
)

data class LibraryUiState(
    val tracks: List<Track> = emptyList(),
    /** Danh sách đã lọc + sắp xếp, tính sẵn trong ViewModel. */
    val visible: List<Track> = emptyList(),
    val loading: Boolean = false,
    val scanning: Boolean = false,
    val scanned: Boolean = false,
    val query: String = "",
    val sort: SortMode = SortMode.NAME,
    val error: String? = null,
    val progress: Int = 0,
    val progressTotal: Int = 0,
)

data class PlaylistsUiState(
    val playlists: List<Playlist> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val recent: List<String> = emptyList(),
)

data class ImportUiState(
    val source: com.norr.music.data.remote.MusicSource =
        com.norr.music.data.remote.MusicSource.ARCHIVE,
    val query: String = "",
    val results: List<com.norr.music.data.remote.RemoteTrack> = emptyList(),
    val selected: Set<String> = emptySet(),
    val searching: Boolean = false,
    val downloading: Boolean = false,
    val error: String? = null,
    val total: Int = 0,
    val completed: Int = 0,
    val failed: Int = 0,
    val currentTitle: String = "",
    val lastOutcome: String? = null,
)

/** Tiến trình dọn thư viện: sửa tên, nghệ sĩ, thêm ảnh bìa. */
data class FixUiState(
    val running: Boolean = false,
    val done: Int = 0,
    val total: Int = 0,
    val fixed: Int = 0,
    val notFound: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
    val current: String = "",
    val summary: String? = null,
    val unmatched: List<String> = emptyList(),
    val failedNames: List<String> = emptyList(),
)

/** Kết quả quét một playlist YouTube (chỉ đọc tên bài, không tải âm thanh). */
data class ScanUiState(
    val link: String = "",
    val scanning: Boolean = false,
    val error: String? = null,
    val tracks: List<com.norr.music.data.remote.YouTubeTrack> = emptyList(),
    /** videoId của những bài đã có sẵn trong thư viện máy. */
    val owned: Set<String> = emptySet(),
    /** true khi chỉ đọc được 15 bài do dùng RSS không có API key. */
    val limited: Boolean = false,
) {
    val ownedCount: Int get() = owned.size
    val missingCount: Int get() = (tracks.size - owned.size).coerceAtLeast(0)
}

