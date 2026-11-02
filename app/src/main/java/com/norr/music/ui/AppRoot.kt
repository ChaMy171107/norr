package com.norr.music.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.norr.music.data.StoragePermissions
import com.norr.music.data.Track
import com.norr.music.ui.components.AddToPlaylistSheet
import com.norr.music.ui.components.CreatePlaylistDialog
import com.norr.music.ui.components.QueueSheet
import com.norr.music.ui.components.RenameTrackDialog
import com.norr.music.ui.components.WallpaperBackground
import com.norr.music.ui.theme.Ink
import com.norr.music.ui.theme.Surface1
import com.norr.music.ui.theme.Surface2
import com.norr.music.ui.theme.TextMuted
import com.norr.music.ui.theme.TextPrimary
import com.norr.music.ui.theme.Violet

private enum class Tab(val label: String, val icon: ImageVector) {
    LIBRARY("Thư viện", Icons.Rounded.LibraryMusic),
    PLAYLISTS("Danh sách", Icons.AutoMirrored.Rounded.QueueMusic),
    IMPORT("Tải nhạc", Icons.Rounded.CloudDownload),
    SETTINGS("Cài đặt", Icons.Rounded.Settings),
}

@Composable
fun AppRoot(vm: AppViewModel) {
    val context = LocalContext.current
    val library by vm.library.collectAsStateWithLifecycle()
    val playback by vm.playback.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val wallpaper by vm.wallpaper.collectAsStateWithLifecycle()
    val wallpaperStyle by vm.wallpaperStyle.collectAsStateWithLifecycle()
    val appIcon by vm.appIcon.collectAsStateWithLifecycle()
    val customIcon by vm.customIcon.collectAsStateWithLifecycle()
    val importState by vm.importState.collectAsStateWithLifecycle()
    val scanState by vm.scanState.collectAsStateWithLifecycle()
    val fixState by vm.fixState.collectAsStateWithLifecycle()
    val downloadFolder by vm.downloadFolder.collectAsStateWithLifecycle()
    val detail by vm.playlistDetail.collectAsStateWithLifecycle()
    val queue by vm.queue.collectAsStateWithLifecycle()

    var hasStorageAccess by remember { mutableStateOf(StoragePermissions.hasAccess(context)) }
    var playerOpen by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(Tab.LIBRARY) }
    var renameTarget by remember { mutableStateOf<Track?>(null) }
    var coverTarget by remember { mutableStateOf<Track?>(null) }
    var addTargets by remember { mutableStateOf<List<Track>>(emptyList()) }
    var createPlaylistOpen by remember { mutableStateOf(false) }
    var renamePlaylistTarget by remember { mutableStateOf<com.norr.music.data.Playlist?>(null) }
    var queueOpen by remember { mutableStateOf(false) }
    var jamendoKeyOpen by remember { mutableStateOf(false) }
    var youtubeKeyOpen by remember { mutableStateOf(false) }
    var scanOpen by remember { mutableStateOf(false) }
    var playbackOptionsOpen by remember { mutableStateOf(false) }
    var editMetadataTarget by remember { mutableStateOf<Track?>(null) }
    var selection by remember { mutableStateOf<Set<String>>(emptySet()) }
    val snackbar = remember { SnackbarHostState() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasStorageAccess = StoragePermissions.hasAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasStorageAccess) {
        if (hasStorageAccess) vm.ensureLibraryLoaded()
    }

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    // Cho phép kích hoạt dọn thư viện từ ngoài (adb) mà không cần chạm màn hình.
    // Dùng khi kiểm thử trên máy thật; vô hại trong sử dụng bình thường.
    LaunchedEffect(Unit) {
        val activity = context as? android.app.Activity
        if (activity?.intent?.getBooleanExtra("run_fix", false) == true) {
            vm.runLibraryFix()
        }
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { hasStorageAccess = StoragePermissions.hasAccess(context) }

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasStorageAccess = granted || StoragePermissions.hasAccess(context) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !StoragePermissions.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val requestStorageAccess: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { settingsLauncher.launch(StoragePermissions.allFilesIntent(context)) }
        } else {
            legacyPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val target = coverTarget
        if (uri != null && target != null) vm.changeCover(target, uri)
        coverTarget = null
    }

    val launchCoverPicker: (Track) -> Unit = { track ->
        coverTarget = track
        runCatching {
            coverPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }

    val wallpaperPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) vm.setWallpaper(uri)
    }

    val launchWallpaperPicker: () -> Unit = {
        runCatching {
            wallpaperPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }

    val customIconPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) vm.setCustomIcon(uri) }

    val launchCustomIconPicker: () -> Unit = {
        runCatching {
            customIconPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> if (uri != null) vm.setDownloadFolder(uri) }

    val launchFolderPicker: () -> Unit = {
        runCatching { folderPicker.launch(null) }
    }

    val openUrl: (String) -> Unit = { url ->
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) vm.exportBackup(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) vm.importBackup(uri) }

    val launchExport: () -> Unit = {
        runCatching { exportLauncher.launch("norr-playlists.json") }
    }

    val launchImport: () -> Unit = {
        runCatching { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
    }

    BackHandler(enabled = playerOpen) { playerOpen = false }
    BackHandler(enabled = !playerOpen && detail != null) { vm.closePlaylist() }

    Box(modifier = Modifier.fillMaxSize().background(Ink)) {
        WallpaperBackground(style = wallpaperStyle, bitmap = wallpaper)

        val selectedTracks = remember(selection, library.tracks) {
            library.tracks.filter { it.id in selection }
        }

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbar,
                    modifier = Modifier.padding(16.dp),
                )
            },
            bottomBar = {
                if (selection.isNotEmpty()) {
                    SelectionActionBar(
                        count = selection.size,
                        onAddToPlaylist = { addTargets = selectedTracks },
                        onFavorite = {
                            selectedTracks.forEach(vm::toggleFavorite)
                            selection = emptySet()
                        },
                        onPlayNow = {
                            if (selectedTracks.isNotEmpty()) {
                                vm.playFromPlaylist(selectedTracks, 0)
                                playerOpen = true
                            }
                            selection = emptySet()
                        },
                    )
                } else {
                    Column {
                        AnimatedVisibility(visible = playback.current != null) {
                            com.norr.music.ui.components.MiniPlayer(
                                playback = playback,
                                onOpen = { playerOpen = true },
                                onTogglePlay = vm::togglePlayPause,
                                onNext = vm::next,
                            )
                        }
                        NorrBottomBar(
                            current = tab,
                            onSelect = { tab = it },
                        )
                    }
                }
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 24 })
                            .togetherWith(fadeOut(tween(160)))
                    },
                    label = "tab",
                ) { target ->
                    when (target) {
                        Tab.LIBRARY -> LibraryScreen(
                            library = library,
                            playback = playback,
                            hasStorageAccess = hasStorageAccess,
                            selection = selection,
                            onRequestAccess = requestStorageAccess,
                            onScan = { vm.refreshLibrary() },
                            onQueryChange = vm::setQuery,
                            onSortChange = vm::setSort,
                            onPlay = { track, queue ->
                                vm.playTrack(track, queue)
                                playerOpen = true
                            },
                            onRename = { renameTarget = it },
                            onCover = launchCoverPicker,
                            onRemoveCover = vm::removeCover,
                            onToggleFavorite = vm::toggleFavorite,
                            onAddToPlaylist = { addTargets = listOf(it) },
                            onPlayNext = vm::playNext,
                            onEditMetadata = { editMetadataTarget = it },
                            onLongPress = { selection = setOf(it.id) },
                            onToggleSelection = { track ->
                                selection = if (track.id in selection) {
                                    selection - track.id
                                } else {
                                    selection + track.id
                                }
                            },
                            onSelectAll = { selection = library.visible.mapTo(HashSet()) { it.id } },
                            onClearSelection = { selection = emptySet() },
                            favorites = playlists.favorites,
                        )

                        Tab.PLAYLISTS -> PlaylistsScreen(
                            state = playlists,
                            tracksForPaths = vm::tracksForPaths,
                            onCreate = { createPlaylistOpen = true },
                            onOpen = { vm.openPlaylist(it) },
                            onOpenSmart = vm::openSmartList,
                            onPlayAll = { tracks ->
                                if (tracks.isNotEmpty()) {
                                    vm.playFromPlaylist(tracks, 0)
                                    playerOpen = true
                                }
                            },
                        )

                        Tab.IMPORT -> ImportScreen(
                            state = importState,
                            jamendoConfigured = vm.jamendoClientId.isNotBlank(),
                            onSourceChange = vm::setImportSource,
                            onQueryChange = vm::setImportQuery,
                            onSearch = vm::runImportSearch,
                            onToggle = vm::toggleImportSelection,
                            onSelectAll = vm::selectAllImport,
                            onClearSelection = vm::clearImportSelection,
                            onDownload = vm::downloadSelected,
                            onOpenScan = { scanOpen = true },
                        )

                        Tab.SETTINGS -> SettingsScreen(
                            trackCount = library.tracks.size,
                            wallpaperStyle = wallpaperStyle,
                            hasCustomImage = wallpaper != null,
                            appIcon = appIcon,
                            customIcon = customIcon,
                            pinSupported = vm.pinSupported,
                            jamendoConfigured = vm.jamendoClientId.isNotBlank(),
                            youtubeConfigured = vm.youtubeApiKey.isNotBlank(),
                            fixState = fixState,
                            downloadFolderPath = downloadFolder.path,
                            isCustomDownloadFolder = downloadFolder.isCustom,
                            onPickWallpaperStyle = vm::setWallpaperStyle,
                            onPickWallpaper = launchWallpaperPicker,
                            onPickAppIcon = vm::setAppIcon,
                            onPickCustomIcon = launchCustomIconPicker,
                            onPinCustomIcon = vm::pinCustomIcon,
                            onClearCustomIcon = vm::clearCustomIcon,
                            onEditJamendoKey = { jamendoKeyOpen = true },
                            onEditYoutubeKey = { youtubeKeyOpen = true },
                            onRunFix = vm::runLibraryFix,
                            onPickDownloadFolder = launchFolderPicker,
                            onResetDownloadFolder = vm::resetDownloadFolder,
                            onRescan = { vm.refreshLibrary() },
                            scanning = library.scanning,
                            onExport = launchExport,
                            onImport = launchImport,
                        )
                    }
                }

                detail?.let { current ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(tween(280)) { it } + fadeIn(tween(180)),
                        exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(160)),
                    ) {
                        PlaylistDetailScreen(
                            detail = current,
                            playback = playback,
                            onBack = { vm.closePlaylist() },
                            onPlay = { index ->
                                vm.playFromPlaylist(current.tracks, index)
                                playerOpen = true
                            },
                            onRemove = { vm.removeFromPlaylist(current.playlist.id, it) },
                            onRename = { renamePlaylistTarget = current.playlist },
                            onDelete = {
                                vm.deletePlaylist(current.playlist.id)
                            },
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = playerOpen && playback.current != null,
            enter = slideInVertically(tween(320)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(260)) { it } + fadeOut(tween(160)),
        ) {
            PlayerScreen(
                playback = playback,
                onClose = { playerOpen = false },
                onTogglePlay = vm::togglePlayPause,
                onPrevious = vm::previous,
                onNext = vm::next,
                onToggleLoop = vm::toggleLoop,
                onToggleShuffle = vm::toggleShuffle,
                onSeek = vm::seekTo,
                onRename = { renameTarget = it },
                onPickCover = launchCoverPicker,
                onRemoveCover = vm::removeCover,
                onToggleFavorite = vm::toggleFavorite,
                isFavorite = playback.current?.let(vm::isFavorite) ?: false,
                onStartTracking = vm::startPositionUpdates,
                onStopTracking = vm::stopPositionUpdates,
                onOpenQueue = { queueOpen = true },
                onOpenOptions = { playbackOptionsOpen = true },
            )
        }

        AnimatedVisibility(
            visible = scanOpen,
            enter = slideInVertically(tween(260)) { it },
            exit = slideOutVertically(tween(220)) { it },
        ) {
            PlaylistScanScreen(
                state = scanState,
                hasApiKey = vm.youtubeApiKey.isNotBlank(),
                onLinkChange = vm::setScanLink,
                onScan = vm::runScan,
                onOpenUrl = openUrl,
                onOpenSettings = {
                    scanOpen = false
                    tab = Tab.SETTINGS
                    youtubeKeyOpen = true
                },
                onClose = { scanOpen = false },
            )
        }

        if (playbackOptionsOpen) {
            PlaybackOptionsSheet(
                playback = playback,
                onDismiss = { playbackOptionsOpen = false },
                onSpeed = vm::setPlaybackSpeed,
                onSleep = vm::setSleepTimer,
                onEqEnabled = vm::setEqualizerEnabled,
                onEqPreset = vm::setEqualizerPreset,
            )
        }

        if (queueOpen) {
            QueueSheet(
                entries = queue,
                onDismiss = { queueOpen = false },
                onJump = { index ->
                    vm.jumpToQueueIndex(index)
                },
                onMove = { from, to -> vm.moveInQueue(from, to) },
                onRemove = { index -> vm.removeFromQueue(index) },
            )
        }

        renameTarget?.let { target ->
            RenameTrackDialog(
                track = target,
                onDismiss = { renameTarget = null },
                onConfirm = { newName ->
                    vm.renameTrack(target, newName)
                    renameTarget = null
                },
            )
        }

        if (createPlaylistOpen) {
            CreatePlaylistDialog(
                onDismiss = { createPlaylistOpen = false },
                onConfirm = { name ->
                    vm.createPlaylist(name)
                    createPlaylistOpen = false
                },
            )
        }

        renamePlaylistTarget?.let { target ->
            com.norr.music.ui.components.RenamePlaylistDialog(
                current = target.name,
                onDismiss = { renamePlaylistTarget = null },
                onConfirm = { name ->
                    vm.renamePlaylist(target.id, name)
                    renamePlaylistTarget = null
                },
            )
        }

        editMetadataTarget?.let { target ->
            com.norr.music.ui.components.EditMetadataDialog(
                track = target,
                onDismiss = { editMetadataTarget = null },
                onConfirm = { title, artist, album ->
                    vm.updateMetadata(target, title, artist, album)
                    editMetadataTarget = null
                },
            )
        }

        if (jamendoKeyOpen) {
            com.norr.music.ui.components.TextInputDialog(
                title = "Jamendo client_id",
                initial = vm.jamendoClientId,
                placeholder = "Dán client_id vào đây",
                note = "Đăng ký miễn phí tại devportal.jamendo.com → tạo ứng dụng → " +
                    "lấy Client ID. Không có key thì nguồn Jamendo không tìm được nhạc.",
                onDismiss = { jamendoKeyOpen = false },
                onConfirm = { value ->
                    vm.setJamendoClientId(value)
                    jamendoKeyOpen = false
                },
            )
        }

        if (youtubeKeyOpen) {
            com.norr.music.ui.components.TextInputDialog(
                title = "YouTube API key",
                initial = vm.youtubeApiKey,
                placeholder = "Dán API key vào đây",
                note = "Google Cloud Console → bật YouTube Data API v3 → tạo API key. " +
                    "Miễn phí, 10.000 lượt/ngày. Key này chỉ để đọc tên bài trong playlist.",
                onDismiss = { youtubeKeyOpen = false },
                onConfirm = { value ->
                    vm.setYouTubeApiKey(value)
                    youtubeKeyOpen = false
                },
            )
        }

        if (addTargets.isNotEmpty()) {
            AddToPlaylistSheet(
                tracks = addTargets,
                playlists = playlists.playlists,
                onDismiss = { addTargets = emptyList() },
                onPick = { playlistId ->
                    vm.addToPlaylist(playlistId, addTargets)
                    addTargets = emptyList()
                },
                onCreateNew = {
                    addTargets = emptyList()
                    createPlaylistOpen = true
                },
            )
        }
    }
}

