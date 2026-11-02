# Giữ nguyên các lớp cần thiết khi bật minify (R8).

# mp3agic dùng reflection để đọc/ghi tag ID3.
-keep class com.mpatric.mp3agic.** { *; }

# jaudiotagger dùng reflection mạnh để đọc tag đa định dạng.
-keep class org.jaudiotagger.** { *; }

# Media3 ExoPlayer dùng reflection cho định dạng codec.
-keep class com.google.android.exoplayer2.** { *; }

# Compose UI dùng reflection cho các @Composable.
-keep class androidx.compose.** { *; }

# kotlinx.coroutines
-keepclassmembernames class kotlinx.coroutines.** { *; }

# BuildConfig
-keep class com.norr.music.BuildConfig { *; }
