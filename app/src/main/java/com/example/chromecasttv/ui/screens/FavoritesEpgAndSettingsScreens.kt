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

@Composable
fun FavoritesReorderScreen(
    favoriteChannels: List<TvChannel>,
    lockedIds: Set<String>,
    brokenIds: Set<String>,
    onPlayChannel: (TvChannel) -> Unit,
    onMoveChannel: (channelId: String, delta: Int) -> Unit,
    onSortFavorites: (mode: String) -> Unit = {},
    onRemoveFavorite: (channelId: String) -> Unit
) {
    val listState = rememberLazyListState()
    val headerFocusRequester = remember { FocusRequester() }
    val rowFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    // Hangi kanalın şu an "Tut & Taşı" (Grab) modunda olduğunu tutar
    var grabbedChannelId by remember { mutableStateOf<String?>(null) }
    var pendingFocusChannelId by remember { mutableStateOf<String?>(null) }

    // Kanal taşındığında veya bir kanal silindiğinde odağı (focus) ve kaydırmayı hedef kanalda tut!
    LaunchedEffect(favoriteChannels, grabbedChannelId, pendingFocusChannelId) {
        val targetId = grabbedChannelId ?: pendingFocusChannelId
        if (targetId != null) {
            val targetIdx = favoriteChannels.indexOfFirst { it.id == targetId }
            if (targetIdx != -1) {
                try {
                    listState.animateScrollToItem((targetIdx - 2).coerceAtLeast(0))
                } catch (_: Exception) {}
                delay(25L)
                try {
                    rowFocusRequesters[targetId]?.requestFocus()
                } catch (_: Exception) {}
            } else if (favoriteChannels.isNotEmpty()) {
                val fallbackId = favoriteChannels.first().id
                try { rowFocusRequesters[fallbackId]?.requestFocus() } catch (_: Exception) {}
            } else {
                try { headerFocusRequester.requestFocus() } catch (_: Exception) {}
            }
            if (pendingFocusChannelId != null) {
                pendingFocusChannelId = null
            }
        }
    }

    fun removeChannelSafely(index: Int, channelId: String) {
        val nextNeighborId = favoriteChannels.getOrNull(index + 1)?.id
            ?: favoriteChannels.getOrNull(index - 1)?.id
        if (grabbedChannelId == channelId) {
            grabbedChannelId = null
        }
        pendingFocusChannelId = nextNeighborId
        onRemoveFavorite(channelId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(TvThemeColors.SurfaceElevated)
                .border(
                    width = 1.5.dp,
                    color = if (grabbedChannelId != null) TvThemeColors.FavoriteGold else TvThemeColors.BullRedBright.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (grabbedChannelId != null) {
                        "📌 TAŞIMA MODU AKTİF: ▲/▼ ile Kaydırın • ►: 5 Sıra Yukarı • OK: Bırak • ◄: Favoriden Çıkar"
                    } else {
                        "⭐ Favori Kanallarınız (${favoriteChannels.size} Kanal) • Kolay Tut & Taşı Sıralama"
                    },
                    color = if (grabbedChannelId != null) TvThemeColors.FavoriteGold else TvThemeColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Text(
                    text = "Kanal üzerinde ► (Sağ) tuşuna basıp tutun, ▲/▼ ile istediğiniz sıraya kaydırıp OK ile bırakın.",
                    color = TvThemeColors.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TvCompactChipButton(
                    text = "🔤 A-Z Sırala",
                    icon = Icons.Rounded.SwapHoriz,
                    onClick = { onSortFavorites("AZ") },
                    modifier = Modifier.focusRequester(headerFocusRequester)
                )
                TvCompactChipButton(
                    text = "🔢 Kanal No",
                    icon = Icons.Rounded.Tag,
                    onClick = { onSortFavorites("NUMBER") }
                )
                TvCompactChipButton(
                    text = "📂 Kategori",
                    icon = Icons.Rounded.Category,
                    onClick = { onSortFavorites("CATEGORY") }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (favoriteChannels.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(TvThemeColors.SurfaceCard),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Henüz favori kanal eklemediniz.\nCanlı TV veya Özel M3U listesinde kanalın üstüne uzun basarak (OK Basılı Tut) favorilere ekleyebilirsiniz.",
                    color = TvThemeColors.TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                itemsIndexed(favoriteChannels, key = { _, ch -> "fav_${ch.id}" }) { index, channel ->
                    val requester = remember(channel.id) {
                        rowFocusRequesters.getOrPut(channel.id) { FocusRequester() }
                    }
                    var rowFocused by remember { mutableStateOf(false) }
                    val isGrabbed = grabbedChannelId == channel.id
                    val isBroken = brokenIds.contains(channel.id) || channel.isBroken

                    val scope = rememberCoroutineScope()
                    var okDown by remember(channel.id) { mutableStateOf(false) }
                    var longPressArmed by remember(channel.id) { mutableStateOf(false) }
                    var holdJob by remember(channel.id) { mutableStateOf<Job?>(null) }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(requester)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isGrabbed -> TvThemeColors.FavoriteGold.copy(alpha = 0.26f)
                                    rowFocused -> TvThemeColors.SurfaceElevated
                                    else -> TvThemeColors.SurfaceCard
                                }
                            )
                            .border(
                                width = if (isGrabbed) 2.5.dp else if (rowFocused) 2.dp else 1.dp,
                                color = when {
                                    isGrabbed -> TvThemeColors.FavoriteGold
                                    rowFocused -> TvThemeColors.BullRedBright
                                    else -> Color.White.copy(alpha = 0.08f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .onFocusChanged {
                                rowFocused = it.isFocused
                                if (!it.isFocused) {
                                    okDown = false
                                    longPressArmed = false
                                    holdJob?.cancel()
                                }
                            }
                            .onPreviewKeyEvent { event ->
                                val keyCode = event.nativeKeyEvent.keyCode
                                val action = event.nativeKeyEvent.action
                                val isRepeat = event.nativeKeyEvent.repeatCount > 0

                                // [1] EĞER KANAL "TUT & TAŞI" (GRAB) MODUNDAYSA:
                                if (isGrabbed) {
                                    if (action == KeyEvent.ACTION_DOWN) {
                                        when (keyCode) {
                                            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP -> {
                                                if (!isRepeat) onMoveChannel(channel.id, -1)
                                                return@onPreviewKeyEvent true
                                            }
                                            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                                                if (!isRepeat) onMoveChannel(channel.id, +1)
                                                return@onPreviewKeyEvent true
                                            }
                                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                if (!isRepeat) onMoveChannel(channel.id, -5)
                                                return@onPreviewKeyEvent true
                                            }
                                            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DEL -> {
                                                if (!isRepeat) removeChannelSafely(index, channel.id)
                                                return@onPreviewKeyEvent true
                                            }
                                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BACK -> {
                                                if (!isRepeat) grabbedChannelId = null
                                                return@onPreviewKeyEvent true
                                            }
                                        }
                                    } else if (action == KeyEvent.ACTION_UP && (
                                                keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                                keyCode == KeyEvent.KEYCODE_ENTER ||
                                                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                                            )
                                    ) {
                                        return@onPreviewKeyEvent true
                                    }
                                    return@onPreviewKeyEvent false
                                }

                                // [2] NORMAL GEZİNME MODU:
                                // ► (Sağ yön tuşu): Kanalı anında "Tut & Taşı" moduna alır!
                                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                                    if (action == KeyEvent.ACTION_DOWN && !isRepeat) {
                                        grabbedChannelId = channel.id
                                    }
                                    return@onPreviewKeyEvent true
                                }

                                // OK Tuşu: Kısa basınca İzle, Uzun basıp bırakınca Favoriden Çıkar (Focus kaybolmadan!)
                                val isCenterKey = keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                        keyCode == KeyEvent.KEYCODE_ENTER ||
                                        keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
                                if (isCenterKey) {
                                    when (action) {
                                        KeyEvent.ACTION_DOWN -> {
                                            if (!isRepeat && !okDown) {
                                                okDown = true
                                                longPressArmed = false
                                                holdJob?.cancel()
                                                holdJob = scope.launch {
                                                    delay(450L)
                                                    if (okDown) {
                                                        longPressArmed = true
                                                    }
                                                }
                                            } else if (isRepeat && okDown && event.nativeKeyEvent.eventTime - event.nativeKeyEvent.downTime >= 420L) {
                                                longPressArmed = true
                                                holdJob?.cancel()
                                            }
                                            true
                                        }
                                        KeyEvent.ACTION_UP -> {
                                            holdJob?.cancel()
                                            if (okDown) {
                                                val shouldRemove = longPressArmed
                                                okDown = false
                                                longPressArmed = false
                                                if (shouldRemove) {
                                                    removeChannelSafely(index, channel.id)
                                                } else {
                                                    onPlayChannel(channel)
                                                }
                                            }
                                            true
                                        }
                                        else -> false
                                    }
                                } else {
                                    false
                                }
                            }
                            .focusable()
                            .clickable {
                                if (isGrabbed) grabbedChannelId = null else onPlayChannel(channel)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isGrabbed) TvThemeColors.FavoriteGold else TvThemeColors.FavoriteGold.copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isGrabbed) "📌 #${index + 1}" else "⭐ #${index + 1}",
                                color = if (isGrabbed) Color.Black else TvThemeColors.FavoriteGold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        ChannelBrandLogoBox(channel = channel, boxSize = 42.dp, fontSize = 10.sp)

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = channel.name,
                                    color = TvThemeColors.TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "(CH ${channel.number} • ${channel.category})",
                                    color = TvThemeColors.AccentCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                if (isBroken) {
                                    Text(
                                        text = "⚠️ ÇALIŞMIYOR",
                                        color = TvThemeColors.BrokenRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = when {
                                    isGrabbed -> "📌 TAŞINIYOR: ▲/▼ ile Sırayı Değiştirin • ►: 5 Sıra Yukarı • OK: Bırak • ◄: Favoriden Çıkar"
                                    rowFocused -> "OK: İzle  •  ► (Sağ Tuş): 📌 Tut ve Sırasını Değiştir  •  OK Basılı Tut: Favoriden Çıkar"
                                    else -> "Şu An: ${channel.currentProgramTitle} • ${channel.sources.size} Akış Kaynağı"
                                },
                                color = when {
                                    isGrabbed -> TvThemeColors.FavoriteGold
                                    rowFocused -> TvThemeColors.AccentCyan
                                    else -> TvThemeColors.TextSecondary
                                },
                                fontSize = 11.sp,
                                fontWeight = if (isGrabbed || rowFocused) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isGrabbed) TvThemeColors.FavoriteGold
                                    else if (rowFocused) TvThemeColors.BullRedPrimary.copy(alpha = 0.28f)
                                    else Color.White.copy(alpha = 0.06f)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isGrabbed) "▲/▼ TAŞI • OK BIRAK" else "► TUT & SIRALA",
                                color = if (isGrabbed) Color.Black else TvThemeColors.FavoriteGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun MultiViewSplitScreen(
    state: TvUiState,
    onToggleGridMode: () -> Unit,
    onChangeSlotChannel: (slotIndex: Int, delta: Int) -> Unit,
    onExpandSlotToFull: (TvChannel) -> Unit
) {
    val slotCount = if (state.isMultiViewFourGrid) 4 else 2
    val slots = remember(state.multiViewChannels, state.builtInChannels, slotCount) {
        val base = state.multiViewChannels.ifEmpty { state.builtInChannels.take(4) }
        List(slotCount) { idx ->
            base.getOrElse(idx) {
                state.builtInChannels.getOrElse(idx % state.builtInChannels.size.coerceAtLeast(1)) {
                    TvChannel("empty_$idx", idx + 1, "Kanal", "TV", 0xFF1E3A8AL, 0xFF3B82F6L, "Genel", sources = emptyList())
                }
            }
        }
    }

    var activeAudioSlot by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .padding(bottom = 6.dp)
        ) {
            Text(
                text = "Seçili ekranın sesi otomatik açılır • ▲/▼: Kanal Değiştir • OK: Tam Ekran",
                color = TvThemeColors.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1
            )
            TvCompactChipButton(
                text = if (state.isMultiViewFourGrid) "Görünüm: 4'lü (2x2)" else "Görünüm: 2'li Yan Yana",
                icon = Icons.Rounded.GridView,
                onClick = onToggleGridMode
            )
        }

        if (state.isMultiViewFourGrid) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    MultiViewPlayerCell(
                        slotIndex = 0, channel = slots[0], isAudioActive = activeAudioSlot == 0,
                        onFocused = { activeAudioSlot = 0 },
                        onZapSlot = { d -> onChangeSlotChannel(0, d) },
                        onExpand = { onExpandSlotToFull(slots[0]) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    MultiViewPlayerCell(
                        slotIndex = 1, channel = slots[1], isAudioActive = activeAudioSlot == 1,
                        onFocused = { activeAudioSlot = 1 },
                        onZapSlot = { d -> onChangeSlotChannel(1, d) },
                        onExpand = { onExpandSlotToFull(slots[1]) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    MultiViewPlayerCell(
                        slotIndex = 2, channel = slots[2], isAudioActive = activeAudioSlot == 2,
                        onFocused = { activeAudioSlot = 2 },
                        onZapSlot = { d -> onChangeSlotChannel(2, d) },
                        onExpand = { onExpandSlotToFull(slots[2]) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    MultiViewPlayerCell(
                        slotIndex = 3, channel = slots[3], isAudioActive = activeAudioSlot == 3,
                        onFocused = { activeAudioSlot = 3 },
                        onZapSlot = { d -> onChangeSlotChannel(3, d) },
                        onExpand = { onExpandSlotToFull(slots[3]) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                MultiViewPlayerCell(
                    slotIndex = 0, channel = slots[0], isAudioActive = activeAudioSlot == 0,
                    onFocused = { activeAudioSlot = 0 },
                    onZapSlot = { d -> onChangeSlotChannel(0, d) },
                    onExpand = { onExpandSlotToFull(slots[0]) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                MultiViewPlayerCell(
                    slotIndex = 1, channel = slots[1], isAudioActive = activeAudioSlot == 1,
                    onFocused = { activeAudioSlot = 1 },
                    onZapSlot = { d -> onChangeSlotChannel(1, d) },
                    onExpand = { onExpandSlotToFull(slots[1]) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
fun EpgTimelineGuideScreen(
    channels: List<TvChannel>,
    epgMap: Map<String, List<EpgProgram>>,
    favoriteIds: Set<String> = emptySet(),
    onPlayChannel: (TvChannel) -> Unit,
    onToggleFavorite: (String) -> Unit = {}
) {
    val now = remember { System.currentTimeMillis() }
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(channels, key = { it.id }) { channel ->
                val programs = epgMap[channel.epgChannelId].orEmpty()
                var rowFocused by remember { mutableStateOf(false) }
                val isFavorite = favoriteIds.contains(channel.id)
                val catColor = TvThemeColors.categoryColor(channel.category)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (rowFocused) TvThemeColors.SurfaceElevated else TvThemeColors.SurfaceCard)
                        .border(
                            width = if (rowFocused) 2.dp else 0.dp,
                            color = if (rowFocused) TvThemeColors.BullRedBright else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .onFocusChanged { rowFocused = it.isFocused }
                        .tvLongPressFavoriteModifier(
                            itemKey = "epg_${channel.id}",
                            onShortClick = { onPlayChannel(channel) },
                            onLongPress = { onToggleFavorite(channel.id) }
                        )
                        .focusable()
                        .clickable { onPlayChannel(channel) }
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .width(205.dp)
                            .padding(end = 10.dp)
                    ) {
                        ChannelBrandLogoBox(channel = channel, boxSize = 36.dp, fontSize = 9.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${channel.number}. ${channel.name}",
                                color = TvThemeColors.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (rowFocused) "OK Basılı Tut: ⭐ Favori" else "${channel.category} • ${channel.sources.size} Akış",
                                color = if (rowFocused) TvThemeColors.FavoriteGold else catColor,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = null,
                            tint = if (isFavorite) TvThemeColors.FavoriteGold else Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        programs.take(3).forEach { prog ->
                            val isLive = prog.isCurrentlyLive(now)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isLive) catColor.copy(alpha = 0.22f)
                                        else Color.Black.copy(alpha = 0.28f)
                                    )
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = prog.timeWindowText,
                                    color = if (isLive) TvThemeColors.AccentCyan else TvThemeColors.TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = prog.title,
                                    color = TvThemeColors.TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun M3uPlaylistManagerScreen(
    state: TvUiState,
    onAddRemotePlaylist: (name: String, m3uUrl: String, epgUrl: String) -> Unit,
    onAddXtreamAccount: (name: String, serverUrl: String, username: String, password: String) -> Unit,
    onPickLocalFile: () -> Unit,
    onScanDownloadFolder: () -> Unit,
    onUpdatePin: (String) -> Unit,
    onToggleAutoStart: () -> Unit = {},
    onBackupToDevice: () -> Unit = {},
    onRestoreFromDevice: () -> Unit = {},
    onRestoreHiddenChannels: () -> Unit = {},
    onRefreshQrServer: () -> Unit = {},
    onRemovePlaylist: (String) -> Unit,
    onRefreshAll: () -> Unit
) {
    var activeSubTab by remember { mutableIntStateOf(0) }

    var nameInput by remember { mutableStateOf("") }
    var m3uUrlInput by remember { mutableStateOf("") }
    var epgUrlInput by remember { mutableStateOf("") }

    var xtreamName by remember { mutableStateOf("") }
    var xtreamServer by remember { mutableStateOf("") }
    var xtreamUser by remember { mutableStateOf("") }
    var xtreamPass by remember { mutableStateOf("") }

    var newPinInput by remember { mutableStateOf(state.parentalPin) }

    // Kumanda Aşağı/Yukarı ok tuşlarının hiçbir sekmede takılmaması için açık FocusRequester zinciri
    val tab0FocusRequester = remember { FocusRequester() }
    val tab1FocusRequester = remember { FocusRequester() }
    val tab2FocusRequester = remember { FocusRequester() }
    val tab3FocusRequester = remember { FocusRequester() }

    val field1FocusRequester = remember { FocusRequester() }
    val field2FocusRequester = remember { FocusRequester() }
    val field3FocusRequester = remember { FocusRequester() }
    val field4FocusRequester = remember { FocusRequester() }
    val submitFocusRequester = remember { FocusRequester() }
    val bottomBackupFocusRequester = remember { FocusRequester() }

    val activeTabRequester = when (activeSubTab) {
        0 -> tab0FocusRequester
        1 -> tab1FocusRequester
        2 -> tab2FocusRequester
        else -> tab3FocusRequester
    }

    // M3U ekranı açıldığında QR web sunucusunun aktif olduğundan emin ol
    LaunchedEffect(Unit) {
        onRefreshQrServer()
    }

    val effectiveWebUrl = remember(state.localWebServerUrl) {
        state.localWebServerUrl.ifBlank {
            "http://${LocalTransferWebServer.getLocalWifiIpAddress()}:8585"
        }
    }

    fun focusFirstFieldOfCurrentSubTab() {
        try {
            field1FocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1.22f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(TvThemeColors.SurfaceCard)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvCompactChipButton(
                        text = if (activeSubTab == 0) "✓ Dosya & İçe Aktar" else "Dosya & İçe Aktar",
                        icon = Icons.Rounded.FolderOpen,
                        onClick = {
                            activeSubTab = 0
                            focusFirstFieldOfCurrentSubTab()
                        },
                        modifier = Modifier
                            .focusRequester(tab0FocusRequester)
                            .onPreviewKeyEvent { ev ->
                                if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN && ev.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                                    focusFirstFieldOfCurrentSubTab()
                                    true
                                } else false
                            }
                    )
                    TvCompactChipButton(
                        text = if (activeSubTab == 1) "✓ Xtream Codes API" else "Xtream Codes API",
                        icon = Icons.Rounded.Verified,
                        onClick = {
                            activeSubTab = 1
                            focusFirstFieldOfCurrentSubTab()
                        },
                        modifier = Modifier
                            .focusRequester(tab1FocusRequester)
                            .onPreviewKeyEvent { ev ->
                                if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN && ev.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                                    focusFirstFieldOfCurrentSubTab()
                                    true
                                } else false
                            }
                    )
                    TvCompactChipButton(
                        text = if (activeSubTab == 2) "✓ M3U URL" else "M3U URL",
                        icon = Icons.Rounded.CloudDownload,
                        onClick = {
                            activeSubTab = 2
                            focusFirstFieldOfCurrentSubTab()
                        },
                        modifier = Modifier
                            .focusRequester(tab2FocusRequester)
                            .onPreviewKeyEvent { ev ->
                                if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN && ev.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                                    focusFirstFieldOfCurrentSubTab()
                                    true
                                } else false
                            }
                    )
                    TvCompactChipButton(
                        text = if (activeSubTab == 3) "✓ Telefon QR & PIN" else "Telefon QR & PIN",
                        icon = Icons.Rounded.SettingsRemote,
                        onClick = {
                            activeSubTab = 3
                            onRefreshQrServer()
                            focusFirstFieldOfCurrentSubTab()
                        },
                        modifier = Modifier
                            .focusRequester(tab3FocusRequester)
                            .onPreviewKeyEvent { ev ->
                                if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN && ev.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                                    focusFirstFieldOfCurrentSubTab()
                                    true
                                } else false
                            }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (activeSubTab) {
                    0 -> {
                        Text(
                            text = "Cihaz Hafızasından (.m3u / .m3u8) Dosya Seç",
                            color = TvThemeColors.BullRedBright,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Eklediğiniz M3U dosyalarındaki kanallar sol menüdeki 'Özel M3U Kanalları' sekmesinde listelenir.",
                            color = TvThemeColors.TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TvActionButton(
                                text = "Dosyadan M3U Seç",
                                icon = Icons.Rounded.FolderOpen,
                                isPrimary = true,
                                onClick = onPickLocalFile,
                                modifier = Modifier
                                    .focusRequester(field1FocusRequester)
                                    .onPreviewKeyEvent { ev ->
                                        if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                            when (ev.nativeKeyEvent.keyCode) {
                                                KeyEvent.KEYCODE_DPAD_UP -> {
                                                    try { activeTabRequester.requestFocus() } catch (_: Exception) {}
                                                    true
                                                }
                                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                    try { bottomBackupFocusRequester.requestFocus() } catch (_: Exception) {}
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    }
                            )
                            TvActionButton(
                                text = "Download Klasörünü Tara",
                                icon = Icons.Rounded.ManageSearch,
                                isPrimary = false,
                                onClick = onScanDownloadFolder,
                                modifier = Modifier.onPreviewKeyEvent { ev ->
                                    if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                        when (ev.nativeKeyEvent.keyCode) {
                                            KeyEvent.KEYCODE_DPAD_UP -> {
                                                try { activeTabRequester.requestFocus() } catch (_: Exception) {}
                                                true
                                            }
                                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                try { bottomBackupFocusRequester.requestFocus() } catch (_: Exception) {}
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                            )
                        }
                    }

                    1 -> {
                        Text(
                            text = "Xtream Codes API Hesabı Bağla (▲/▼ ile Kutular Arası Geçin, OK ile Yazın)",
                            color = TvThemeColors.BullRedBright,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TvInputField(
                            label = "1. Paket Adı",
                            value = xtreamName,
                            placeholder = "Örn: VIP Xtream Hesabım",
                            onValueChange = { xtreamName = it },
                            focusRequester = field1FocusRequester,
                            upFocusRequester = activeTabRequester,
                            downFocusRequester = field2FocusRequester
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TvInputField(
                            label = "2. Sunucu Adresi (http://sunucu:port)",
                            value = xtreamServer,
                            placeholder = "http://ornek-iptv.com:8080",
                            onValueChange = { xtreamServer = it },
                            focusRequester = field2FocusRequester,
                            upFocusRequester = field1FocusRequester,
                            downFocusRequester = field3FocusRequester,
                            showUrlQuickChips = true
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                TvInputField(
                                    label = "3. Kullanıcı Adı",
                                    value = xtreamUser,
                                    placeholder = "kullanici_adi",
                                    onValueChange = { xtreamUser = it },
                                    focusRequester = field3FocusRequester,
                                    upFocusRequester = field2FocusRequester,
                                    downFocusRequester = submitFocusRequester
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                TvInputField(
                                    label = "4. Şifre",
                                    value = xtreamPass,
                                    placeholder = "sifre",
                                    onValueChange = { xtreamPass = it },
                                    focusRequester = field4FocusRequester,
                                    upFocusRequester = field2FocusRequester,
                                    downFocusRequester = submitFocusRequester
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TvActionButton(
                            text = "⚡ Xtream Hesabını Bağla ve Kanalları Yükle",
                            icon = Icons.Rounded.Verified,
                            isPrimary = true,
                            onClick = {
                                if (xtreamServer.isNotBlank() && xtreamUser.isNotBlank()) {
                                    onAddXtreamAccount(xtreamName, xtreamServer, xtreamUser, xtreamPass)
                                    xtreamName = ""; xtreamServer = ""; xtreamUser = ""; xtreamPass = ""
                                }
                            },
                            modifier = Modifier
                                .focusRequester(submitFocusRequester)
                                .onPreviewKeyEvent { ev ->
                                    if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                        when (ev.nativeKeyEvent.keyCode) {
                                            KeyEvent.KEYCODE_DPAD_UP -> {
                                                try { field3FocusRequester.requestFocus() } catch (_: Exception) {}
                                                true
                                            }
                                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                try { bottomBackupFocusRequester.requestFocus() } catch (_: Exception) {}
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                        )
                    }

                    2 -> {
                        Text(
                            text = "Uzaktan M3U / M3U8 URL Ekle (▲/▼ ile Kutular Arası Geçin, OK ile Yazın)",
                            color = TvThemeColors.BullRedBright,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TvInputField(
                            label = "1. Liste Adı (İsteğe Bağlı)",
                            value = nameInput,
                            placeholder = "Örn: Ev IPTV Listem (Boş bırakabilirsiniz)",
                            onValueChange = { nameInput = it },
                            focusRequester = field1FocusRequester,
                            upFocusRequester = activeTabRequester,
                            downFocusRequester = field2FocusRequester
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TvInputField(
                            label = "2. M3U / M3U8 URL Adresi (OK Tuşuyla Yazın veya Panodan Yapıştırın)",
                            value = m3uUrlInput,
                            placeholder = "https://ornek.com/playlist.m3u8",
                            onValueChange = { m3uUrlInput = it },
                            focusRequester = field2FocusRequester,
                            upFocusRequester = field1FocusRequester,
                            downFocusRequester = field3FocusRequester,
                            showUrlQuickChips = true
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TvInputField(
                            label = "3. XMLTV EPG URL (İsteğe Bağlı)",
                            value = epgUrlInput,
                            placeholder = "https://ornek.com/epg.xml",
                            onValueChange = { epgUrlInput = it },
                            focusRequester = field3FocusRequester,
                            upFocusRequester = field2FocusRequester,
                            downFocusRequester = submitFocusRequester,
                            showUrlQuickChips = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TvActionButton(
                            text = "🚀 M3U URL Kaydet ve Kanalları Yükle",
                            icon = Icons.Rounded.CloudDownload,
                            isPrimary = true,
                            onClick = {
                                if (m3uUrlInput.isNotBlank()) {
                                    onAddRemotePlaylist(nameInput, m3uUrlInput, epgUrlInput)
                                    nameInput = ""; m3uUrlInput = ""; epgUrlInput = ""
                                }
                            },
                            modifier = Modifier
                                .focusRequester(submitFocusRequester)
                                .onPreviewKeyEvent { ev ->
                                    if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                        when (ev.nativeKeyEvent.keyCode) {
                                            KeyEvent.KEYCODE_DPAD_UP -> {
                                                try { field3FocusRequester.requestFocus() } catch (_: Exception) {}
                                                true
                                            }
                                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                try { bottomBackupFocusRequester.requestFocus() } catch (_: Exception) {}
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                        )
                    }

                    3 -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            QrMatrixVisualBox(seedText = effectiveWebUrl)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Telefondan Kablosuz M3U & Xtream Gönder",
                                    color = TvThemeColors.BullRedBright,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Telefon kameranızla QR kodu okutun veya tarayıcıdan şu adrese girin:",
                                    color = TvThemeColors.TextSecondary,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = effectiveWebUrl,
                                    color = TvThemeColors.AccentCyan,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                TvCompactChipButton(
                                    text = "🔄 QR & Sunucuyu Yenile",
                                    icon = Icons.Rounded.Refresh,
                                    onClick = onRefreshQrServer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.width(210.dp)) {
                                TvInputField(
                                    label = "4 Haneli Ebeveyn PIN Kodu",
                                    value = newPinInput,
                                    placeholder = "0000",
                                    onValueChange = { if (it.length <= 4) newPinInput = it },
                                    focusRequester = field1FocusRequester,
                                    upFocusRequester = activeTabRequester,
                                    downFocusRequester = submitFocusRequester
                                )
                            }
                            TvActionButton(
                                text = "PIN Güncelle",
                                icon = Icons.Rounded.Verified,
                                isPrimary = false,
                                onClick = { onUpdatePin(newPinInput) },
                                modifier = Modifier
                                    .focusRequester(submitFocusRequester)
                                    .onPreviewKeyEvent { ev ->
                                        if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                            when (ev.nativeKeyEvent.keyCode) {
                                                KeyEvent.KEYCODE_DPAD_UP -> {
                                                    try { field1FocusRequester.requestFocus() } catch (_: Exception) {}
                                                    true
                                                }
                                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                    try { bottomBackupFocusRequester.requestFocus() } catch (_: Exception) {}
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    }
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvCompactChipButton(
                        text = "💾 Yedek Al (Download)",
                        icon = Icons.Rounded.CloudDownload,
                        onClick = onBackupToDevice,
                        modifier = Modifier.focusRequester(bottomBackupFocusRequester)
                    )
                    TvCompactChipButton(
                        text = "♻️ Yedekten Geri Yükle",
                        icon = Icons.Rounded.Refresh,
                        onClick = onRestoreFromDevice
                    )
                    TvCompactChipButton(
                        text = if (state.autoStartLastChannel) "⚡ Son Kanaldan Başla: AÇIK" else "⚡ Son Kanaldan Başla: KAPALI",
                        icon = Icons.Rounded.Bolt,
                        onClick = onToggleAutoStart
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvActionButton(
                        text = if (state.isLoadingM3u) "Yükleniyor..." else "Tüm Kaynakları Yenile",
                        icon = Icons.Rounded.Refresh,
                        isPrimary = false,
                        onClick = onRefreshAll
                    )
                    if (state.hiddenChannelIds.isNotEmpty()) {
                        TvActionButton(
                            text = "Gizlenen Kanalları Geri Yükle (${state.hiddenChannelIds.size})",
                            icon = Icons.Rounded.CheckCircle,
                            isPrimary = false,
                            onClick = onRestoreHiddenChannels
                        )
                    }
                }
            }
        }

        // SAĞ PANEL: HER ZAMAN GÖRÜNÜR GERÇEK QR KOD KARTI + EKLENEN M3U LİSTELERİ
        Column(
            modifier = Modifier
                .weight(0.82f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(TvThemeColors.SurfaceCard)
                .padding(14.dp)
        ) {
            // Sabit QR ve Telefon Bağlantı Kartı (Hangi sekmede olursanız olun her zaman görünür!)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TvThemeColors.SurfaceElevated)
                    .border(1.5.dp, TvThemeColors.BullRedBright.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                    .padding(10.dp)
            ) {
                QrMatrixVisualBox(seedText = effectiveWebUrl)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📱 Telefondan QR ile M3U Yükle",
                        color = TvThemeColors.BullRedBright,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Text(
                        text = "Kamerayla QR okutun veya telefondan girin:",
                        color = TvThemeColors.TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = effectiveWebUrl,
                        color = TvThemeColors.AccentCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    TvCompactChipButton(
                        text = "🔄 QR Yenile",
                        icon = Icons.Rounded.QrCode2,
                        onClick = onRefreshQrServer
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Eklenen M3U Listeleri (${state.playlists.size})",
                color = TvThemeColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (state.playlists.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Henüz özel liste eklenmedi.\nÜstteki QR kodu telefonunuzla okutarak veya soldan URL/Dosya seçerek hemen ekleyebilirsiniz.",
                        color = TvThemeColors.TextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.playlists, key = { it.id }) { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.28f))
                                .padding(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    color = TvThemeColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (item.isLocalFile) "Yerel Dosya" else item.locationOrUrl,
                                    color = TvThemeColors.TextSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            TvCompactChipButton(
                                text = "Sil",
                                icon = Icons.Rounded.DeleteOutline,
                                onClick = { onRemovePlaylist(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