@Composable
private fun SelectionActionBar(
    count: Int,
    onAddToPlaylist: () -> Unit,
    onFavorite: () -> Unit,
    onPlayNow: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SelectionAction(
            label = "Thêm vào DS",
            icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
            primary = true,
            modifier = Modifier.weight(1.4f),
            onClick = onAddToPlaylist,
        )
        SelectionAction(
            label = "Yêu thích",
            icon = Icons.Rounded.FavoriteBorder,
            primary = false,
            modifier = Modifier.weight(1.2f),
            onClick = onFavorite,
        )
        SelectionAction(
            label = "Phát",
            icon = Icons.Rounded.PlayArrow,
            primary = false,
            modifier = Modifier.weight(1f),
            onClick = onPlayNow,
        )
    }
}

@Composable
private fun SelectionAction(
    label: String,
    icon: ImageVector,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(if (primary) Violet else Surface2)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (primary) Ink else TextPrimary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (primary) Ink else TextPrimary,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun NorrBottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    NavigationBar(
        containerColor = Surface1.copy(alpha = 0.94f),
        tonalElevation = 0.dp,
        modifier = Modifier.navigationBarsPadding(),
    ) {
        Tab.entries.forEach { item ->
            val selected = item == current
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(item) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selected) Violet else TextMuted,
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Violet else TextMuted,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Violet.copy(alpha = 0.16f),
                ),
            )
        }
    }
}
