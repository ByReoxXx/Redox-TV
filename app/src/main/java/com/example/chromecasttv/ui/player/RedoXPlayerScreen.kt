@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

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

import org.json.JSONArray
import org.json.JSONObject

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun RedoXPlayerScreen(
    playingChannel: TvChannel,
    previousChannel: TvChannel? = null,
    allChannels: List<TvChannel>,
    favoriteIds: Set<String>,
    lockedIds: Set<String>,
    brokenIds: Set<String>,
    preferredSources: Map<String, Int> = emptyMap(),
    preferredScales: Map<String, String> = emptyMap(),
    epgMap: Map<String, List<EpgProgram>>,
    sleepTimerRemaining: Int?,
    onZapChannel: (delta: Int) -> Unit,
    onRecallPreviousChannel: () -> Unit = {},
    onSelectChannelDirect: (TvChannel) -> Unit,
    onSelectChannelByNumber: (Int) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onHideChannel: (String) -> Unit = {},
    onSavePreferredSource: (String, Int) -> Unit = { _, _ -> },
    onSavePreferredScale: (String, String) -> Unit = { _, _ -> },
    onMarkBroken: (String) -> Unit,
    onClearBroken: (String) -> Unit,
    onMinimizeToPip: () -> Unit,
    onEnterSystemPip: () -> Unit,
    onOpenMultiView: () -> Unit,
    onCycleSleepTimer: () -> Unit,
    onExitPlayer: () -> Unit
) {
    val context = LocalContext.current
    val playerSurfaceFocusRequester = remember { FocusRequester() }
    val primaryActionFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    var showOsd by remember { mutableStateOf(true) }
    var osdInteractiveFocused by remember { mutableStateOf(false) }
    var showChannelDrawer by remember { mutableStateOf(false) }
    var osdInteractionTick by remember { mutableIntStateOf(0) }

    var playbackError by remember { mutableStateOf<String?>(null) }
    var autoFailoverBanner by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(true) }
    var isPausedByTimeshift by remember(playingChannel.id) { mutableStateOf(false) }
    var forceMaxBitrate by remember { mutableStateOf(true) }
    var scaleMode by remember(playingChannel.id) {
        val savedScale = preferredScales[playingChannel.id]?.let { name ->
            PlayerScaleMode.entries.find { it.name == name }
        } ?: PlayerScaleMode.FIT
        mutableStateOf(savedScale)
    }
    var videoResolutionBadge by remember { mutableStateOf(playingChannel.qualityBadge) }
    var liveBitrateText by remember { mutableStateOf("") }

    var currentSourceIndex by remember(playingChannel.id) {
        val savedIdx = (preferredSources[playingChannel.id] ?: 0)
            .coerceIn(0, (playingChannel.sources.size - 1).coerceAtLeast(0))
        mutableIntStateOf(savedIdx)
    }
    val activeSource = playingChannel.sources.getOrElse(currentSourceIndex) {
        StreamSource("FHD 1080p", playingChannel.primaryStreamUrl, StreamType.HLS)
    }

    var audioTracks by remember { mutableStateOf<List<TrackOption>>(emptyList()) }
    var subtitleTracks by remember { mutableStateOf<List<TrackOption>>(emptyList()) }
    var activeAudioLabel by remember { mutableStateOf("Ses") }
    var activeSubtitleLabel by remember { mutableStateOf("Altyazı") }

    var typedChannelDigits by remember { mutableStateOf("") }
    var digitJob by remember { mutableStateOf<Job?>(null) }

    fun bumpOsdTimer(focusControls: Boolean = false) {
        showOsd = true
        osdInteractionTick += 1
        if (focusControls) {
            osdInteractiveFocused = true
        }
    }

    LaunchedEffect(showOsd, showChannelDrawer, osdInteractionTick) {
        if (showOsd && !showChannelDrawer) {
            delay(3800L)
            showOsd = false
            osdInteractiveFocused = false
            try { playerSurfaceFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    LaunchedEffect(showOsd, osdInteractiveFocused) {
        if (showOsd && osdInteractiveFocused && !showChannelDrawer) {
            delay(60L)
            try { primaryActionFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    LaunchedEffect(autoFailoverBanner) {
        if (autoFailoverBanner != null) {
            delay(3000L)
            autoFailoverBanner = null
        }
    }

    val exoPlayer = remember {
        createTvExoPlayer(context, forceMaxBitrate = true).apply {
            playWhenReady = true
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        }
    }

    fun switchToNextSourceManually() {
        if (playingChannel.sources.size > 1) {
            val nextIdx = (currentSourceIndex + 1) % playingChannel.sources.size
            currentSourceIndex = nextIdx
            onSavePreferredSource(playingChannel.id, nextIdx)
            val nextLabel = playingChannel.sources[nextIdx].label
            autoFailoverBanner = "⚡ Kaynak Hafızaya Kaydedildi (${nextIdx + 1}/${playingChannel.sources.size}): $nextLabel"
            bumpOsdTimer()
        } else {
            exoPlayer.seekToDefaultPosition()
            exoPlayer.prepare()
            exoPlayer.play()
            isPausedByTimeshift = false
            autoFailoverBanner = "🔄 Yayın Akışı Yenilendi"
            bumpOsdTimer()
        }
    }

    fun toggleTimeshiftPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            isPausedByTimeshift = true
            autoFailoverBanner = "⏸️ Canlı Yayın Duraklatıldı (Timeshift Aktif)"
        } else {
            exoPlayer.play()
            isPausedByTimeshift = false
            autoFailoverBanner = "▶️ Yayın Devam Ediyor"
        }
        bumpOsdTimer()
    }

    fun rewindTimeshift15Sec() {
        val targetPos = (exoPlayer.currentPosition - 15_000L).coerceAtLeast(0L)
        exoPlayer.seekTo(targetPos)
        exoPlayer.play()
        isPausedByTimeshift = false
        autoFailoverBanner = "⏪ Canlı Tampondan 15 Sn Geri Sarıldı"
        bumpOsdTimer()
    }

    fun jumpToLiveEdge() {
        exoPlayer.seekToDefaultPosition()
        exoPlayer.play()
        isPausedByTimeshift = false
        autoFailoverBanner = "🔴 Canlı Yayın Noktasına Dönüldü"
        bumpOsdTimer()
    }

    fun toggleBitrateMode() {
        forceMaxBitrate = !forceMaxBitrate
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setForceHighestSupportedBitrate(forceMaxBitrate)
            .setMaxVideoBitrate(if (forceMaxBitrate) Int.MAX_VALUE else 2_800_000)
            .build()
        autoFailoverBanner = if (forceMaxBitrate) {
            "🔥 Kalite Modu: En Yüksek Bitrate (1080p) Kilitlendi"
        } else {
            "⚡ Kalite Modu: Hızlı & Dengeli Akış Aktif"
        }
        bumpOsdTimer()
    }

    fun appendChannelDigit(digit: Char) {
        if (typedChannelDigits.length < 4) {
            typedChannelDigits += digit
            digitJob?.cancel()
            digitJob = coroutineScope.launch {
                delay(1100)
                val targetNum = typedChannelDigits.toIntOrNull()
                typedChannelDigits = ""
                if (targetNum != null) {
                    onSelectChannelByNumber(targetNum)
                }
            }
        }
    }

    fun cycleAudioTrack() {
        val groups = exoPlayer.currentTracks.groups
        if (audioTracks.size <= 1) {
            autoFailoverBanner = "🔊 Bu yayında tek ses izi bulunuyor"
            bumpOsdTimer()
            return
        }
        val currentIdx = audioTracks.indexOfFirst { it.isSelected }.coerceAtLeast(0)
        val nextOpt = audioTracks[(currentIdx + 1) % audioTracks.size]
        val group = groups.getOrNull(nextOpt.groupIndex) ?: return
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, nextOpt.trackIndex))
            .build()
        activeAudioLabel = "Ses: ${nextOpt.label}"
        bumpOsdTimer()
    }

    fun cycleSubtitleTrack() {
        val groups = exoPlayer.currentTracks.groups
        if (subtitleTracks.isEmpty()) {
            autoFailoverBanner = "💬 Bu yayında altyazı izi bulunmuyor"
            bumpOsdTimer()
            return
        }
        val currentIdx = subtitleTracks.indexOfFirst { it.isSelected }
        if (currentIdx == -1 || currentIdx + 1 < subtitleTracks.size) {
            val nextOpt = subtitleTracks[(currentIdx + 1).coerceAtLeast(0)]
            val group = groups.getOrNull(nextOpt.groupIndex) ?: return
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, nextOpt.trackIndex))
                .build()
            activeSubtitleLabel = "Altyazı: ${nextOpt.label}"
        } else {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            activeSubtitleLabel = "Altyazı: Kapalı"
        }
        bumpOsdTimer()
    }

    var autoTriedCount by remember(playingChannel.id) { mutableIntStateOf(0) }

    DisposableEffect(exoPlayer, playingChannel.id, currentSourceIndex) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    playbackError = null
                    autoTriedCount = 0
                    onClearBroken(playingChannel.id)
                    if (preferredSources[playingChannel.id] != currentSourceIndex) {
                        onSavePreferredSource(playingChannel.id, currentSourceIndex)
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val aList = mutableListOf<TrackOption>()
                val sList = mutableListOf<TrackOption>()
                tracks.groups.forEachIndexed { gIdx, group ->
                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        for (tIdx in 0 until group.length) {
                            val fmt = group.getTrackFormat(tIdx)
                            val lbl = fmt.label ?: fmt.language?.uppercase() ?: "SES ${aList.size + 1}"
                            aList.add(TrackOption(lbl, gIdx, tIdx, group.isTrackSelected(tIdx)))
                        }
                    } else if (group.type == C.TRACK_TYPE_TEXT) {
                        for (tIdx in 0 until group.length) {
                            val fmt = group.getTrackFormat(tIdx)
                            val lbl = fmt.label ?: fmt.language?.uppercase() ?: "ALTYAZI ${sList.size + 1}"
                            sList.add(TrackOption(lbl, gIdx, tIdx, group.isTrackSelected(tIdx)))
                        }
                    }
                }
                audioTracks = aList
                subtitleTracks = sList
                aList.firstOrNull { it.isSelected }?.let { activeAudioLabel = "Ses: ${it.label}" }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.height > 0) {
                    videoResolutionBadge = when {
                        videoSize.height >= 2160 -> "4K UHD"
                        videoSize.height >= 1080 -> "1080p FHD"
                        videoSize.height >= 720 -> "720p HD"
                        else -> "${videoSize.height}p"
                    }
                    val estBitrate = when {
                        videoSize.height >= 2160 -> "14.0 Mbps"
                        videoSize.height >= 1080 -> "5.2 Mbps"
                        videoSize.height >= 720 -> "2.6 Mbps"
                        else -> "1.4 Mbps"
                    }
                    liveBitrateText = estBitrate
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (playingChannel.sources.size > 1 && autoTriedCount + 1 < playingChannel.sources.size) {
                    autoTriedCount += 1
                    val nextIdx = (currentSourceIndex + 1) % playingChannel.sources.size
                    autoFailoverBanner = "⚡ Kaynak yanıt vermedi, Alternatif Akışa (${nextIdx + 1}/${playingChannel.sources.size}) geçildi"
                    currentSourceIndex = nextIdx
                } else {
                    isBuffering = false
                    playbackError = "⚠️ Tüm alternatif kaynaklar denendi. 'Alternatif Yayın' butonuyla tekrar deneyebilirsiniz."
                    onMarkBroken(playingChannel.id)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(playingChannel.id, currentSourceIndex) {
        playbackError = null
        isBuffering = true
        osdInteractiveFocused = false
        videoResolutionBadge = playingChannel.qualityBadge
        bumpOsdTimer(focusControls = false)
        try { playerSurfaceFocusRequester.requestFocus() } catch (_: Exception) {}

        if (activeSource.url.isNotBlank()) {
            exoPlayer.stop()
            exoPlayer.setMediaSource(buildStreamMediaSource(context, activeSource))
            exoPlayer.prepare()
            exoPlayer.play()
        }
        delay(3200L)
        if (isBuffering && exoPlayer.playbackState != Player.STATE_READY && playingChannel.sources.size > 1 && autoTriedCount + 1 < playingChannel.sources.size) {
            autoTriedCount += 1
            val nextIdx = (currentSourceIndex + 1) % playingChannel.sources.size
            autoFailoverBanner = "⚡ Yayın gecikti, hızlı Alternatif Akışa (${nextIdx + 1}/${playingChannel.sources.size}) geçildi"
            currentSourceIndex = nextIdx
        }
    }

    LaunchedEffect(Unit) {
        try { playerSurfaceFocusRequester.requestFocus() } catch (_: Exception) {}
    }

    BackHandler {
        when {
            showChannelDrawer -> {
                showChannelDrawer = false
                try { playerSurfaceFocusRequester.requestFocus() } catch (_: Exception) {}
            }
            showOsd -> {
                showOsd = false
                osdInteractiveFocused = false
                try { playerSurfaceFocusRequester.requestFocus() } catch (_: Exception) {}
            }
            else -> onExitPlayer()
        }
    }

    val isBrokenChannel = brokenIds.contains(playingChannel.id) || playingChannel.isBroken
    val catColor = if (isBrokenChannel) TvThemeColors.BrokenRed else TvThemeColors.categoryColor(playingChannel.category)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(playerSurfaceFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    val keyCode = event.nativeKeyEvent.keyCode
                    val isRepeat = event.nativeKeyEvent.repeatCount > 0

                    if (isRepeat && (
                                keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                                keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                                keyCode == KeyEvent.KEYCODE_CHANNEL_UP ||
                                keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN ||
                                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                            )
                    ) {
                        return@onPreviewKeyEvent true
                    }

                    if (showOsd) {
                        osdInteractionTick += 1
                    }
                    if (!showChannelDrawer && !osdInteractiveFocused) {
                        when {
                            keyCode == KeyEvent.KEYCODE_0 && typedChannelDigits.isEmpty() && previousChannel != null -> {
                                autoFailoverBanner = "🔄 Önceki Kanala Dönüldü: ${previousChannel.name}"
                                onRecallPreviousChannel()
                                true
                            }
                            keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                                val digit = ('0'.code + (keyCode - KeyEvent.KEYCODE_0)).toChar()
                                appendChannelDigit(digit)
                                true
                            }
                            keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                            keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE ||
                            keyCode == KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                toggleTimeshiftPause()
                                true
                            }
                            keyCode == KeyEvent.KEYCODE_MEDIA_REWIND -> {
                                rewindTimeshift15Sec()
                                true
                            }
                            keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_CHANNEL_UP -> {
                                onZapChannel(+1)
                                true
                            }
                            keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                                onZapChannel(-1)
                                true
                            }
                            keyCode == KeyEvent.KEYCODE_DPAD_LEFT -> {
                                showChannelDrawer = true
                                showOsd = false
                                true
                            }
                            keyCode == KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                switchToNextSourceManually()
                                true
                            }
                            keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER -> {
                                bumpOsdTimer(focusControls = true)
                                true
                            }
                            else -> false
                        }
                    } else false
                } else false
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                    resizeMode = scaleMode.resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.keepScreenOn = true
                view.resizeMode = scaleMode.resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        if (typedChannelDigits.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(28.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TvThemeColors.GlassOverlay)
                    .border(2.dp, TvThemeColors.BullRedBright, RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "CH $typedChannelDigits",
                    color = TvThemeColors.BullRedBright,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        autoFailoverBanner?.let { bannerText ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
                    .clip(RoundedCornerShape(50))
                    .background(TvThemeColors.SurfaceElevated)
                    .border(1.5.dp, TvThemeColors.BullRedBright, RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Sensors,
                    contentDescription = null,
                    tint = TvThemeColors.BullRedBright,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = bannerText,
                    color = TvThemeColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isBuffering && playbackError == null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(14.dp))
                    .background(TvThemeColors.GlassOverlay)
                    .border(1.dp, TvThemeColors.BullRedBright.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Sensors,
                    contentDescription = null,
                    tint = TvThemeColors.BullRedBright,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${playingChannel.name} • ${activeSource.label} (${currentSourceIndex + 1}/${playingChannel.sources.size}) bağlanıyor...",
                    color = TvThemeColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        playbackError?.let { err ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TvThemeColors.GlassOverlay)
                    .border(2.dp, TvThemeColors.BrokenRed, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WifiOff,
                    contentDescription = null,
                    tint = TvThemeColors.BrokenRed,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = err,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                TvCompactChipButton(
                    text = "⚡ Alternatif Kaynağı Dene (${currentSourceIndex + 1}/${playingChannel.sources.size})",
                    icon = Icons.Rounded.Refresh,
                    onClick = { switchToNextSourceManually() }
                )
            }
        }

        // SOL HIZLI KANAL LİSTESİ ÇEKMECESİ (140ms HIZLI SLIDE)
        AnimatedVisibility(
            visible = showChannelDrawer,
            enter = slideInHorizontally(animationSpec = tween(140), initialOffsetX = { -it }) + fadeIn(tween(120)),
            exit = slideOutHorizontally(animationSpec = tween(120), targetOffsetX = { -it }) + fadeOut(tween(100)),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            var selectedDrawerCat by remember { mutableStateOf("Tümü") }
            val drawerCategories = remember(allChannels, favoriteIds) {
                val list = mutableListOf("Tümü", "⭐ Favoriler")
                list.addAll(allChannels.map { it.category }.distinct())
                list
            }
            val filteredDrawerChannels = remember(allChannels, selectedDrawerCat, favoriteIds) {
                when (selectedDrawerCat) {
                    "Tümü" -> allChannels
                    "⭐ Favoriler" -> allChannels.filter { favoriteIds.contains(it.id) }
                    else -> allChannels.filter { it.category.equals(selectedDrawerCat, ignoreCase = true) }
                }
            }
            val currentChannelIndex = remember(playingChannel.id, filteredDrawerChannels) {
                filteredDrawerChannels.indexOfFirst { it.id == playingChannel.id }.coerceAtLeast(0)
            }
            val listState = rememberLazyListState(
                initialFirstVisibleItemIndex = (currentChannelIndex - 3).coerceAtLeast(0)
            )

            Column(
                modifier = Modifier
                    .width(370.dp)
                    .fillMaxHeight()
                    .background(TvThemeColors.GlassOverlay)
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hızlı Kanal Listesi (${filteredDrawerChannels.size})",
                            color = TvThemeColors.BullRedBright,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "OK: İzle  •  OK Basılı Tut: ⭐ Favori Ekle/Çıkar",
                            color = TvThemeColors.FavoriteGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                    TvCompactChipButton(
                        text = "Kapat",
                        icon = Icons.Rounded.Clear,
                        onClick = {
                            showChannelDrawer = false
                            try { playerSurfaceFocusRequester.requestFocus() } catch (_: Exception) {}
                        }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(drawerCategories, key = { "dcat_$it" }) { dCat ->
                        val isSel = selectedDrawerCat == dCat
                        TvCompactChipButton(
                            text = if (isSel) "✓ $dCat" else dCat,
                            icon = if (dCat == "⭐ Favoriler") Icons.Rounded.Star else Icons.Rounded.Category,
                            onClick = { selectedDrawerCat = dCat }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    itemsIndexed(filteredDrawerChannels, key = { _, ch -> "drawer_${ch.id}" }) { _, ch ->
                        var itemFocused by remember { mutableStateOf(false) }
                        val isCurrent = ch.id == playingChannel.id
                        val isFav = favoriteIds.contains(ch.id)
                        val isBroken = brokenIds.contains(ch.id) || ch.isBroken
                        val itemColor = if (isBroken) TvThemeColors.BrokenRed else TvThemeColors.categoryColor(ch.category)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        itemFocused -> TvThemeColors.BullRedBright
                                        isCurrent -> itemColor.copy(alpha = 0.35f)
                                        else -> Color.White.copy(alpha = 0.06f)
                                    }
                                )
                                .border(
                                    width = if (isCurrent && !itemFocused) 1.5.dp else 0.dp,
                                    color = if (isCurrent && !itemFocused) TvThemeColors.BullRedBright else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .onFocusChanged { itemFocused = it.isFocused }
                                .tvLongPressFavoriteModifier(
                                    itemKey = "drawer_fav_${ch.id}",
                                    onShortClick = {
                                        onSelectChannelDirect(ch)
                                        showChannelDrawer = false
                                        try { playerSurfaceFocusRequester.requestFocus() } catch (_: Exception) {}
                                    },
                                    onLongPress = {
                                        val wasFav = favoriteIds.contains(ch.id)
                                        onToggleFavorite(ch.id)
                                        autoFailoverBanner = if (wasFav) {
                                            "☆ ${ch.name} favorilerden çıkarıldı"
                                        } else {
                                            "⭐ ${ch.name} favorilere eklendi"
                                        }
                                    }
                                )
                                .focusable()
                                .clickable {
                                    onSelectChannelDirect(ch)
                                    showChannelDrawer = false
                                    try { playerSurfaceFocusRequester.requestFocus() } catch (_: Exception) {}
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "${ch.number}",
                                color = if (itemFocused) Color.White else itemColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                modifier = Modifier.width(30.dp)
                            )
                            ChannelBrandLogoBox(channel = ch, boxSize = 28.dp, fontSize = 8.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ch.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        isBroken -> "⚠️ ÇALIŞMIYOR"
                                        itemFocused -> if (isFav) "⭐ Favoride • Uzun Bas: Çıkar" else "OK Basılı Tut: ⭐ Favoriye Ekle"
                                        else -> "${ch.sources.size} Akış • ${ch.currentProgramTitle}"
                                    },
                                    color = when {
                                        itemFocused -> Color.White.copy(alpha = 0.92f)
                                        isBroken -> TvThemeColors.BrokenRed
                                        else -> TvThemeColors.TextSecondary
                                    },
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isFav) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = null,
                                tint = if (isFav) TvThemeColors.FavoriteGold else Color.White.copy(alpha = if (itemFocused) 0.7f else 0.25f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        // ALT İNTERAKTİF OSD VE HIZLI KONTROL ÇUBUĞU (140ms HAFİF SLIDE + 3.8 SN OTOMATİK GİZLEME)
        AnimatedVisibility(
            visible = showOsd && !showChannelDrawer,
            enter = slideInVertically(animationSpec = tween(140), initialOffsetY = { it }) + fadeIn(tween(120)),
            exit = slideOutVertically(animationSpec = tween(140), targetOffsetY = { it }) + fadeOut(tween(120)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(TvThemeColors.GlassOverlay)
                    .border(1.5.dp, catColor.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ChannelBrandLogoBox(
                        channel = playingChannel,
                        boxSize = 48.dp,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(TvThemeColors.BullRedBright)
                            )
                            Text(
                                text = "${playingChannel.number}. ${playingChannel.name}",
                                color = TvThemeColors.TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (isBrokenChannel) TvThemeColors.BrokenRed else TvThemeColors.BullRedPrimary.copy(alpha = 0.25f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                val bitrateStr = if (liveBitrateText.isNotBlank()) " • $liveBitrateText" else ""
                                Text(
                                    text = "$videoResolutionBadge$bitrateStr",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Şu An: ${playingChannel.currentProgramTitle}  •  Sıradaki: ${playingChannel.nextProgramTitle}",
                            color = TvThemeColors.TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (playingChannel.sources.size > 1) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.width(460.dp)
                        ) {
                            itemsIndexed(playingChannel.sources) { idx, src ->
                                TvCompactChipButton(
                                    text = if (idx == currentSourceIndex) "✓ Akış ${idx + 1}: ${src.label}" else "Akış ${idx + 1}/${playingChannel.sources.size}",
                                    icon = Icons.Rounded.Sensors,
                                    onClick = {
                                        autoTriedCount = 0
                                        currentSourceIndex = idx
                                        onSavePreferredSource(playingChannel.id, idx)
                                        bumpOsdTimer(focusControls = true)
                                    }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "►: Kaynak Değiştir • ◄: Kanal Listesi • OK: Kontroller",
                            color = TvThemeColors.AccentCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        TvCompactChipButton(
                            text = if (playingChannel.sources.size > 1) {
                                "⚡ Alternatif Yayın (${currentSourceIndex + 1}/${playingChannel.sources.size})"
                            } else {
                                "🔄 Yayını Yenile"
                            },
                            icon = Icons.Rounded.Refresh,
                            onClick = { switchToNextSourceManually() },
                            modifier = Modifier.focusRequester(primaryActionFocusRequester)
                        )
                    }
                    if (previousChannel != null) {
                        item {
                            TvCompactChipButton(
                                text = "🔄 Önceki: ${previousChannel.name} (0)",
                                icon = Icons.Rounded.SwapHoriz,
                                onClick = { onRecallPreviousChannel() }
                            )
                        }
                    }
                    item {
                        TvCompactChipButton(
                            text = if (isPausedByTimeshift) "▶️ Devam Et" else "⏸️ Duraklat",
                            icon = Icons.Rounded.PlayArrow,
                            onClick = { toggleTimeshiftPause() }
                        )
                    }
                    item {
                        TvCompactChipButton(
                            text = "⏪ -15 Sn Geri",
                            icon = Icons.Rounded.History,
                            onClick = { rewindTimeshift15Sec() }
                        )
                    }
                    item {
                        TvCompactChipButton(
                            text = "🔴 Canlıya Dön",
                            icon = Icons.Rounded.FiberManualRecord,
                            onClick = { jumpToLiveEdge() }
                        )
                    }
                    item {
                        TvCompactChipButton(
                            text = if (forceMaxBitrate) "🔥 Bitrate: Maksimum (1080p)" else "⚡ Bitrate: Hızlı/Dengeli",
                            icon = Icons.Rounded.Sensors,
                            onClick = { toggleBitrateMode() }
                        )
                    }
                    item {
                        TvCompactChipButton(
                            text = "📺 Kanal Listesi",
                            icon = Icons.Rounded.LiveTv,
                            onClick = {
                                showChannelDrawer = true
                                showOsd = false
                            }
                        )
                    }
                    item {
                        TvCompactChipButton(
                            text = if (favoriteIds.contains(playingChannel.id)) "⭐ Favoride" else "⭐ Favori Ekle",
                            icon = if (favoriteIds.contains(playingChannel.id)) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            onClick = {
                                onToggleFavorite(playingChannel.id)
                                bumpOsdTimer(focusControls = true)
                            }
                        )
                    }
                    item {
                        TvCompactChipButton(
                            text = "📐 ${scaleMode.label}",
                            icon = Icons.Rounded.AspectRatio,
                            onClick = {
                                val modes = PlayerScaleMode.entries
                                val nextMode = modes[(modes.indexOf(scaleMode) + 1) % modes.size]
                                scaleMode = nextMode
                                onSavePreferredScale(playingChannel.id, nextMode.name)
                                bumpOsdTimer(focusControls = true)
                            }
                        )
                    }
                    item {
                        TvCompactChipButton(
                            text = "Kanalı Gizle",
                            icon = Icons.Rounded.DeleteOutline,
                            onClick = { onHideChannel(playingChannel.id) }
                        )
                    }
                    item {
                        TvCompactChipButton(
                            text = activeAudioLabel,
                            icon = Icons.Rounded.MusicNote,
                            onClick = { cycleAudioTrack() }
                        )
                    }
                    item {
                        TvCompactChipButton(
                            text = activeSubtitleLabel,
                            icon = Icons.Rounded.InsertDriveFile,
                            onClick = { cycleSubtitleTrack() }
                        )
                    }
                    item {
                        TvCompactChipButton(
                            text = "🖼️ Küçült (PiP)",
                            icon = Icons.Rounded.SmartDisplay,
                            onClick = onMinimizeToPip
                        )
                    }
                    item {
                        TvCompactChipButton(
                            text = "2x2 Ekran",
                            icon = Icons.Rounded.GridView,
                            onClick = onOpenMultiView
                        )
                    }
                    item {
                        TvCompactChipButton(
                            text = "Gizle",
                            icon = Icons.Rounded.Clear,
                            onClick = {
                                showOsd = false
                                osdInteractiveFocused = false
                                try { playerSurfaceFocusRequester.requestFocus() } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }
        }
    }
}
