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

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun MultiViewPlayerCell(
    slotIndex: Int,
    channel: TvChannel,
    isAudioActive: Boolean,
    onFocused: () -> Unit,
    onZapSlot: (Int) -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }
    val exoPlayer = remember {
        createTvExoPlayer(context).apply {
            playWhenReady = true
            volume = if (isAudioActive) 1f else 0f
        }
    }

    LaunchedEffect(isAudioActive) {
        exoPlayer.volume = if (isAudioActive) 1f else 0f
    }

    LaunchedEffect(channel.primaryStreamUrl) {
        val src = channel.sources.firstOrNull()
        if (src != null && src.url.isNotBlank()) {
            exoPlayer.setMediaSource(buildStreamMediaSource(context, src))
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black)
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) TvThemeColors.BullRedBright else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(14.dp)
            )
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN && event.nativeKeyEvent.repeatCount == 0 && isFocused) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP -> { onZapSlot(+1); true }
                        KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> { onZapSlot(-1); true }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { onExpand(); true }
                        else -> false
                    }
                } else false
            }
            .clickable { onExpand() }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Text(
                text = "${slotIndex + 1}. ${channel.name}",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = if (isAudioActive) "🔊 SES AÇIK" else "🔇 SESSİZ",
                color = if (isAudioActive) TvThemeColors.AccentEmerald else TvThemeColors.TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun FloatingMiniPipPlayer(
    channel: TvChannel,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }
    val exoPlayer = remember { createTvExoPlayer(context).apply { playWhenReady = true } }

    LaunchedEffect(channel.primaryStreamUrl) {
        val src = channel.sources.firstOrNull()
        if (src != null && src.url.isNotBlank()) {
            exoPlayer.setMediaSource(buildStreamMediaSource(context, src))
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Column(
        modifier = modifier
            .width(260.dp)
            .height(165.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(TvThemeColors.GlassOverlay)
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) TvThemeColors.BullRedBright else TvThemeColors.AccentIndigo,
                shape = RoundedCornerShape(14.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onExpand() }
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        keepScreenOn = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(TvThemeColors.SurfaceElevated)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "PiP • ${channel.name}",
                color = TvThemeColors.AccentCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            TvCompactChipButton(
                text = "Kapat",
                icon = Icons.Rounded.Clear,
                onClick = onClose
            )
        }
    }
}

@Composable
fun ParentalPinDialog(
    channel: TvChannel,
    onVerifyPin: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorShake by remember { mutableStateOf(false) }

    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(390.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(TvThemeColors.SurfaceElevated)
                .border(2.dp, TvThemeColors.BullRedBright, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Verified,
                contentDescription = null,
                tint = TvThemeColors.FavoriteGold,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Ebeveyn Kilidi (${channel.name})",
                color = TvThemeColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(4) { idx ->
                    val filled = idx < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(
                                width = 1.5.dp,
                                color = if (filled) TvThemeColors.BullRedBright else Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (filled) "●" else "",
                            color = TvThemeColors.BullRedBright,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (errorShake) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Hatalı PIN! Tekrar deneyin.",
                    color = TvThemeColors.LiveRed,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items((0..9).toList()) { num ->
                    TvCompactChipButton(
                        text = num.toString(),
                        icon = Icons.Rounded.SettingsRemote,
                        onClick = {
                            if (enteredPin.length < 4) {
                                enteredPin += num.toString()
                                if (enteredPin.length == 4) {
                                    if (!onVerifyPin(enteredPin)) {
                                        errorShake = true
                                        enteredPin = ""
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TvCompactChipButton(text = "Temizle", icon = Icons.Rounded.Clear, onClick = { enteredPin = "" })
                TvCompactChipButton(text = "İptal", icon = Icons.Rounded.Clear, onClick = onDismiss)
            }
        }
    }
}

@Composable
fun TvTopStatusHeader(
    state: TvUiState,
    onQuickHealthCheckClick: () -> Unit = {},
    onQuickMultiViewClick: () -> Unit,
    onQuickSearchClick: () -> Unit,
    onQuickAddM3uClick: () -> Unit
) {
    var currentTimeText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            currentTimeText = timeFormat.format(Date())
            delay(15_000L)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = state.selectedTab.icon,
                contentDescription = null,
                tint = TvThemeColors.BullRedBright,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = state.selectedTab.title,
                color = TvThemeColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(TvThemeColors.BullRedPrimary.copy(alpha = 0.22f))
                    .padding(horizontal = 9.dp, vertical = 2.dp)
            ) {
                val countText = when (state.selectedTab) {
                    MainNavTab.FAVORITES -> "${state.favoriteOrderList.size} Favori"
                    MainNavTab.CUSTOM_M3U -> "${state.customM3uChannels.size} Kanal"
                    else -> "${state.builtInChannels.size} Kanal"
                }
                Text(
                    text = countText,
                    color = TvThemeColors.BullRedBright,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TvCompactChipButton(
                text = when {
                    state.isScanningHealth -> "🩺 Taranıyor (${state.healthScanProgress})"
                    state.healthyChannelIds.isNotEmpty() -> "🟢 Aktif (${state.healthyChannelIds.size}/${state.builtInChannels.size + state.customM3uChannels.size})"
                    else -> "🩺 Tümünü Test Et"
                },
                icon = Icons.Rounded.Sensors,
                onClick = onQuickHealthCheckClick
            )
            TvCompactChipButton(
                text = "2x2 Ekran",
                icon = Icons.Rounded.GridView,
                onClick = onQuickMultiViewClick
            )
            TvCompactChipButton(
                text = "Ara",
                icon = Icons.Rounded.Search,
                onClick = onQuickSearchClick
            )
            TvCompactChipButton(
                text = "M3U / QR",
                icon = Icons.Rounded.SettingsRemote,
                onClick = onQuickAddM3uClick
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(TvThemeColors.SurfaceCard)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = TvThemeColors.BullRedBright,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = currentTimeText,
                    color = TvThemeColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun TvCompactChipButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (isFocused) TvThemeColors.BullRedBright else TvThemeColors.SurfaceCard)
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(9.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (isFocused) Color.White else TvThemeColors.BullRedBright,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun TvSideNavigationRail(
    selectedTab: MainNavTab,
    builtInCount: Int,
    customM3uCount: Int,
    favoriteCount: Int,
    sleepTimerMinutes: Int?,
    onSelectTab: (MainNavTab) -> Unit,
    onOpenLanding: () -> Unit,
    onCycleSleepTimer: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(215.dp)
            .fillMaxHeight()
            .background(TvThemeColors.SidebarGlass)
            .padding(vertical = 14.dp, horizontal = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                RedBullEmblemBox(boxSize = 42.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "RedoX TV",
                        color = TvThemeColors.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Text(
                        text = "Red Bull 4K Edition",
                        color = TvThemeColors.BullRedBright,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            MainNavTab.entries.forEach { tab ->
                var isFocused by remember { mutableStateOf(false) }
                val isSelected = selectedTab == tab

                val badgeText = when (tab) {
                    MainNavTab.ALL_CHANNELS -> builtInCount.toString()
                    MainNavTab.FAVORITES -> favoriteCount.toString()
                    MainNavTab.CUSTOM_M3U -> customM3uCount.toString()
                    MainNavTab.MULTI_VIEW -> "2x2"
                    else -> null
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            when {
                                isFocused -> TvThemeColors.BullRedPrimary.copy(alpha = 0.34f)
                                isSelected -> TvThemeColors.SurfaceElevated
                                else -> Color.Transparent
                            }
                        )
                        .border(
                            width = if (isFocused) 2.dp else if (isSelected) 1.dp else 0.dp,
                            color = if (isFocused) TvThemeColors.BullRedBright else if (isSelected) TvThemeColors.BullRedPrimary.copy(alpha = 0.5f) else Color.Transparent,
                            shape = RoundedCornerShape(11.dp)
                        )
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = if (isSelected || isFocused) TvThemeColors.BullRedBright else TvThemeColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tab.title,
                            color = if (isSelected || isFocused) TvThemeColors.TextPrimary else TvThemeColors.TextSecondary,
                            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = tab.subtitle,
                            color = TvThemeColors.TextSecondary.copy(alpha = 0.75f),
                            fontSize = 9.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    if (badgeText != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(TvThemeColors.BullRedPrimary.copy(alpha = 0.32f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = TvThemeColors.TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        var sleepFocused by remember { mutableStateOf(false) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (sleepFocused) TvThemeColors.BullRedBright else TvThemeColors.SurfaceCard)
                .onFocusChanged { sleepFocused = it.isFocused }
                .focusable()
                .clickable { onCycleSleepTimer() }
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Bedtime,
                contentDescription = null,
                tint = if (sleepFocused) Color.White else TvThemeColors.FavoriteGold,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (sleepTimerMinutes != null) "Uyku: $sleepTimerMinutes dk" else "Uyku Zamanlayıcı: Kapalı",
                color = TvThemeColors.TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun TvActionButton(
    text: String,
    icon: ImageVector,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isFocused -> TvThemeColors.BullRedBright
                    isPrimary -> TvThemeColors.BullRedPrimary
                    else -> TvThemeColors.SurfaceCard
                }
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = Color.White,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Kumanda OK (DPAD_CENTER / ENTER) tuşuna:
 * - Kısa basıldığında: onShortClick() (Kanalı açar)
 * - Uzun basıldığında (~450ms basılı tutulduğunda) veya MENU/BOOKMARK tuşuna basıldığında:
 *   onLongPress() (Kanalı açmadan doğrudan Favorilere ekler / çıkarır)
 */
@Composable
fun Modifier.tvLongPressFavoriteModifier(
    itemKey: Any,
    onShortClick: () -> Unit,
    onLongPress: () -> Unit
): Modifier {
    val scope = rememberCoroutineScope()
    val currentOnShortClick by rememberUpdatedState(onShortClick)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    var isCenterKeyDown by remember(itemKey) { mutableStateOf(false) }
    var longPressTriggered by remember(itemKey) { mutableStateOf(false) }
    var longPressJob by remember(itemKey) { mutableStateOf<Job?>(null) }

    DisposableEffect(itemKey) {
        onDispose {
            longPressJob?.cancel()
        }
    }

    return this
        .onFocusChanged { focusState ->
            if (!focusState.isFocused && !focusState.hasFocus) {
                isCenterKeyDown = false
                longPressTriggered = false
                longPressJob?.cancel()
            }
        }
        .onPreviewKeyEvent { event ->
            val keyCode = event.nativeKeyEvent.keyCode
            val isCenterKey = keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                    keyCode == KeyEvent.KEYCODE_ENTER ||
                    keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
            if (isCenterKey) {
                when (event.nativeKeyEvent.action) {
                    KeyEvent.ACTION_DOWN -> {
                        if (event.nativeKeyEvent.repeatCount == 0 && !isCenterKeyDown) {
                            isCenterKeyDown = true
                            longPressTriggered = false
                            longPressJob?.cancel()
                            longPressJob = scope.launch {
                                delay(450L)
                                if (isCenterKeyDown && !longPressTriggered) {
                                    longPressTriggered = true
                                    currentOnLongPress()
                                }
                            }
                        } else if (event.nativeKeyEvent.repeatCount > 0 || event.nativeKeyEvent.isLongPress) {
                            if (isCenterKeyDown && !longPressTriggered &&
                                (event.nativeKeyEvent.isLongPress || event.nativeKeyEvent.eventTime - event.nativeKeyEvent.downTime >= 400L)
                            ) {
                                longPressTriggered = true
                                longPressJob?.cancel()
                                currentOnLongPress()
                            }
                        }
                        true
                    }
                    KeyEvent.ACTION_UP -> {
                        longPressJob?.cancel()
                        if (isCenterKeyDown) {
                            val wasLongPress = longPressTriggered
                            isCenterKeyDown = false
                            longPressTriggered = false
                            if (!wasLongPress) {
                                currentOnShortClick()
                            }
                        }
                        true
                    }
                    else -> false
                }
            } else if (
                event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                event.nativeKeyEvent.repeatCount == 0 &&
                (keyCode == KeyEvent.KEYCODE_BOOKMARK || keyCode == KeyEvent.KEYCODE_MENU)
            ) {
                currentOnLongPress()
                true
            } else {
                false
            }
        }
}

/**
 * İki Aşamalı Android TV Input Kutusu (TvInputField):
 * 1. Gezinme Modu (isEditing = false):
 *    - Normal bir TV butonu gibi davranır (.focusable()).
 *    - Kumandanın Yukarı (▲) ve Aşağı (▼) yön tuşları ASLA metin kutusu içinde sıkışmaz;
 *      upFocusRequester / downFocusRequester ile doğrudan üstteki veya alttaki kutuya geçer!
 * 2. Yazma Modu (isEditing = true):
 *    - Kutu üzerindeyken OK tuşuna basıldığında açılır, Android TV klavyesini çağırır
 *      ve tek tıkla "📋 Panodan Yapıştır", "https://", ".m3u8", "✓ Bitti" çiplerini gösterir.
 */
@Composable
fun TvInputField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    showUrlQuickChips: Boolean = false
) {
    val context = LocalContext.current
    val outerRequester = remember { focusRequester ?: FocusRequester() }
    val innerTextRequester = remember { FocusRequester() }
    var boxFocused by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    fun showAndroidTvSoftKeyboard() {
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        } catch (_: Exception) {}
    }

    fun pasteFromClipboard() {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clipText = cm?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
            if (!clipText.isNullOrEmpty()) {
                onValueChange(clipText)
            }
        } catch (_: Exception) {}
    }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            delay(40L)
            try {
                innerTextRequester.requestFocus()
                showAndroidTvSoftKeyboard()
            } catch (_: Exception) {}
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 3.dp)
        ) {
            Text(
                text = label,
                color = if (boxFocused || isEditing) TvThemeColors.BullRedBright else TvThemeColors.TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (boxFocused || isEditing) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
            Text(
                text = when {
                    isEditing -> "⌨️ Yazma Modu Açık (▼/▲ veya Geri: Kutudan Çık)"
                    boxFocused -> "OK: Yaz / Düzenle  •  ▲/▼: Diğer Kutuya Geç"
                    else -> ""
                },
                color = if (isEditing) TvThemeColors.FavoriteGold else TvThemeColors.AccentCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }

        if (!isEditing) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(outerRequester)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (boxFocused) TvThemeColors.SurfaceElevated else Color.Black.copy(alpha = 0.38f)
                    )
                    .border(
                        width = if (boxFocused) 2.dp else 1.dp,
                        color = if (boxFocused) TvThemeColors.BullRedBright else Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .onFocusChanged { boxFocused = it.isFocused }
                    .onPreviewKeyEvent { event ->
                        if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                            when (event.nativeKeyEvent.keyCode) {
                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                    if (downFocusRequester != null) {
                                        try { downFocusRequester.requestFocus() } catch (_: Exception) {}
                                        true
                                    } else false
                                }
                                KeyEvent.KEYCODE_DPAD_UP -> {
                                    if (upFocusRequester != null) {
                                        try { upFocusRequester.requestFocus() } catch (_: Exception) {}
                                        true
                                    } else false
                                }
                                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                    isEditing = true
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
                    .focusable()
                    .clickable { isEditing = true }
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Text(
                    text = value.ifEmpty { placeholder },
                    color = if (value.isEmpty()) TvThemeColors.TextSecondary.copy(alpha = 0.55f) else TvThemeColors.TextPrimary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (boxFocused) "⌨️ OK: Yaz" else "Düzenle",
                    color = if (boxFocused) TvThemeColors.FavoriteGold else TvThemeColors.TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(TvThemeColors.SurfaceElevated)
                    .border(2.dp, TvThemeColors.FavoriteGold, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(color = TvThemeColors.TextPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(TvThemeColors.FavoriteGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(innerTextRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                when (event.nativeKeyEvent.keyCode) {
                                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                                        isEditing = false
                                        try {
                                            (downFocusRequester ?: outerRequester).requestFocus()
                                        } catch (_: Exception) {}
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_UP -> {
                                        isEditing = false
                                        try {
                                            (upFocusRequester ?: outerRequester).requestFocus()
                                        } catch (_: Exception) {}
                                        true
                                    }
                                    KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_BACK -> {
                                        isEditing = false
                                        try {
                                            (downFocusRequester ?: outerRequester).requestFocus()
                                        } catch (_: Exception) {}
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        },
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = TvThemeColors.TextSecondary.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TvCompactChipButton(
                        text = "✓ Bitti (Aşağı Geç)",
                        icon = Icons.Rounded.CheckCircle,
                        onClick = {
                            isEditing = false
                            try {
                                (downFocusRequester ?: outerRequester).requestFocus()
                            } catch (_: Exception) {}
                        }
                    )
                    TvCompactChipButton(
                        text = "📋 Yapıştır",
                        icon = Icons.Rounded.InsertDriveFile,
                        onClick = { pasteFromClipboard() }
                    )
                    if (showUrlQuickChips) {
                        TvCompactChipButton(
                            text = "https://",
                            icon = Icons.Rounded.Public,
                            onClick = {
                                if (!value.startsWith("http")) onValueChange("https://$value")
                            }
                        )
                        TvCompactChipButton(
                            text = "http://",
                            icon = Icons.Rounded.Public,
                            onClick = {
                                if (!value.startsWith("http")) onValueChange("http://$value")
                            }
                        )
                        TvCompactChipButton(
                            text = ".m3u8",
                            icon = Icons.Rounded.Tag,
                            onClick = {
                                if (!value.endsWith(".m3u8")) onValueChange("$value.m3u8")
                            }
                        )
                    }
                    if (value.isNotEmpty()) {
                        TvCompactChipButton(
                            text = "Temizle",
                            icon = Icons.Rounded.Clear,
                            onClick = { onValueChange("") }
                        )
                    }
                }
            }
        }
    }
}


/**
 * Chromecast 4K / Google TV Dahili Kumanda Destekli M3U Dosya Seçici ve Klasör Gezgini.
 *
 * Google TV / Android TV cihazlarında sistem dosya seçicisi (`DocumentsUI`) bulunmadığı için
 * "Bunu yapabilecek uygulamanız yok" hatasını tamamen ortadan kaldırır ve uygulama içinden
 * kumanda ile tüm klasörleri (`Download`, `Ana Dizin`, `Documents`, `USB Bellek`) gezmeyi sağlar.
 */
@Composable
fun TvBuiltInM3uFilePickerDialog(
    onScanM3uFiles: () -> List<File>,
    onSelectFile: (File) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var permissionRefreshTrigger by remember { mutableIntStateOf(0) }

    val hasAllFilesPermission = remember(permissionRefreshTrigger) {
        if (Build.VERSION.SDK_INT >= 30) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    val runtimePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        permissionRefreshTrigger += 1
    }

    val allFilesSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        permissionRefreshTrigger += 1
    }

    fun requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                allFilesSettingsLauncher.launch(intent)
            } catch (_: Exception) {
                try {
                    allFilesSettingsLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                } catch (_: Exception) {
                    runtimePermLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        } else {
            runtimePermLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasAllFilesPermission && Build.VERSION.SDK_INT < 30) {
            runtimePermLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val defaultDownloadDir = remember {
        val dl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (dl != null && dl.exists()) dl else File("/sdcard/Download")
    }

    // browseMode: 0 = Otomatik Bulunan M3U'lar, 1 = Klasör Gezgini
    var browseMode by remember { mutableIntStateOf(0) }
    var currentDir by remember { mutableStateOf(defaultDownloadDir) }

    val discoveredM3uFiles = remember(permissionRefreshTrigger, browseMode) {
        onScanM3uFiles()
    }

    val directoryEntries = remember(currentDir, permissionRefreshTrigger) {
        try {
            val list = currentDir.listFiles()?.toList().orEmpty()
            list.filter { f ->
                if (f.name.startsWith(".")) false
                else if (f.isDirectory) true
                else {
                    val low = f.name.lowercase(Locale.US)
                    low.endsWith(".m3u") || low.endsWith(".m3u8") || low.endsWith(".txt")
                }
            }.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase(Locale.getDefault()) })
        } catch (_: Exception) {
            emptyList()
        }
    }

    BackHandler {
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.84f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(760.dp)
                .heightIn(min = 420.dp, max = 560.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(TvThemeColors.SurfaceCard)
                .border(1.5.dp, TvThemeColors.BullRedPrimary.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        tint = TvThemeColors.BullRedBright,
                        modifier = Modifier.size(26.dp)
                    )
                    Column {
                        Text(
                            text = "📂 RedoX TV Dahili M3U Dosya Seçici",
                            color = TvThemeColors.TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Harici dosya yöneticisine gerek olmadan kumanda ile .m3u / .m3u8 seçin",
                            color = TvThemeColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                TvCompactChipButton(
                    text = "✕ Kapat",
                    icon = Icons.Rounded.Clear,
                    onClick = onDismiss
                )
            }

            if (!hasAllFilesPermission) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TvThemeColors.BullRedDark.copy(alpha = 0.45f))
                        .border(1.dp, TvThemeColors.BullRedBright, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "🔓 Download Klasöründeki M3U Dosyalarını Görmek İçin İzin Gerekli",
                            color = TvThemeColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Android TV güvenlik kısıtlaması nedeniyle 'Tüm Dosyalara Erişim' iznini bir kez açın.",
                            color = TvThemeColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    TvActionButton(
                        text = "İzni Şimdi Aç",
                        icon = Icons.Rounded.Verified,
                        isPrimary = true,
                        onClick = { requestStorageAccess() }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TvCompactChipButton(
                    text = if (browseMode == 0) "✓ Bulunan M3U'lar (${discoveredM3uFiles.size})" else "Bulunan M3U'lar (${discoveredM3uFiles.size})",
                    icon = Icons.Rounded.ManageSearch,
                    onClick = { browseMode = 0 }
                )
                TvCompactChipButton(
                    text = if (browseMode == 1 && currentDir.absolutePath.contains("Download")) "✓ Download Klasörü" else "Download Klasörü",
                    icon = Icons.Rounded.CloudDownload,
                    onClick = {
                        browseMode = 1
                        currentDir = defaultDownloadDir
                    }
                )
                TvCompactChipButton(
                    text = if (browseMode == 1 && currentDir.absolutePath == "/sdcard") "✓ Ana Dizin (/sdcard)" else "Ana Dizin (/sdcard)",
                    icon = Icons.Rounded.Home,
                    onClick = {
                        browseMode = 1
                        currentDir = Environment.getExternalStorageDirectory() ?: File("/sdcard")
                    }
                )
                TvCompactChipButton(
                    text = if (browseMode == 1 && currentDir.absolutePath == "/storage") "✓ USB / Depolama" else "USB / Depolama",
                    icon = Icons.Rounded.Dns,
                    onClick = {
                        browseMode = 1
                        currentDir = File("/storage")
                    }
                )
                TvCompactChipButton(
                    text = "Yenile",
                    icon = Icons.Rounded.Refresh,
                    onClick = { permissionRefreshTrigger += 1 }
                )
            }

            if (browseMode == 0) {
                if (discoveredM3uFiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TvThemeColors.BackgroundDeep.copy(alpha = 0.6f))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Henüz otomatik bulunan .m3u / .m3u8 dosyası yok.",
                                color = TvThemeColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "İpucu: Üstteki 'Download Klasörü' veya 'Ana Dizin (/sdcard)' sekmesinden klasörleri gezebilir ya da telefonunuzdan QR Web Kumandası ile doğrudan M3U dosyası gönderebilirsiniz.",
                                color = TvThemeColors.TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(discoveredM3uFiles, key = { it.absolutePath }) { file ->
                            val kbSize = (file.length() / 1024L).coerceAtLeast(1L)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(TvThemeColors.SurfaceElevated)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                    Text(
                                        text = "🎵 ${file.name}",
                                        color = TvThemeColors.TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${file.parent ?: ""} • ${kbSize} KB",
                                        color = TvThemeColors.TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                TvCompactChipButton(
                                    text = "✓ Bu M3U'yu Yükle",
                                    icon = Icons.Rounded.CheckCircle,
                                    onClick = {
                                        onSelectFile(file)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Konum: ${currentDir.absolutePath}",
                        color = TvThemeColors.AccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    val parentFile = currentDir.parentFile
                    if (parentFile != null) {
                        TvCompactChipButton(
                            text = "⬆️ Üst Klasör (..)",
                            icon = Icons.Rounded.FolderOpen,
                            onClick = { currentDir = parentFile }
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(directoryEntries, key = { it.absolutePath }) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(TvThemeColors.SurfaceElevated)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(
                                    text = if (entry.isDirectory) "📁 ${entry.name}" else "🎵 ${entry.name}",
                                    color = TvThemeColors.TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!entry.isDirectory) {
                                    val kb = (entry.length() / 1024L).coerceAtLeast(1L)
                                    Text(
                                        text = "$kb KB • M3U/Liste Dosyası",
                                        color = TvThemeColors.TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            if (entry.isDirectory) {
                                TvCompactChipButton(
                                    text = "Klasörü Aç",
                                    icon = Icons.Rounded.FolderOpen,
                                    onClick = { currentDir = entry }
                                )
                            } else {
                                TvCompactChipButton(
                                    text = "✓ Seç ve Yükle",
                                    icon = Icons.Rounded.CheckCircle,
                                    onClick = {
                                        onSelectFile(entry)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

