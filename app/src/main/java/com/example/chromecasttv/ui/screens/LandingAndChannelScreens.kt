@file:OptIn(
    androidx.tv.material3.ExperimentalTvMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class
)

package com.example.chromecasttv

import android.app.PictureInPictureParams
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.view.inputmethod.InputMethodManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.util.Rational
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.URL
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.*

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.TextureView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.DefaultRenderersFactory
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

// ============================================================================
// HAFİF VE ANINDA AÇILAN KIRMIZI BOĞA KARŞILAMA EKRANI (LANDING PAGE)
// ============================================================================
@Composable
fun RedoXLandingScreen(
    state: TvUiState,
    onStartLiveTv: () -> Unit,
    onResumeLastChannel: (TvChannel) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenMultiView: () -> Unit,
    onOpenM3uManager: () -> Unit,
    onToggleAutoStart: () -> Unit = {}
) {
    val primaryFocusRequester = remember { FocusRequester() }
    val lastChannel = state.recentChannels.firstOrNull() ?: state.builtInChannels.firstOrNull()

    LaunchedEffect(Unit) {
        try { primaryFocusRequester.requestFocus() } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF2D0612), Color(0xFF0A0D16), Color(0xFF05070C)),
                    radius = 1100f
                )
            )
            .padding(horizontal = 42.dp, vertical = 32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(TvThemeColors.AccentEmerald)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${state.builtInChannels.size} Canlı Kanal Hazır • Hızlı Başlangıç Modu",
                        color = TvThemeColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TvCompactChipButton(
                        text = if (state.autoStartLastChannel) "⚡ Otomatik Başlat: AÇIK" else "⚡ Otomatik Başlat: KAPALI",
                        icon = Icons.Rounded.Bolt,
                        onClick = onToggleAutoStart
                    )
                    Text(
                        text = "TV • Tablet • Mobil • RedoX TV v1.6.1",
                        color = TvThemeColors.BullRedBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RedBullEmblemBox(boxSize = 112.dp)

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "REDOX TV",
                    color = TvThemeColors.TextPrimary,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "ULTRA 4K & 1080P KESİNTİSİZ CANLI TELEVİZYON",
                    color = TvThemeColors.BullRedBright,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LandingActionCard(
                        title = "Canlı TV'ye Başla",
                        subtitle = "${state.builtInChannels.size} Ulusal, Spor & Haber",
                        icon = Icons.Rounded.LiveTv,
                        accentColor = TvThemeColors.BullRedBright,
                        isPrimary = true,
                        onClick = onStartLiveTv,
                        modifier = Modifier.focusRequester(primaryFocusRequester)
                    )

                    if (lastChannel != null) {
                        LandingActionCard(
                            title = "Hemen İzle: ${lastChannel.name}",
                            subtitle = "Son İzlenen • CH ${lastChannel.number}",
                            icon = Icons.Rounded.PlayCircleFilled,
                            accentColor = TvThemeColors.AccentCyan,
                            isPrimary = false,
                            onClick = { onResumeLastChannel(lastChannel) }
                        )
                    }

                    LandingActionCard(
                        title = "Favori Kanallarım",
                        subtitle = "${state.favoriteOrderList.size} Sıralı Favori Kanal",
                        icon = Icons.Rounded.Star,
                        accentColor = TvThemeColors.FavoriteGold,
                        isPrimary = false,
                        onClick = onOpenFavorites
                    )

                    LandingActionCard(
                        title = "2x2 Bölünmüş Ekran",
                        subtitle = "4 Kanalı Aynı Anda İzle",
                        icon = Icons.Rounded.GridView,
                        accentColor = TvThemeColors.AccentEmerald,
                        isPrimary = false,
                        onClick = onOpenMultiView
                    )

                    LandingActionCard(
                        title = "Özel M3U Yükle",
                        subtitle = "${state.customM3uChannels.size} Özel Kanal Ekli",
                        icon = Icons.Rounded.UploadFile,
                        accentColor = TvThemeColors.AccentIndigo,
                        isPrimary = false,
                        onClick = onOpenM3uManager
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Hızlı Kanal Başlat:",
                    color = TvThemeColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val quickChannels = state.recentChannels.ifEmpty { state.builtInChannels.take(10) }
                    items(quickChannels, key = { "landing_ch_${it.id}" }) { ch ->
                        TvCompactChipButton(
                            text = "${ch.number}. ${ch.name}",
                            icon = Icons.Rounded.PlayArrow,
                            onClick = { onResumeLastChannel(ch) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LandingActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(172.dp)
            .height(126.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                when {
                    isFocused -> accentColor.copy(alpha = 0.32f)
                    isPrimary -> TvThemeColors.BullRedDark.copy(alpha = 0.50f)
                    else -> TvThemeColors.SurfaceCard
                }
            )
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) Color.White else accentColor.copy(alpha = 0.45f),
                shape = RoundedCornerShape(18.dp)
            )
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = TvThemeColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TvThemeColors.TextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RedoXTvRootScreen(
    viewModel: TvViewModel,
    onEnterSystemPip: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    var showBuiltInFilePicker by remember { mutableStateOf(false) }
    var lastWatchedChannelId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.activePlayingChannel?.id) {
        state.activePlayingChannel?.id?.let { lastWatchedChannelId = it }
    }

    if (state.activePlayingChannel != null) {
        RedoXPlayerScreen(
            playingChannel = state.activePlayingChannel!!,
            previousChannel = state.previousPlayingChannel,
            allChannels = (state.builtInChannels + state.customM3uChannels).ifEmpty { state.builtInChannels },
            favoriteIds = state.favoriteIds,
            lockedIds = state.lockedChannelIds,
            brokenIds = state.brokenChannelIds,
            preferredSources = state.preferredSources,
            preferredScales = state.preferredScales,
            epgMap = state.epgMap,
            sleepTimerRemaining = state.sleepTimerRemainingMinutes,
            onZapChannel = { delta -> viewModel.zapChannel(delta) },
            onRecallPreviousChannel = { viewModel.recallPreviousChannel() },
            onSelectChannelDirect = { ch -> viewModel.requestPlayChannel(ch) },
            onSelectChannelByNumber = { num -> viewModel.playChannelByNumber(num) },
            onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
            onToggleLock = { id -> viewModel.toggleChannelLock(id) },
            onHideChannel = { id -> viewModel.hideChannel(id) },
            onSavePreferredSource = { id, idx -> viewModel.saveChannelPreferredSource(id, idx) },
            onSavePreferredScale = { id, scale -> viewModel.saveChannelPreferredScale(id, scale) },
            onMarkBroken = { id -> viewModel.markChannelAsBroken(id) },
            onClearBroken = { id -> viewModel.clearChannelBrokenStatus(id) },
            onMinimizeToPip = { viewModel.minimizeToPip() },
            onEnterSystemPip = onEnterSystemPip,
            onOpenMultiView = { viewModel.openMultiViewFromPlayer() },
            onCycleSleepTimer = { viewModel.cycleSleepTimer() },
            onExitPlayer = { viewModel.closePlayer() }
        )
        return
    }

    if (state.showLandingScreen) {
        RedoXLandingScreen(
            state = state,
            onStartLiveTv = { viewModel.dismissLandingAndOpenTab(MainNavTab.ALL_CHANNELS) },
            onResumeLastChannel = { ch -> viewModel.requestPlayChannel(ch) },
            onOpenFavorites = { viewModel.dismissLandingAndOpenTab(MainNavTab.FAVORITES) },
            onOpenMultiView = { viewModel.dismissLandingAndOpenTab(MainNavTab.MULTI_VIEW) },
            onOpenM3uManager = { viewModel.dismissLandingAndOpenTab(MainNavTab.M3U_MANAGER) },
            onToggleAutoStart = { viewModel.toggleAutoStartLastChannel() }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvThemeColors.BackgroundDeep)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            TvSideNavigationRail(
                selectedTab = state.selectedTab,
                builtInCount = state.builtInChannels.size,
                customM3uCount = state.customM3uChannels.size,
                favoriteCount = state.favoriteOrderList.size,
                sleepTimerMinutes = state.sleepTimerRemainingMinutes,
                onSelectTab = { viewModel.selectTab(it) },
                onOpenLanding = { viewModel.openLandingScreen() },
                onCycleSleepTimer = { viewModel.cycleSleepTimer() }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                TvTopStatusHeader(
                    state = state,
                    onQuickHealthCheckClick = { viewModel.runChannelHealthCheck() },
                    onQuickMultiViewClick = { viewModel.selectTab(MainNavTab.MULTI_VIEW) },
                    onQuickSearchClick = { viewModel.selectTab(MainNavTab.SEARCH) },
                    onQuickAddM3uClick = { viewModel.selectTab(MainNavTab.M3U_MANAGER) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // HAFİF VE AKICI 140ms CROSSFADE SEKME GEÇİŞİ
                Crossfade(
                    targetState = state.selectedTab,
                    animationSpec = tween(durationMillis = 140),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    label = "fast_tab_crossfade"
                ) { targetTab ->
                    when (targetTab) {
                        MainNavTab.ALL_CHANNELS -> {
                            ChannelShowcaseScreen(
                                title = "RedoX Yerleşik Canlı TV",
                                channels = state.builtInChannels,
                                categories = state.categories,
                                selectedCategory = state.selectedCategory,
                                favoriteIds = state.favoriteIds,
                                lockedIds = state.lockedChannelIds,
                                brokenIds = state.brokenChannelIds,
                                healthyIds = state.healthyChannelIds,
                                hiddenCount = state.hiddenChannelIds.size,
                                recentChannels = state.recentChannels,
                                lastWatchedChannelId = lastWatchedChannelId,
                                onSelectCategory = { viewModel.selectCategory(it) },
                                onPlayChannel = { viewModel.requestPlayChannel(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onToggleLock = { viewModel.toggleChannelLock(it) },
                                onHideChannel = { viewModel.hideChannel(it) },
                                onRestoreHidden = { viewModel.restoreAllHiddenChannels() }
                            )
                        }
                        MainNavTab.FAVORITES -> {
                            FavoritesReorderScreen(
                                favoriteChannels = state.orderedFavoriteChannels,
                                lockedIds = state.lockedChannelIds,
                                brokenIds = state.brokenChannelIds,
                                onPlayChannel = { viewModel.requestPlayChannel(it) },
                                onMoveChannel = { id, delta -> viewModel.moveFavoriteChannel(id, delta) },
                                onSortFavorites = { mode -> viewModel.sortFavoritesBy(mode) },
                                onRemoveFavorite = { id -> viewModel.toggleFavorite(id) }
                            )
                        }
                        MainNavTab.CUSTOM_M3U -> {
                            ChannelShowcaseScreen(
                                title = "Eklenen Özel M3U Kanalları",
                                channels = state.customM3uChannels,
                                categories = state.customCategories,
                                selectedCategory = state.selectedCategory,
                                favoriteIds = state.favoriteIds,
                                lockedIds = state.lockedChannelIds,
                                brokenIds = state.brokenChannelIds,
                                healthyIds = state.healthyChannelIds,
                                hiddenCount = state.hiddenChannelIds.size,
                                recentChannels = emptyList(),
                                lastWatchedChannelId = lastWatchedChannelId,
                                onSelectCategory = { viewModel.selectCategory(it) },
                                onPlayChannel = { viewModel.requestPlayChannel(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onToggleLock = { viewModel.toggleChannelLock(it) },
                                onHideChannel = { viewModel.hideChannel(it) },
                                onRestoreHidden = { viewModel.restoreAllHiddenChannels() },
                                emptyMessage = "Henüz özel bir M3U listesi eklemediniz. 'M3U & Dosya Yükle' sekmesinden dosya veya link ekleyebilirsiniz."
                            )
                        }
                        MainNavTab.MULTI_VIEW -> {
                            MultiViewSplitScreen(
                                state = state,
                                onToggleGridMode = { viewModel.toggleMultiViewMode() },
                                onChangeSlotChannel = { slot, delta ->
                                    viewModel.changeMultiViewSlotChannel(slot, delta)
                                },
                                onExpandSlotToFull = { ch -> viewModel.requestPlayChannel(ch) }
                            )
                        }
                        MainNavTab.EPG_GUIDE -> {
                            EpgTimelineGuideScreen(
                                channels = state.builtInChannels + state.customM3uChannels,
                                epgMap = state.epgMap,
                                favoriteIds = state.favoriteIds,
                                onPlayChannel = { viewModel.requestPlayChannel(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) }
                            )
                        }
                        MainNavTab.SEARCH -> {
                            ChannelSearchAndNumpadScreen(
                                allChannels = state.builtInChannels + state.customM3uChannels,
                                searchQuery = state.searchQuery,
                                favoriteIds = state.favoriteIds,
                                lockedIds = state.lockedChannelIds,
                                brokenIds = state.brokenChannelIds,
                                onQueryChange = { viewModel.updateSearchQuery(it) },
                                onPlayChannel = { viewModel.requestPlayChannel(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) }
                            )
                        }
                        MainNavTab.M3U_MANAGER -> {
                            M3uPlaylistManagerScreen(
                                state = state,
                                onAddRemotePlaylist = { name, m3u, epg ->
                                    viewModel.addRemoteM3uPlaylist(name, m3u, epg)
                                },
                                onAddXtreamAccount = { name, srv, usr, pwd ->
                                    viewModel.addXtreamCodesAccount(name, srv, usr, pwd)
                                },
                                onPickLocalFile = {
                                    showBuiltInFilePicker = true
                                },
                                onScanDownloadFolder = {
                                    viewModel.scanAndImportDownloadFolderM3u()
                                },
                                onUpdatePin = { newPin -> viewModel.updateParentalPin(newPin) },
                                onToggleAutoStart = { viewModel.toggleAutoStartLastChannel() },
                                onBackupToDevice = { viewModel.backupSettingsToDevice() },
                                onRestoreFromDevice = { viewModel.restoreSettingsFromDevice() },
                                onRestoreHiddenChannels = { viewModel.restoreAllHiddenChannels() },
                                onRefreshQrServer = { viewModel.refreshWifiTransferServer() },
                                onRemovePlaylist = { id -> viewModel.removePlaylist(id) },
                                onRefreshAll = { viewModel.reloadAllChannelsAndEpg() }
                            )
                        }
                    }
                }
            }
        }

        if (state.pipChannel != null && state.selectedTab != MainNavTab.MULTI_VIEW) {
            FloatingMiniPipPlayer(
                channel = state.pipChannel!!,
                onExpand = { viewModel.expandPipToFullScreen() },
                onClose = { viewModel.closePip() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            )
        }

        if (state.pendingPinChannel != null) {
            ParentalPinDialog(
                channel = state.pendingPinChannel!!,
                onVerifyPin = { pin -> viewModel.verifyPinAndPlay(pin) },
                onDismiss = { viewModel.dismissPinDialog() }
            )
        }

        if (showBuiltInFilePicker) {
            TvBuiltInM3uFilePickerDialog(
                onScanM3uFiles = { viewModel.scanAllM3uFilesOnDevice() },
                onSelectFile = { file -> viewModel.importLocalM3uFile(file) },
                onDismiss = { showBuiltInFilePicker = false }
            )
        }

        state.statusMessage?.let { message ->
            LaunchedEffect(message) {
                delay(3500)
                viewModel.clearStatusMessage()
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TvThemeColors.SurfaceElevated)
                    .border(1.5.dp, TvThemeColors.BullRedBright, RoundedCornerShape(12.dp))
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = TvThemeColors.BullRedBright,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    color = TvThemeColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun ChannelShowcaseScreen(
    title: String,
    channels: List<TvChannel>,
    categories: List<String>,
    selectedCategory: String,
    favoriteIds: Set<String>,
    lockedIds: Set<String>,
    brokenIds: Set<String>,
    healthyIds: Set<String> = emptySet(),
    hiddenCount: Int = 0,
    recentChannels: List<TvChannel>,
    lastWatchedChannelId: String? = null,
    onSelectCategory: (String) -> Unit,
    onPlayChannel: (TvChannel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onHideChannel: (String) -> Unit = {},
    onRestoreHidden: () -> Unit = {},
    emptyMessage: String = "Bu kategoride kanal bulunamadı."
) {
    val filtered = remember(channels, selectedCategory) {
        if (selectedCategory == "Tümü") channels
        else channels.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    var localFocusedChannel by remember(filtered) {
        val initial = if (lastWatchedChannelId != null) {
            filtered.find { it.id == lastWatchedChannelId } ?: filtered.firstOrNull()
        } else {
            filtered.firstOrNull()
        }
        mutableStateOf(initial)
    }

    val gridState = rememberLazyGridState()
    val cardFocusRequesters = remember { HashMap<String, FocusRequester>() }

    // Kanaldan çıkıp (Geri tuşu) listeye döndüğümüzde sol menü yerine doğrudan son izlenen
    // kanalın kartına kaydır ve kumanda odağını (Focus) o kanalın üstüne ver!
    LaunchedEffect(lastWatchedChannelId, filtered.size, selectedCategory) {
        if (filtered.isNotEmpty()) {
            val targetId = lastWatchedChannelId ?: localFocusedChannel?.id ?: filtered.first().id
            val targetIdx = filtered.indexOfFirst { it.id == targetId }.coerceAtLeast(0)
            try {
                gridState.scrollToItem(targetIdx)
            } catch (_: Exception) {}
            delay(60L)
            try {
                cardFocusRequesters[filtered[targetIdx].id]?.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        localFocusedChannel?.let { previewChannel ->
            FocusedChannelHeroBanner(
                channel = previewChannel,
                isFavorite = favoriteIds.contains(previewChannel.id),
                isLocked = lockedIds.contains(previewChannel.id),
                isBroken = brokenIds.contains(previewChannel.id) || previewChannel.isBroken,
                isHealthy = healthyIds.contains(previewChannel.id),
                hiddenCount = hiddenCount,
                onWatchClick = { onPlayChannel(previewChannel) },
                onToggleFavorite = { onToggleFavorite(previewChannel.id) },
                onToggleLock = { onToggleLock(previewChannel.id) },
                onHideChannel = { onHideChannel(previewChannel.id) },
                onRestoreHidden = onRestoreHidden
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (categories.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                itemsIndexed(categories, key = { idx, c -> "cat_${c}_$idx" }) { _, category ->
                    var isFocused by remember { mutableStateOf(false) }
                    val isSelected = selectedCategory == category
                    val catColor = TvThemeColors.categoryColor(category)
                    val catIcon = TvThemeColors.categoryIcon(category)
                    val count = if (category == "Tümü") channels.size else channels.count { it.category.equals(category, ignoreCase = true) }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                when {
                                    isFocused -> TvThemeColors.BullRedBright
                                    isSelected -> catColor.copy(alpha = 0.28f)
                                    else -> TvThemeColors.SurfaceCard
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = when {
                                    isFocused -> Color.White
                                    isSelected -> catColor
                                    else -> Color.White.copy(alpha = 0.08f)
                                },
                                shape = RoundedCornerShape(50)
                            )
                            .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
                            .clickable { onSelectCategory(category) }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = catIcon,
                            contentDescription = category,
                            tint = if (isFocused) Color.White else catColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$category ($count)",
                            color = TvThemeColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(TvThemeColors.SurfaceCard),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyMessage,
                    color = TvThemeColors.TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 190.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                itemsIndexed(filtered, key = { idx, ch -> "${ch.id}_$idx" }) { _, channel ->
                    val isBroken = brokenIds.contains(channel.id) || channel.isBroken
                    val requester = cardFocusRequesters.getOrPut(channel.id) { FocusRequester() }
                    TvChannelGridCard(
                        channel = channel,
                        isFavorite = favoriteIds.contains(channel.id),
                        isLocked = lockedIds.contains(channel.id),
                        isBroken = isBroken,
                        isHealthy = healthyIds.contains(channel.id),
                        focusRequester = requester,
                        onFocused = { localFocusedChannel = channel },
                        onClick = { onPlayChannel(channel) },
                        onLongClick = { onToggleFavorite(channel.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun FocusedChannelHeroBanner(
    channel: TvChannel,
    isFavorite: Boolean,
    isLocked: Boolean,
    isBroken: Boolean,
    isHealthy: Boolean = false,
    hiddenCount: Int = 0,
    onWatchClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleLock: () -> Unit,
    onHideChannel: () -> Unit = {},
    onRestoreHidden: () -> Unit = {}
) {
    val catColor = if (isBroken) TvThemeColors.BrokenRed else TvThemeColors.categoryColor(channel.category)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(94.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        TvThemeColors.SurfaceElevated,
                        catColor.copy(alpha = 0.16f),
                        TvThemeColors.SurfaceElevated
                    )
                )
            )
            .border(1.5.dp, catColor.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChannelBrandLogoBox(
            channel = channel,
            boxSize = 62.dp,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (isBroken) TvThemeColors.BrokenRed else TvThemeColors.LiveRed)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = if (isBroken) "⚠️ ÇALIŞMIYOR" else "CANLI • CH ${channel.number}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(TvThemeColors.AccentCyan.copy(alpha = 0.18f))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${channel.qualityBadge} • ${channel.sources.size} Akış",
                        color = TvThemeColors.AccentCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                if (isHealthy && !isBroken) {
                    Text(
                        text = "🟢 AKTİF AKIŞ",
                        color = TvThemeColors.AccentEmerald,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                if (isLocked) {
                    Text(
                        text = "🔒 PIN KİLİTLİ",
                        color = TvThemeColors.FavoriteGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Text(
                    text = "${channel.category} • OK Basılı Tut: ⭐ Favori",
                    color = TvThemeColors.FavoriteGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = channel.name,
                color = TvThemeColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = if (isBroken) "⚠️ Bu kanalın akışı şu an yanıt vermiyor - M3U8 bekleniyor."
                       else "Şu An: ${channel.currentProgramTitle} (${channel.currentProgramTime})  •  Sıradaki: ${channel.nextProgramTitle}",
                color = if (isBroken) TvThemeColors.BrokenRed else TvThemeColors.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TvActionButton(
                text = "İzle",
                icon = Icons.Rounded.PlayArrow,
                isPrimary = true,
                onClick = onWatchClick
            )
            TvActionButton(
                text = if (isFavorite) "Favoride" else "Favori+",
                icon = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                isPrimary = false,
                onClick = onToggleFavorite
            )
            TvActionButton(
                text = if (isLocked) "Kilitli" else "Kilitle",
                icon = Icons.Rounded.Verified,
                isPrimary = false,
                onClick = onToggleLock
            )
            TvActionButton(
                text = "Gizle",
                icon = Icons.Rounded.DeleteOutline,
                isPrimary = false,
                onClick = onHideChannel
            )
            if (hiddenCount > 0) {
                TvActionButton(
                    text = "Geri Yükle ($hiddenCount)",
                    icon = Icons.Rounded.Refresh,
                    isPrimary = false,
                    onClick = onRestoreHidden
                )
            }
        }
    }
}

@Composable
fun TvChannelGridCard(
    channel: TvChannel,
    isFavorite: Boolean,
    isLocked: Boolean = false,
    isBroken: Boolean = false,
    isHealthy: Boolean = false,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val catColor = if (isBroken) TvThemeColors.BrokenRed else TvThemeColors.categoryColor(channel.category)
    var cardFocused by remember { mutableStateOf(false) }

    val focusBorderColor = when {
        isBroken -> TvThemeColors.BrokenRed
        isFavorite -> TvThemeColors.FavoriteGold
        else -> TvThemeColors.BullRedBright
    }

    // Evrensel TV (D-Pad) + Tablet/Telefon (Dokunmatik) Kart Bileşeni:
    // combinedClickable sayesinde telefon ve tabletlerde tek dokunuşla açılır, basılı tutunca favoriye ekler;
    // TV kumandasında ise D-Pad odağı ve OK kısa/uzun basma kusursuz çalışır.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(118.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clip(RoundedCornerShape(14.dp))
            .background(if (cardFocused) TvThemeColors.SurfaceElevated else TvThemeColors.SurfaceCard)
            .border(
                width = if (cardFocused) 2.5.dp else 1.dp,
                color = if (cardFocused) focusBorderColor else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp)
            )
            .onFocusChanged {
                cardFocused = it.isFocused || it.hasFocus
                if (it.isFocused || it.hasFocus) onFocused()
            }
            .tvLongPressFavoriteModifier(
                itemKey = channel.id,
                onShortClick = onClick,
                onLongPress = onLongClick
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(11.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (isBroken) TvThemeColors.BrokenRed.copy(alpha = 0.25f) else catColor.copy(alpha = 0.22f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (isBroken) "⚠️ ÇALIŞMIYOR" else "${channel.number}",
                            color = if (isBroken) TvThemeColors.BrokenRed else catColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = channel.category,
                        color = TvThemeColors.TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isHealthy && !isBroken) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(TvThemeColors.AccentEmerald)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (isLocked) {
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = TvThemeColors.FavoriteGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (channel.sources.size > 1) "${channel.sources.size}x" else channel.qualityBadge,
                        color = TvThemeColors.AccentCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = null,
                        tint = if (isFavorite) TvThemeColors.FavoriteGold else Color.White.copy(alpha = if (cardFocused) 0.55f else 0.22f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                ChannelBrandLogoBox(
                    channel = channel,
                    boxSize = 42.dp,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.name,
                        color = TvThemeColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            isBroken -> "⚠️ Yeni M3U8 Akışı Bekleniyor"
                            cardFocused -> if (isFavorite) "⭐ Favoride • Uzun Bas: Çıkar" else "OK / Dokun: İzle • Uzun Bas: ⭐ Favori"
                            else -> channel.currentProgramTitle
                        },
                        color = when {
                            isBroken -> TvThemeColors.BrokenRed
                            cardFocused -> TvThemeColors.FavoriteGold
                            else -> TvThemeColors.TextSecondary
                        },
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(channel.currentProgress)
                        .background(catColor)
                )
            }
        }
    }
}

@Composable
fun ChannelSearchAndNumpadScreen(
    allChannels: List<TvChannel>,
    searchQuery: String,
    favoriteIds: Set<String>,
    lockedIds: Set<String>,
    brokenIds: Set<String>,
    onQueryChange: (String) -> Unit,
    onPlayChannel: (TvChannel) -> Unit,
    onToggleFavorite: (String) -> Unit = {}
) {
    val results = remember(allChannels, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            allChannels
        } else {
            allChannels.filter {
                it.name.contains(q, ignoreCase = true) ||
                        it.category.contains(q, ignoreCase = true) ||
                        it.number.toString() == q
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                TvInputField(
                    label = "Canlı Kanal Adı, Kategori veya Kanal Numarası Yazın (OK Basılı Tut: ⭐ Favori Ekle/Çıkar)",
                    value = searchQuery,
                    placeholder = "Örn: ATV, Show, Kanal D, NTV, beIN, TRT veya 14...",
                    onValueChange = onQueryChange
                )
            }
            if (searchQuery.isNotEmpty()) {
                TvActionButton(
                    text = "Temizle",
                    icon = Icons.Rounded.Clear,
                    isPrimary = false,
                    onClick = { onQueryChange("") }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val quickTags = listOf("Ulusal", "Spor", "Haber", "Belgesel", "Müzik", "Kıbrıs", "Yerel", "Dünya")
            items(quickTags) { tag ->
                TvCompactChipButton(
                    text = "#$tag",
                    icon = Icons.Rounded.Tag,
                    onClick = { onQueryChange(tag) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 190.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            itemsIndexed(results, key = { idx, ch -> "search_${ch.id}_$idx" }) { _, ch ->
                TvChannelGridCard(
                    channel = ch,
                    isFavorite = favoriteIds.contains(ch.id),
                    isLocked = lockedIds.contains(ch.id),
                    isBroken = brokenIds.contains(ch.id) || ch.isBroken,
                    onFocused = {},
                    onClick = { onPlayChannel(ch) },
                    onLongClick = { onToggleFavorite(ch.id) }
                )
            }
        }
    }
}
