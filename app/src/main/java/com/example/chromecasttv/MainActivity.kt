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

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun createTvExoPlayer(context: Context, forceMaxBitrate: Boolean = true): ExoPlayer {
    val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            12_000,
            32_000,
            700,
            1_300
        )
        .setBackBuffer(35_000, true)
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    val httpFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(4_500)
        .setReadTimeoutMs(5_500)

    val mediaSourceFactory = DefaultMediaSourceFactory(context)
        .setDataSourceFactory(httpFactory)

    return ExoPlayer.Builder(context)
        .setLoadControl(loadControl)
        .setMediaSourceFactory(mediaSourceFactory)
        .build()
        .apply {
            setWakeMode(C.WAKE_MODE_NETWORK)
            trackSelectionParameters = trackSelectionParameters
                .buildUpon()
                .setForceHighestSupportedBitrate(forceMaxBitrate)
                .setMaxVideoBitrate(Int.MAX_VALUE)
                .build()
        }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun buildStreamMediaSource(context: Context, source: StreamSource): MediaSource {
    val httpFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(4_500)
        .setReadTimeoutMs(5_500)

    return DefaultMediaSourceFactory(context)
        .setDataSourceFactory(httpFactory)
        .createMediaSource(MediaItem.fromUri(Uri.parse(source.url)))
}

object TvThemeColors {
    val BackgroundDeep = Color(0xFF070910)
    val SidebarGlass = Color(0xFF0C101B)
    val SurfaceCard = Color(0xFF121828)
    val SurfaceElevated = Color(0xFF1B243B)
    val GlassOverlay = Color(0xF2080B14)
    val BullRedPrimary = Color(0xFFE11D48)
    val BullRedBright = Color(0xFFFF1744)
    val BullRedDark = Color(0xFF881337)
    val AccentCyan = Color(0xFF00F0FF)
    val AccentIndigo = Color(0xFF6366F1)
    val AccentEmerald = Color(0xFF10B981)
    val AccentRose = Color(0xFFF43F5E)
    val AccentAmber = Color(0xFFF59E0B)
    val AccentPurple = Color(0xFFA855F7)
    val FavoriteGold = Color(0xFFFFD54F)
    val LiveRed = Color(0xFFFF1744)
    val BrokenRed = Color(0xFFDC2626)
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)

    fun categoryColor(category: String): Color {
        val lower = category.lowercase(Locale.getDefault())
        return when {
            lower.contains("spor") -> AccentEmerald
            lower.contains("haber") -> AccentRose
            lower.contains("belgesel") -> AccentCyan
            lower.contains("sinema") || lower.contains("film") -> AccentPurple
            lower.contains("müzik") || lower.contains("muzik") -> AccentAmber
            lower.contains("çocuk") || lower.contains("cocuk") -> Color(0xFFEC4899)
            lower.contains("ulusal") -> BullRedPrimary
            lower.contains("dünya") -> Color(0xFF38BDF8)
            lower.contains("yerel") -> Color(0xFF14B8A6)
            lower.contains("kıbrıs") -> Color(0xFFF97316)
            lower.contains("dini") -> Color(0xFF84CC16)
            else -> BullRedPrimary
        }
    }

    fun categoryIcon(category: String): ImageVector {
        val lower = category.lowercase(Locale.getDefault())
        return when {
            lower == "tümü" -> Icons.Rounded.GridView
            lower.contains("ulusal") -> Icons.Rounded.Tv
            lower.contains("haber") -> Icons.Rounded.Newspaper
            lower.contains("spor") -> Icons.Rounded.SportsSoccer
            lower.contains("belgesel") -> Icons.Rounded.Public
            lower.contains("müzik") || lower.contains("muzik") -> Icons.Rounded.MusicNote
            lower.contains("sinema") || lower.contains("film") -> Icons.Rounded.Movie
            lower.contains("çocuk") || lower.contains("cocuk") -> Icons.Rounded.ChildCare
            lower.contains("dünya") -> Icons.Rounded.Language
            lower.contains("yerel") -> Icons.Rounded.Public
            lower.contains("kıbrıs") -> Icons.Rounded.Public
            lower.contains("dini") -> Icons.Rounded.History
            else -> Icons.Rounded.LiveTv
        }
    }
}

// ============================================================================
// YENİ TASARIM 3B KIRMIZI BOĞA (RED BULL) ARMASI (SIFIR GECİKMELİ STATİK ÇİZİM)
// ============================================================================
@Composable
fun RedBullEmblemBox(
    boxSize: Dp = 44.dp
) {
    Box(
        modifier = Modifier
            .size(boxSize)
            .clip(RoundedCornerShape(boxSize * 0.24f))
            .background(Color(0xFF0A0407))
            .border(1.5.dp, TvThemeColors.BullRedBright, RoundedCornerShape(boxSize * 0.24f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = size.minDimension / 108f
            drawPath(Path().apply { moveTo(s * 54.0f, s * 6.0f); cubicTo(s * 82.0f, s * 6.0f, s * 102.0f, s * 26.0f, s * 102.0f, s * 54.0f); cubicTo(s * 102.0f, s * 82.0f, s * 82.0f, s * 102.0f, s * 54.0f, s * 102.0f); cubicTo(s * 26.0f, s * 102.0f, s * 6.0f, s * 82.0f, s * 6.0f, s * 54.0f); cubicTo(s * 6.0f, s * 26.0f, s * 26.0f, s * 6.0f, s * 54.0f, s * 6.0f); close() }, color = Color(0xFF380612L))
            drawPath(Path().apply { moveTo(s * 54.0f, s * 10.0f); cubicTo(s * 79.0f, s * 10.0f, s * 98.0f, s * 29.0f, s * 98.0f, s * 54.0f); cubicTo(s * 98.0f, s * 79.0f, s * 79.0f, s * 98.0f, s * 54.0f, s * 98.0f); cubicTo(s * 29.0f, s * 98.0f, s * 10.0f, s * 79.0f, s * 10.0f, s * 54.0f); cubicTo(s * 10.0f, s * 29.0f, s * 29.0f, s * 10.0f, s * 54.0f, s * 10.0f); close() }, color = Color(0xFF160308L))
            drawPath(Path().apply { moveTo(s * 54.0f, s * 18.0f); cubicTo(s * 74.0f, s * 18.0f, s * 90.0f, s * 34.0f, s * 90.0f, s * 54.0f); cubicTo(s * 90.0f, s * 74.0f, s * 74.0f, s * 90.0f, s * 54.0f, s * 90.0f); cubicTo(s * 34.0f, s * 90.0f, s * 18.0f, s * 74.0f, s * 18.0f, s * 54.0f); cubicTo(s * 18.0f, s * 34.0f, s * 34.0f, s * 18.0f, s * 54.0f, s * 18.0f); close() }, color = Color(0xFFFFB300L))
            drawPath(Path().apply { moveTo(s * 54.0f, s * 21.0f); cubicTo(s * 72.0f, s * 21.0f, s * 87.0f, s * 36.0f, s * 87.0f, s * 54.0f); cubicTo(s * 87.0f, s * 72.0f, s * 72.0f, s * 87.0f, s * 54.0f, s * 87.0f); cubicTo(s * 36.0f, s * 87.0f, s * 21.0f, s * 72.0f, s * 21.0f, s * 54.0f); cubicTo(s * 21.0f, s * 36.0f, s * 36.0f, s * 21.0f, s * 54.0f, s * 21.0f); close() }, color = Color(0xFF1E040AL))
            drawPath(Path().apply { moveTo(s * 38.0f, s * 34.0f); cubicTo(s * 24.0f, s * 33.0f, s * 14.0f, s * 24.0f, s * 10.0f, s * 9.0f); cubicTo(s * 7.0f, s * 22.0f, s * 14.0f, s * 38.0f, s * 32.0f, s * 43.0f); close() }, color = Color(0xFFFF1744L))
            drawPath(Path().apply { moveTo(s * 38.0f, s * 34.0f); cubicTo(s * 25.0f, s * 33.0f, s * 16.0f, s * 25.0f, s * 10.0f, s * 9.0f); cubicTo(s * 12.0f, s * 20.0f, s * 20.0f, s * 31.0f, s * 35.0f, s * 38.0f); close() }, color = Color(0xFFFF5252L))
            drawPath(Path().apply { moveTo(s * 70.0f, s * 34.0f); cubicTo(s * 84.0f, s * 33.0f, s * 94.0f, s * 24.0f, s * 98.0f, s * 9.0f); cubicTo(s * 101.0f, s * 22.0f, s * 94.0f, s * 38.0f, s * 76.0f, s * 43.0f); close() }, color = Color(0xFFFF1744L))
            drawPath(Path().apply { moveTo(s * 70.0f, s * 34.0f); cubicTo(s * 83.0f, s * 33.0f, s * 92.0f, s * 25.0f, s * 98.0f, s * 9.0f); cubicTo(s * 96.0f, s * 20.0f, s * 88.0f, s * 31.0f, s * 73.0f, s * 38.0f); close() }, color = Color(0xFFFF5252L))
            drawPath(Path().apply { moveTo(s * 32.0f, s * 43.0f); lineTo(s * 11.0f, s * 47.0f); lineTo(s * 20.0f, s * 56.0f); lineTo(s * 34.0f, s * 52.0f); close() }, color = Color(0xFFB71C1CL))
            drawPath(Path().apply { moveTo(s * 76.0f, s * 43.0f); lineTo(s * 97.0f, s * 47.0f); lineTo(s * 88.0f, s * 56.0f); lineTo(s * 74.0f, s * 52.0f); close() }, color = Color(0xFFB71C1CL))
            drawPath(Path().apply { moveTo(s * 42.0f, s * 80.0f); cubicTo(s * 35.0f, s * 86.0f, s * 39.0f, s * 101.0f, s * 54.0f, s * 101.0f); cubicTo(s * 69.0f, s * 101.0f, s * 73.0f, s * 86.0f, s * 66.0f, s * 80.0f); lineTo(s * 62.0f, s * 82.0f); cubicTo(s * 66.0f, s * 86.0f, s * 64.0f, s * 95.0f, s * 54.0f, s * 95.0f); cubicTo(s * 44.0f, s * 95.0f, s * 42.0f, s * 86.0f, s * 46.0f, s * 82.0f); close() }, color = Color(0xFFFFD54FL))
            drawPath(Path().apply { moveTo(s * 38.0f, s * 31.0f); lineTo(s * 54.0f, s * 26.0f); lineTo(s * 70.0f, s * 31.0f); lineTo(s * 76.0f, s * 44.0f); lineTo(s * 79.0f, s * 57.0f); lineTo(s * 71.0f, s * 69.0f); lineTo(s * 67.0f, s * 84.0f); lineTo(s * 54.0f, s * 90.0f); lineTo(s * 41.0f, s * 84.0f); lineTo(s * 37.0f, s * 69.0f); lineTo(s * 29.0f, s * 57.0f); lineTo(s * 32.0f, s * 44.0f); close() }, color = Color(0xFFC60021L))
            drawPath(Path().apply { moveTo(s * 40.0f, s * 32.0f); lineTo(s * 54.0f, s * 28.0f); lineTo(s * 68.0f, s * 32.0f); lineTo(s * 73.0f, s * 45.0f); lineTo(s * 62.0f, s * 55.0f); lineTo(s * 63.0f, s * 75.0f); lineTo(s * 54.0f, s * 84.0f); lineTo(s * 45.0f, s * 75.0f); lineTo(s * 46.0f, s * 55.0f); lineTo(s * 35.0f, s * 45.0f); close() }, color = Color(0xFFFF1744L))
            drawPath(Path().apply { moveTo(s * 31.0f, s * 56.0f); lineTo(s * 43.0f, s * 60.0f); lineTo(s * 43.0f, s * 78.0f); lineTo(s * 38.0f, s * 68.0f); close() }, color = Color(0xFFE60026L))
            drawPath(Path().apply { moveTo(s * 77.0f, s * 56.0f); lineTo(s * 65.0f, s * 60.0f); lineTo(s * 65.0f, s * 78.0f); lineTo(s * 70.0f, s * 68.0f); close() }, color = Color(0xFFE60026L))
            drawPath(Path().apply { moveTo(s * 47.0f, s * 33.0f); lineTo(s * 54.0f, s * 47.0f); lineTo(s * 61.0f, s * 33.0f); lineTo(s * 54.0f, s * 37.0f); close() }, color = Color(0xFF4A0410L))
            drawPath(Path().apply { moveTo(s * 33.0f, s * 49.0f); lineTo(s * 49.0f, s * 57.0f); lineTo(s * 47.0f, s * 63.0f); lineTo(s * 32.0f, s * 56.0f); close() }, color = Color(0xFF3A020BL))
            drawPath(Path().apply { moveTo(s * 75.0f, s * 49.0f); lineTo(s * 59.0f, s * 57.0f); lineTo(s * 61.0f, s * 63.0f); lineTo(s * 76.0f, s * 56.0f); close() }, color = Color(0xFF3A020BL))
            drawPath(Path().apply { moveTo(s * 35.0f, s * 51.0f); lineTo(s * 47.0f, s * 57.0f); lineTo(s * 45.0f, s * 61.0f); lineTo(s * 35.0f, s * 56.0f); close() }, color = Color(0xFFFFFFFFL))
            drawPath(Path().apply { moveTo(s * 73.0f, s * 51.0f); lineTo(s * 61.0f, s * 57.0f); lineTo(s * 63.0f, s * 61.0f); lineTo(s * 73.0f, s * 56.0f); close() }, color = Color(0xFFFFFFFFL))
            drawPath(Path().apply { moveTo(s * 39.0f, s * 53.0f); lineTo(s * 46.0f, s * 57.0f); lineTo(s * 44.0f, s * 60.0f); lineTo(s * 38.0f, s * 56.0f); close() }, color = Color(0xFFFFD54FL))
            drawPath(Path().apply { moveTo(s * 69.0f, s * 53.0f); lineTo(s * 62.0f, s * 57.0f); lineTo(s * 64.0f, s * 60.0f); lineTo(s * 70.0f, s * 56.0f); close() }, color = Color(0xFFFFD54FL))
            drawPath(Path().apply { moveTo(s * 43.0f, s * 73.0f); lineTo(s * 54.0f, s * 69.0f); lineTo(s * 65.0f, s * 73.0f); lineTo(s * 67.0f, s * 84.0f); lineTo(s * 54.0f, s * 90.0f); lineTo(s * 41.0f, s * 84.0f); close() }, color = Color(0xFF88091EL))
            drawPath(Path().apply { moveTo(s * 46.0f, s * 74.0f); lineTo(s * 54.0f, s * 71.0f); lineTo(s * 62.0f, s * 74.0f); lineTo(s * 64.0f, s * 83.0f); lineTo(s * 54.0f, s * 88.0f); lineTo(s * 44.0f, s * 83.0f); close() }, color = Color(0xFFFF1744L))
            drawPath(Path().apply { moveTo(s * 45.0f, s * 77.0f); lineTo(s * 50.0f, s * 79.0f); lineTo(s * 49.0f, s * 84.0f); lineTo(s * 44.0f, s * 82.0f); close() }, color = Color(0xFF1A0307L))
            drawPath(Path().apply { moveTo(s * 63.0f, s * 77.0f); lineTo(s * 58.0f, s * 79.0f); lineTo(s * 59.0f, s * 84.0f); lineTo(s * 64.0f, s * 82.0f); close() }, color = Color(0xFF1A0307L))
        }
    }
}

enum class StreamType { HLS, DASH, PROGRESSIVE }

@Immutable
data class StreamSource(
    val label: String,
    val url: String,
    val type: StreamType = StreamType.HLS
)

@Immutable
data class TvChannel(
    val id: String,
    val number: Int,
    val name: String,
    val shortLogoText: String,
    val brandPrimaryColor: Long,
    val brandSecondaryColor: Long,
    val category: String,
    val logoUrl: String = "",
    val sources: List<StreamSource>,
    val qualityBadge: String = "FHD",
    val epgChannelId: String = id,
    val sourcePlaylistName: String = "RedoX Yerleşik Paket",
    val isCustomM3u: Boolean = false,
    val isBroken: Boolean = false,
    val currentProgramTitle: String = "Canlı Yayın Kuşağı",
    val currentProgramTime: String = "Canlı",
    val nextProgramTitle: String = "Ana Kuşak",
    val currentProgress: Float = 0.45f
) {
    val primaryStreamUrl: String
        get() = sources.firstOrNull()?.url ?: ""
}

@Immutable
data class EpgProgram(
    val channelId: String,
    val title: String,
    val description: String,
    val category: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val timeWindowText: String
) {
    fun isCurrentlyLive(nowMillis: Long): Boolean = nowMillis in startTimeMillis..endTimeMillis

    fun progressFraction(nowMillis: Long): Float {
        val total = (endTimeMillis - startTimeMillis).coerceAtLeast(1L)
        val elapsed = (nowMillis - startTimeMillis).coerceIn(0L, total)
        return elapsed.toFloat() / total.toFloat()
    }
}

@Immutable
data class M3uPlaylistSource(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val locationOrUrl: String,
    val epgXmlUrl: String = "",
    val isLocalFile: Boolean = false
)

@Immutable
data class TrackOption(
    val label: String,
    val groupIndex: Int,
    val trackIndex: Int,
    val isSelected: Boolean
)

enum class MainNavTab(val title: String, val subtitle: String, val icon: ImageVector) {
    ALL_CHANNELS("Canlı TV", "299 Kanal • 1184 Yedek Akış", Icons.Rounded.LiveTv),
    FAVORITES("Favorilerim", "Sıralanabilir Liste", Icons.Rounded.Star),
    CUSTOM_M3U("Özel M3U Kanalları", "Eklenen Listelerim", Icons.Rounded.FolderOpen),
    MULTI_VIEW("Bölünmüş Ekran", "2'li & 4'lü Canlı İzle", Icons.Rounded.GridView),
    EPG_GUIDE("Yayın Rehberi", "Canlı EPG Akışı", Icons.Rounded.CalendarMonth),
    SEARCH("Kanal Ara", "İsim veya Numara", Icons.Rounded.Search),
    M3U_MANAGER("M3U & Dosya Yükle", "Dosya, Xtream & QR", Icons.Rounded.UploadFile)
}

enum class PlayerScaleMode(val label: String, val resizeMode: Int) {
    FIT("16:9 Orijinal", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Ekranı Doldur", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("Sinema Zoom", AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
}

@Composable
fun ChannelBrandLogoBox(
    channel: TvChannel,
    boxSize: Dp = 44.dp,
    fontSize: TextUnit = 12.sp
) {
    val context = LocalContext.current
    val primary = Color(channel.brandPrimaryColor)
    val secondary = Color(channel.brandSecondaryColor)
    val hasLogoUrl = channel.logoUrl.isNotBlank()
    var logoLoadFailed by remember(channel.logoUrl) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(boxSize)
            .clip(RoundedCornerShape(boxSize * 0.25f))
            .background(
                if (hasLogoUrl && !logoLoadFailed) {
                    Brush.linearGradient(listOf(Color(0xFF161F33), Color(0xFF0E1525)))
                } else {
                    Brush.linearGradient(listOf(primary, secondary))
                }
            )
            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(boxSize * 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        if (!hasLogoUrl || logoLoadFailed) {
            Text(
                text = channel.shortLogoText,
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = (fontSize.value + 1f).sp,
                maxLines = 2,
                overflow = TextOverflow.Clip,
                modifier = Modifier.padding(2.dp)
            )
        } else {
            val req = remember(channel.logoUrl) {
                ImageRequest.Builder(context)
                    .data(channel.logoUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/125.0.0.0")
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = req,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                onError = { logoLoadFailed = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            )
        }
    }
}

object LocalTransferWebServer {
    fun getLocalWifiIpAddress(): String {
        var fallbackIp: String? = null
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces != null && interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val name = iface.name.lowercase(Locale.getDefault())
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue
                        if (name.startsWith("wlan") || name.startsWith("eth") || name.startsWith("en")) {
                            if (ip.startsWith("192.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                                return ip
                            }
                        }
                        if (ip.startsWith("192.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            if (fallbackIp == null) fallbackIp = ip
                        } else if (fallbackIp == null && !ip.startsWith("169.254.")) {
                            fallbackIp = ip
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return fallbackIp ?: "192.168.1.1"
    }

    fun buildMobileWebHtml(): String = """
        <!DOCTYPE html>
        <html lang="tr">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>RedoX TV - Telefondan Kanal Gönder</title>
            <style>
                body { background:#070910; color:#F8FAFC; font-family:sans-serif; padding:18px; margin:0; }
                .card { background:#121828; border:1px solid #FF174455; border-radius:16px; padding:18px; margin-bottom:16px; }
                h2 { color:#FF1744; margin-top:0; font-size:18px; }
                label { display:block; font-size:13px; color:#94A3B8; margin-bottom:6px; margin-top:12px; }
                input, textarea { width:100%; box-sizing:border-box; background:#070910; color:#fff; border:1px solid #334155; border-radius:10px; padding:12px; font-size:14px; }
                button { width:100%; background:linear-gradient(90deg,#FF1744,#BE123C); color:#fff; font-weight:bold; border:none; border-radius:10px; padding:14px; font-size:15px; margin-top:14px; cursor:pointer; }
            </style>
        </head>
        <body>
            <div class="card">
                <h2>🐂 RedoX TV Uzaktan Yükleyici</h2>
                <form method="POST" action="/add_m3u">
                    <label>Liste Adı</label>
                    <input name="name" placeholder="Örn: Telefon IPTV Listem" required />
                    <label>M3U / M3U8 URL Linki</label>
                    <input name="url" placeholder="https://ornek.com/playlist.m3u8" />
                    <label>Veya M3U Dosya İçeriğini (#EXTM3U...) Yapıştırın</label>
                    <textarea name="raw_content" rows="4" placeholder="#EXTM3U&#10;#EXTINF:-1,Kanal Adı&#10;https://..."></textarea>
                    <button type="submit">🚀 RedoX TV'ye Gönder</button>
                </form>
            </div>
            <div class="card">
                <h2>🔑 Xtream Codes API Gönder</h2>
                <form method="POST" action="/add_xtream">
                    <label>Paket Adı</label>
                    <input name="name" placeholder="Xtream Paketi" />
                    <label>Sunucu Adresi (http://sunucu:port)</label>
                    <input name="server" placeholder="http://ornek.com:8080" required />
                    <label>Kullanıcı Adı</label>
                    <input name="username" placeholder="kullanici_adi" required />
                    <label>Şifre</label>
                    <input name="password" type="password" placeholder="sifre" required />
                    <button type="submit">⚡ Xtream Hesabını TV'ye Bağla</button>
                </form>
            </div>
            <div class="card">
                <h2>💾 Favori &amp; M3U Yedekleme / Geri Yükleme</h2>
                <p style="font-size:13px;color:#94A3B8;margin-top:0;">Sıralı favorilerinizi, kanal kaynak hafızasını ve M3U listelerinizi telefonunuza yedekleyin veya geri yükleyin.</p>
                <a href="/backup" style="display:block;text-align:center;text-decoration:none;background:#10B981;color:#fff;font-weight:bold;border-radius:10px;padding:13px;font-size:15px;margin-bottom:14px;">⬇️ Yedeği Telefona İndir (redox_backup.txt)</a>
                <form method="POST" action="/restore_backup">
                    <label>Yedek İçeriğini Buraya Yapıştırıp Geri Yükleyin</label>
                    <textarea name="backup_data" rows="4" placeholder="REDOX_BACKUP_V9..." required></textarea>
                    <button type="submit">♻️ Yedeği TV'ye Geri Yükle</button>
                </form>
            </div>
        </body>
        </html>
    """.trimIndent()
}

object M3uAndEpgParser {
    private val attrRegex = Regex("([a-zA-Z0-9_-]+)=\"([^\"]*)\"")

    fun buildXtreamUrls(serverUrl: String, username: String, password: String): Pair<String, String> {
        val cleanBase = serverUrl.trim().trimEnd('/')
        val prefix = if (cleanBase.startsWith("http://") || cleanBase.startsWith("https://")) cleanBase else "http://$cleanBase"
        val m3uUrl = "$prefix/get.php?username=${Uri.encode(username.trim())}&password=${Uri.encode(password.trim())}&type=m3u_plus&output=ts"
        val epgUrl = "$prefix/xmltv.php?username=${Uri.encode(username.trim())}&password=${Uri.encode(password.trim())}"
        return m3uUrl to epgUrl
    }

    fun makeShortLogoText(name: String): String {
        val clean = name.replace(Regex("(?i)\\b(HD|FHD|4K|UHD|TV|TR|TÜRKİYE|TURKIYE|CANLI)\\b"), "").trim()
        val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            words.isEmpty() -> name.take(4).uppercase()
            words.size == 1 -> words[0].take(6).uppercase()
            else -> (words[0].take(5) + "\n" + words[1].take(5)).uppercase()
        }
    }

    fun parseM3uContent(
        rawM3u: String,
        sourceName: String,
        startingChannelNumber: Int = 1,
        isCustom: Boolean = true
    ): List<TvChannel> {
        val channels = mutableListOf<TvChannel>()
        val lines = rawM3u.lines()

        var currentTitle = ""
        var currentLogo = ""
        var currentGroup = "Özel Liste"
        var currentTvgId = ""
        var channelCounter = startingChannelNumber

        val palette = listOf(
            0xFF1E3A8AL to 0xFF3B82F6L,
            0xFF991B1BL to 0xFFEF4444L,
            0xFF065F46L to 0xFF10B981L,
            0xFF5B21B6L to 0xFF8B5CF6L,
            0xFF9A3412L to 0xFFF97316L,
            0xFF155E75L to 0xFF06B6D4L
        )

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                val attrs = mutableMapOf<String, String>()
                attrRegex.findAll(line).forEach { match ->
                    attrs[match.groupValues[1].lowercase()] = match.groupValues[2]
                }
                currentTvgId = attrs["tvg-id"] ?: attrs["tvg-name"] ?: ""
                currentLogo = attrs["tvg-logo"] ?: ""
                currentGroup = normalizeCategory(attrs["group-title"] ?: sourceName)

                val commaIndex = line.lastIndexOf(',')
                currentTitle = if (commaIndex != -1 && commaIndex + 1 < line.length) {
                    line.substring(commaIndex + 1).trim()
                } else {
                    attrs["tvg-name"] ?: "Kanal $channelCounter"
                }
            } else if (!line.startsWith("#") && (
                        line.startsWith("http://") ||
                        line.startsWith("https://") ||
                        line.startsWith("rtmp://") ||
                        line.startsWith("rtsp://")
                    )
            ) {
                val finalName = currentTitle.ifBlank { "Kanal $channelCounter" }
                val badge = when {
                    finalName.contains("4K", ignoreCase = true) || finalName.contains("UHD", ignoreCase = true) -> "4K"
                    finalName.contains("FHD", ignoreCase = true) || finalName.contains("1080", ignoreCase = true) -> "FHD"
                    else -> "HD"
                }
                val finalId = currentTvgId.ifBlank {
                    "${sourceName}_${finalName.lowercase().replace(Regex("[^a-z0-9]"), "_")}_$channelCounter"
                }
                val (c1, c2) = palette[channelCounter % palette.size]

                channels.add(
                    TvChannel(
                        id = finalId,
                        number = channelCounter++,
                        name = finalName,
                        shortLogoText = makeShortLogoText(finalName),
                        brandPrimaryColor = c1,
                        brandSecondaryColor = c2,
                        category = currentGroup,
                        logoUrl = currentLogo,
                        sources = listOf(StreamSource("FHD 1080p", line, StreamType.HLS)),
                        qualityBadge = badge,
                        epgChannelId = currentTvgId.ifBlank { finalId },
                        sourcePlaylistName = sourceName,
                        isCustomM3u = isCustom
                    )
                )

                currentTitle = ""
                currentLogo = ""
                currentGroup = "Özel Liste"
                currentTvgId = ""
            }
        }
        return channels
    }

    private fun normalizeCategory(raw: String): String {
        val lower = raw.trim().lowercase(Locale.getDefault())
        return when {
            lower.isBlank() -> "Genel"
            lower.contains("news") || lower.contains("haber") -> "Haber"
            lower.contains("sport") || lower.contains("spor") -> "Spor"
            lower.contains("doc") || lower.contains("belgesel") || lower.contains("science") -> "Belgesel"
            lower.contains("music") || lower.contains("müzik") || lower.contains("muzik") -> "Müzik"
            lower.contains("movie") || lower.contains("cinema") || lower.contains("sinema") || lower.contains("film") -> "Sinema"
            lower.contains("kid") || lower.contains("children") || lower.contains("çocuk") || lower.contains("cocuk") -> "Çocuk"
            lower.contains("general") || lower.contains("ulusal") || lower.contains("entertainment") -> "Ulusal"
            else -> raw.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }

    suspend fun downloadText(urlString: String): String = withContext(Dispatchers.IO) {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 12_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/125.0.0.0")
        }
        try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

class TvRepository(private val appContext: Context) {
    private val prefs = appContext.getSharedPreferences("redoxtv_clean_prefs_v5", Context.MODE_PRIVATE)

    fun loadFavoriteOrderList(): List<String> {
        val raw = prefs.getString("fav_order_csv", "") ?: ""
        return raw.split(",").filter { it.isNotBlank() }.distinct()
    }

    fun saveFavoriteOrderList(ids: List<String>) {
        prefs.edit().putString("fav_order_csv", ids.distinct().joinToString(",")).apply()
    }

    fun loadHiddenChannelIds(): Set<String> {
        val raw = prefs.getString("hidden_ids_csv", "") ?: ""
        return raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    fun saveHiddenChannelIds(ids: Set<String>) {
        prefs.edit().putString("hidden_ids_csv", ids.joinToString(",")).apply()
    }

    fun loadAutoStartLastChannel(): Boolean = prefs.getBoolean("auto_start_last_channel", false)

    fun saveAutoStartLastChannel(enabled: Boolean) {
        prefs.edit().putBoolean("auto_start_last_channel", enabled).apply()
    }

    fun loadPreferredSources(): Map<String, Int> {
        val raw = prefs.getString("preferred_sources_map", "") ?: ""
        if (raw.isBlank()) return emptyMap()
        val map = mutableMapOf<String, Int>()
        raw.split(";").forEach { entry ->
            val parts = entry.split("=")
            if (parts.size == 2) {
                val idx = parts[1].toIntOrNull()
                if (parts[0].isNotBlank() && idx != null) {
                    map[parts[0]] = idx
                }
            }
        }
        return map
    }

    fun savePreferredSources(map: Map<String, Int>) {
        val raw = map.entries.joinToString(";") { "${it.key}=${it.value}" }
        prefs.edit().putString("preferred_sources_map", raw).apply()
    }

    fun loadPreferredScales(): Map<String, String> {
        val raw = prefs.getString("preferred_scales_map", "") ?: ""
        if (raw.isBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        raw.split(";").forEach { entry ->
            val parts = entry.split("=")
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                map[parts[0]] = parts[1]
            }
        }
        return map
    }

    fun savePreferredScales(map: Map<String, String>) {
        val raw = map.entries.joinToString(";") { "${it.key}=${it.value}" }
        prefs.edit().putString("preferred_scales_map", raw).apply()
    }

    fun exportBackupPayload(): String {
        val favs = prefs.getString("fav_order_csv", "") ?: ""
        val locked = prefs.getString("locked_ids_csv", "") ?: ""
        val hidden = prefs.getString("hidden_ids_csv", "") ?: ""
        val pin = loadParentalPin()
        val autoStart = loadAutoStartLastChannel()
        val prefSources = prefs.getString("preferred_sources_map", "") ?: ""
        val prefScales = prefs.getString("preferred_scales_map", "") ?: ""
        val playlists = prefs.getString("custom_m3u_playlists_str", "") ?: ""
        return buildString {
            appendLine("REDOX_BACKUP_V9")
            appendLine("FAVS:$favs")
            appendLine("LOCKED:$locked")
            appendLine("HIDDEN:$hidden")
            appendLine("PIN:$pin")
            appendLine("AUTO_START:$autoStart")
            appendLine("PREF_SOURCES:$prefSources")
            appendLine("PREF_SCALES:$prefScales")
            appendLine("PLAYLISTS:$playlists")
        }
    }

    fun importBackupPayload(payload: String): Boolean {
        val trimmed = payload.trim()
        if (!trimmed.startsWith("REDOX_BACKUP_V9")) return false
        val editor = prefs.edit()
        trimmed.lines().drop(1).forEach { line ->
            val idx = line.indexOf(':')
            if (idx > 0) {
                val key = line.substring(0, idx).trim()
                val value = line.substring(idx + 1).trim()
                when (key) {
                    "FAVS" -> editor.putString("fav_order_csv", value)
                    "LOCKED" -> editor.putString("locked_ids_csv", value)
                    "HIDDEN" -> editor.putString("hidden_ids_csv", value)
                    "PIN" -> if (value.length == 4) editor.putString("parental_pin_code", value)
                    "AUTO_START" -> editor.putBoolean("auto_start_last_channel", value.toBoolean())
                    "PREF_SOURCES" -> editor.putString("preferred_sources_map", value)
                    "PREF_SCALES" -> editor.putString("preferred_scales_map", value)
                    "PLAYLISTS" -> editor.putString("custom_m3u_playlists_str", value)
                }
            }
        }
        editor.apply()
        return true
    }

    fun saveBackupFileToDevice(): String? {
        return try {
            val payload = exportBackupPayload()
            val internalFile = File(appContext.filesDir, "redox_tv_backup.txt")
            internalFile.writeText(payload)
            try {
                val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (dlDir != null && (dlDir.exists() || dlDir.mkdirs())) {
                    val dlFile = File(dlDir, "redox_tv_backup.txt")
                    dlFile.writeText(payload)
                    return dlFile.absolutePath
                }
            } catch (_: Exception) {}
            internalFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun restoreBackupFileFromDevice(): Boolean {
        return try {
            val candidates = listOfNotNull(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.let { File(it, "redox_tv_backup.txt") },
                File("/sdcard/Download/redox_tv_backup.txt"),
                File("/storage/emulated/0/Download/redox_tv_backup.txt"),
                File(appContext.filesDir, "redox_tv_backup.txt")
            )
            val existing = candidates.firstOrNull { it.exists() && it.isFile } ?: return false
            importBackupPayload(existing.readText())
        } catch (_: Exception) {
            false
        }
    }

    suspend fun probeChannelStreamHealth(channel: TvChannel): Boolean = withContext(Dispatchers.IO) {
        for (src in channel.sources) {
            val urlStr = src.url.trim()
            if (urlStr.isBlank()) continue
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 1_800
                    readTimeout = 1_800
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/125.0.0.0")
                }
                val code = conn.responseCode
                if (code in 200..399) {
                    return@withContext true
                }
            } catch (_: Exception) {
            } finally {
                try { conn?.disconnect() } catch (_: Exception) {}
            }
        }
        false
    }

    fun loadLockedChannelIds(): Set<String> {
        val raw = prefs.getString("locked_ids_csv", "") ?: ""
        return raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    fun saveLockedChannelIds(ids: Set<String>) {
        prefs.edit().putString("locked_ids_csv", ids.joinToString(",")).apply()
    }

    fun loadBrokenChannelIds(): Set<String> {
        val raw = prefs.getString("broken_ids_csv", "") ?: ""
        return raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    fun saveBrokenChannelIds(ids: Set<String>) {
        prefs.edit().putString("broken_ids_csv", ids.joinToString(",")).apply()
    }

    fun loadParentalPin(): String = prefs.getString("parental_pin_code", "0000") ?: "0000"
    fun saveParentalPin(newPin: String) = prefs.edit().putString("parental_pin_code", newPin).apply()

    fun loadRecentChannelIds(): List<String> {
        val raw = prefs.getString("recent_channel_ids", "") ?: ""
        return raw.split(",").filter { it.isNotBlank() }
    }

    fun pushRecentChannelId(channelId: String): List<String> {
        val current = loadRecentChannelIds().toMutableList()
        current.remove(channelId)
        current.add(0, channelId)
        val trimmed = current.take(10)
        prefs.edit().putString("recent_channel_ids", trimmed.joinToString(",")).apply()
        return trimmed
    }

    fun loadSavedPlaylists(): List<M3uPlaylistSource> {
        val raw = prefs.getString("custom_m3u_playlists_str", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(";;;").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 4) {
                M3uPlaylistSource(
                    id = parts[0],
                    name = parts[1],
                    locationOrUrl = parts[2],
                    epgXmlUrl = parts[3],
                    isLocalFile = parts.getOrElse(4) { "false" }.toBoolean()
                )
            } else null
        }
    }

    fun savePlaylists(playlists: List<M3uPlaylistSource>) {
        val serialized = playlists.joinToString(";;;") {
            "${it.id}|${it.name}|${it.locationOrUrl}|${it.epgXmlUrl}|${it.isLocalFile}"
        }
        prefs.edit().putString("custom_m3u_playlists_str", serialized).apply()
    }

    suspend fun saveRawM3uTextAsLocalPlaylist(rawContent: String, customName: String): M3uPlaylistSource? = withContext(Dispatchers.IO) {
        try {
            if (!rawContent.contains("#EXTINF", ignoreCase = true) && !rawContent.contains("http", ignoreCase = true)) {
                return@withContext null
            }
            val playlistDir = File(appContext.filesDir, "imported_playlists")
            playlistDir.mkdirs()
            val fileId = UUID.randomUUID().toString()
            val targetFile = File(playlistDir, "$fileId.m3u")
            targetFile.writeText(rawContent)

            M3uPlaylistSource(
                id = fileId,
                name = customName.ifBlank { "Telefondan Gönderilen M3U" },
                locationOrUrl = targetFile.absolutePath,
                isLocalFile = true
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun importM3uFromUri(uri: Uri, customName: String): M3uPlaylistSource? = withContext(Dispatchers.IO) {
        try {
            val playlistDir = File(appContext.filesDir, "imported_playlists")
            playlistDir.mkdirs()
            val fileId = UUID.randomUUID().toString()
            val targetFile = File(playlistDir, "$fileId.m3u")
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            val displayName = customName.ifBlank {
                uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Yerel M3U Dosyası"
            }
            M3uPlaylistSource(
                id = fileId,
                name = displayName,
                locationOrUrl = targetFile.absolutePath,
                isLocalFile = true
            )
        } catch (_: Exception) {
            null
        }
    }

    fun scanDownloadFolderForM3uFiles(): List<File> {
        val result = mutableListOf<File>()
        try {
            val dirs = listOfNotNull(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                File("/sdcard/Download"),
                File("/storage/emulated/0/Download"),
                appContext.getExternalFilesDir(null)
            ).distinctBy { it.absolutePath }

            for (dir in dirs) {
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.forEach { file ->
                        if (file.isFile && (file.name.endsWith(".m3u", ignoreCase = true) || file.name.endsWith(".m3u8", ignoreCase = true))) {
                            result.add(file)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return result
    }

    private fun ch(
        id: String,
        number: Int,
        name: String,
        logoText: String,
        c1: Long,
        c2: Long,
        cat: String,
        badge: String = "FHD",
        logoUrl: String = "",
        sources: List<StreamSource>
    ): TvChannel = TvChannel(
        id = id,
        number = number,
        name = name,
        shortLogoText = logoText,
        brandPrimaryColor = c1,
        brandSecondaryColor = c2,
        category = cat,
        logoUrl = logoUrl,
        sources = sources,
        qualityBadge = badge,
        isCustomM3u = false
    )

    private val aciptvTurkishM3uUrl = "https://aciptv.acvid.site/m3ulist/testchannel.m3u"

    fun getStaticBuiltInChannels(): List<TvChannel> =
        getBuiltInChannelsChunk1() +
        getBuiltInChannelsChunk2() +
        getBuiltInChannelsChunk3() +
        getBuiltInChannelsChunk4() +
        getBuiltInChannelsChunk5() +
        getBuiltInChannelsChunk6() +
        getBuiltInChannelsChunk7() +
        getBuiltInChannelsChunk8()

    fun getBuiltInChannels(): List<TvChannel> {
        val base = getStaticBuiltInChannels()
        val cachedTrM3u = loadCachedAciptvTurkishM3u()
        return if (cachedTrM3u.isNotBlank()) {
            mergeTurkishM3uIntoBuiltIn(base, cachedTrM3u)
        } else {
            base
        }
    }

    private fun loadCachedAciptvTurkishM3u(): String {
        return try {
            val f = File(appContext.filesDir, "aciptv_turkish_cached.m3u")
            if (f.exists() && f.length() > 10L) f.readText() else ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun saveCachedAciptvTurkishM3u(content: String) {
        try {
            val f = File(appContext.filesDir, "aciptv_turkish_cached.m3u")
            f.writeText(content)
        } catch (_: Exception) {}
    }

    /**
     * https://aciptv.acvid.site/m3ulist/testchannel.m3u adresinden yalnızca Türk kanallarını
     * (group-title="Turkish", .tr tvg-id veya Türkçe grup başlıklarını) akıllıca süzer ve önbelleğe alır.
     */
    suspend fun fetchAndCacheAciptvTurkishM3u(): String = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(aciptvTurkishM3uUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 9000
                readTimeout = 12000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "RedoXTV/1.4 (Android TV; Chromecast 4K)")
            }
            val sb = StringBuilder("#EXTM3U\n")
            var matchedCount = 0
            conn.inputStream.bufferedReader().useLines { lines ->
                var pendingExtInf: String? = null
                for (rawLine in lines) {
                    val line = rawLine.trim()
                    if (line.startsWith("#EXTINF", ignoreCase = true)) {
                        val lower = line.lowercase(Locale.US)
                        val isTurkish = lower.contains("group-title=\"turkish\"") ||
                            lower.contains("group-title=\"tr") ||
                            lower.contains("group-title=\"ulusal") ||
                            lower.contains("group-title=\"haber") ||
                            lower.contains("group-title=\"spor") ||
                            lower.contains("group-title=\"yerel") ||
                            lower.contains(".tr\"") ||
                            lower.contains(".tr@")
                        pendingExtInf = if (isTurkish) line else null
                    } else if (line.isNotEmpty() && !line.startsWith("#")) {
                        val ext = pendingExtInf
                        if (ext != null && (line.startsWith("http://") || line.startsWith("https://"))) {
                            sb.append(ext).append("\n").append(line).append("\n")
                            matchedCount++
                        }
                        pendingExtInf = null
                    }
                }
            }
            val filtered = sb.toString()
            if (matchedCount > 0) {
                saveCachedAciptvTurkishM3u(filtered)
                filtered
            } else {
                loadCachedAciptvTurkishM3u()
            }
        } catch (_: Exception) {
            loadCachedAciptvTurkishM3u()
        }
    }

    private fun normalizeChannelKey(name: String): String {
        val cleaned = name
            .replace(Regex("\\[.*?\\]|\\(.*?\\)"), " ")
            .replace('İ', 'i').replace('I', 'i').replace('ı', 'i')
            .lowercase(Locale.getDefault())
            .replace('ş', 's').replace('ğ', 'g').replace('ü', 'u').replace('ö', 'o').replace('ç', 'c')
            .replace(Regex("\\b(tv|hd|fhd|sd|tr|turkiye|turk|kanali|kanal|live|backup)\\b"), " ")
            .replace(Regex("[^a-z0-9]"), "")
        return cleaned
    }

    private fun inferTurkishCategory(channelName: String): String {
        val lower = channelName.lowercase(Locale.getDefault())
        return when {
            lower.contains("spor") || lower.contains("tjk") || lower.contains("tay ") || lower.contains("fb ") || lower.contains("gs ") -> "Spor"
            lower.contains("haber") || lower.contains("ntv") || lower.contains("cnn") || lower.contains("ulke") || lower.contains("ülke") || lower.contains("tele 1") || lower.contains("halk") || lower.contains("sözcü") || lower.contains("sozcu") || lower.contains("ekol") || lower.contains("tbmm") || lower.contains("meclis") || lower.contains("bloomberg") -> "Haber"
            lower.contains("cocuk") || lower.contains("çocuk") || lower.contains("baby") || lower.contains("minika") || lower.contains("cartoon") || lower.contains("zarok") -> "Çocuk"
            lower.contains("kral") || lower.contains("power") || lower.contains("nr1") || lower.contains("dream") || lower.contains("tatlises") || lower.contains("müzik") || lower.contains("muzik") || lower.contains("akustik") -> "Müzik"
            lower.contains("belgesel") || lower.contains("tarih") || lower.contains("yaban") || lower.contains("çiftçi") || lower.contains("ciftci") || lower.contains("trt 2") -> "Belgesel"
            lower.contains("diyanet") || lower.contains("semerkand") || lower.contains("lalegül") || lower.contains("lalegul") || lower.contains("kuran") || lower.contains("islam") || lower.contains("ilahi") || lower.contains("sat 7") || lower.contains("hayat") || lower.contains("mihrap") || lower.contains("kudüs") || lower.contains("kudus") -> "Dini"
            lower.contains("kıbrıs") || lower.contains("kibris") || lower.contains("brt") || lower.contains("ada tv") || lower.contains("sim tv") -> "Kıbrıs"
            lower.contains("trt ") || lower.contains("atv") || lower.contains("kanal d") || lower.contains("show") || lower.contains("star") || lower.contains("now") || lower.contains("tv8") || lower.contains("kanal 7") || lower.contains("beyaz") || lower.contains("360") || lower.contains("teve2") || lower.contains("tlc") || lower.contains("dmax") || lower.contains("cnbc") || lower.contains("a2") -> "Ulusal"
            else -> "Yerel"
        }
    }

    fun mergeTurkishM3uIntoBuiltIn(baseList: List<TvChannel>, rawTurkishM3u: String): List<TvChannel> {
        if (rawTurkishM3u.isBlank()) return baseList
        val parsedTr = M3uAndEpgParser.parseM3uContent(rawTurkishM3u, "ACIPTV TR", baseList.size + 1, false)
        if (parsedTr.isEmpty()) return baseList

        val mutableList = baseList.toMutableList()
        val keyToIndex = HashMap<String, Int>(mutableList.size * 2)
        mutableList.forEachIndexed { index, ch ->
            val k = normalizeChannelKey(ch.name)
            if (k.isNotEmpty() && !keyToIndex.containsKey(k)) {
                keyToIndex[k] = index
            }
        }

        for (trCh in parsedTr) {
            val cleanTitle = trCh.name
                .replace(Regex("\\s*(\\(\\d+p\\)|\\[Not 24/7\\]|\\[Geo-blocked\\])\\s*", RegexOption.IGNORE_CASE), "")
                .trim()
                .ifBlank { trCh.name }
            val normKey = normalizeChannelKey(cleanTitle)
            val existingIdx = if (normKey.isNotEmpty()) keyToIndex[normKey] else null

            if (existingIdx != null) {
                val existing = mutableList[existingIdx]
                val existingUrls = existing.sources.map { it.url }.toSet()
                val newSources = trCh.sources.filterNot { existingUrls.contains(it.url) }.mapIndexed { i, src ->
                    src.copy(label = "ACIPTV Akışı (${existing.sources.size + i + 1})")
                }
                val mergedLogo = existing.logoUrl.ifBlank { trCh.logoUrl }
                if (newSources.isNotEmpty() || mergedLogo != existing.logoUrl) {
                    mutableList[existingIdx] = existing.copy(
                        logoUrl = mergedLogo,
                        sources = existing.sources + newSources
                    )
                }
            } else {
                val nextNum = mutableList.size + 1
                val inferredCat = inferTurkishCategory(cleanTitle)
                val added = trCh.copy(
                    id = "ch_aciptv_${nextNum}_${normKey.take(14)}",
                    number = nextNum,
                    name = cleanTitle,
                    category = inferredCat,
                    isCustomM3u = false
                )
                keyToIndex[normKey] = mutableList.size
                mutableList.add(added)
            }
        }
        return mutableList
    }

    private fun getBuiltInChannelsChunk1(): List<TvChannel> = listOf(
        ch(
            id = "ch_1_360_tv", number = 1, name = "360 TV", logoText = "360",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Ulusal", badge = "HD", logoUrl = "https://i.imgur.com/agn47sQ.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://turkmedya-live.ercdn.net/tv360/tv360_1080p.m3u8", StreamType.HLS), StreamSource("HD 720p", "https://turkmedya-live.ercdn.net/tv360/tv360.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://turkmedya-live.ercdn.net/tv360/tv360_720p.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "https://tv.ensonhaber.com/tv360/tv360.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://turkmedya-live.ercdn.net/tv360/tv360_480p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_2_a2_tv", number = 2, name = "A2 TV", logoText = "A2",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Ulusal", badge = "FHD", logoUrl = "https://iatv.tmgrup.com.tr/site/v2/a2tv/i/a2tv-logo.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/a2tv/a2tv_1080p.m3u8", StreamType.HLS), StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/a2tv/a2tv.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/a2tv/a2tv_720p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/a2tv/a2tv_480p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_3_atv", number = 3, name = "ATV", logoText = "ATV",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Ulusal", badge = "FHD", logoUrl = "https://i.imgur.com/HyVUwFC.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/atv/atv_1080p.m3u8", StreamType.HLS), StreamSource("Yedek Akış", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/atv/atv.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/atv/atv_360p.m3u8", StreamType.HLS), StreamSource("HD 720p Hızlı Akış", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/atv/atv_720p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/atv/atv_480p.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "http://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/atv/atv_1080p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_4_beyaz_tv", number = 4, name = "Beyaz TV", logoText = "BEYAZ",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Ulusal", badge = "FHD", logoUrl = "https://i.imgur.com/uykIdML.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://beyaztv-live.daioncdn.net/beyaztv/beyaztv_1080p.m3u8", StreamType.HLS), StreamSource("Yedek Akış", "https://beyaztv-live.daioncdn.net/beyaztv/beyaztv.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://beyaztv-live.daioncdn.net/beyaztv/beyaztv_720p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://beyaztv-live.daioncdn.net/beyaztv/beyaztv_480p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_5_cem_tv", number = 5, name = "Cem TV", logoText = "CEM",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Ulusal", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://panel.5gtvhosting.com/hls/cemtv/cemtv.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://panel.5gtvhosting.com/hls/cemtv/cemtv.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_6_cine1_tv", number = 6, name = "Cine1 TV", logoText = "CINE1",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Ulusal", badge = "HD", logoUrl = "https://i.ibb.co/zhxTBMR/cine1.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://canliyayin.cine1.com.tr/memfs/cbaef080-a742-4644-9e9e-2b9f6a5103c3.m3u8", StreamType.HLS), StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_cine1/cine1/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://canliyayin.cine1.com.tr/memfs/cbaef080-a742-4644-9e9e-2b9f6a5103c3.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_cine1/cine1/chunks.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_7_cine5_tv", number = 7, name = "Cine5 TV", logoText = "CINE5",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Ulusal", badge = "HD", logoUrl = "https://i.ibb.co/7vNbSyP/cine5.png",
            sources = listOf(StreamSource("Standart Akış", "https://cdn-cine5tv.yayin.com.tr/cine5tv/cine5tv/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış", "https://host.onlineradyotv.net:8081/cine5tv/index.m3u8", StreamType.HLS), StreamSource("Hızlı Chunks Akışı", "https://cdn-cine5tv.yayin.com.tr/cine5tv/cine5tv/chunks.m3u8", StreamType.HLS), StreamSource("Mono Doğrudan Akış", "https://cdn-cine5tv.yayin.com.tr/cine5tv/cine5tv/tracks-v1a1/mono.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_8_cnbc_e", number = 8, name = "CNBC-e", logoText = "CNBC-E",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Ulusal", badge = "FHD", logoUrl = "https://s.cnbce.com/dist/images/logo-nav.png",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://hnpsechtsc.turknet.ercdn.net/xpnvudnlsv/cnbc-e/cnbc-e.m3u8?tv=1", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://hnpsechtsc.turknet.ercdn.net/xpnvudnlsv/cnbc-e/cnbc-e.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://hnpsechtsc.turknet.ercdn.net/xpnvudnlsv/cnbc-e/cnbc-e_1080p.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://hnpsechtsc.turknet.ercdn.net/xpnvudnlsv/cnbc-e/cnbc-e_1080p.m3u8?tv=1", StreamType.HLS), StreamSource("HD 720p Hızlı Akış", "https://hnpsechtsc.turknet.ercdn.net/xpnvudnlsv/cnbc-e/cnbc-e_720p.m3u8?tv=1", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://hnpsechtsc.turknet.ercdn.net/xpnvudnlsv/cnbc-e/cnbc-e_480p.m3u8?tv=1", StreamType.HLS))
        ),
        ch(
            id = "ch_9_dizi_tv", number = 9, name = "Dizi TV", logoText = "DIZI",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Ulusal", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/dizi-tv/playlist/0.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/dizi-tv/playlist/0.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_10_dmax", number = 10, name = "DMAX", logoText = "DMAX",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Ulusal", badge = "HD", logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/98/DMAX_BLACK.svg/512px-DMAX_BLACK.svg.png",
            sources = listOf(StreamSource("HD 720p", "https://dogus-live.daioncdn.net/dmax/dmax_720p.m3u8", StreamType.HLS), StreamSource("Yedek Akış", "http://dygvideo.dygdigital.com/live/hls/dmaxdai?m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://dogus-live.daioncdn.net/dmax/dmax_1080p.m3u8", StreamType.HLS), StreamSource("Adaptif Ana Akış", "https://dogus-live.daioncdn.net/dmax/dmax.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://dogus-live.daioncdn.net/dmax/dmax_480p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_11_euro_d", number = 11, name = "Euro D", logoText = "EURO\nD",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Ulusal", badge = "HD", logoUrl = "https://i.ibb.co/jMQTJmh/euro-d.png",
            sources = listOf(StreamSource("HD 720p", "https://live.duhnet.tv/S2/HLS_LIVE/eurodnp/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://live.duhnet.tv/S2/HLS_LIVE/eurodnp/track_4_1000/playlist.m3u8", StreamType.HLS), StreamSource("Duhnet Hızlı (Track 2)", "https://live.duhnet.tv/S2/HLS_LIVE/eurodnp/track_2_550/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.duhnet.tv/S2/HLS_LIVE/eurodnp/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.duhnet.tv/S2/HLS_LIVE/eurodnp/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_12_euro_star", number = 12, name = "Euro Star", logoText = "EURO\nSTAR",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Ulusal", badge = "HD", logoUrl = "https://i.ibb.co/CbdPSX6/euro-star.png",
            sources = listOf(StreamSource("HD 720p", "https://xrklj56s.rocketcdn.com/eurostar.smil/chunklist.sltur.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://xrklj56s.rocketcdn.com/eurostar.smil/chunklist.sltur.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_13_flash_tv", number = 13, name = "Flash TV", logoText = "FLASH",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Ulusal", badge = "HD", logoUrl = "https://i.imgur.com/L0NHKBM.png",
            sources = listOf(StreamSource("HD 720p", "https://mn-nl.mncdn.com/blutv_flashtv/live.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://flashhaber-live.ercdn.net/flashhaber/flashhaber_720p.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://mn-nl.mncdn.com/blutv_flashtv/live.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://flashhaber-live.ercdn.net/flashhaber/flashhaber_1080p.m3u8", StreamType.HLS), StreamSource("Adaptif Ana Akış", "https://flashhaber-live.ercdn.net/flashhaber/flashhaber.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://flashhaber-live.ercdn.net/flashhaber/flashhaber_480p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_14_kanal_7", number = 14, name = "Kanal 7", logoText = "KANAL\n7",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Ulusal", badge = "FHD", logoUrl = "https://i.imgur.com/0gq9xOm.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://kanal7-live.daioncdn.net/kanal7/kanal7_1080p.m3u8", StreamType.HLS), StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://kanal7-live.daioncdn.net/kanal7/kanal7.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://kanal7.daioncdn.net/kanal7/kanal7.m3u8?app=f99587ad-1637-494d-8255-da35b09d17a1", StreamType.HLS), StreamSource("HD 720p Hızlı Akış", "https://kanal7-live.daioncdn.net/kanal7/kanal7_720p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://kanal7-live.daioncdn.net/kanal7/kanal7_480p.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://kanal7.daioncdn.net/kanal7/kanal7_1080p.m3u8?app=f99587ad-1637-494d-8255-da35b09d17a1", StreamType.HLS))
        ),
        ch(
            id = "ch_15_kanal_7_avrupa", number = 15, name = "Kanal 7 Avrupa", logoText = "KANAL\n7",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Ulusal", badge = "HD", logoUrl = "https://i.ibb.co/F4P8ddm/kanal-7-avrupa.png",
            sources = listOf(StreamSource("HD 720p", "https://livetv.radyotvonline.net/kanal7live/kanal7avr/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://livetv.radyotvonline.net/kanal7live/kanal7avr/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://livetv.radyotvonline.net/kanal7live/kanal7avr/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_16_kanal_d", number = 16, name = "Kanal D", logoText = "KANAL\nD",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Ulusal", badge = "FHD", logoUrl = "https://i.imgur.com/9o1atM6.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://ackaxsqacw.turknet.ercdn.net/ozfkfbbjba/kanald/kanald_1080p.m3u8", StreamType.HLS), StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://demiroren.daioncdn.net/kanald/kanald.m3u8?app=kanald_web&ce=3", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://demiroren.daioncdn.net/kanald/kanald.m3u8?app=da2109ea-5dfe-4107-89ab-23593336ed61", StreamType.HLS), StreamSource("Alternatif Akış 4", "https://live.duhnet.tv/S2/HLS_LIVE/kanalddainp/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 5", "https://demiroren.daioncdn.net/kanald/kanald.m3u8?app=kanald_web", StreamType.HLS), StreamSource("Alternatif Akış 6", "https://ackaxsqacw.turknet.ercdn.net/ozfkfbbjba/kanald/kanald.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_17_meltem_tv", number = 17, name = "Meltem TV", logoText = "MELTEM",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Ulusal", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://vhxyrsly.rocketcdn.com/meltemtv/chunklist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://vhxyrsly.rocketcdn.com/meltemtv/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://vhxyrsly.rocketcdn.com/meltemtv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_18_now_tv", number = 18, name = "NOW TV", logoText = "NOW",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Ulusal", badge = "HD", logoUrl = "https://i.imgur.com/5EYjWK7.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://uycyyuuzyh.turknet.ercdn.net/nphindgytw/nowtv/nowtv_720p.m3u8", StreamType.HLS), StreamSource("HD 720p", "https://uycyyuuzyh.turknet.ercdn.net/nphindgytw/nowtv/nowtv.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://uycyyuuzyh.turknet.ercdn.net/nphindgytw/nowtv/nowtv.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://uycyyuuzyh.turknet.ercdn.net/nphindgytw/nowtv/nowtv_1080p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://uycyyuuzyh.turknet.ercdn.net/nphindgytw/nowtv/nowtv_480p.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "http://uycyyuuzyh.turknet.ercdn.net/nphindgytw/nowtv/nowtv_1080p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_19_show_max", number = 19, name = "Show Max", logoText = "SHOW\nMAX",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Ulusal", badge = "HD", logoUrl = "https://i.ibb.co/qJnvXJQ/show-max.png",
            sources = listOf(StreamSource("Standart Akış", "http://rmtftbjlne.turknet.ercdn.net/bpeytmnqyp/showmax/showmax.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://rmtftbjlne.turknet.ercdn.net/bpeytmnqyp/showmax/showmax.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "http://rmtftbjlne.turknet.ercdn.net/bpeytmnqyp/showmax/showmax_1080p.m3u8", StreamType.HLS), StreamSource("HD 720p Hızlı Akış", "http://rmtftbjlne.turknet.ercdn.net/bpeytmnqyp/showmax/showmax_720p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "http://rmtftbjlne.turknet.ercdn.net/bpeytmnqyp/showmax/showmax_480p.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://rmtftbjlne.turknet.ercdn.net/bpeytmnqyp/showmax/showmax_1080p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_20_show_tv", number = 20, name = "Show TV", logoText = "SHOW",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Ulusal", badge = "FHD", logoUrl = "https://i.ibb.co/Rv0rWhZ/show.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://rmtftbjlne.turknet.ercdn.net/bpeytmnqyp/showtv/showtv_1080p.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://rmtftbjlne.turknet.ercdn.net:443/bpeytmnqyp/showtv/showtv_1080p.m3u8", StreamType.HLS), StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://ciner-live.daioncdn.net/showtv/showtv_1080p.m3u8", StreamType.HLS), StreamSource("Yedek Akış", "http://ciner.daioncdn.net/showtv/showtv.m3u8?app=showtv_web", StreamType.HLS), StreamSource("Alternatif Akış 5", "https://ciner.daioncdn.net/showtv/showtv_1080p.m3u8?ex=1664766175&st=RBzhSuGauna0OGld-DJUVA&tv=1&sid=88iye2ldl2ew&app=4bc856ef-4c68-4a94-bc87-37dfaaa66558&ce=3", StreamType.HLS), StreamSource("Alternatif Akış 6", "https://rmtftbjlne.turknet.ercdn.net/bpeytmnqyp/showtv/showtv.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_21_star_tv", number = 21, name = "Star TV", logoText = "STAR",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Ulusal", badge = "HD", logoUrl = "https://i.imgur.com/9O3DHRB.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://dogus.daioncdn.net/startv/startv.m3u8?ce=3&app=1740db13-3ee9-4858-8d2c-b1a9cfaa3718", StreamType.HLS), StreamSource("HD 720p", "https://dogus.daioncdn.net/startv/startv_720p.m3u8?&sid=8l4w3lst4co5&app=a20ac41e-bdc3-4aa1-934d-26b484480ac9&ce=3", StreamType.HLS), StreamSource("Yedek Akış", "https://dygvideo.dygdigital.com/live/hls/startv4puhu", StreamType.HLS), StreamSource("Alternatif Akış 4", "http://176.65.153.66:6001/play/a01u/index.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 5", "http://dygvideo.dygdigital.com/live/hls/stardai?m3u8", StreamType.HLS), StreamSource("Alternatif Akış 6", "https://dogus.daioncdn.net/startv/startv.m3u8?app=a20ac41e-bdc3-4aa1-934d-26b484480ac9", StreamType.HLS))
        ),
        ch(
            id = "ch_22_tabii_tv", number = 22, name = "Tabii TV", logoText = "TABII",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Ulusal", badge = "FHD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://ceokzokgtd.erbvr.com/tabiitv/tabiitv_1080p.m3u8", StreamType.HLS), StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://ceokzokgtd.fdc.ercdn.net/giuqlxwlrl/tabiitv/tabiitv.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://ceokzokgtd.erbvr.com/tabiitv/tabiitv_1080p.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://ceokzokgtd.fdc.ercdn.net/giuqlxwlrl/tabiitv/tabiitv_1080p.m3u8", StreamType.HLS), StreamSource("HD 720p Hızlı Akış", "https://ceokzokgtd.fdc.ercdn.net/giuqlxwlrl/tabiitv/tabiitv_720p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://ceokzokgtd.fdc.ercdn.net/giuqlxwlrl/tabiitv/tabiitv_480p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_23_teve2", number = 23, name = "Teve2", logoText = "TEVE2",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Ulusal", badge = "FHD", logoUrl = "https://i.imgur.com/rsoSLih.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://ackaxsqacw.turknet.ercdn.net/ozfkfbbjba/teve2/teve2_1080p.m3u8", StreamType.HLS), StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://demiroren-live.daioncdn.net/teve2/teve2.m3u8", StreamType.HLS), StreamSource("Yedek Akış", "https://ackaxsqacw.turknet.ercdn.net/ozfkfbbjba/teve2/teve2.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "https://live.duhnet.tv/S2/HLS_LIVE/teve2np/track_4_1000/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 5", "https://live.duhnet.tv/S2/HLS_LIVE/teve2np/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 6", "https://teve2.duhnet.tv/S2/HLS_LIVE/teve2np/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_24_tgrt_eu", number = 24, name = "TGRT EU", logoText = "TGRT\nEU",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Ulusal", badge = "HD", logoUrl = "https://i.ibb.co/MCXCRcn/tgrteu1.png",
            sources = listOf(StreamSource("Standart Akış", "https://tgrt-live.ercdn.net/tgrteu/tgrteu.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://umperisn.mywire.org/index.php?id=594199&token=DeaTHLesS", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://tgrt-live.ercdn.net/tgrteu/tgrteu_1080p.m3u8", StreamType.HLS), StreamSource("HD 720p Hızlı Akış", "https://tgrt-live.ercdn.net/tgrteu/tgrteu_720p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://tgrt-live.ercdn.net/tgrteu/tgrteu_480p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_25_tivi_6", number = 25, name = "Tivi 6", logoText = "TIVI\n6",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Ulusal", badge = "HD", logoUrl = "https://i.ibb.co/9H3DMWz/tivi6.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://live.artidijitalmedya.com/artidijital_tivi6/tivi6/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://live.artidijitalmedya.com/artidijital_tivi6/tivi6/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_tivi6/tivi6/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_tivi6/tivi6/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_26_tlc", number = 26, name = "TLC", logoText = "TLC",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Ulusal", badge = "HD", logoUrl = "https://i.imgur.com/8m3PMUf.png",
            sources = listOf(StreamSource("HD 720p", "https://dogus-live.daioncdn.net/tlc/tlc.m3u8", StreamType.HLS), StreamSource("Yedek Akış", "http://dygvideo.dygdigital.com/live/hls/tlctvdai?m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://dogus-live.daioncdn.net/tlc/tlc_1080p.m3u8", StreamType.HLS), StreamSource("HD 720p Hızlı Akış", "https://dogus-live.daioncdn.net/tlc/tlc_720p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://dogus-live.daioncdn.net/tlc/tlc_480p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_27_trt_1", number = 27, name = "TRT 1", logoText = "TRT\n1",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Ulusal", badge = "FHD", logoUrl = "https://i.imgur.com/j786OLG.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "http://89.187.191.41/TRT-1-HD-TR/video.m3u8", StreamType.HLS), StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://tv-trt1.medya.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://trt.daioncdn.net/trt-1/master.m3u8?app=e435840d-2653-45a3-afef-082d4ea998f3", StreamType.HLS), StreamSource("Alternatif Akış 4", "https://helga.iptv2022.com/sh/trt_1/index.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 5", "https://trt.daioncdn.net/trt-1/master_720p.m3u8?app=web", StreamType.HLS), StreamSource("Alternatif Akış 6", "https://trt.daioncdn.net/trt-1/master.m3u8?app=web", StreamType.HLS))
        ),
        ch(
            id = "ch_28_trt_2", number = 28, name = "TRT 2", logoText = "TRT\n2",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Ulusal", badge = "FHD", logoUrl = "https://i.imgur.com/lNWrOE2.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://tv-trt2.medya.trt.com.tr/master_1080.m3u8", StreamType.HLS), StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://tv-trt2.medya.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://umperisn.mywire.org/index.php?id=1846&token=DeaTHLesS", StreamType.HLS), StreamSource("HD 720p (Hızlı Yedek)", "https://tv-trt2.medya.trt.com.tr/master_720.m3u8", StreamType.HLS), StreamSource("SD 480p (Kesintisiz Yedek)", "https://tv-trt2.medya.trt.com.tr/master_480.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_29_tv4", number = 29, name = "TV4", logoText = "TV4",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Ulusal", badge = "HD", logoUrl = "https://i.imgur.com/UpsQsbd.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://turkmedya-live.ercdn.net/tv4/tv4_1080p.m3u8", StreamType.HLS), StreamSource("HD 720p", "https://turkmedya-live.ercdn.net/tv4/tv4.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://turkmedya-live.ercdn.net/tv4/tv4_720p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://turkmedya-live.ercdn.net/tv4/tv4_480p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_30_tv8", number = 30, name = "TV8", logoText = "TV8",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Ulusal", badge = "FHD", logoUrl = "https://upload.wikimedia.org/wikipedia/tr/thumb/6/68/Tv8_Yeni_Logo.png/960px-Tv8_Yeni_Logo.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://tv8.daioncdn.net/tv8/tv8_1080p.m3u8?&sid=8sx8v82ropt3&app=7ddc255a-ef47-4e81-ab14-c0e5f2949788&ce=3", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://rkhubpaomb.turknet.ercdn.net/fwjkgpasof/tv8/tv8_1080p.m3u8", StreamType.HLS), StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://tv8.daioncdn.net/tv8/tv8.m3u8?app=7ddc255a-ef47-4e81-ab14-c0e5f2949788&ce=3", StreamType.HLS), StreamSource("Alternatif Akış 4", "https://tv8.daioncdn.net/tv8/tv8.m3u8?app=7ddc255a-ef47-4e81-ab14-c0e5f2949788", StreamType.HLS), StreamSource("Alternatif Akış 5", "https://tv8.daioncdn.net/tv8/tv8_1080p.m3u8?&sid=6q8954bl1bhp&app=7ddc255a-ef47-4e81-ab14-c0e5f2949788&ce=3", StreamType.HLS), StreamSource("Alternatif Akış 6", "https://rkhubpaomb.turknet.ercdn.net/fwjkgpasof/tv8/tv8.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_31_tv8_5", number = 31, name = "TV8.5", logoText = "TV8.5",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Ulusal", badge = "FHD", logoUrl = "https://i.ibb.co/6PRYHdW/tv85.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://rkhubpaomb.turknet.ercdn.net/fwjkgpasof/tv8bucuk/tv8bucuk_1080p.m3u8", StreamType.HLS), StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://tv8.daioncdn.net/tv8bucuk/tv8bucuk_1080p.m3u8?app=tv8bucuk_web&ce=3", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://rkhubpaomb.turknet.ercdn.net/fwjkgpasof/tv8bucuk/tv8bucuk.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "https://tv8.daioncdn.net/tv8bucuk/tv8bucuk_1080p.m3u8?&sid=7tra76g4cefx&app=bf58ab52-4865-4c81-b223-26b41009801e&ce=3", StreamType.HLS), StreamSource("HD 720p Hızlı Akış", "https://rkhubpaomb.turknet.ercdn.net/fwjkgpasof/tv8bucuk/tv8bucuk_720p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://rkhubpaomb.turknet.ercdn.net/fwjkgpasof/tv8bucuk/tv8bucuk_480p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_32_woman_tv", number = 32, name = "Woman TV", logoText = "WOMAN",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Ulusal", badge = "FHD", logoUrl = "https://i.ibb.co/bHTjDyk/woman-tv.png",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://s01.webcaster.cloud/wmtv/live_1080p.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://s01.webcaster.cloud/wmtv/live_1080p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_33_a_spor", number = 33, name = "A Spor", logoText = "A\nSPOR",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Spor", badge = "FHD", logoUrl = "https://i.imgur.com/ZhkZzLf.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/aspor/aspor_1080p.m3u8", StreamType.HLS), StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/aspor/aspor.m3u8", StreamType.HLS), StreamSource("HD 720p Hızlı Akış", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/aspor/aspor_720p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/aspor/aspor_480p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_34_bein_sports", number = 34, name = "beIN SPORTS", logoText = "BEIN\nSPORT",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Spor", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://redbox-beinsports-xumo.amagi.tv/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://redbox-beinsports-xumo.amagi.tv/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://redbox-beinsports-xumo.amagi.tv/chunklist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_35_bein_sports_xtra", number = 35, name = "beIN SPORTS XTRA", logoText = "BEIN\nSPORT",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Spor", badge = "FHD", logoUrl = "",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://bein-xtra-bein.amagi.tv/playlist.m3u8", StreamType.HLS), StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://bein-xtra-roku-us.amagi.tv/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://bein-xtra-bein.amagi.tv/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://bein-xtra-bein.amagi.tv/chunklist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_36_cbc_sport_azerbaycan", number = 36, name = "CBC Sport Azerbaycan", logoText = "CBC\nSPORT",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Spor", badge = "FHD", logoUrl = "",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://mn-nl.mncdn.com/cbcsports_live/cbcsports/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Playlist)", "https://mn-nl.mncdn.com/cbcsports_live/cbcsports/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_37_fb_tv", number = 37, name = "FB TV", logoText = "FB",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Spor", badge = "HD", logoUrl = "https://i.ibb.co/315SYpK/fb-tv.png",
            sources = listOf(StreamSource("Standart Akış", "https://fbtv.fenerbahce.org/fenerbahcetv.stream_720p/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış", "https://1hskrdto.rocketcdn.com/fenerbahcetv.smil/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://1hskrdto.rocketcdn.com/fenerbahcetv.smil/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://fbtv.fenerbahce.org/fenerbahcetv.stream_720p/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://fbtv.fenerbahce.org/fenerbahcetv.stream_720p/index.m3u8", StreamType.HLS))
        )
    )

    private fun getBuiltInChannelsChunk2(): List<TvChannel> = listOf(
        ch(
            id = "ch_38_gs_tv", number = 38, name = "GS TV", logoText = "GS",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Spor", badge = "HD", logoUrl = "https://i.ibb.co/6RqMpGT/gs-tv.png",
            sources = listOf(StreamSource("Standart Akış", "https://owifavo5.rocketcdn.com/gstv/gstv.smil/chunklist_b1828000_sltur.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://owifavo5.rocketcdn.com/gstv/gstv.smil/chunklist_b1828000_sltur.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_39_ht_spor_tv", number = 39, name = "HT Spor TV", logoText = "HT\nSPOR",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Spor", badge = "FHD", logoUrl = "https://www.htspor.com/images/manifest/social-share-logo.png",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://ciner.daioncdn.net/ht-spor/ht-spor.m3u8?app=web", StreamType.HLS), StreamSource("Yedek Akış", "https://ciner-live.ercdn.net/htspor/htspor.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://ciner.daioncdn.net/ht-spor/ht-spor.m3u8?app=web&dfp_paln=AQzzBGQEBm1BO0E2iVe9tLfcsqJ3gAOMJ63P8Ch&ppid=675956db19f9bb686edda21d15d911e8", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://ciner.daioncdn.net/ht-spor/ht-spor_1080p.m3u8?app=web", StreamType.HLS), StreamSource("HD 720p Hızlı Akış", "https://ciner.daioncdn.net/ht-spor/ht-spor_720p.m3u8?app=web", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://ciner.daioncdn.net/ht-spor/ht-spor_480p.m3u8?app=web", StreamType.HLS))
        ),
        ch(
            id = "ch_40_mma_tv", number = 40, name = "MMA TV", logoText = "MMA",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Spor", badge = "FHD", logoUrl = "",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://alchimie-mmatv-samsung.amagi.tv/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://alchimie-mmatv-samsung.amagi.tv/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://alchimie-mmatv-samsung.amagi.tv/chunklist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_41_motosport_tv", number = 41, name = "MotoSport TV", logoText = "MOTOSP",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Spor", badge = "FHD", logoUrl = "",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://alchimie-motosport-tv-rakuten.amagi.tv/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://alchimie-motosport-tv-rakuten.amagi.tv/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://alchimie-motosport-tv-rakuten.amagi.tv/chunklist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_42_real_madrid_tv", number = 42, name = "Real Madrid TV", logoText = "REAL\nMADRI",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Spor", badge = "FHD", logoUrl = "",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://cinedigm-rmtv-lg.amagi.tv/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://cinedigm-rmtv-lg.amagi.tv/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://cinedigm-rmtv-lg.amagi.tv/chunklist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_43_sports_tv", number = 43, name = "Sports TV", logoText = "SPORTS",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Spor", badge = "FHD", logoUrl = "https://i.ibb.co/4Rm7Z6Z/sports22.png",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://live.sportstv.com.tr/hls/low/sportstv_fhd/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Playlist)", "https://live.sportstv.com.tr/hls/low/sportstv_fhd/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Master)", "https://live.sportstv.com.tr/hls/low/sportstv_fhd/master.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_44_s_f_r_tv", number = 44, name = "Sıfır TV", logoText = "SIFIR",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Spor", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://ahr7chck.rocketcdn.com/sifirtv.stream/chunklist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://umperisn.mywire.org/index.php?id=611874&token=DeaTHLesS", StreamType.HLS), StreamSource("Yedek Akış (Playlist)", "https://ahr7chck.rocketcdn.com/sifirtv.stream/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_45_tabii_spor_6", number = 45, name = "Tabii Spor 6", logoText = "TABII\nSPOR",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Spor", badge = "HD", logoUrl = "https://cms-tabii-public-image.tabii.com/int/w300/43020.jpeg",
            sources = listOf(StreamSource("HD 720p", "https://vbtob9hyq58eiophct5qctxr2.medya.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("FHD 1080p (TRT CDN)", "https://vbtob9hyq58eiophct5qctxr2.medya.trt.com.tr/master_1080.m3u8", StreamType.HLS), StreamSource("HD 720p (Hızlı Yedek)", "https://vbtob9hyq58eiophct5qctxr2.medya.trt.com.tr/master_720.m3u8", StreamType.HLS), StreamSource("SD 480p (Kesintisiz Yedek)", "https://vbtob9hyq58eiophct5qctxr2.medya.trt.com.tr/master_480.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_46_tennis_channel_inter", number = 46, name = "Tennis Channel International", logoText = "TENNI\nCHANN",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Spor", badge = "FHD", logoUrl = "",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://tennischannel-intl-samsung-uk.amagi.tv/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://tennischannel-intl-samsung-uk.amagi.tv/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://tennischannel-intl-samsung-uk.amagi.tv/chunklist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_47_tjk_tv", number = 47, name = "TJK TV", logoText = "TJK",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Spor", badge = "FHD", logoUrl = "https://i.ibb.co/PrkXrRn/tjk-tv.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://tjktv-live.tjk.org/tjktv_1080p.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://tjktv-live.tjk.org/tjktv.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://tjktv-live.tjk.org/tjktv_1080p.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "http://tjktv-live.tjk.org/tjktv.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_48_tjk_tv_yurt_d___", number = 48, name = "TJK TV Yurt Dışı", logoText = "TJK\nYURT",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Spor", badge = "FHD", logoUrl = "https://i.ibb.co/PrkXrRn/tjk-tv.png",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://tjktv-live.tjk.org/tjktv2/tjktv2_1080p.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://tjktv-live.tjk.org/tjktv2/tjktv2_1080p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_49_trace_sportstars", number = 49, name = "Trace SportStars", logoText = "TRACE\nSPORT",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Spor", badge = "FHD", logoUrl = "",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://trace-sportstars-tcl.amagi.tv/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://trace-sportstars-tcl.amagi.tv/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://trace-sportstars-tcl.amagi.tv/chunklist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_50_trt_spor", number = 50, name = "TRT Spor", logoText = "TRT\nSPOR",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Spor", badge = "FHD", logoUrl = "https://i.imgur.com/N2wGZyf.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://tv-trtspor1.medya.trt.com.tr/master_1080.m3u8", StreamType.HLS), StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://tv-trtspor1.medya.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("Yedek Akış", "https://trt.daioncdn.net/trtspor/master.m3u8?app=web", StreamType.HLS), StreamSource("Alternatif Akış 4", "http://umperisn.mywire.org/index.php?id=2016&token=DeaTHLesS", StreamType.HLS), StreamSource("HD 720p (Hızlı Yedek)", "https://tv-trtspor1.medya.trt.com.tr/master_720.m3u8", StreamType.HLS), StreamSource("SD 480p (Kesintisiz Yedek)", "https://tv-trtspor1.medya.trt.com.tr/master_480.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_51_trt_spor_y_ld_z", number = 51, name = "TRT Spor Yıldız", logoText = "TRT\nSPOR",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Spor", badge = "FHD", logoUrl = "https://i.imgur.com/ysKteM8.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://tv-trtspor2.medya.trt.com.tr/master_1080p.m3u8", StreamType.HLS), StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://tv-trtspor2.medya.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("Yedek Akış", "https://trt.daioncdn.net/trtspor-yildiz/master.m3u8?app=web", StreamType.HLS), StreamSource("Alternatif Akış 4", "http://umperisn.mywire.org/index.php?id=2019&token=DeaTHLesS", StreamType.HLS), StreamSource("FHD 1080p (TRT CDN)", "https://tv-trtspor2.medya.trt.com.tr/master_1080.m3u8", StreamType.HLS), StreamSource("HD 720p (Hızlı Yedek)", "https://tv-trtspor2.medya.trt.com.tr/master_720.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_52_24_tv", number = 52, name = "24 TV", logoText = "24",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Haber", badge = "HD", logoUrl = "https://i.imgur.com/8FO41es.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://turkmedya-live.ercdn.net/tv24/tv24_1080p.m3u8", StreamType.HLS), StreamSource("HD 720p", "https://turkmedya-live.ercdn.net/tv24/tv24_720p.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://tv.ensonhaber.com/tv24/tv24.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "https://turkmedya-live.ercdn.net/tv24/tv24.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 5", "https://ensonhaber-live.ercdn.net/tv24/tv24.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://turkmedya-live.ercdn.net/tv24/tv24_480p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_53_a_haber", number = 53, name = "A Haber", logoText = "A\nHABER",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Haber", badge = "FHD", logoUrl = "https://upload.wikimedia.org/wikipedia/commons/7/7c/Ahaber_Logo.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/ahaber/ahaber_1080p.m3u8", StreamType.HLS), StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/ahaber/ahaber.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://trkvz-live.ercdn.net/ahaberhd/ahaberhd.m3u8", StreamType.HLS), StreamSource("HD 720p Hızlı Akış", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/ahaber/ahaber_720p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/ahaber/ahaber_480p.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://trkvz-live.ercdn.net/ahaberhd/ahaberhd_1080p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_54_a_para_hd", number = 54, name = "A Para HD", logoText = "A\nPARA",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Haber", badge = "FHD", logoUrl = "https://i.ibb.co/KwXzY6B/apara.png",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://trkvz-live.ercdn.net/aparahd/aparahd.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://trkvz-live.ercdn.net/aparahd/aparahd_1080p.m3u8", StreamType.HLS), StreamSource("HD 720p Hızlı Akış", "https://trkvz-live.ercdn.net/aparahd/aparahd_720p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://trkvz-live.ercdn.net/aparahd/aparahd_480p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_55_anadolu_ajans_", number = 55, name = "Anadolu Ajansı", logoText = "ANADO\nAJANS",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Haber", badge = "HD", logoUrl = "https://i.ibb.co/jLd8Fcb/aa.png",
            sources = listOf(StreamSource("Standart Akış", "http://mtulqxgomrllive.mediatriple.net/mtulqxgomrllive/broadcast_59f9c0c785b88.smil/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "http://mtulqxgomrllive.mediatriple.net/mtulqxgomrllive/broadcast_59f9c0c785b88.smil/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "http://mtulqxgomrllive.mediatriple.net/mtulqxgomrllive/broadcast_59f9c0c785b88.smil/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_56_beng_t_rk_tv", number = 56, name = "Bengütürk TV", logoText = "BENGÜT",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Haber", badge = "FHD", logoUrl = "https://i.ibb.co/CHWhMvQ/bengu-turk.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://tv.ensonhaber.com/benguturk/benguturk_720p.m3u8", StreamType.HLS), StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://ensonhaber-live.ercdn.net/benguturk/benguturk.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://tv.ensonhaber.com/benguturk/benguturk.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "http://tv.ensonhaber.com/benguturk/benguturk_720p.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://ensonhaber-live.ercdn.net/benguturk/benguturk_1080p.m3u8", StreamType.HLS), StreamSource("HD 720p Hızlı Akış", "https://ensonhaber-live.ercdn.net/benguturk/benguturk_720p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_57_bloomberg_ht", number = 57, name = "Bloomberg HT", logoText = "BLOOM\nHT",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Haber", badge = "HD", logoUrl = "https://i.imgur.com/bmkXfIE.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://tv.ensonhaber.com/bloomberght/bloomberght_720p.m3u8", StreamType.HLS), StreamSource("HD 720p", "https://ciner-live.daioncdn.net/bloomberght/bloomberght_720p.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://tv.ensonhaber.com/bloomberght/bloomberght.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "https://ensonhaber-live.ercdn.net/bloomberght/bloomberght.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 5", "https://ciner.daioncdn.net/bloomberght/bloomberght.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://ciner-live.daioncdn.net/bloomberght/bloomberght_1080p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_58_dha", number = 58, name = "DHA", logoText = "DHA",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Haber", badge = "HD", logoUrl = "https://i.ibb.co/K6BKFqY/dha.png",
            sources = listOf(StreamSource("Standart Akış", "https://603c568fccdf5.streamlock.net/live/dhaweb1_C5efC/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://603c568fccdf5.streamlock.net/live/dhaweb1_C5efC/chunklist_w630024058.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://603c568fccdf5.streamlock.net/live/dhaweb1_C5efC/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://603c568fccdf5.streamlock.net/live/dhaweb1_C5efC/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_59_ekol_tv", number = 59, name = "Ekol TV", logoText = "EKOL",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Haber", badge = "FHD", logoUrl = "https://i.ibb.co/Ln92Dj2/Ekoltv.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://ekoltv-live.ercdn.net/ekoltv/ekoltv_1080p.m3u8", StreamType.HLS), StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://ekoltv-live.ercdn.net/ekoltv/ekoltv.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://helga.iptv2022.com/sh/ekol_tv/index.m3u8", StreamType.HLS), StreamSource("HD 720p Hızlı Akış", "https://ekoltv-live.ercdn.net/ekoltv/ekoltv_720p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://ekoltv-live.ercdn.net/ekoltv/ekoltv_480p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_60_euronews_english", number = 60, name = "Euronews English", logoText = "EURON\nENGLI",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Haber", badge = "FHD", logoUrl = "",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://alchimie-euronews-uk-lg.amagi.tv/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://alchimie-euronews-uk-lg.amagi.tv/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://alchimie-euronews-uk-lg.amagi.tv/chunklist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_61_haber_global", number = 61, name = "Haber Global", logoText = "HABER\nGLOBA",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Haber", badge = "HD", logoUrl = "https://i.ibb.co/4ZpZWkf/haber-global.png",
            sources = listOf(StreamSource("HD 720p", "https://tv.ensonhaber.com/haberglobal/haberglobal_720p.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://ensonhaber-live.ercdn.net/haberglobal/haberglobal.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://tv.ensonhaber.com/haberglobal/haberglobal.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "http://tv.ensonhaber.com/haberglobal/haberglobal_720p.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://ensonhaber-live.ercdn.net/haberglobal/haberglobal_1080p.m3u8", StreamType.HLS), StreamSource("HD 720p Hızlı Akış", "https://ensonhaber-live.ercdn.net/haberglobal/haberglobal_720p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_62_habert_rk_tv", number = 62, name = "Habertürk TV", logoText = "HABERT",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Haber", badge = "FHD", logoUrl = "https://i.imgur.com/6Tw3rUp.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://rmtftbjlne.turknet.ercdn.net/bpeytmnqyp/haberturktv/haberturktv_1080p.m3u8", StreamType.HLS), StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://ciner-live.daioncdn.net/haberturktv/haberturktv_1080p.m3u8", StreamType.HLS), StreamSource("Yedek Akış", "https://rmtftbjlne.turknet.ercdn.net/bpeytmnqyp/haberturktv/haberturktv.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "https://ciner.daioncdn.net/haberturktv/haberturktv.m3u8?app=c98ab0b0-50cc-495b-bb37-778e91f5ff5b", StreamType.HLS), StreamSource("Alternatif Akış 5", "https://tv.ensonhaber.com/haberturk/haberturk.m3u8", StreamType.HLS), StreamSource("HD 720p Hızlı Akış", "https://rmtftbjlne.turknet.ercdn.net/bpeytmnqyp/haberturktv/haberturktv_720p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_63_halk_tv", number = 63, name = "Halk TV", logoText = "HALK",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Haber", badge = "FHD", logoUrl = "https://i.imgur.com/xM0HA30.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://halktv-live.daioncdn.net/halktv/halktv_1080p.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://halktv-live.daioncdn.net/halktv/halktv.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://halktv.daioncdn.net/halktv/halktv.m3u8?app=c86957d3-74a7-44da-9ad2-dc358c769609", StreamType.HLS), StreamSource("Alternatif Akış 4", "https://halktv.daioncdn.net/halktv/halktv_1080p.m3u8?app=c86957d3-74a7-44da-9ad2-dc358c769609", StreamType.HLS), StreamSource("Alternatif Akış 5", "https://halktv-live.daioncdn.net/halktv/halktv_720p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://halktv-live.daioncdn.net/halktv/halktv_480p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_64_ntv", number = 64, name = "NTV", logoText = "NTV",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Haber", badge = "HD", logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0c/NTV_%28Turkey%29_logo.svg/960px-NTV_%28Turkey%29_logo.svg.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://dogus.daioncdn.net/ntv/ntv_1080p.m3u8", StreamType.HLS), StreamSource("HD 720p", "https://dogus.daioncdn.net/ntv/ntv.m3u8?app=ntv_web", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://dogus.daioncdn.net/ntv/ntv.m3u8?ce=3", StreamType.HLS), StreamSource("Alternatif Akış 4", "https://tatatam.trt1sultanfatihss.workers.dev/http://dogus.daioncdn.net/ntv/ntv_1080p.m3u8?&sid=8chq5o7vxulf&app=7c4bb802-0c41-409e-8cd2-799984995694&ce=3", StreamType.HLS), StreamSource("Alternatif Akış 5", "https://dogus.daioncdn.net/ntv/ntv_720p.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 6", "https://dogus.daioncdn.net/ntv/ntv.m3u8?app=7c4bb802-0c41-409e-8cd2-799984995694", StreamType.HLS))
        ),
        ch(
            id = "ch_sozcu_tv", number = 65, name = "Sözcü TV", logoText = "SÖZCÜ\nTV",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Haber", badge = "FHD", logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3e/S%C3%B6zc%C3%BC_TV_logo.svg/960px-S%C3%B6zc%C3%BC_TV_logo.svg.png",
            sources = listOf(StreamSource("FHD 1080p (Ana Yayın)", "http://45.88.139.156/yt1/szctv.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://45.88.139.156/yt1/szctv.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_65_tbmm_tv", number = 66, name = "TBMM TV", logoText = "TBMM",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Haber", badge = "HD", logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/TBMM_TV_logo.svg/960px-TBMM_TV_logo.svg.png",
            sources = listOf(StreamSource("HD 720p", "https://meclistv-live.ercdn.net/meclistv/meclistv.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://meclistv-live.ercdn.net/meclistv/meclistv_720p.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://meclistv-live.ercdn.net/meclistv/meclistv.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://meclistv-live.ercdn.net/meclistv/meclistv_1080p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://meclistv-live.ercdn.net/meclistv/meclistv_480p.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "http://meclistv-live.ercdn.net/meclistv/meclistv_1080p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_66_tele_1", number = 67, name = "Tele 1", logoText = "TELE\n1",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Haber", badge = "FHD", logoUrl = "https://upload.wikimedia.org/wikipedia/tr/4/43/Tele1_logosu.png",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://tele1-live.ercdn.net/tele1/tele1.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://tele1-live.ercdn.net/tele1/tele1_1080p.m3u8", StreamType.HLS), StreamSource("HD 720p Hızlı Akış", "https://tele1-live.ercdn.net/tele1/tele1_720p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://tele1-live.ercdn.net/tele1/tele1_480p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_67_telenews", number = 68, name = "Telenews", logoText = "TELENE",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Haber", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://cdn-telenews.yayin.com.tr/telenews/tracks-v1a1/mono.m3u8", StreamType.HLS), StreamSource("Standart Akış", "https://cdn-telenews.yayin.com.tr/telenews/index.m3u8", StreamType.HLS), StreamSource("Adaptif Playlist Akışı", "https://cdn-telenews.yayin.com.tr/telenews/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "http://cdn-telenews.yayin.com.tr/telenews/tracks-v1a1/mono.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_68_tgrt_haber_hd", number = 69, name = "TGRT Haber HD", logoText = "TGRT\nHABER",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Haber", badge = "HD", logoUrl = "https://i.ibb.co/K5frjVY/tgrt-haber.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://tgrthaber-live.daioncdn.net/tgrthaber/tgrthaber_1080p.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://canli.tgrthaber.com/tgrt.m3u8", StreamType.HLS), StreamSource("Standart Akış", "https://tgrthaber-live.daioncdn.net/tgrthaber/tgrthaber.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "https://b01c02nl.mediatriple.net/videoonlylive/mtsxxkzwwuqtglive/broadcast_5fe4598be8e5d.smil/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 5", "https://tgrthaber-live.daioncdn.net/tgrthaber/tgrthaber_720p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://tgrthaber-live.daioncdn.net/tgrthaber/tgrthaber_480p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_69_th_t_rk_haber_tv", number = 70, name = "TH Türk Haber TV", logoText = "TH\nTÜRK",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Haber", badge = "HD", logoUrl = "https://i.ibb.co/ySv65wg/turk-haber.png",
            sources = listOf(StreamSource("Standart Akış", "https://edge1.socialsmart.tv/turkhaber/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://edge1.socialsmart.tv/turkhaber/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/turkhaber/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/turkhaber/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_70_trt_haber", number = 71, name = "TRT Haber", logoText = "TRT\nHABER",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Haber", badge = "HD", logoUrl = "https://i.imgur.com/OVfo8Ab.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://tv-trthaber.medya.trt.com.tr/master_1080.m3u8", StreamType.HLS), StreamSource("HD 720p", "https://tv-trthaber.medya.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://tv-trthaber.medya.trt.com.tr/master_720.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "https://tv.ensonhaber.com/trthaber/trthaber.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 5", "https://ensonhaber-live.ercdn.net/trthaber/trthaber.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 6", "http://tv-trthaber.medya.trt.com.tr/master.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_71_tvnet", number = 72, name = "TVNET", logoText = "TVNET",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Haber", badge = "FHD", logoUrl = "https://i.imgur.com/mQo8yWQ.png",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://mn-nl.mncdn.com/tvnet/tvnet/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://tvnet-live.lg.mncdn.com/tvnet/tvnet/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://mn-nl.mncdn.com/tvnet/tvnet/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://mn-nl.mncdn.com/tvnet/tvnet/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_72_tyt_t_rk", number = 73, name = "TYT Türk", logoText = "TYT\nTÜRK",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Haber", badge = "FHD", logoUrl = "",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://tytturk-live.ercdn.net/tytturk/tytturk.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://tytturk-live.ercdn.net/tytturk/tytturk_1080p.m3u8", StreamType.HLS), StreamSource("HD 720p Hızlı Akış", "https://tytturk-live.ercdn.net/tytturk/tytturk_720p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://tytturk-live.ercdn.net/tytturk/tytturk_480p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_73__lke_tv", number = 74, name = "Ülke TV", logoText = "ÜLKE",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Haber", badge = "HD", logoUrl = "https://i.imgur.com/wdWR7Qk.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://livetv.radyotvonline.net/kanal7live/ulketv/chunklist.m3u8", StreamType.HLS), StreamSource("HD 720p", "https://livetv.radyotvonline.net/kanal7live/ulketv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://stream.ilketv.com.tr/hls/ilkecanli.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://livetv.radyotvonline.net/kanal7live/ulketv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_74_agro_tv", number = 75, name = "Agro TV", logoText = "AGRO",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Belgesel", badge = "HD", logoUrl = "https://i.ibb.co/1RnLJpf/agro.png",
            sources = listOf(StreamSource("Standart Akış", "https://yayin30.haber100.com/live/agrotv/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://yayin30.haber100.com/live/agrotv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://yayin30.haber100.com/live/agrotv/index.m3u8", StreamType.HLS))
        )
    )

    private fun getBuiltInChannelsChunk3(): List<TvChannel> = listOf(
        ch(
            id = "ch_75_bizimev_tv", number = 76, name = "Bizimev TV", logoText = "BIZIME",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Belgesel", badge = "HD", logoUrl = "https://i.imgur.com/eJa6sFY.png",
            sources = listOf(StreamSource("HD 720p", "https://mn-nl.mncdn.com/blutv_bizimev/bizimev_sd.smil/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://mn-nl.mncdn.com/blutv_bizimev/bizimev_sd.smil/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://mn-nl.mncdn.com/blutv_bizimev/bizimev_sd.smil/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_76_cgtn_documentary", number = 77, name = "CGTN Documentary", logoText = "CGTN\nDOCUM",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Belgesel", badge = "HD", logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d6/CGTN_Documentary_logo.png/960px-CGTN_Documentary_logo.png",
            sources = listOf(StreamSource("Standart Akış", "https://mn-nl.mncdn.com/dogusdyg_drone/cgtn/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://mn-nl.mncdn.com/dogusdyg_drone/cgtn/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://mn-nl.mncdn.com/dogusdyg_drone/cgtn/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_77_gzt_tv", number = 78, name = "GZT TV", logoText = "GZT",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Belgesel", badge = "FHD", logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ef/GZT_logo.svg/960px-GZT_logo.svg.png",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://gzttv-live.lg.mncdn.com/gzttv-live/gzttv/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://gzttv-live.lg.mncdn.com/gzttv-live/gzttv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://gzttv-live.lg.mncdn.com/gzttv-live/gzttv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_78_love_nature_hd", number = 79, name = "Love Nature HD", logoText = "LOVE\nNATUR",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Belgesel", badge = "FHD", logoUrl = "",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://lovenatureus-eng-plex.amagi.tv/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://lovenatureus-eng-plex.amagi.tv/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://lovenatureus-eng-plex.amagi.tv/chunklist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_79_national_geographic", number = 80, name = "National Geographic", logoText = "NATIO\nGEOGR",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Belgesel", badge = "FHD", logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fc/Natgeologo.svg/960px-Natgeologo.svg.png",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://saran-live.ercdn.net/natgeohd/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Playlist)", "https://saran-live.ercdn.net/natgeohd/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Master)", "https://saran-live.ercdn.net/natgeohd/master.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_80_national_geographic_", number = 81, name = "National Geographic Wild", logoText = "NATIO\nGEOGR",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Belgesel", badge = "FHD", logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/27/National_Geographic_Wild_logo.svg/960px-National_Geographic_Wild_logo.svg.png",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://saran-live.ercdn.net/natgeowild/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Playlist)", "https://saran-live.ercdn.net/natgeowild/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Master)", "https://saran-live.ercdn.net/natgeowild/master.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_81_tgrt_belgesel", number = 82, name = "TGRT Belgesel", logoText = "TGRT\nBELGE",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Belgesel", badge = "HD", logoUrl = "https://i.ibb.co/JHH0xcX/tgrtbel22.png",
            sources = listOf(StreamSource("Standart Akış", "https://tgrt-live.ercdn.net/tgrtbelgesel/tgrtbelgesel.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://b01c02nl.mediatriple.net/videoonlylive/mtsxxkzwwuqtglive/broadcast_5fe462afc6a0e.smil/playlist.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://tgrt-live.ercdn.net/tgrtbelgesel/tgrtbelgesel_1080p.m3u8", StreamType.HLS), StreamSource("HD 720p Hızlı Akış", "https://tgrt-live.ercdn.net/tgrtbelgesel/tgrtbelgesel_720p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://tgrt-live.ercdn.net/tgrtbelgesel/tgrtbelgesel_480p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_82_trt_belgesel", number = 83, name = "TRT Belgesel", logoText = "TRT\nBELGE",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Belgesel", badge = "FHD", logoUrl = "https://i.imgur.com/MGO87pe.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://tv-trtbelgesel.medya.trt.com.tr/master_1080.m3u8", StreamType.HLS), StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://tv-trtbelgesel-dai.medya.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("Yedek Akış", "https://tv-trtbelgesel.medya.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("HD 720p (Hızlı Yedek)", "https://tv-trtbelgesel.medya.trt.com.tr/master_720.m3u8", StreamType.HLS), StreamSource("SD 480p (Kesintisiz Yedek)", "https://tv-trtbelgesel.medya.trt.com.tr/master_480.m3u8", StreamType.HLS), StreamSource("FHD 1080p (TRT CDN)", "https://tv-trtbelgesel-dai.medya.trt.com.tr/master_1080.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_83_wild_earth_tv", number = 84, name = "Wild Earth TV", logoText = "WILD\nEARTH",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Belgesel", badge = "FHD", logoUrl = "",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://wildearth-adsfree-ono.amagi.tv/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://wildearth-adsfree-ono.amagi.tv/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://wildearth-adsfree-ono.amagi.tv/chunklist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_84_yaban_tv", number = 85, name = "Yaban TV", logoText = "YABAN",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Belgesel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("HD 720p", "https://59cba4d34b678.streamlock.net/live1/yabantv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://sewv654wfcsdwfi87fwvgbngh.siauliairsavlt.pw/iptv/9XRHZ7MZ9PNWS6/6868/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://59cba4d34b678.streamlock.net/live1/yabantv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://59cba4d34b678.streamlock.net/live1/yabantv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_85__ift_i_tv", number = 86, name = "Çiftçi TV", logoText = "ÇIFTÇI",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Belgesel", badge = "HD", logoUrl = "https://i.ibb.co/vHwMgx0/ciftci.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://live.artidijitalmedya.com/artidijital_ciftcitv/ciftcitv/playlist.m3u8", StreamType.HLS), StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_ciftcitv/ciftcitv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_ciftcitv/ciftcitv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_ciftcitv/ciftcitv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_86_dream_t_rk", number = 87, name = "Dream Türk", logoText = "DREAM\nTÜRK",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Müzik", badge = "HD", logoUrl = "https://i.ibb.co/fxRrKmB/dream-turk.png",
            sources = listOf(StreamSource("HD 720p", "https://live.duhnet.tv/S2/HLS_LIVE/dreamturknp/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://live.duhnet.tv//S2/HLS_LIVE/dreamturknp/track_4_1250/playlist.m3u8", StreamType.HLS), StreamSource("Duhnet HD (Track 4)", "https://live.duhnet.tv/S2/HLS_LIVE/dreamturknp/track_4_1000/playlist.m3u8", StreamType.HLS), StreamSource("Duhnet Hızlı (Track 2)", "https://live.duhnet.tv/S2/HLS_LIVE/dreamturknp/track_2_550/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_87_kral_pop_tv", number = 88, name = "Kral Pop TV", logoText = "KRAL\nPOP",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Müzik", badge = "HD", logoUrl = "https://i.imgur.com/ch365lh.png",
            sources = listOf(StreamSource("Standart Akış", "https://dogus-live.daioncdn.net/kralpoptv/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış", "http://dygvideo.dygdigital.com/live/hls/kralpop", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://dygvideo.dygdigital.com/live/hls/puhukralpopdai?m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "https://dogus.daioncdn.net/kralpoptv/kralpoptv.m3u8?app=f38a38b4-ce55-4040-8676-9826937d6128", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://dogus.daioncdn.net/kralpoptv/kralpoptv_1080p.m3u8?app=f38a38b4-ce55-4040-8676-9826937d6128", StreamType.HLS), StreamSource("HD 720p Hızlı Akış", "https://dogus.daioncdn.net/kralpoptv/kralpoptv_720p.m3u8?app=f38a38b4-ce55-4040-8676-9826937d6128", StreamType.HLS))
        ),
        ch(
            id = "ch_88_number_1_a_k", number = 89, name = "Number 1 Aşk", logoText = "NUMBE\n1",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Müzik", badge = "HD", logoUrl = "https://i.ibb.co/5cJ0KtQ/nr1-ask.png",
            sources = listOf(StreamSource("Standart Akış", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e18f9cea15_1/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e18f9cea15_1/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e18f9cea15_1/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e18f9cea15_1/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_89_number_1_damar", number = 90, name = "Number 1 Damar", logoText = "NUMBE\n1",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Müzik", badge = "HD", logoUrl = "https://i.ibb.co/2yCF7ms/nr1-damar.png",
            sources = listOf(StreamSource("Standart Akış", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e198784bdc_1/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e198784bdc_1/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e198784bdc_1/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e198784bdc_1/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_90_number_1_dance", number = 91, name = "Number 1 Dance", logoText = "NUMBE\n1",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Müzik", badge = "HD", logoUrl = "https://i.ibb.co/kXFSFTN/nr1-dance.png",
            sources = listOf(StreamSource("Standart Akış", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e2aa8acf44_1/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e2aa8acf44_1/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e2aa8acf44_1/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e2aa8acf44_1/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_91_number_1_tv_hd", number = 92, name = "Number 1 TV HD", logoText = "NUMBE\n1",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Müzik", badge = "HD", logoUrl = "https://i.ibb.co/sybLTBG/nr1.png",
            sources = listOf(StreamSource("Standart Akış", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e17cd6360b_1/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/broadcast_5c9e17cd59e8b.smil/chunklist_b400000.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/broadcast_5c9e187770143.smil/chunklist_b400000.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/broadcast_5c9e17cd59e8b.smil/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_92_number_1_t_rk", number = 93, name = "Number 1 Türk", logoText = "NUMBE\n1",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Müzik", badge = "HD", logoUrl = "https://i.imgur.com/18zjk3q.png",
            sources = listOf(StreamSource("HD 720p", "https://mn-nl.mncdn.com/blutv_nr1turk2/live.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/broadcast_5c9e17cd59e8b.smil/chunklist_b400000.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/broadcast_5c9e187770143.smil/chunklist_b400000.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/broadcast_5c9e17cd59e8b.smil/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_93_power_dance", number = 94, name = "Power Dance", logoText = "POWER\nDANCE",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Müzik", badge = "HD", logoUrl = "https://i.ibb.co/5hwZ7xw/power-dance.png",
            sources = listOf(StreamSource("Standart Akış", "https://listen.powerapp.com.tr/dance/dance.smil/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://livetv.powerapp.com.tr/dance/dance.smil/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://listen.powerapp.com.tr/dance/dance.smil/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://listen.powerapp.com.tr/dance/dance.smil/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_94_power_love", number = 95, name = "Power Love", logoText = "POWER\nLOVE",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Müzik", badge = "HD", logoUrl = "https://i.ibb.co/bJL1VnX/power-love.png",
            sources = listOf(StreamSource("Standart Akış", "https://listen.powerapp.com.tr/plove/love.smil/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://livetv.powerapp.com.tr/plove/love.smil/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://listen.powerapp.com.tr/plove/love.smil/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://listen.powerapp.com.tr/plove/love.smil/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_95_power_tv", number = 96, name = "Power TV", logoText = "POWER",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Müzik", badge = "HD", logoUrl = "https://i.ibb.co/JrXDQJy/power-tv.png",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_powertv/powertv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://livetv.powerapp.com.tr/powerTV/powerhd.smil/playlists.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://livetv.powerapp.com.tr/powerTV/powerhd.smil/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_powertv/powertv/chunks.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_96_power_tv_hd", number = 97, name = "Power TV HD", logoText = "POWER",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Müzik", badge = "HD", logoUrl = "https://i.ibb.co/JrXDQJy/power-tv.png",
            sources = listOf(StreamSource("Standart Akış", "https://livetv.powerapp.com.tr/powerTV/powerhd.smil/chunklist_b2650000_sltur.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://live.artidijitalmedya.com/artidijital_powertv/powertv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://livetv.powerapp.com.tr/powerTV/powerhd.smil/playlists.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "https://livetv.powerapp.com.tr/powerTV/powerhd.smil/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_powertv/powertv/chunks.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_97_powert_rk_akustik", number = 98, name = "PowerTürk Akustik", logoText = "POWER\nAKUST",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Müzik", badge = "HD", logoUrl = "https://i.ibb.co/XFY7dCW/powerturk-akustik.png",
            sources = listOf(StreamSource("Standart Akış", "https://listen.powerapp.com.tr/pturkakustik/akustik.smil/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://listen.powerapp.com.tr/pturkakustik/akustik.smil/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://listen.powerapp.com.tr/pturkakustik/akustik.smil/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_98_powert_rk_slow", number = 99, name = "PowerTürk Slow", logoText = "POWER\nSLOW",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Müzik", badge = "HD", logoUrl = "https://i.ibb.co/R4pcKqK/powerturk-slow.png",
            sources = listOf(StreamSource("Standart Akış", "https://listen.powerapp.com.tr/pturkslow/slow.smil/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://listen.powerapp.com.tr/pturkslow/slow.smil/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://listen.powerapp.com.tr/pturkslow/slow.smil/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_99_powert_rk_taptaze", number = 100, name = "PowerTürk Taptaze", logoText = "POWER\nTAPTA",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Müzik", badge = "HD", logoUrl = "https://i.ibb.co/F5K5xs9/powerturk-taptaze.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://livetv.powerapp.com.tr/pturktaptaze/taptaze.smil/playlist.m3u8", StreamType.HLS), StreamSource("Standart Akış", "https://listen.powerapp.com.tr/pturktaptaze/taptaze.smil/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://livetv.powerapp.com.tr/pturktaptaze/taptaze.smil/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://livetv.powerapp.com.tr/pturktaptaze/taptaze.smil/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://livetv.powerapp.com.tr/pturktaptaze/taptaze.smil/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_100_powert_rk_tv_hd", number = 101, name = "PowerTürk TV HD", logoText = "POWERT",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Müzik", badge = "FHD", logoUrl = "https://i.ibb.co/xXcBD2C/powerturk.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://live.artidijitalmedya.com/artidijital_powerturktv/powerturktv/playlist.m3u8", StreamType.HLS), StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://livetv.powerapp.com.tr/powerturkTV/powerturkhd.smil/playlists.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://livetv.powerapp.com.tr/powerturkTV/powerturkhd.smil/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_powerturktv/powerturktv/chunks.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_101_slow_karadeniz_tv", number = 102, name = "Slow Karadeniz TV", logoText = "SLOW\nKARAD",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Müzik", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://yayin.slowkaradeniztv.com:3390/hybrid/play.m3u8", StreamType.HLS), StreamSource("Standart Akış", "https://yayin.slowkaradeniztv.com:3390/multi_web/play.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://yayin.slowkaradeniztv.com:3390/hybrid/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "https://yayin.slowkaradeniztv.com:3390/hybrid/chunklist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_102_tatl_ses_tv", number = 103, name = "Tatlıses TV", logoText = "TATLIS",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Müzik", badge = "HD", logoUrl = "https://i.ibb.co/yRy9zwt/tatlises-tv.png",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_tatlisestv/tatlisestv/chunks.m3u8", StreamType.HLS), StreamSource("Adaptif Akış (Playlist)", "https://live.artidijitalmedya.com/artidijital_tatlisestv/tatlisestv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://live.artidijitalmedya.com/artidijital_tatlisestv/tatlisestv/chunks.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_103_trt_m_zik", number = 104, name = "TRT Müzik", logoText = "TRT\nMÜZIK",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Müzik", badge = "HD", logoUrl = "https://i.imgur.com/fIVFCEd.png",
            sources = listOf(StreamSource("HD 720p", "https://tv-trtmuzik.medya.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://tv-trtmuzik.medya.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("FHD 1080p (TRT CDN)", "https://tv-trtmuzik.medya.trt.com.tr/master_1080.m3u8", StreamType.HLS), StreamSource("HD 720p (Hızlı Yedek)", "https://tv-trtmuzik.medya.trt.com.tr/master_720.m3u8", StreamType.HLS), StreamSource("SD 480p (Kesintisiz Yedek)", "https://tv-trtmuzik.medya.trt.com.tr/master_480.m3u8", StreamType.HLS), StreamSource("FHD 1080p (TRT CDN)", "http://tv-trtmuzik.medya.trt.com.tr/master_1080.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_104_babytv_t_rkiye", number = 105, name = "BabyTV Türkiye", logoText = "BABYTV",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Çocuk", badge = "FHD", logoUrl = "https://i.imgur.com/4BDJ5FT.png",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://saran-live.ercdn.net/babytv/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Playlist)", "https://saran-live.ercdn.net/babytv/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Master)", "https://saran-live.ercdn.net/babytv/master.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_105_disney_junior", number = 106, name = "Disney Junior", logoText = "DISNE\nJUNIO",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Çocuk", badge = "FHD", logoUrl = "https://www.dsmart.com.tr/api/v1/public/images/kanallar/disneyjr.png",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://saran-live.ercdn.net/disneyjunior/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Playlist)", "https://saran-live.ercdn.net/disneyjunior/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Master)", "https://saran-live.ercdn.net/disneyjunior/master.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_106_minika_go", number = 107, name = "Minika Go", logoText = "MINIK\nGO",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Çocuk", badge = "HD", logoUrl = "https://i.imgur.com/qIoipDq.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/minikago/minikago_720p.m3u8", StreamType.HLS), StreamSource("HD 720p", "http://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/minikago/minikago_720p.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/minikago/minikago.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/minikago/minikago_1080p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/minikago/minikago_480p.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "http://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/minikago/minikago_1080p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_107_minika__ocuk", number = 108, name = "Minika Çocuk", logoText = "MINIK\nÇOCUK",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Çocuk", badge = "HD", logoUrl = "https://i.imgur.com/VCywMTv.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/minikago_cocuk/minikago_cocuk_720p.m3u8", StreamType.HLS), StreamSource("HD 720p", "http://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/minikago_cocuk/minikago_cocuk_720p.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/minikago_cocuk/minikago_cocuk.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/minikago_cocuk/minikago_cocuk_1080p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/minikago_cocuk/minikago_cocuk_480p.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "http://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/minikago_cocuk/minikago_cocuk_1080p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_108_trt_diyanet__ocuk", number = 109, name = "TRT Diyanet Çocuk", logoText = "TRT\nDIYAN",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Çocuk", badge = "HD", logoUrl = "https://i.imgur.com/8PmXz9t.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://tv-trtdiyanetcocuk.medya.trt.com.tr/master_1080.m3u8", StreamType.HLS), StreamSource("HD 720p", "https://tv-trtdiyanetcocuk.medya.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://tv-trtdiyanetcocuk.medya.trt.com.tr/master_1440.m3u8", StreamType.HLS), StreamSource("HD 720p (Hızlı Yedek)", "https://tv-trtdiyanetcocuk.medya.trt.com.tr/master_720.m3u8", StreamType.HLS), StreamSource("SD 480p (Kesintisiz Yedek)", "https://tv-trtdiyanetcocuk.medya.trt.com.tr/master_480.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_109_trt_eba_i_lkokul", number = 110, name = "TRT EBA İlkokul", logoText = "TRT\nEBA",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Çocuk", badge = "HD", logoUrl = "https://i.imgur.com/wDvZfk8.png",
            sources = listOf(StreamSource("HD 720p", "https://tv-e-okul00.medya.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("FHD 1080p (TRT CDN)", "https://tv-e-okul00.medya.trt.com.tr/master_1080.m3u8", StreamType.HLS), StreamSource("HD 720p (Hızlı Yedek)", "https://tv-e-okul00.medya.trt.com.tr/master_720.m3u8", StreamType.HLS), StreamSource("SD 480p (Kesintisiz Yedek)", "https://tv-e-okul00.medya.trt.com.tr/master_480.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_110_trt_eba_lise", number = 111, name = "TRT EBA Lise", logoText = "TRT\nEBA",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Çocuk", badge = "HD", logoUrl = "https://i.imgur.com/IebUZx1.png",
            sources = listOf(StreamSource("HD 720p", "https://tv-e-okul02.medya.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("FHD 1080p (TRT CDN)", "https://tv-e-okul02.medya.trt.com.tr/master_1080.m3u8", StreamType.HLS), StreamSource("HD 720p (Hızlı Yedek)", "https://tv-e-okul02.medya.trt.com.tr/master_720.m3u8", StreamType.HLS), StreamSource("SD 480p (Kesintisiz Yedek)", "https://tv-e-okul02.medya.trt.com.tr/master_480.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_111_trt_eba_ortaokul", number = 112, name = "TRT EBA Ortaokul", logoText = "TRT\nEBA",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Çocuk", badge = "HD", logoUrl = "https://i.imgur.com/yfPTvRx.png",
            sources = listOf(StreamSource("HD 720p", "https://tv-e-okul01.medya.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("FHD 1080p (TRT CDN)", "https://tv-e-okul01.medya.trt.com.tr/master_1080.m3u8", StreamType.HLS), StreamSource("HD 720p (Hızlı Yedek)", "https://tv-e-okul01.medya.trt.com.tr/master_720.m3u8", StreamType.HLS), StreamSource("SD 480p (Kesintisiz Yedek)", "https://tv-e-okul01.medya.trt.com.tr/master_480.m3u8", StreamType.HLS))
        )
    )

    private fun getBuiltInChannelsChunk4(): List<TvChannel> = listOf(
        ch(
            id = "ch_112_trt_gen_", number = 113, name = "TRT Genç", logoText = "TRT\nGENÇ",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Çocuk", badge = "FHD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://tv-trtgenc.medya.trt.com.tr/master_1080.m3u8", StreamType.HLS), StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://tv-trtgenc.medya.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("HD 720p (Hızlı Yedek)", "https://tv-trtgenc.medya.trt.com.tr/master_720.m3u8", StreamType.HLS), StreamSource("SD 480p (Kesintisiz Yedek)", "https://tv-trtgenc.medya.trt.com.tr/master_480.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_113_trt__ocuk", number = 114, name = "TRT Çocuk", logoText = "TRT\nÇOCUK",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Çocuk", badge = "HD", logoUrl = "https://i.imgur.com/QLFmD6d.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://tv-trtcocuk.medya.trt.com.tr/master_1080.m3u8", StreamType.HLS), StreamSource("HD 720p", "https://tv-trtcocuk.medya.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://tv-trtcocuk.medya.trt.com.tr/master_720.m3u8", StreamType.HLS), StreamSource("SD 480p (Kesintisiz Yedek)", "https://tv-trtcocuk.medya.trt.com.tr/master_480.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_114_brt_1_hd_k_br_s", number = 115, name = "BRT 1 HD Kıbrıs", logoText = "BRT\n1",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Kıbrıs", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge.kuzeykibrissmart.tv/brt1hd/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://edge.kuzeykibrissmart.tv/brt1hd/bant1/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://edge.kuzeykibrissmart.tv/brt1hd/bant1/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_115_brt_2_hd_k_br_s", number = 116, name = "BRT 2 HD Kıbrıs", logoText = "BRT\n2",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Kıbrıs", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge.kuzeykibrissmart.tv/brt2hd/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://edge.kuzeykibrissmart.tv/brt2hd/bant1/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://edge.kuzeykibrissmart.tv/brt2hd/bant1/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_116_kanal_t_k_br_s", number = 117, name = "Kanal T Kıbrıs", logoText = "KANAL\nT",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Kıbrıs", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge.kuzeykibrissmart.tv/kanalt/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://edge.kuzeykibrissmart.tv/kanalt/bant1/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://edge.kuzeykibrissmart.tv/kanalt/bant1/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_117_ktv_k_br_s_tv", number = 118, name = "KTV Kıbrıs TV", logoText = "KTV\nKIBRI",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Kıbrıs", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge.kuzeykibrissmart.tv/kibristv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://edge.kuzeykibrissmart.tv/kibristv/bant1/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://edge.kuzeykibrissmart.tv/kibristv/bant1/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_118_k_br_s_ada_tv", number = 119, name = "Kıbrıs Ada TV", logoText = "KIBRI\nADA",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Kıbrıs", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge.kuzeykibrissmart.tv/adatv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://edge.kuzeykibrissmart.tv/adatv/bant1/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://edge.kuzeykibrissmart.tv/adatv/bant1/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_119_k_br_s_gen__tv", number = 120, name = "Kıbrıs Genç TV", logoText = "KIBRI\nGENÇ",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Kıbrıs", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge.kuzeykibrissmart.tv/kibrisgenctv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://edge.kuzeykibrissmart.tv/kibrisgenctv/bant1/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://edge.kuzeykibrissmart.tv/kibrisgenctv/bant1/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_120_sim_tv_k_br_s", number = 121, name = "Sim TV Kıbrıs", logoText = "SIM\nKIBRI",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Kıbrıs", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge.kuzeykibrissmart.tv/simtv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://edge.kuzeykibrissmart.tv/simtv/bant1/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://edge.kuzeykibrissmart.tv/simtv/bant1/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_121_tv_2020_k_br_s", number = 122, name = "TV 2020 Kıbrıs", logoText = "2020\nKIBRI",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Kıbrıs", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge.kuzeykibrissmart.tv/tv2020/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://edge.kuzeykibrissmart.tv/tv2020/bant1/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://edge.kuzeykibrissmart.tv/tv2020/bant1/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_122_al_zahra_t_rk_tv", number = 123, name = "Al Zahra Türk TV", logoText = "AL\nZAHRA",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Dini", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.al-zahratv.com/live/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.al-zahratv.com/live/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.al-zahratv.com/live/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_123_diyanet_tv", number = 124, name = "Diyanet TV", logoText = "DIYANE",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Dini", badge = "FHD", logoUrl = "https://i.ibb.co/k9phm3v/diyanettv.png",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://eustr73.mediatriple.net/videoonlylive/mtikoimxnztxlive/broadcast_5e3bf95a47e07.smil/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://eustr73.mediatriple.net/videoonlylive/mtikoimxnztxlive/broadcast_5e3bf95a47e07.smil/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://eustr73.mediatriple.net/videoonlylive/mtikoimxnztxlive/broadcast_5e3bf95a47e07.smil/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_124_dost_tv", number = 125, name = "Dost TV", logoText = "DOST",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Dini", badge = "HD", logoUrl = "https://i.ibb.co/jwgqtSB/dosttv.png",
            sources = listOf(StreamSource("Standart Akış", "https://dost.stream.emsal.im/tv/live.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://dost.stream.emsal.im/tv/live.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_125_gonca_tv", number = 126, name = "Gonca TV", logoText = "GONCA",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Dini", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge.taksimbilisim.com/goncatv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://edge.taksimbilisim.com/goncatv/bant1/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://edge.taksimbilisim.com/goncatv/bant1/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_126_kanal_on4_tv", number = 127, name = "Kanal On4 TV", logoText = "KANAL\nON4",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Dini", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge1.socialsmart.tv/on4/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Adaptif Akış (Edge1 Playlist)", "https://edge1.socialsmart.tv/on4/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/on4/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/on4/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_127_kud_s_tv", number = 128, name = "Kudüs TV", logoText = "KUDÜS",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Dini", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://yayin30.haber100.com/live/kudustv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Playlist)", "https://yayin30.haber100.com/live/kudustv/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_128_laleg_l_tv", number = 129, name = "Lalegül TV", logoText = "LALEGÜ",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Dini", badge = "HD", logoUrl = "https://i.ibb.co/wrL2khn/lalegul.png",
            sources = listOf(StreamSource("Standart Akış", "https://lbl.netmedya.net/hls/lalegultv.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://lbl.netmedya.net/hls/lalegultv.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_129_mukabele_tv", number = 130, name = "Mukabele TV", logoText = "MUKABE",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Dini", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/mukabele-tv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/mukabele-tv/playlist/0.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/mukabele-tv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/mukabele-tv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_130_rehber_tv", number = 131, name = "Rehber TV", logoText = "REHBER",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Dini", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://cdn4.yayin.com.tr/rehbertv/tracks-v1a1/mono.m3u8", StreamType.HLS), StreamSource("Adaptif Playlist Akışı", "https://cdn4.yayin.com.tr/rehbertv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://cdn4.yayin.com.tr/rehbertv/tracks-v1a1/mono.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_131_semerkand_tv", number = 132, name = "Semerkand TV", logoText = "SEMERK",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Dini", badge = "HD", logoUrl = "https://i.ibb.co/k8L3Mq1/semerkandtv.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://b01c02nl.mediatriple.net/videoonlylive/mtisvwurbfcyslive/broadcast_58d915bd40efc.smil/mt_0.m3u8", StreamType.HLS), StreamSource("Standart Akış", "https://b01c02nl.mediatriple.net/videoonlylive/mtisvwurbfcyslive/broadcast_58d915bd40efc.smil/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://b01c02nl.mediatriple.net/videoonlylive/mtisvwurbfcyslive/broadcast_58d915bd40efc.smil/mt_0.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://b01c02nl.mediatriple.net/videoonlylive/mtisvwurbfcyslive/broadcast_58d915bd40efc.smil/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://b01c02nl.mediatriple.net/videoonlylive/mtisvwurbfcyslive/broadcast_58d915bd40efc.smil/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_132_semerkand_tv_way", number = 133, name = "Semerkand TV Way", logoText = "SEMER\nWAY",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Dini", badge = "HD", logoUrl = "https://i.ibb.co/k8L3Mq1/semerkandtv.png",
            sources = listOf(StreamSource("Standart Akış", "https://b01c02nl.mediatriple.net/videoonlylive/mtisvwurbfcyslive/broadcast_6409fdaa68111.smil/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://b01c02nl.mediatriple.net/videoonlylive/mtisvwurbfcyslive/broadcast_6409fdaa68111.smil/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://b01c02nl.mediatriple.net/videoonlylive/mtisvwurbfcyslive/broadcast_6409fdaa68111.smil/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_133_vav_tv", number = 134, name = "Vav TV", logoText = "VAV",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Dini", badge = "FHD", logoUrl = "https://i.ibb.co/bj1H8LCm/vavtv.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/kltr-sanat-tv/playlist/0.m3u8", StreamType.HLS), StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://trkvz-live.ercdn.net/vavtv/vavtv.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/kltr-sanat-tv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "http://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/kltr-sanat-tv/playlist/0.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://trkvz-live.ercdn.net/vavtv/vavtv_1080p.m3u8", StreamType.HLS), StreamSource("HD 720p Hızlı Akış", "https://trkvz-live.ercdn.net/vavtv/vavtv_720p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_134_aksu_tv_kahramanmara", number = 135, name = "Aksu TV Kahramanmaraş", logoText = "AKSU\nKAHRA",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_aksutv/aksutv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_aksutv/aksutv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_aksutv/aksutv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_aksutv/aksutv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_135_alanya_posta_tv", number = 136, name = "Alanya Posta TV", logoText = "ALANY\nPOSTA",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Yerel", badge = "FHD", logoUrl = "",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://api-tv3.yayin.com.tr/postatv/postatv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Chunks Akışı", "https://api-tv3.yayin.com.tr/postatv/postatv/chunks.m3u8", StreamType.HLS), StreamSource("Mono Doğrudan Akış", "https://api-tv3.yayin.com.tr/postatv/postatv/tracks-v1a1/mono.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://api-tv3.yayin.com.tr/postatv/postatv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://api-tv3.yayin.com.tr/postatv/postatv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_136_alta__tv_ordu", number = 137, name = "Altaş TV Ordu", logoText = "ALTAŞ\nORDU",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Yerel", badge = "FHD", logoUrl = "",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://edge1.socialsmart.tv/altastv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Akış (Edge1 Chunks)", "https://edge1.socialsmart.tv/altastv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/altastv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/altastv/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_137_anadolu_net_tv", number = 138, name = "Anadolu Net TV", logoText = "ANADO\nNET",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_anadolunet/anadolunet/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://live.artidijitalmedya.com/artidijital_anadolunet/anadolunet/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_anadolunet/anadolunet/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_anadolunet/anadolunet/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_138_aras_tv", number = 139, name = "Aras TV", logoText = "ARAS",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://2.rtmp.org/tv217/yayin.stream/chunklist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://1.rtmp.org/tv217/yayin.stream/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Playlist)", "https://2.rtmp.org/tv217/yayin.stream/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "http://1.rtmp.org/tv217/yayin.stream/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "http://1.rtmp.org/tv217/yayin.stream/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_139_art_world_tv", number = 140, name = "Art World TV", logoText = "ART\nWORLD",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_artworld/artworld/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://live.artidijitalmedya.com/artidijital_artworld/artworld/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_artworld/artworld/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_artworld/artworld/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_140_bir_tv", number = 141, name = "Bir TV", logoText = "BIR",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://edge.socialsmart.tv/birtv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Standart Akış", "https://edge1.socialsmart.tv/birtv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Adaptif Akış (Edge1 Playlist)", "https://edge1.socialsmart.tv/birtv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/birtv/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_141_bodrum_kent_tv", number = 142, name = "Bodrum Kent TV", logoText = "BODRU\nKENT",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge1.socialsmart.tv/bodrumkenttv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Adaptif Akış (Edge1 Playlist)", "https://edge1.socialsmart.tv/bodrumkenttv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/bodrumkenttv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/bodrumkenttv/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_142_brtv_karab_k", number = 143, name = "BRTV Karabük", logoText = "BRTV\nKARAB",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_brtv/brtv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_brtv/brtv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_brtv/brtv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_brtv/brtv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_143_bursa_as_tv", number = 144, name = "Bursa AS TV", logoText = "BURSA\nAS",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_astv/astv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_astv/astv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_astv/astv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_astv/astv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_144_bursa_line_tv", number = 145, name = "Bursa Line TV", logoText = "BURSA\nLINE",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge1.socialsmart.tv/linetv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Adaptif Akış (Edge1 Playlist)", "https://edge1.socialsmart.tv/linetv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/linetv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/linetv/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_145_bursa_on6_tv", number = 146, name = "Bursa On6 TV", logoText = "BURSA\nON6",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_kanal16/kanal16/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_kanal16/kanal16/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_kanal16/kanal16/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_kanal16/kanal16/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_146_deha_tv_denizli", number = 147, name = "Deha TV Denizli", logoText = "DEHA\nDENIZ",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_dehatv/dehatv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_dehatv/dehatv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_dehatv/dehatv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_dehatv/dehatv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_147_deniz_postas__tv_kay", number = 148, name = "Deniz Postası TV Kayseri", logoText = "DENIZ\nPOSTA",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://live.artidijitalmedya.com/artidijital_denizpostasi/denizpostasi/chunks.m3u8", StreamType.HLS), StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_denizpostasi/denizpostasi/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://live.artidijitalmedya.com/artidijital_denizpostasi/denizpostasi/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_denizpostasi/denizpostasi/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_denizpostasi/denizpostasi/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_148_derin_tv", number = 149, name = "Derin TV", logoText = "DERIN",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://cdn4.yayin.com.tr/derintv/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Playlist)", "https://cdn4.yayin.com.tr/derintv/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Master)", "https://cdn4.yayin.com.tr/derintv/master.m3u8", StreamType.HLS))
        )
    )

    private fun getBuiltInChannelsChunk5(): List<TvChannel> = listOf(
        ch(
            id = "ch_149_dim_tv_alanya", number = 150, name = "Dim TV Alanya", logoText = "DIM\nALANY",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_dimtv/dimtv/chunks.m3u8", StreamType.HLS), StreamSource("Adaptif Akış (Playlist)", "https://live.artidijitalmedya.com/artidijital_dimtv/dimtv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://live.artidijitalmedya.com/artidijital_dimtv/dimtv/chunks.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_150_edirne_tv", number = 151, name = "Edirne TV", logoText = "EDIRNE",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://yayin.edirnetv.com:8088/hls/etvcanliyayin.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://yayin.edirnetv.com:8088/hls/etvcanliyayin.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_151_er_tv_malatya", number = 152, name = "Er TV Malatya", logoText = "ER\nMALAT",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_ertv_new/ertv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_ertv_new/ertv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_ertv_new/ertv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_ertv_new/ertv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_152_erciyes_tv_kayseri", number = 153, name = "Erciyes TV Kayseri", logoText = "ERCIY\nKAYSE",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_erciyestv/erciyestv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_erciyestv/erciyestv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_erciyestv/erciyestv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_erciyestv/erciyestv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_153_erci__tv_van", number = 154, name = "Erciş TV Van", logoText = "ERCIŞ\nVAN",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://cdn1-ercistv.yayin.com.tr/ercistv/amlst:ercistv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Playlist)", "https://cdn1-ercistv.yayin.com.tr/ercistv/amlst:ercistv/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_154_es_tv_eski_ehir", number = 155, name = "ES TV Eskişehir", logoText = "ES\nESKIŞ",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_estv/estv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://live.artidijitalmedya.com/artidijital_estv/estv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_estv/estv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_estv/estv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_155_etv_kayseri", number = 156, name = "ETV Kayseri", logoText = "ETV\nKAYSE",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_etv/etv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_etv/etv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_etv/etv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_etv/etv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_156_etv_manisa", number = 157, name = "ETV Manisa", logoText = "ETV\nMANIS",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge1.socialsmart.tv/manisaetv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Akış (Edge1 Chunks)", "https://edge1.socialsmart.tv/manisaetv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/manisaetv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/manisaetv/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_157_fortuna_tv", number = 158, name = "Fortuna TV", logoText = "FORTUN",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge1.socialsmart.tv/ftvturk/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Akış (Edge1 Chunks)", "https://edge1.socialsmart.tv/ftvturk/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/ftvturk/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/ftvturk/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_158_frt_tv_fethiye", number = 159, name = "FRT TV Fethiye", logoText = "FRT\nFETHI",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge1.socialsmart.tv/frttv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Adaptif Akış (Edge1 Playlist)", "https://edge1.socialsmart.tv/frttv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/frttv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/frttv/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_159_grt_gaziantep_tv", number = 160, name = "GRT Gaziantep TV", logoText = "GRT\nGAZIA",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_grt/grt/chunks.m3u8", StreamType.HLS), StreamSource("Adaptif Akış (Playlist)", "https://live.artidijitalmedya.com/artidijital_grt/grt/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://live.artidijitalmedya.com/artidijital_grt/grt/chunks.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_160_gurbet_24_tv", number = 161, name = "Gurbet 24 TV", logoText = "GURBE\n24",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "http://cdn-gurbet24.yayin.com.tr/gurbet24/gurbet24/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Chunks Akışı", "http://cdn-gurbet24.yayin.com.tr/gurbet24/gurbet24/chunks.m3u8", StreamType.HLS), StreamSource("Mono Doğrudan Akış", "http://cdn-gurbet24.yayin.com.tr/gurbet24/gurbet24/tracks-v1a1/mono.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "http://cdn-gurbet24.yayin.com.tr/gurbet24/gurbet24/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "http://cdn-gurbet24.yayin.com.tr/gurbet24/gurbet24/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_161_g_neydo_u_tv__anl_ur", number = 162, name = "Güneydoğu TV Şanlıurfa", logoText = "GÜNEY\nŞANLI",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge1.socialsmart.tv/gtv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Akış (Edge1 Chunks)", "https://edge1.socialsmart.tv/gtv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/gtv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/gtv/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_162_haber_61_trabzon", number = 163, name = "Haber 61 Trabzon", logoText = "HABER\n61",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "http://win8.yayin.com.tr/haber61tv/smil:haber61tv.smil/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://cdn-haber61tv.yayin.com.tr/haber61tv/haber61tv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Chunks Akışı", "http://win8.yayin.com.tr/haber61tv/smil:haber61tv.smil/chunks.m3u8", StreamType.HLS), StreamSource("Mono Doğrudan Akış", "http://win8.yayin.com.tr/haber61tv/smil:haber61tv.smil/tracks-v1a1/mono.m3u8", StreamType.HLS), StreamSource("Hızlı Chunks Akışı", "https://cdn-haber61tv.yayin.com.tr/haber61tv/haber61tv/chunks.m3u8", StreamType.HLS), StreamSource("Mono Doğrudan Akış", "https://cdn-haber61tv.yayin.com.tr/haber61tv/haber61tv/tracks-v1a1/mono.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_163_hunat_tv_kayseri", number = 164, name = "Hunat TV Kayseri", logoText = "HUNAT\nKAYSE",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_hunattv/hunattv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_hunattv/hunattv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_hunattv/hunattv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_hunattv/hunattv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_164_i_kra___diyar_tv", number = 165, name = "İkra / Diyar TV", logoText = "İKRA\n/",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_diyartv/diyartv/chunks.m3u8", StreamType.HLS), StreamSource("Adaptif Akış (Playlist)", "https://live.artidijitalmedya.com/artidijital_diyartv/diyartv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://live.artidijitalmedya.com/artidijital_diyartv/diyartv/chunks.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_165_i_zmir_time_35_tv", number = 166, name = "İzmir Time 35 TV", logoText = "İZMIR\nTIME",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://cdn-time35tv.yayin.com.tr/time35tv/time35tv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Chunks Akışı", "https://cdn-time35tv.yayin.com.tr/time35tv/time35tv/chunks.m3u8", StreamType.HLS), StreamSource("Mono Doğrudan Akış", "https://cdn-time35tv.yayin.com.tr/time35tv/time35tv/tracks-v1a1/mono.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://cdn-time35tv.yayin.com.tr/time35tv/time35tv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://cdn-time35tv.yayin.com.tr/time35tv/time35tv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_166_i__el_tv_mersin", number = 167, name = "İçel TV Mersin", logoText = "İÇEL\nMERSI",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge1.socialsmart.tv/iceltv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Akış (Edge1 Chunks)", "https://edge1.socialsmart.tv/iceltv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/iceltv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/iceltv/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_167_kanal_12", number = 168, name = "Kanal 12", logoText = "KANAL\n12",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_kanal12/kanal12/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://live.artidijitalmedya.com/artidijital_kanal12/kanal12/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_kanal12/kanal12/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_kanal12/kanal12/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_168_kanal_13", number = 169, name = "Kanal 13", logoText = "KANAL\n13",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://cdn4.yayin.com.tr/kanal13/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Playlist)", "https://cdn4.yayin.com.tr/kanal13/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Master)", "https://cdn4.yayin.com.tr/kanal13/master.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_169_kanal_15_burdur", number = 170, name = "Kanal 15 Burdur", logoText = "KANAL\n15",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_kanal15/kanal15/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_kanal15/kanal15/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_kanal15/kanal15/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_kanal15/kanal15/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_170_kanal_19__orum", number = 171, name = "Kanal 19 Çorum", logoText = "KANAL\n19",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.euromediacenter.com/kanal19/tracks-v1a1/mono.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://live.euromediacenter.com/kanal19/tracks-v1a1/mono.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_171_kanal_23_elaz__", number = 172, name = "Kanal 23 Elazığ", logoText = "KANAL\n23",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_kanal23/kanal23/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_kanal23/kanal23/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_kanal23/kanal23/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_kanal23/kanal23/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_172_kanal_26_eski_ehir", number = 173, name = "Kanal 26 Eskişehir", logoText = "KANAL\n26",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_kanal26/kanal26/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://live.artidijitalmedya.com/artidijital_kanal26/kanal26/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_kanal26/kanal26/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_kanal26/kanal26/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_173_kanal_3_afyon", number = 174, name = "Kanal 3 Afyon", logoText = "KANAL\n3",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_kanal3/kanal3/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_kanal3/kanal3/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_kanal3/kanal3/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_kanal3/kanal3/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_174_kanal_32_isparta", number = 175, name = "Kanal 32 Isparta", logoText = "KANAL\n32",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge1.socialsmart.tv/kanal32/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://edge1.socialsmart.tv/kanal32/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/kanal32/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/kanal32/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_175_kanal_33_mersin", number = 176, name = "Kanal 33 Mersin", logoText = "KANAL\n33",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge1.socialsmart.tv/kanal33/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Akış (Edge1 Chunks)", "https://edge1.socialsmart.tv/kanal33/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/kanal33/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/kanal33/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_176_kanal_34_i_stanbul", number = 177, name = "Kanal 34 İstanbul", logoText = "KANAL\n34",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.euromediacenter.com/kanal34/tracks-v1a1/mono.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://live.euromediacenter.com/kanal34/tracks-v1a1/mono.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_177_kanal_53_rize", number = 178, name = "Kanal 53 Rize", logoText = "KANAL\n53",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://kanal53.ozelip.com:3448/hybrid/play.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://kanal53.ozelip.com:3448/hybrid/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://kanal53.ozelip.com:3448/hybrid/chunklist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "http://kanal53.ozelip.com:3448/hybrid/play.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_178_kanal_58_sivas", number = 179, name = "Kanal 58 Sivas", logoText = "KANAL\n58",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_kanal58/kanal58/chunks.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://live.artidijitalmedya.com/artidijital_kanal58/kanal58/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://live.artidijitalmedya.com/artidijital_kanal58/kanal58/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_kanal58/kanal58/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_kanal58/kanal58/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_179_kanal_f_rat_elaz__", number = 180, name = "Kanal Fırat Elazığ", logoText = "KANAL\nFIRAT",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_kanalfirat/kanalfirat/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_kanalfirat/kanalfirat/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_kanalfirat/kanalfirat/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_kanalfirat/kanalfirat/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_180_kanal_plus", number = 181, name = "Kanal Plus", logoText = "KANAL\nPLUS",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_kanalplus/kanalplus/mpeg/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://live.artidijitalmedya.com/artidijital_kanalplus/kanalplus/mpeg/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_kanalplus/kanalplus/mpeg/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_kanalplus/kanalplus/mpeg/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_181_kanal_urfa", number = 182, name = "Kanal Urfa", logoText = "KANAL\nURFA",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge1.socialsmart.tv/kanalurfa/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://edge1.taksimbilisim.com/kanalurfa/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Hızlı Akış (Edge1 Chunks)", "https://edge1.socialsmart.tv/kanalurfa/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/kanalurfa/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/kanalurfa/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_182_kanal_v_antalya", number = 183, name = "Kanal V Antalya", logoText = "KANAL\nV",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://live.artidijitalmedya.com/artidijital_kanalv/kanalv/chunks.m3u8", StreamType.HLS), StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_kanalv/kanalv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://live.artidijitalmedya.com/artidijital_kanalv/kanalv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_kanalv/kanalv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_kanalv/kanalv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_183_kanal_z_zonguldak", number = 184, name = "Kanal Z Zonguldak", logoText = "KANAL\nZ",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_kanalz/kanalz/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_kanalz/kanalz/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_kanalz/kanalz/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_kanalz/kanalz/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_184_kardelen_tv_erzurum", number = 185, name = "Kardelen TV Erzurum", logoText = "KARDE\nERZUR",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge1.socialsmart.tv/kardelentv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Akış (Edge1 Chunks)", "https://edge1.socialsmart.tv/kardelentv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/kardelentv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/kardelentv/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_185_kay_tv_kayseri", number = 186, name = "Kay TV Kayseri", logoText = "KAY\nKAYSE",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_kaytv/kaytv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_kaytv/kaytv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_kaytv/kaytv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_kaytv/kaytv/index.m3u8", StreamType.HLS))
        )
    )

    private fun getBuiltInChannelsChunk6(): List<TvChannel> = listOf(
        ch(
            id = "ch_186_kocaeli_tv", number = 187, name = "Kocaeli TV", logoText = "KOCAEL",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge1.socialsmart.tv/kocaelitv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://edge1.socialsmart.tv/kocaelitv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/kocaelitv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/kocaelitv/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_187_kontv_konya", number = 188, name = "KonTV Konya", logoText = "KONTV\nKONYA",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://canli.kontv.com.tr/live/kontv.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://canli.kontv.com.tr/live/kontv.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_188_konya_olay_tv", number = 189, name = "Konya Olay TV", logoText = "KONYA\nOLAY",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://live.artidijitalmedya.com/artidijital_konyaolaytv/konyaolaytv/chunks.m3u8", StreamType.HLS), StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_konyaolaytv/konyaolaytv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://live.artidijitalmedya.com/artidijital_konyaolaytv/konyaolaytv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_konyaolaytv/konyaolaytv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_konyaolaytv/konyaolaytv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_koza_tv", number = 190, name = "KOZA TV", logoText = "KOZA\nTV",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "http://176.65.153.35:6003/play/a019/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Playlist)", "http://176.65.153.35:6003/play/a019/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Master)", "http://176.65.153.35:6003/play/a019/master.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_189_ktv_konya", number = 191, name = "KTV Konya", logoText = "KTV\nKONYA",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://cdn-ktvtv.yayin.com.tr/ktvtv/ktvtv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Chunks Akışı", "https://cdn-ktvtv.yayin.com.tr/ktvtv/ktvtv/chunks.m3u8", StreamType.HLS), StreamSource("Mono Doğrudan Akış", "https://cdn-ktvtv.yayin.com.tr/ktvtv/ktvtv/tracks-v1a1/mono.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://cdn-ktvtv.yayin.com.tr/ktvtv/ktvtv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://cdn-ktvtv.yayin.com.tr/ktvtv/ktvtv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_190_life_tv", number = 192, name = "Life TV", logoText = "LIFE",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://live.artidijitalmedya.com/artidijital_lifetv/lifetv/chunks.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://live.artidijitalmedya.com/artidijital_lifetv/lifetv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://live.artidijitalmedya.com/artidijital_lifetv/lifetv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_lifetv/lifetv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_lifetv/lifetv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_191_mavi_karadeniz_tv", number = 193, name = "Mavi Karadeniz TV", logoText = "MAVI\nKARAD",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_mavikaradeniz/mavikaradeniz/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://live.artidijitalmedya.com/artidijital_mavikaradeniz/mavikaradeniz/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_mavikaradeniz/mavikaradeniz/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_mavikaradeniz/mavikaradeniz/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_192_mercan_tv_ad_yaman", number = 194, name = "Mercan TV Adıyaman", logoText = "MERCA\nADIYA",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_mercantv/mercantv/chunks.m3u8", StreamType.HLS), StreamSource("Adaptif Akış (Playlist)", "https://live.artidijitalmedya.com/artidijital_mercantv/mercantv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://live.artidijitalmedya.com/artidijital_mercantv/mercantv/chunks.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_193_metropol_tv", number = 195, name = "Metropol TV", logoText = "METROP",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge1.socialsmart.tv/metropoltv/smil/metropoltv/bant1/chunks.m3u8?app=supersatforum", StreamType.HLS), StreamSource("Adaptif Akış (Edge1 Playlist)", "https://edge1.socialsmart.tv/metropoltv/smil/metropoltv/bant1/playlist.m3u8?app=supersatforum", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/metropoltv/smil/metropoltv/bant1/chunks.m3u8?app=supersatforum", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/metropoltv/smil/metropoltv/bant1/playlist.m3u8?app=supersatforum", StreamType.HLS))
        ),
        ch(
            id = "ch_194_mu_la_t_rk_tv", number = 196, name = "Muğla Türk TV", logoText = "MUĞLA\nTÜRK",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge1.socialsmart.tv/muglaturk/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://edge1.socialsmart.tv/muglaturk/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/muglaturk/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/muglaturk/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_195_olay_t_rk_kayseri", number = 197, name = "Olay Türk Kayseri", logoText = "OLAY\nTÜRK",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_olayturk/olayturk/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_olayturk/olayturk/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_olayturk/olayturk/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_olayturk/olayturk/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_196_ordu_bel_tv", number = 198, name = "Ordu Bel TV", logoText = "ORDU\nBEL",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://cdn1-ordubeltv.yayin.com.tr/ordubeltv/ordubeltv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Chunks Akışı", "https://cdn1-ordubeltv.yayin.com.tr/ordubeltv/ordubeltv/chunks.m3u8", StreamType.HLS), StreamSource("Mono Doğrudan Akış", "https://cdn1-ordubeltv.yayin.com.tr/ordubeltv/ordubeltv/tracks-v1a1/mono.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://cdn1-ordubeltv.yayin.com.tr/ordubeltv/ordubeltv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://cdn1-ordubeltv.yayin.com.tr/ordubeltv/ordubeltv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_197_ruha_tv__anl_urfa", number = 199, name = "Ruha TV Şanlıurfa", logoText = "RUHA\nŞANLI",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://ruhatv.radyotelekom.com.tr:3515/live/ruhatvlive.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://ruhatv.radyotelekom.com.tr:3515/live/ruhatvlive.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_198_sa_l_k_channel", number = 200, name = "Sağlık Channel", logoText = "SAĞLI\nCHANN",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.euromediacenter.com/saglikchannel/tracks-v1a1/mono.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://live.euromediacenter.com/saglikchannel/tracks-v1a1/mono.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_199_su_tv", number = 201, name = "Su TV", logoText = "SU",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_sutv/sutv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_sutv/sutv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_sutv/sutv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_sutv/sutv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_200_sun_tv_mersin", number = 202, name = "Sun TV Mersin", logoText = "SUN\nMERSI",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_sunrtv/sunrtv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_sunrtv/sunrtv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_sunrtv/sunrtv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_sunrtv/sunrtv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_201_tempo_tv", number = 203, name = "Tempo TV", logoText = "TEMPO",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://live.artidijitalmedya.com/artidijital_tempotv/tempotv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://live.artidijitalmedya.com/artidijital_tempotv/tempotv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_tempotv/tempotv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_tempotv/tempotv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_202_ton_tv__anakkale", number = 204, name = "Ton TV Çanakkale", logoText = "TON\nÇANAK",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_tontv/tontv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_tontv/tontv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_tontv/tontv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_tontv/tontv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_203_tv_41_kocaeli", number = 205, name = "TV 41 Kocaeli", logoText = "41\nKOCAE",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://live.artidijitalmedya.com/artidijital_tv41/tv41/chunks.m3u8", StreamType.HLS), StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_tv41/tv41/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://live.artidijitalmedya.com/artidijital_tv41/tv41/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_tv41/tv41/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_tv41/tv41/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_204_tv_48_mu_la", number = 206, name = "TV 48 Muğla", logoText = "48\nMUĞLA",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://ajansplay.com:5443/LiveApp/streams/tv48.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://ajansplay.com:5443/LiveApp/streams/tv48.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_205_tv_52_ordu", number = 207, name = "TV 52 Ordu", logoText = "52\nORDU",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge1.socialsmart.tv/tv52/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Akış (Edge1 Chunks)", "https://edge1.socialsmart.tv/tv52/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/tv52/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/tv52/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_206_t_rk_klinikleri_tv", number = 208, name = "Türk Klinikleri TV", logoText = "TÜRK\nKLINI",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "http://mn-nl.mncdn.com/turkiyeklinikleri/smil:turkiyeklinikleri/chunklist_b3128000.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://mn-nl.mncdn.com/turkiyeklinikleri/smil:turkiyeklinikleri/chunklist_b3128000.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_207_urfanatik_tv", number = 209, name = "Urfanatik TV", logoText = "URFANA",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_urfanatiktv/urfanatiktv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://live.artidijitalmedya.com/artidijital_urfanatiktv/urfanatiktv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_urfanatiktv/urfanatiktv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_urfanatiktv/urfanatiktv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_208_van_g_l__tv", number = 210, name = "Van Gölü TV", logoText = "VAN\nGÖLÜ",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://cdn1-vangolutv.yayin.com.tr/vangolutv/amlst:vangolutv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Chunks Akışı", "https://cdn1-vangolutv.yayin.com.tr/vangolutv/amlst:vangolutv/chunks.m3u8", StreamType.HLS), StreamSource("Mono Doğrudan Akış", "https://cdn1-vangolutv.yayin.com.tr/vangolutv/amlst:vangolutv/tracks-v1a1/mono.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://cdn1-vangolutv.yayin.com.tr/vangolutv/amlst:vangolutv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://cdn1-vangolutv.yayin.com.tr/vangolutv/amlst:vangolutv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_209_vuslat_tv_malatya", number = 211, name = "Vuslat TV Malatya", logoText = "VUSLA\nMALAT",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://live.artidijitalmedya.com/artidijital_vuslattv/vuslattv/chunks.m3u8", StreamType.HLS), StreamSource("Standart Akış", "https://live.artidijitalmedya.com/artidijital_vuslattv/vuslattv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://live.artidijitalmedya.com/artidijital_vuslattv/vuslattv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_vuslattv/vuslattv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_vuslattv/vuslattv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_210__ay_tv_rize", number = 212, name = "Çay TV Rize", logoText = "ÇAY\nRIZE",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge1.socialsmart.tv/caytv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Akış (Edge1 Chunks)", "https://edge1.socialsmart.tv/caytv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/caytv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/caytv/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_211_belemt_rk_tv", number = 213, name = "Belemtürk TV", logoText = "BELEMT",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Dünya", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://edge.socialsmart.tv/belemturktv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Hızlı Akış (Edge1 Chunks)", "https://edge1.socialsmart.tv/belemturktv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Adaptif Akış (Edge1 Playlist)", "https://edge1.socialsmart.tv/belemturktv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/belemturktv/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_212_br_ksel_t_rk_tv", number = 214, name = "Brüksel Türk TV", logoText = "BRÜKS\nTÜRK",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Dünya", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.euromediacenter.com/brukselturk/tracks-v1a1/mono.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://live.euromediacenter.com/brukselturk/tracks-v1a1/mono.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_213_finest_tv", number = 215, name = "Finest TV", logoText = "FINEST",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Dünya", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "http://media.finest.tv/hls/live.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://media.finest.tv/hls/live.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_214_fx_t_rkiye", number = 216, name = "FX Türkiye", logoText = "FX",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Dünya", badge = "FHD", logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4d/FX_International_logo.svg/960px-FX_International_logo.svg.png",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://saran-live.ercdn.net/fx/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Playlist)", "https://saran-live.ercdn.net/fx/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Master)", "https://saran-live.ercdn.net/fx/master.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_215_h_r_tv", number = 217, name = "Hür TV", logoText = "HÜR",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Dünya", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://live.euromediacenter.com/hurtv/tracks-v1a1/mono.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://live.euromediacenter.com/hurtv/tracks-v1a1/mono.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_216_i_svi_re_tv", number = 218, name = "İsviçre TV", logoText = "İSVIÇR",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Dünya", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://i41.ozelip.com:3269/hybrid/play.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://i41.ozelip.com:3269/hybrid/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://i41.ozelip.com:3269/hybrid/chunklist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "http://i41.ozelip.com:3269/hybrid/play.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_217_kanal_avrupa", number = 219, name = "Kanal Avrupa", logoText = "KANAL\nAVRUP",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Dünya", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Standart Akış", "https://api-tv27.yayin.com.tr/kanalavrupa/index.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://cdn-kanalavrupa.yayin.com.tr/kanalavrupa/tracks-v2a1/mono.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://api-tv27.yayin.com.tr/kanalavrupa/tracks-v1a1/mono.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Playlist)", "https://api-tv27.yayin.com.tr/kanalavrupa/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Master)", "https://api-tv27.yayin.com.tr/kanalavrupa/master.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_218_trt_arabi", number = 220, name = "TRT Arabi", logoText = "TRT\nARABI",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Dünya", badge = "HD", logoUrl = "https://i.imgur.com/yyhWOZs.png",
            sources = listOf(StreamSource("HD 720p", "https://tv-trtarabi.medya.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("FHD 1080p (TRT CDN)", "https://tv-trtarabi.medya.trt.com.tr/master_1080.m3u8", StreamType.HLS), StreamSource("HD 720p (Hızlı Yedek)", "https://tv-trtarabi.medya.trt.com.tr/master_720.m3u8", StreamType.HLS), StreamSource("SD 480p (Kesintisiz Yedek)", "https://tv-trtarabi.medya.trt.com.tr/master_480.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_219_trt_avaz", number = 221, name = "TRT Avaz", logoText = "TRT\nAVAZ",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Dünya", badge = "HD", logoUrl = "https://i.imgur.com/VhTwXu5.png",
            sources = listOf(StreamSource("HD 720p", "https://tv-trtavaz.medya.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://tv-trtavaz.medya.trt.com.tr/master_720.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://tv-trtavaz.medya.trt.com.tr/master_1440.m3u8", StreamType.HLS), StreamSource("FHD 1080p (TRT CDN)", "https://tv-trtavaz.medya.trt.com.tr/master_1080.m3u8", StreamType.HLS), StreamSource("SD 480p (Kesintisiz Yedek)", "https://tv-trtavaz.medya.trt.com.tr/master_480.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_220_trt_kurd_", number = 222, name = "TRT Kurdî", logoText = "TRT\nKURDÎ",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Dünya", badge = "HD", logoUrl = "https://i.imgur.com/6BpymfB.png",
            sources = listOf(StreamSource("HD 720p", "https://tv-trtkurdi.medya.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("FHD 1080p (TRT CDN)", "https://tv-trtkurdi.medya.trt.com.tr/master_1080.m3u8", StreamType.HLS), StreamSource("HD 720p (Hızlı Yedek)", "https://tv-trtkurdi.medya.trt.com.tr/master_720.m3u8", StreamType.HLS), StreamSource("SD 480p (Kesintisiz Yedek)", "https://tv-trtkurdi.medya.trt.com.tr/master_480.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_221_trt_t_rk", number = 223, name = "TRT Türk", logoText = "TRT\nTÜRK",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Dünya", badge = "HD", logoUrl = "https://i.imgur.com/OSTOQNw.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://tv-trtturk.medya.trt.com.tr/master_1080.m3u8", StreamType.HLS), StreamSource("HD 720p", "https://tv-trtturk.medya.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://tv-trtturk.medya.trt.com.tr/master_720.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "https://tv-trtturk.medya.trt.com.tr/master_1440.m3u8", StreamType.HLS), StreamSource("SD 480p (Kesintisiz Yedek)", "https://tv-trtturk.medya.trt.com.tr/master_480.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_222_trt_world", number = 224, name = "TRT World", logoText = "TRT\nWORLD",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Dünya", badge = "FHD", logoUrl = "https://i.imgur.com/JEA2xpv.png",
            sources = listOf(StreamSource("FHD 1080p (En Yüksek Bitrate)", "https://tv-trtworld.medya.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://tv-trtworld.medya.trt.com.tr/master_1080.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://tv-trtworld.medya.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("HD 720p (Hızlı Yedek)", "https://tv-trtworld.medya.trt.com.tr/master_720.m3u8", StreamType.HLS), StreamSource("SD 480p (Kesintisiz Yedek)", "https://tv-trtworld.medya.trt.com.tr/master_480.m3u8", StreamType.HLS), StreamSource("FHD 1080p (TRT CDN)", "http://tv-trtworld.medya.trt.com.tr/master_1080.m3u8", StreamType.HLS))
        )
    )


    // ULTRA HIZLI ÖN-HESAPLAMALI EPG ÜRETİCİ (222 KANAL İÇİN SADECE 1 KEZ SAAT FORMATLAR!)

    private fun getBuiltInChannelsChunk7(): List<TvChannel> = listOf(
        ch(
            id = "ch_tr_anadolu_net", number = 225, name = "Anadolu Net TV", logoText = "ANADO\nNET",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Ulusal", badge = "FHD", logoUrl = "https://www.anadolunettv.com/d/r/logo.png",
            sources = listOf(StreamSource("FHD 1080p", "https://live.artidijitalmedya.com/artidijital_anadolunet/anadolunet/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış", "https://live.artidijitalmedya.com/artidijital_anadolunettv/anadolunettv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://live.artidijitalmedya.com/artidijital_anadolunet/anadolunet/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_anadolunet/anadolunet/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_anadolunet/anadolunet/index.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_anadolunettv/anadolunettv/chunks.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_atv_avrupa", number = 226, name = "ATV Avrupa", logoText = "ATV\nAVRUP",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Ulusal", badge = "HD", logoUrl = "https://i.imgur.com/HyVUwFC.png",
            sources = listOf(StreamSource("Canlı Akış", "http://umperisn.mywire.org/index.php?id=1875&token=DeaTHLesS", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_show_turk", number = 227, name = "Show Türk", logoText = "SHOW\nTÜRK",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Ulusal", badge = "HD", logoUrl = "https://i.imgur.com/ESCf2dj.png",
            sources = listOf(StreamSource("Canlı Akış", "http://umperisn.mywire.org/index.php?id=1874&token=DeaTHLesS", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://ciner.daioncdn.net/showtv/showtv.m3u8?app=showtv_web", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://rmtftbjlne.turknet.ercdn.net/bpeytmnqyp/showtv/showtv.m3u8", StreamType.HLS), StreamSource("FHD 1080p Akış", "https://ciner.daioncdn.net/showtv/showtv_1080p.m3u8?app=showtv_web", StreamType.HLS), StreamSource("HD 720p Hızlı Akış", "https://ciner.daioncdn.net/showtv/showtv_720p.m3u8?app=showtv_web", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://ciner.daioncdn.net/showtv/showtv_480p.m3u8?app=showtv_web", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_kanald_drama", number = 228, name = "Kanal D Drama", logoText = "KANAL\nDRAMA",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Ulusal", badge = "FHD", logoUrl = "https://i.imgur.com/WIeZMKT.png",
            sources = listOf(StreamSource("FHD 1080p", "https://cdn-uw2-prod.tsv2.amagi.tv/linear/amg01602-themahqfrance-vivekanald-samsungspain/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://cdn-uw2-prod.tsv2.amagi.tv/linear/amg01602-themahqfrance-vivekanald-samsungspain/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://cdn-uw2-prod.tsv2.amagi.tv/linear/amg01602-themahqfrance-vivekanald-samsungspain/chunklist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_tv4", number = 229, name = "TV 4", logoText = "TV 4",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Ulusal", badge = "HD", logoUrl = "https://i.postimg.cc/FKkHW8KK/tv4.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://turkmedya-live.ercdn.net/tv4/tv4_1080p.m3u8", StreamType.HLS), StreamSource("HD 720p", "https://turkmedya-live.ercdn.net/tv4/tv4_720p.m3u8", StreamType.HLS), StreamSource("Yedek Akış", "https://turkmedya-live.ercdn.net/tv4/tv4.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://turkmedya-live.ercdn.net/tv4/tv4_480p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_tv5_hd", number = 230, name = "TV 5 HD", logoText = "TV 5",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Ulusal", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("HD Akış", "https://ip252.ozelip.com:1936/protv5/protv5/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://ip252.ozelip.com:1936/protv5/protv5/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://ip252.ozelip.com:1936/protv5/protv5/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_trt3", number = 231, name = "TRT 3", logoText = "TRT 3",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Ulusal", badge = "FHD", logoUrl = "",
            sources = listOf(StreamSource("FHD 1080p", "https://tv-trt3.live.trt.com.tr/master.m3u8", StreamType.HLS), StreamSource("FHD 1080p (TRT CDN)", "https://tv-trt3.live.trt.com.tr/master_1080.m3u8", StreamType.HLS), StreamSource("HD 720p (Hızlı Yedek)", "https://tv-trt3.live.trt.com.tr/master_720.m3u8", StreamType.HLS), StreamSource("SD 480p (Kesintisiz Yedek)", "https://tv-trt3.live.trt.com.tr/master_480.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_life_tv", number = 232, name = "Life TV", logoText = "LIFE\nTV",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Ulusal", badge = "HD", logoUrl = "https://i.ibb.co/9hCVNks/life-tv.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://live.artidijitalmedya.com/artidijital_lifetv/lifetv/chunks.m3u8", StreamType.HLS), StreamSource("Canlı Akış", "https://live.artidijitalmedya.com/artidijital_lifetv/lifetv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://live.artidijitalmedya.com/artidijital_lifetv/lifetv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_lifetv/lifetv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_lifetv/lifetv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_woman_tv", number = 233, name = "Woman Kadın TV", logoText = "WOMAN\nTV",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Ulusal", badge = "HD", logoUrl = "https://i.ibb.co/DKHDfbr/wmtv.png",
            sources = listOf(StreamSource("Canlı Akış", "https://embedlp.b-cdn.net/womantv.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://embedlp.b-cdn.net/womantv.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_aile_tv", number = 234, name = "Aile TV", logoText = "AİLE\nTV",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Ulusal", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/aile-tv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/aile-tv/playlist/0.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/aile-tv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/aile-tv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_finans_turk", number = 235, name = "Finans Türk TV", logoText = "FİNAN\nTÜRK",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Haber", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://yayin30.haber100.com/live/finansturk/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://yayin30.haber100.com/live/finansturk/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://yayin30.haber100.com/live/finansturk/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_turkhaber", number = 236, name = "TürkHaber TV", logoText = "TÜRK\nHABER",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Haber", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "https://edge1.socialsmart.tv/turkhaber/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Akış (Edge1 Chunks)", "https://edge1.socialsmart.tv/turkhaber/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/turkhaber/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/turkhaber/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_haber61", number = 237, name = "Haber 61 Trabzon", logoText = "HABER\n61",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Haber", badge = "HD", logoUrl = "https://i.ibb.co/mHt7rQf/Tryerel.png",
            sources = listOf(StreamSource("Canlı Akış", "https://cdn-haber61tv.yayin.com.tr/haber61tv/haber61tv/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış", "https://cdn-haber61tv.yayin.com.tr/haber61tv/smil:haber61tv.smil/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Chunks Akışı", "https://cdn-haber61tv.yayin.com.tr/haber61tv/haber61tv/chunks.m3u8", StreamType.HLS), StreamSource("Mono Doğrudan Akış", "https://cdn-haber61tv.yayin.com.tr/haber61tv/haber61tv/tracks-v1a1/mono.m3u8", StreamType.HLS), StreamSource("Hızlı Chunks Akışı", "https://cdn-haber61tv.yayin.com.tr/haber61tv/smil:haber61tv.smil/chunks.m3u8", StreamType.HLS), StreamSource("Mono Doğrudan Akış", "https://cdn-haber61tv.yayin.com.tr/haber61tv/smil:haber61tv.smil/tracks-v1a1/mono.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_tay_tv", number = 238, name = "TAY TV", logoText = "TAY\nTV",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Spor", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "https://duhnet.hipodrom.com/S2/HLS_LIVE/mislitaynp/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://duhnet.hipodrom.com/S2/HLS_LIVE/mislitaynp/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://duhnet.hipodrom.com/S2/HLS_LIVE/mislitaynp/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_ztk_tv", number = 239, name = "ZTK TV", logoText = "ZTK\nSPOR",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Spor", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/ztktv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/ztktv/playlist/0.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/ztktv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/ztktv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_satranc_tv", number = 240, name = "Satranç TV", logoText = "SATRA\nTV",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Spor", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "http://139.162.182.79/live/test/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Playlist)", "http://139.162.182.79/live/test/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Master)", "http://139.162.182.79/live/test/master.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_tarih_tv", number = 241, name = "Tarih TV", logoText = "TARİH\nTV",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Belgesel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/Tarih-tv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/Tarih-tv/playlist/0.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/Tarih-tv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/Tarih-tv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_ciftci_tv", number = 242, name = "Çiftçi TV", logoText = "ÇİFTÇ\nTV",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Belgesel", badge = "HD", logoUrl = "https://i.ibb.co/vHwMgx0/ciftci.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://live.artidijitalmedya.com/artidijital_ciftcitv/ciftcitv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_ciftcitv/ciftcitv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_ciftcitv/ciftcitv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_ciftcitv/ciftcitv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_kultur_sanat", number = 243, name = "Kültür Sanat TV", logoText = "KÜLTÜ\nSANAT",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Belgesel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/kltr-sanat-tv/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/kltr-sanat-tv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/kltr-sanat-tv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_babyfirst", number = 244, name = "BabyFirst Türkiye", logoText = "BABYF\nTV",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Çocuk", badge = "HD", logoUrl = "https://i.imgur.com/WxLHBwu.png",
            sources = listOf(StreamSource("HD 720p", "http://206.212.244.63/639/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Playlist)", "http://206.212.244.63/639/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Master)", "http://206.212.244.63/639/master.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_babytv_tr", number = 245, name = "BabyTV Türkiye", logoText = "BABYT\nTR",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Çocuk", badge = "FHD", logoUrl = "https://i.imgur.com/4BDJ5FT.png",
            sources = listOf(StreamSource("FHD 1080p", "https://saran-live.ercdn.net/babytv/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Playlist)", "https://saran-live.ercdn.net/babytv/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Master)", "https://saran-live.ercdn.net/babytv/master.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_zarok_tv", number = 246, name = "Zarok TV", logoText = "ZAROK\nTV",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Çocuk", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "https://zindikurmanci.zaroktv.com.tr/hls/stream.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://zindikurmanci.zaroktv.com.tr/hls/stream.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_fashion_one", number = 247, name = "Fashion One TV", logoText = "FASHI\nONE",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Müzik", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/broadcast_57e4f8c600b6d.smil/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/broadcast_5c9e2ee690051.smil/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/broadcast_57e4f8c600b6d.smil/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/broadcast_57e4f8c600b6d.smil/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_chill_tv", number = 248, name = "Chill TV", logoText = "CHILL\nTV",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Müzik", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/chill-tv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/chill-tv/playlist/0.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/chill-tv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/chill-tv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_sat7_turk", number = 249, name = "Sat 7 Türk", logoText = "SAT 7\nTÜRK",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Dini", badge = "HD", logoUrl = "https://i.ibb.co/Cm1kp8f/sat7turk.png",
            sources = listOf(StreamSource("Canlı Akış", "https://svs.itworkscdn.net/sat7turklive/sat7turk.smil/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış", "https://svs.itworkscdn.net/sat7turklive/sat7turk.smil/playlist_dvr.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://live.artidijitalmedya.com/artidijital_sat7turk/sat7turk/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://svs.itworkscdn.net/sat7turklive/sat7turk.smil/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://svs.itworkscdn.net/sat7turklive/sat7turk.smil/index.m3u8", StreamType.HLS), StreamSource("Adaptif Akış (Playlist)", "https://live.artidijitalmedya.com/artidijital_sat7turk/sat7turk/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_kanal_hayat", number = 250, name = "Kanal Hayat", logoText = "KANAL\nHAYAT",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Dini", badge = "HD", logoUrl = "https://i.ibb.co/1qFR0jX/Kanalhayat.png",
            sources = listOf(StreamSource("Canlı Akış", "https://livecdn.use1-0004.jwplive.com/live/sites/WpgVdplm/media/icNwL50b/live.isml/live.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://tbn02a.ltnschedule.com/hls/nx21i.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://livecdn.use1-0004.jwplive.com/live/sites/WpgVdplm/media/icNwL50b/live.isml/live.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "http://tbn02a.ltnschedule.com/hls/nx21i.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_kurani_kerim", number = 251, name = "Kur'an-ı Kerim TV", logoText = "KURAN\nTV",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Dini", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/kurani-kerim-tv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/kurani-kerim-dersleri-tv/playlist/0.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/kurani-kerim-tv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/kurani-kerim-tv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_ilahiler_tv", number = 252, name = "İlahiler TV", logoText = "İLAHİ\nTV",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Dini", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/ilahiler-tv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/ilahiler-tv/playlist/0.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/ilahiler-tv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/ilahiler-tv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_islam_tv", number = 253, name = "İslam TV", logoText = "İSLAM\nTV",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Dini", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/islamtv/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/islamtv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/islamtv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_asri_saadet", number = 254, name = "Asr-ı Saadet TV", logoText = "ASRI\nSAADE",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Dini", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/asr-i-saadet-tv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/asr-i-saadet-tv/playlist/0.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/asr-i-saadet-tv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/asr-i-saadet-tv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_mihrap_tv", number = 255, name = "Mihrap TV", logoText = "MİHRA\nTV",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Dini", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "https://yayin30.haber100.com/live/mihraptv/chunklist_w2099091652.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://yayin30.haber100.com/live/mihraptv/chunklist_w2099091652.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_ikra_tv", number = 256, name = "İkra TV", logoText = "İKRA\nTV",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Dini", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "https://rtmp.ikratv.com/live/kanal1/v3/index.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Playlist)", "https://rtmp.ikratv.com/live/kanal1/v3/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Master)", "https://rtmp.ikratv.com/live/kanal1/v3/master.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_kudus_tv", number = 257, name = "Kudüs TV", logoText = "KUDÜS\nTV",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Dini", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "https://yayin1.haber100.com/kudustvapp/kudustv/chunklist_w1714163689.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://yayin1.haber100.com/kudustvapp/kudustv/chunklist_w1714163689.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_vaaz_tv", number = 258, name = "Vaaz TV", logoText = "VAAZ\nTV",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Dini", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/vaaz-tv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/vaaz-tv/playlist/0.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/vaaz-tv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/vaaz-tv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_ada_tv", number = 259, name = "Ada TV Kıbrıs", logoText = "ADA\nTV",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Kıbrıs", badge = "HD", logoUrl = "https://i.imgur.com/YWmkUWU.png",
            sources = listOf(StreamSource("Canlı Akış", "https://yayin1.canlitv.fun/live/kibrisadatv.stream/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://yayin1.canlitv.fun/live/kibrisadatv.stream/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://yayin1.canlitv.fun/live/kibrisadatv.stream/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_altas_tv", number = 260, name = "Altaş TV Ordu", logoText = "ALTAŞ\nTV",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Yerel", badge = "FHD", logoUrl = "https://i.imgur.com/XaJ7fp6.png",
            sources = listOf(StreamSource("FHD 1080p", "https://edge1.socialsmart.tv/altastv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Akış (Edge1 Chunks)", "https://edge1.socialsmart.tv/altastv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/altastv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/altastv/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_aras_tv", number = 261, name = "Aras TV Erzurum", logoText = "ARAS\nTV",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Yerel", badge = "HD", logoUrl = "https://i.imgur.com/JsDCyWG.png",
            sources = listOf(StreamSource("Canlı Akış", "http://1.rtmp.org/tv217/yayin.stream/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "http://1.rtmp.org/tv217/yayin.stream/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "http://1.rtmp.org/tv217/yayin.stream/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_atv_alanya", number = 262, name = "ATV Alanya", logoText = "ATV\nALANY",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Yerel", badge = "FHD", logoUrl = "https://i.imgur.com/bUwarR0.png",
            sources = listOf(StreamSource("FHD 1080p", "https://cdn-alanyatv.yayin.com.tr/alanyatv/alanyatv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://cdn-alanyatv.yayin.com.tr/alanyatv/alanyatv/chunklist_w116897790.m3u8", StreamType.HLS), StreamSource("Hızlı Chunks Akışı", "https://cdn-alanyatv.yayin.com.tr/alanyatv/alanyatv/chunks.m3u8", StreamType.HLS), StreamSource("Mono Doğrudan Akış", "https://cdn-alanyatv.yayin.com.tr/alanyatv/alanyatv/tracks-v1a1/mono.m3u8", StreamType.HLS))
        ),
    )

    private fun getBuiltInChannelsChunk8(): List<TvChannel> = listOf(
        ch(
            id = "ch_tr_as_tv_bursa", number = 263, name = "AS TV Bursa", logoText = "AS TV\nBURSA",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Yerel", badge = "HD", logoUrl = "https://i.ibb.co/mHt7rQf/Tryerel.png",
            sources = listOf(StreamSource("Canlı Akış", "https://live.artidijitalmedya.com/artidijital_astv/astv/chunks.m3u8", StreamType.HLS), StreamSource("Adaptif Akış (Playlist)", "https://live.artidijitalmedya.com/artidijital_astv/astv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://live.artidijitalmedya.com/artidijital_astv/astv/chunks.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_line_tv_bursa", number = 264, name = "Line TV Bursa", logoText = "LINE\nBURSA",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "https://edge1.socialsmart.tv/linetv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Akış (Edge1 Chunks)", "https://edge1.socialsmart.tv/linetv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/linetv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/linetv/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_es_tv_eskisehir", number = 265, name = "ES TV Eskişehir", logoText = "ES TV\nESKİŞ",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Yerel", badge = "HD", logoUrl = "https://i.ibb.co/mHt7rQf/Tryerel.png",
            sources = listOf(StreamSource("Canlı Akış", "https://live.artidijitalmedya.com/artidijital_estv/estv/chunks.m3u8", StreamType.HLS), StreamSource("Adaptif Akış (Playlist)", "https://live.artidijitalmedya.com/artidijital_estv/estv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://live.artidijitalmedya.com/artidijital_estv/estv/chunks.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_tv264_sakarya", number = 266, name = "TV 264 Sakarya", logoText = "TV\n264",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Yerel", badge = "HD", logoUrl = "https://i.ibb.co/mHt7rQf/Tryerel.png",
            sources = listOf(StreamSource("Canlı Akış", "https://b01c02nl.mediatriple.net/videoonlylive/mtdxkkitgbrckilive/broadcast_5ee244263fd6d.smil/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış", "http://mc1.mediatriple.net/videoonlylive/mtdxkkitgbrckilive/broadcast_5ee244263fd6d.smil/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://b01c02nl.mediatriple.net/videoonlylive/mtdxkkitgbrckilive/broadcast_5ee244263fd6d.smil/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://b01c02nl.mediatriple.net/videoonlylive/mtdxkkitgbrckilive/broadcast_5ee244263fd6d.smil/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_tv1_kayseri", number = 267, name = "TV 1 Kayseri", logoText = "TV 1\nKAYSE",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Yerel", badge = "HD", logoUrl = "https://i.imgur.com/8CtrYVb.png",
            sources = listOf(StreamSource("Canlı Akış", "https://edge1.socialsmart.tv/tv1/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış", "https://edge1.socialsmart.tv/tv1/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/tv1/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/tv1/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_kent_turk", number = 268, name = "Kent Türk TV", logoText = "KENT\nTÜRK",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Yerel", badge = "HD", logoUrl = "https://i.imgur.com/FQKPjsm.png",
            sources = listOf(StreamSource("Canlı Akış", "https://live.artidijitalmedya.com/artidijital_kentturktv/kentturktv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_kentturktv/kentturktv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_kentturktv/kentturktv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_kentturktv/kentturktv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_mercan_tv", number = 269, name = "Mercan TV Adıyaman", logoText = "MERCA\nTV",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Yerel", badge = "HD", logoUrl = "https://i.imgur.com/3CWV8bS.png",
            sources = listOf(StreamSource("Canlı Akış", "https://live.artidijitalmedya.com/artidijital_mercantv/mercantv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://live.artidijitalmedya.com/artidijital_mercantv/mercantv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_mercantv/mercantv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_mercantv/mercantv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_metropol_denizli", number = 270, name = "Metropol TV Denizli", logoText = "METRO\nDENİZ",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Yerel", badge = "HD", logoUrl = "https://i.ibb.co/mHt7rQf/Tryerel.png",
            sources = listOf(StreamSource("Canlı Akış", "https://edge1.socialsmart.tv/metropoltv/smil/metropoltv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Akış (Edge1 Chunks)", "https://edge1.socialsmart.tv/metropoltv/smil/metropoltv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/metropoltv/smil/metropoltv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/metropoltv/smil/metropoltv/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_mugla_turk", number = 271, name = "Muğla Türk TV", logoText = "MUĞLA\nTÜRK",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Yerel", badge = "HD", logoUrl = "https://i.ibb.co/mHt7rQf/Tryerel.png",
            sources = listOf(StreamSource("Canlı Akış", "https://edge1.socialsmart.tv/muglaturk/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Akış (Edge1 Chunks)", "https://edge1.socialsmart.tv/muglaturk/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/muglaturk/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/muglaturk/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_frt_fethiye", number = 272, name = "FRT Fethiye TV", logoText = "FRT\nFETHİ",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Yerel", badge = "HD", logoUrl = "https://i.ibb.co/mHt7rQf/Tryerel.png",
            sources = listOf(StreamSource("Canlı Akış", "https://edge1.socialsmart.tv/frttv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Adaptif Akış (Edge1 Playlist)", "https://edge1.socialsmart.tv/frttv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/frttv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/frttv/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_hrt_akdeniz", number = 273, name = "HRT Akdeniz Hatay", logoText = "HRT\nAKDEN",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Yerel", badge = "HD", logoUrl = "https://i.ibb.co/mHt7rQf/Tryerel.png",
            sources = listOf(StreamSource("Canlı Akış", "https://vod02.cdn.web.tv/cn/ov/cnovvu97lqw_,240,360,480,.mp4.urlset/playlist-f3-v1-a1.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://vod02.cdn.web.tv/cn/ov/cnovvu97lqw_,240,360,480,.mp4.urlset/playlist-f3-v1-a1.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_ordu_bel", number = 274, name = "Ordu Bel TV", logoText = "ORDU\nBEL",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Yerel", badge = "HD", logoUrl = "https://i.ibb.co/mHt7rQf/Tryerel.png",
            sources = listOf(StreamSource("Canlı Akış", "https://cdn1-ordubeltv.yayin.com.tr/ordubeltv/ordubeltv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Chunks Akışı", "https://cdn1-ordubeltv.yayin.com.tr/ordubeltv/ordubeltv/chunks.m3u8", StreamType.HLS), StreamSource("Mono Doğrudan Akış", "https://cdn1-ordubeltv.yayin.com.tr/ordubeltv/ordubeltv/tracks-v1a1/mono.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://cdn1-ordubeltv.yayin.com.tr/ordubeltv/ordubeltv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://cdn1-ordubeltv.yayin.com.tr/ordubeltv/ordubeltv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_diyar_tv", number = 275, name = "Diyar TV Diyarbakır", logoText = "DİYAR\nTV",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Yerel", badge = "HD", logoUrl = "https://i.ibb.co/mHt7rQf/Tryerel.png",
            sources = listOf(StreamSource("Canlı Akış", "https://live.artidijitalmedya.com/artidijital_diyartv/diyartv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_diyartv/diyartv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_diyartv/diyartv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_diyartv/diyartv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_brtv_karabuk", number = 276, name = "BRTV Karabük", logoText = "BRTV\nKARAB",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Yerel", badge = "HD", logoUrl = "https://i.ibb.co/mHt7rQf/Tryerel.png",
            sources = listOf(StreamSource("Canlı Akış", "https://live.artidijitalmedya.com/artidijital_brtv/brtv/chunks.m3u8", StreamType.HLS), StreamSource("Adaptif Akış (Playlist)", "https://live.artidijitalmedya.com/artidijital_brtv/brtv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://live.artidijitalmedya.com/artidijital_brtv/brtv/chunks.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_sun_rtv", number = 277, name = "Sun RTV Mersin", logoText = "SUN\nRTV",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Yerel", badge = "HD", logoUrl = "https://i.ibb.co/mHt7rQf/Tryerel.png",
            sources = listOf(StreamSource("Canlı Akış", "https://live.artidijitalmedya.com/artidijital_sunrtv/sunrtv/chunks.m3u8", StreamType.HLS), StreamSource("Adaptif Akış (Playlist)", "https://live.artidijitalmedya.com/artidijital_sunrtv/sunrtv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://live.artidijitalmedya.com/artidijital_sunrtv/sunrtv/chunks.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_dim_tv_alanya", number = 278, name = "Dim TV Alanya", logoText = "DİM\nTV",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "https://live.artidijitalmedya.com/artidijital_dimtv/dimtv/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Doğrudan Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_dimtv/dimtv/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://live.artidijitalmedya.com/artidijital_dimtv/dimtv/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://live.artidijitalmedya.com/artidijital_dimtv/dimtv/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_tv_den_aydin", number = 279, name = "TV Den Aydın", logoText = "TV\nDEN",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "http://canli.tvden.com.tr/hls/live.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://canli.tvden.com.tr/hls/live.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_erzurum_web", number = 280, name = "Erzurum Web TV", logoText = "ERZUR\nWEB",
            c1 = 0xFF1F2937L, c2 = 0xFF64748BL, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "https://win29.yayin.com.tr/erzurumwebtv/erzurumwebtv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://win29.yayin.com.tr/erzurumwebtv/erzurumwebtv/iptvdelisi.m3u8", StreamType.HLS), StreamSource("Hızlı Chunks Akışı", "https://win29.yayin.com.tr/erzurumwebtv/erzurumwebtv/chunks.m3u8", StreamType.HLS), StreamSource("Mono Doğrudan Akış", "https://win29.yayin.com.tr/erzurumwebtv/erzurumwebtv/tracks-v1a1/mono.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_rize_turk", number = 281, name = "Rize Türk TV", logoText = "RİZE\nTÜRK",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://yayin.rizeturk.com:3777/hybrid/play.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "https://furuncu.kesintisizyayin.com:8044/;", StreamType.PROGRESSIVE), StreamSource("Alternatif Akış 3", "https://yayin.rizeturk.com:3777/hybrid/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "https://yayin.rizeturk.com:3777/hybrid/chunklist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_bir_tv", number = 282, name = "Bir TV", logoText = "BİR\nTV",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://edge.socialsmart.tv/birtv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Canlı Akış", "https://edge1.socialsmart.tv/birtv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Hızlı Akış (Edge1 Chunks)", "https://edge1.socialsmart.tv/birtv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/birtv/bant1/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_anadolum_tv", number = 283, name = "Anadolum TV", logoText = "ANADO\nTV",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "https://cdn4.yayin.com.tr/anadolum/tracks-v1a1/mono.m3u8", StreamType.HLS), StreamSource("Adaptif Playlist Akışı", "https://cdn4.yayin.com.tr/anadolum/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://cdn4.yayin.com.tr/anadolum/tracks-v1a1/mono.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_serhat_tv", number = 284, name = "Serhat TV Kars", logoText = "SERHA\nTV",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Canlı Akış", "https://cdn4.yayin.com.tr/serhattv/tracks-v1a1/mono.m3u8", StreamType.HLS), StreamSource("Adaptif Playlist Akışı", "https://cdn4.yayin.com.tr/serhattv/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://cdn4.yayin.com.tr/serhattv/tracks-v1a1/mono.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_viyana_tv", number = 285, name = "Viyana TV", logoText = "VİYAN\nTV",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Dünya", badge = "HD", logoUrl = "https://i.ibb.co/mHt7rQf/Tryerel.png",
            sources = listOf(StreamSource("Canlı Akış", "http://nrttn172.kesintisizyayin.com:29010/nrttn/nrttn/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "http://nrttn172.kesintisizyayin.com:29010/nrttn/nrttn/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "http://nrttn172.kesintisizyayin.com:29010/nrttn/nrttn/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_persiana_tr", number = 286, name = "Persiana Türkiye", logoText = "PERSI\nTR",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Dünya", badge = "FHD", logoUrl = "",
            sources = listOf(StreamSource("FHD 1080p", "https://turkhls.persiana.live/hls/stream.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 2", "http://turkhls.persiana.live/hls/stream.m3u8", StreamType.HLS))
        )        ,
        ch(
            id = "ch_tr_cnn_turk", number = 287, name = "CNN Türk", logoText = "CNN\nTÜRK",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Haber", badge = "FHD", logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/69/CNN_T%C3%BCrk_logo.svg/960px-CNN_T%C3%BCrk_logo.svg.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://live.duhnet.tv//S2/HLS_LIVE/cnnturknp/track_2_550/playlist.m3u8?&live=true&app=com.cnnturk&st=htljvbPX2R8vvfAY-YUYEw&e=1791036999", StreamType.HLS), StreamSource("Canlı Akış 2 (HD)", "https://live.duhnet.tv/S2/HLS_LIVE/cnnturknp/track_4_1000/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış 3 (Master)", "https://live.duhnet.tv/S2/HLS_LIVE/cnnturknp/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "http://46.32.176.50/cnnturk/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 5", "http://umperisn.mywire.org/index.php?id=594252&token=DeaTHLesS", StreamType.HLS), StreamSource("Duhnet Hızlı (Track 2)", "https://live.duhnet.tv/S2/HLS_LIVE/cnnturknp/track_2_550/playlist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_flash_haber_hd", number = 288, name = "Flash Haber HD", logoText = "FLASH\nHABER",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Haber", badge = "FHD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://flashhaber-live.ercdn.net/flashhaber/flashhaber_1080p.m3u8", StreamType.HLS), StreamSource("Adaptif Akış", "https://flashhaber-live.ercdn.net/flashhaber/flashhaber.m3u8", StreamType.HLS), StreamSource("HD 720p (Hızlı Yedek)", "https://flashhaber-live.ercdn.net/flashhaber/flashhaber_720p.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "https://b01c02nl.mediatriple.net/videoonlylive/mtyycglqauzjhlive/broadcast_67c053c48829f.smil/mt_0.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://flashhaber-live.ercdn.net/flashhaber/flashhaber_480p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_ekol_spor_hd", number = 289, name = "Ekol Spor HD", logoText = "EKOL\nSPOR",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Spor", badge = "FHD", logoUrl = "https://i.ibb.co/Ln92Dj2/Ekoltv.png",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://ekoltv-live.ercdn.net/ekolsport/ekolsport_1080p.m3u8", StreamType.HLS), StreamSource("Adaptif Akış", "https://ekoltv-live.ercdn.net/ekolsport/ekolsport.m3u8", StreamType.HLS), StreamSource("HD 720p (Hızlı Yedek)", "https://ekoltv-live.ercdn.net/ekolsport/ekolsport_720p.m3u8", StreamType.HLS), StreamSource("SD 480p Yedek Akış", "https://ekoltv-live.ercdn.net/ekolsport/ekolsport_480p.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_nr1_turk", number = 290, name = "NR1 Türk TV", logoText = "NR1\nTÜRK",
            c1 = 0xFFBE185DL, c2 = 0xFFF472B6L, cat = "Müzik", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/broadcast_5c9e187770143.smil/chunklist_b400000.m3u8", StreamType.HLS), StreamSource("Adaptif Akış (Playlist)", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/broadcast_5c9e187770143.smil/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "http://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/broadcast_5c9e187770143.smil/chunklist_b400000.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Chunks)", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/broadcast_5c9e187770143.smil/chunklist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/broadcast_5c9e187770143.smil/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_nr1_dance", number = 291, name = "NR1 Dance TV", logoText = "NR1\nDANCE",
            c1 = 0xFF6B21A8L, c2 = 0xFFA855F7L, cat = "Müzik", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e2aa8acf44_1/chunklist.m3u8", StreamType.HLS), StreamSource("Adaptif Akış (Playlist)", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e2aa8acf44_1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e2aa8acf44_1/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_nr1_rap", number = 292, name = "NR1 Rap TV", logoText = "NR1\nRAP",
            c1 = 0xFFC2410CL, c2 = 0xFFF97316L, cat = "Müzik", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e198784bdc_1/chunklist.m3u8", StreamType.HLS), StreamSource("Adaptif Akış (Playlist)", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e198784bdc_1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Akış (Index)", "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e198784bdc_1/index.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_damar_turk", number = 293, name = "Damar Türk TV", logoText = "DAMAR\nTÜRK",
            c1 = 0xFFB91C1CL, c2 = 0xFFEF4444L, cat = "Müzik", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://stream.yayintescil.com:3183/hybrid/play.m3u8", StreamType.HLS), StreamSource("Yedek Akış 2", "https://stream.yayintescil.com:3183/hybrid/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://stream.yayintescil.com:3183/hybrid/chunklist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "http://stream.yayintescil.com:3183/hybrid/play.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_ritim_tv", number = 294, name = "Ritim TV", logoText = "RİTİM\nTV",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Müzik", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://ritimtv.ozelip.com:3632/hybrid/play.m3u8", StreamType.HLS), StreamSource("Yedek Akış 2", "https://ritimtv.ozelip.com:3632/hybrid/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://ritimtv.ozelip.com:3632/hybrid/chunklist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "http://ritimtv.ozelip.com:3632/hybrid/play.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_saban_tv", number = 295, name = "Şaban TV", logoText = "ŞABAN\nTV",
            c1 = 0xFFB45309L, c2 = 0xFFFACC15L, cat = "Ulusal", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://tv156.radyotelekom.com.tr:3996/hybrid/play.m3u8", StreamType.HLS), StreamSource("Yedek Akış 2", "https://tv156.radyotelekom.com.tr:3996/hybrid/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://tv156.radyotelekom.com.tr:3996/hybrid/chunklist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "http://tv156.radyotelekom.com.tr:3996/hybrid/play.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_vrt_tv", number = 296, name = "VRT TV", logoText = "VRT\nTV",
            c1 = 0xFF1E3A8AL, c2 = 0xFF3B82F6L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://vrttv.ozelip.com:3278/hybrid/play.m3u8", StreamType.HLS), StreamSource("Yedek Akış 2", "https://vrttv.ozelip.com:3278/hybrid/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://vrttv.ozelip.com:3278/hybrid/chunklist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "http://vrttv.ozelip.com:3278/hybrid/play.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_tv_b", number = 297, name = "TV B", logoText = "TV B",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Yerel", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://ip44.radyotelekomtv.com:3958/stream/play.m3u8", StreamType.HLS), StreamSource("Yedek Akış 2", "https://ip44.radyotelekomtv.com:3958/stream/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://ip44.radyotelekomtv.com:3958/stream/chunklist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "http://ip44.radyotelekomtv.com:3958/stream/play.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_slow_karadeniz_fm", number = 298, name = "Slow Karadeniz FM", logoText = "SLOW\nFM",
            c1 = 0xFF0E7490L, c2 = 0xFF06B6D4L, cat = "Müzik", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://dinle.slowkaradenizfm.com:9578/;", StreamType.PROGRESSIVE), StreamSource("Slow Karadeniz TV Yayını", "https://yayin.slowkaradeniztv.com:3390/hybrid/play.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 3", "https://yayin.slowkaradeniztv.com:3390/hybrid/playlist.m3u8", StreamType.HLS), StreamSource("Alternatif Akış 4", "https://yayin.slowkaradeniztv.com:3390/hybrid/chunklist.m3u8", StreamType.HLS))
        ),
        ch(
            id = "ch_tr_cay_fm", number = 299, name = "Çay FM", logoText = "ÇAY\nFM",
            c1 = 0xFF047857L, c2 = 0xFF10B981L, cat = "Müzik", badge = "HD", logoUrl = "",
            sources = listOf(StreamSource("Ana Yayın (FHD/HD)", "https://dinle.cayfm.net:8010/;", StreamType.PROGRESSIVE), StreamSource("Çay TV Yayını", "https://edge1.socialsmart.tv/caytv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Adaptif Akış (Edge1 Playlist)", "https://edge1.socialsmart.tv/caytv/bant1/playlist.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Chunks)", "https://edge.socialsmart.tv/caytv/bant1/chunks.m3u8", StreamType.HLS), StreamSource("Yedek Sunucu (Edge Playlist)", "https://edge.socialsmart.tv/caytv/bant1/playlist.m3u8", StreamType.HLS))
        )
    )

    fun generateScheduleForChannelFast(
        channel: TvChannel,
        baseHourStart: Long,
        currentHourOfDay: Int,
        preformattedWindows: List<String>
    ): List<EpgProgram> {
        val oneHour = 60 * 60 * 1000L
        val templates = when (channel.category.lowercase()) {
            "ulusal" -> listOf(
                "Ana Haber & Gündem" to "Türkiye ve dünyadan sıcak gelişmeler.",
                "Prime Time Dizisi" to "Yüksek çözünürlüklü ana yayın kuşağı.",
                "Gündüz Kuşağı & Yaşam" to "Stüdyo konukları ve güncel sohbetler.",
                "Gece Sineması" to "Ödüllü yerli ve yabancı sinema seçkisi."
            )
            "haber" -> listOf(
                "Canlı Haber Maratonu" to "Son dakika gelişmeleri ve canlı bağlantılar.",
                "Ekonomi ve Piyasalar" to "Borsa, döviz, altın ve piyasa analizleri.",
                "Gündem Özel Tartışma" to "Uzman konuklarla derinlemesine analizler.",
                "Gece Bülteni" to "Günün özeti ve yarının manşetleri."
            )
            "spor" -> listOf(
                "Stadyum Canlı & Maç Önü" to "Süper Lig ve Avrupa kupalarından canlı yayın.",
                "Maç Özeti & Teknik Analiz" to "Haftanın golleri ve değerlendirmeler.",
                "Transfer Raporu" to "Kulüplerden son dakika gelişmeleri.",
                "Dünya Spor Kuşağı" to "Uluslararası turnuvalar ve özetler."
            )
            "belgesel" -> listOf(
                "Vahşi Yaşam Atlası (4K)" to "Doğa harikaları ve nadir türlerin izinde.",
                "Uzay ve Bilim Kuşağı" to "Derin uzay gözlemleri ve keşifler.",
                "Antik Medeniyetler" to "Tarihin gizemli yapıları ve kazılar.",
                "Mühendislik Harikaları" to "Dev yapılar ve geleceğin teknolojileri."
            )
            "müzik" -> listOf(
                "Top 20 Hit Klipler" to "Haftanın en çok dinlenen video klipleri.",
                "Akustik Stüdyo" to "Canlı performanslar ve unutulmaz şarkılar.",
                "Nostalji Kuşağı" to "Geçmişten bugüne iz bırakan şarkılar.",
                "Gece Ritmi Non-Stop" to "Kesintisiz müzik performansları."
            )
            else -> listOf(
                "${channel.name} Canlı Yayın" to "${channel.category} kategorisinde kesintisiz yayın.",
                "Özel Kuşak" to "İzleyiciler için hazırlanan güncel içerikler.",
                "Günün Seçkisi" to "${channel.name} ekranlarında öne çıkan program.",
                "Gece Kuşağı" to "Günün özeti ve tekrar bölümler."
            )
        }

        return (-1..3).mapIndexed { i, offset ->
            val slotStart = baseHourStart + (offset * oneHour)
            val slotEnd = slotStart + oneHour
            val idx = (currentHourOfDay + offset).mod(templates.size)
            val (title, desc) = templates[idx]
            EpgProgram(
                channelId = channel.epgChannelId,
                title = title,
                description = desc,
                category = channel.category,
                startTimeMillis = slotStart,
                endTimeMillis = slotEnd,
                timeWindowText = preformattedWindows[i]
            )
        }
    }
}

@Immutable
data class TvExtraSettingsState(
    val previousPlayingChannel: TvChannel? = null,
    val preferredSources: Map<String, Int> = emptyMap(),
    val preferredScales: Map<String, String> = emptyMap(),
    val autoStartLastChannel: Boolean = false,
    val hiddenChannelIds: Set<String> = emptySet(),
    val healthyChannelIds: Set<String> = emptySet(),
    val isScanningHealth: Boolean = false,
    val healthScanProgress: String = "",
    val onlyShowHealthy: Boolean = false
)

@Immutable
data class TvUiState(
    val showLandingScreen: Boolean = true,
    val selectedTab: MainNavTab = MainNavTab.ALL_CHANNELS,
    val builtInChannels: List<TvChannel> = emptyList(),
    val customM3uChannels: List<TvChannel> = emptyList(),
    val categories: List<String> = listOf("Tümü"),
    val customCategories: List<String> = listOf("Tümü"),
    val selectedCategory: String = "Tümü",
    val favoriteOrderList: List<String> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val lockedChannelIds: Set<String> = emptySet(),
    val brokenChannelIds: Set<String> = emptySet(),
    val hiddenChannelIds: Set<String> = emptySet(),
    val healthyChannelIds: Set<String> = emptySet(),
    val isScanningHealth: Boolean = false,
    val healthScanProgress: String = "",
    val onlyShowHealthy: Boolean = false,
    val preferredSources: Map<String, Int> = emptyMap(),
    val preferredScales: Map<String, String> = emptyMap(),
    val autoStartLastChannel: Boolean = false,
    val previousPlayingChannel: TvChannel? = null,
    val parentalPin: String = "0000",
    val pendingPinChannel: TvChannel? = null,
    val recentChannels: List<TvChannel> = emptyList(),
    val epgMap: Map<String, List<EpgProgram>> = emptyMap(),
    val playlists: List<M3uPlaylistSource> = emptyList(),
    val activePlayingChannel: TvChannel? = null,
    val pipChannel: TvChannel? = null,
    val multiViewChannels: List<TvChannel> = emptyList(),
    val isMultiViewFourGrid: Boolean = true,
    val searchQuery: String = "",
    val sleepTimerRemainingMinutes: Int? = null,
    val localWebServerUrl: String = "",
    val isLoadingM3u: Boolean = false,
    val statusMessage: String? = null
) {
    val orderedFavoriteChannels: List<TvChannel>
        get() {
            val all = builtInChannels + customM3uChannels
            val byId = all.associateBy { it.id }
            return favoriteOrderList.mapNotNull { byId[it] }
        }

    val activeTabChannels: List<TvChannel>
        get() = when (selectedTab) {
            MainNavTab.FAVORITES -> orderedFavoriteChannels.ifEmpty { builtInChannels }
            MainNavTab.CUSTOM_M3U -> customM3uChannels.ifEmpty { builtInChannels }
            else -> builtInChannels
        }
}

class TvViewModel(private val repository: TvRepository) : ViewModel() {
    private val initialBuiltIn = repository.getBuiltInChannels()

    private val _showLandingScreen = MutableStateFlow(true)
    private val _selectedTab = MutableStateFlow(MainNavTab.ALL_CHANNELS)
    // ANINDA AÇILIŞ: 222 Kanal 0. milisaniyede doğrudan hazır!
    private val _builtInChannels = MutableStateFlow<List<TvChannel>>(initialBuiltIn)
    private val _customM3uChannels = MutableStateFlow<List<TvChannel>>(emptyList())
    private val _selectedCategory = MutableStateFlow("Tümü")
    private val _favoriteOrder = MutableStateFlow<List<String>>(repository.loadFavoriteOrderList())
    private val _lockedIds = MutableStateFlow<Set<String>>(repository.loadLockedChannelIds())
    private val _brokenIds = MutableStateFlow<Set<String>>(repository.loadBrokenChannelIds())
    private val _parentalPin = MutableStateFlow(repository.loadParentalPin())
    private val _pendingPinChannel = MutableStateFlow<TvChannel?>(null)
    private val _recentIds = MutableStateFlow<List<String>>(repository.loadRecentChannelIds())
    private val _epgMap = MutableStateFlow<Map<String, List<EpgProgram>>>(emptyMap())
    private val _playlists = MutableStateFlow<List<M3uPlaylistSource>>(repository.loadSavedPlaylists())
    private val _activePlayingChannel = MutableStateFlow<TvChannel?>(null)
    private val _pipChannel = MutableStateFlow<TvChannel?>(null)
    private val _multiViewChannels = MutableStateFlow<List<TvChannel>>(initialBuiltIn.take(4))
    private val _isMultiViewFourGrid = MutableStateFlow(true)
    private val _searchQuery = MutableStateFlow("")
    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    private val _localWebServerUrl = MutableStateFlow("http://${LocalTransferWebServer.getLocalWifiIpAddress()}:8585")
    private val _isLoadingM3u = MutableStateFlow(false)
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _extraState = MutableStateFlow(
        TvExtraSettingsState(
            preferredSources = repository.loadPreferredSources(),
            preferredScales = repository.loadPreferredScales(),
            autoStartLastChannel = repository.loadAutoStartLastChannel(),
            hiddenChannelIds = repository.loadHiddenChannelIds()
        )
    )

    private var sleepTimerJob: Job? = null
    private var healthCheckJob: Job? = null
    private var webServerSocket: ServerSocket? = null
    private var lastZapElapsedMillis: Long = 0L

    val uiState: StateFlow<TvUiState> = combine(
        _showLandingScreen,
        _selectedTab,
        _builtInChannels,
        _customM3uChannels,
        _selectedCategory,
        _favoriteOrder,
        _lockedIds,
        _brokenIds,
        _parentalPin,
        _pendingPinChannel,
        _recentIds,
        _epgMap,
        _playlists,
        _activePlayingChannel,
        _pipChannel,
        _multiViewChannels,
        _isMultiViewFourGrid,
        _searchQuery,
        _sleepTimerMinutes,
        _localWebServerUrl,
        _isLoadingM3u,
        _statusMessage,
        _extraState
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val showLanding = values[0] as Boolean
        val tab = values[1] as MainNavTab
        val builtIn = values[2] as List<TvChannel>
        val customCh = values[3] as List<TvChannel>
        val category = values[4] as String
        val favOrder = values[5] as List<String>
        val locked = values[6] as Set<String>
        val broken = values[7] as Set<String>
        val pin = values[8] as String
        val pendingPinCh = values[9] as TvChannel?
        val recentIds = values[10] as List<String>
        val epg = values[11] as Map<String, List<EpgProgram>>
        val playlists = values[12] as List<M3uPlaylistSource>
        val playing = values[13] as TvChannel?
        val pip = values[14] as TvChannel?
        val multiList = values[15] as List<TvChannel>
        val isFourGrid = values[16] as Boolean
        val query = values[17] as String
        val sleepMin = values[18] as Int?
        val webUrl = values[19] as String
        val loading = values[20] as Boolean
        val status = values[21] as String?
        val extra = values[22] as TvExtraSettingsState

        val hidden = extra.hiddenChannelIds
        val visibleBuiltIn = if (hidden.isEmpty()) builtIn else builtIn.filterNot { hidden.contains(it.id) }
        val visibleCustom = if (hidden.isEmpty()) customCh else customCh.filterNot { hidden.contains(it.id) }

        val filteredBuiltIn = if (extra.onlyShowHealthy && extra.healthyChannelIds.isNotEmpty()) {
            visibleBuiltIn.filter { extra.healthyChannelIds.contains(it.id) }
        } else visibleBuiltIn

        val filteredCustom = if (extra.onlyShowHealthy && extra.healthyChannelIds.isNotEmpty()) {
            visibleCustom.filter { extra.healthyChannelIds.contains(it.id) }
        } else visibleCustom

        val all = visibleBuiltIn + visibleCustom
        val favSet = favOrder.toSet()
        val distinctBuiltInCats = listOf("Tümü") + visibleBuiltIn.map { it.category }.distinct()
        val distinctCustomCats = listOf("Tümü") + visibleCustom.map { it.category }.distinct()
        val recentObjs = recentIds.mapNotNull { id -> all.find { it.id == id } }
        val defaultMulti = if (multiList.isNotEmpty()) multiList else visibleBuiltIn.take(4)

        val builtInMarked = if (broken.isEmpty()) filteredBuiltIn else filteredBuiltIn.map { it.copy(isBroken = broken.contains(it.id)) }
        val customMarked = if (broken.isEmpty()) filteredCustom else filteredCustom.map { it.copy(isBroken = broken.contains(it.id)) }

        TvUiState(
            showLandingScreen = showLanding,
            selectedTab = tab,
            builtInChannels = builtInMarked,
            customM3uChannels = customMarked,
            categories = distinctBuiltInCats,
            customCategories = distinctCustomCats,
            selectedCategory = category,
            favoriteOrderList = favOrder,
            favoriteIds = favSet,
            lockedChannelIds = locked,
            brokenChannelIds = broken,
            hiddenChannelIds = hidden,
            healthyChannelIds = extra.healthyChannelIds,
            isScanningHealth = extra.isScanningHealth,
            healthScanProgress = extra.healthScanProgress,
            onlyShowHealthy = extra.onlyShowHealthy,
            preferredSources = extra.preferredSources,
            preferredScales = extra.preferredScales,
            autoStartLastChannel = extra.autoStartLastChannel,
            previousPlayingChannel = extra.previousPlayingChannel,
            parentalPin = pin,
            pendingPinChannel = pendingPinCh,
            recentChannels = recentObjs,
            epgMap = epg,
            playlists = playlists,
            activePlayingChannel = playing,
            pipChannel = pip,
            multiViewChannels = defaultMulti,
            isMultiViewFourGrid = isFourGrid,
            searchQuery = query,
            sleepTimerRemainingMinutes = sleepMin,
            localWebServerUrl = webUrl,
            isLoadingM3u = loading,
            statusMessage = status
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        TvUiState(
            builtInChannels = initialBuiltIn,
            categories = listOf("Tümü") + initialBuiltIn.map { it.category }.distinct(),
            favoriteOrderList = _favoriteOrder.value,
            favoriteIds = _favoriteOrder.value.toSet(),
            hiddenChannelIds = _extraState.value.hiddenChannelIds,
            preferredSources = _extraState.value.preferredSources,
            preferredScales = _extraState.value.preferredScales,
            autoStartLastChannel = _extraState.value.autoStartLastChannel,
            localWebServerUrl = _localWebServerUrl.value
        )
    )

    init {
        if (_extraState.value.autoStartLastChannel) {
            val lastId = _recentIds.value.firstOrNull()
            val targetCh = initialBuiltIn.find { it.id == lastId } ?: initialBuiltIn.firstOrNull()
            if (targetCh != null && !_lockedIds.value.contains(targetCh.id)) {
                _showLandingScreen.value = false
                _activePlayingChannel.value = targetCh
            }
        }
        reloadAllChannelsAndEpg()
        startLocalWifiTransferServer()
    }

    override fun onCleared() {
        super.onCleared()
        try { webServerSocket?.close() } catch (_: Exception) {}
    }

    fun onAppBackgrounded() {
        _pipChannel.value = null
        _activePlayingChannel.value = null
    }

    fun dismissLandingAndOpenTab(tab: MainNavTab = MainNavTab.ALL_CHANNELS) {
        _selectedTab.value = tab
        _showLandingScreen.value = false
        if (tab == MainNavTab.M3U_MANAGER) {
            refreshWifiTransferServer()
        }
    }

    fun openLandingScreen() {
        _activePlayingChannel.value = null
        _pipChannel.value = null
        _showLandingScreen.value = true
    }

    fun refreshWifiTransferServer() {
        val ip = LocalTransferWebServer.getLocalWifiIpAddress()
        val currentSocket = webServerSocket
        if (currentSocket != null && !currentSocket.isClosed && currentSocket.isBound) {
            val activePort = currentSocket.localPort.takeIf { it > 0 } ?: 8585
            _localWebServerUrl.value = "http://$ip:$activePort"
        } else {
            startLocalWifiTransferServer()
        }
    }

    private fun startLocalWifiTransferServer() {
        viewModelScope.launch(Dispatchers.IO) {
            val ip = LocalTransferWebServer.getLocalWifiIpAddress()
            _localWebServerUrl.value = "http://$ip:8585"
            try {
                try { webServerSocket?.close() } catch (_: Exception) {}
                var boundServer: ServerSocket? = null
                var boundPort = 8585
                for (candidatePort in 8585..8595) {
                    try {
                        val s = ServerSocket()
                        s.reuseAddress = true
                        s.bind(InetSocketAddress(candidatePort))
                        boundServer = s
                        boundPort = candidatePort
                        break
                    } catch (_: Exception) {}
                }
                val server = boundServer ?: return@launch
                webServerSocket = server
                _localWebServerUrl.value = "http://${LocalTransferWebServer.getLocalWifiIpAddress()}:$boundPort"

                while (isActive && !server.isClosed) {
                    val client = server.accept()
                    launch(Dispatchers.IO) {
                        try {
                            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                            val requestLine = reader.readLine() ?: ""
                            var contentLength = 0
                            var line = reader.readLine()
                            while (!line.isNullOrEmpty()) {
                                if (line.lowercase().startsWith("content-length:")) {
                                    contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                                }
                                line = reader.readLine()
                            }

                            val bodyChars = CharArray(contentLength)
                            var readTotal = 0
                            while (readTotal < contentLength) {
                                val r = reader.read(bodyChars, readTotal, contentLength - readTotal)
                                if (r == -1) break
                                readTotal += r
                            }
                            val body = String(bodyChars)
                            val params = parseFormUrlEncoded(body)

                            if (requestLine.startsWith("POST /add_m3u")) {
                                val name = params["name"].orEmpty().ifBlank { "Telefondan M3U" }
                                val url = params["url"].orEmpty().trim()
                                val rawContent = params["raw_content"].orEmpty().trim()
                                if (rawContent.isNotBlank()) {
                                    val saved = repository.saveRawM3uTextAsLocalPlaylist(rawContent, name)
                                    if (saved != null) {
                                        _playlists.value = _playlists.value + saved
                                        repository.savePlaylists(_playlists.value)
                                        reloadAllChannelsAndEpg()
                                        _statusMessage.value = "Telefondan '$name' yüklendi!"
                                    }
                                } else if (url.isNotBlank()) {
                                    addRemoteM3uPlaylist(name, url, "")
                                    _statusMessage.value = "Telefondan '$name' eklendi!"
                                }
                            } else if (requestLine.startsWith("POST /add_xtream")) {
                                val name = params["name"].orEmpty().ifBlank { "Xtream Paketi" }
                                val srv = params["server"].orEmpty()
                                val usr = params["username"].orEmpty()
                                val pwd = params["password"].orEmpty()
                                if (srv.isNotBlank() && usr.isNotBlank()) {
                                    addXtreamCodesAccount(name, srv, usr, pwd)
                                    _statusMessage.value = "Telefondan '$name' Xtream bağlandı!"
                                }
                            } else if (requestLine.startsWith("POST /restore_backup")) {
                                val backupData = params["backup_data"].orEmpty()
                                if (restoreFromBackupString(backupData)) {
                                    _statusMessage.value = "♻️ Telefondan yedek başarıyla geri yüklendi!"
                                }
                            }

                            if (requestLine.startsWith("GET /backup")) {
                                val backupBytes = repository.exportBackupPayload().toByteArray(Charsets.UTF_8)
                                val dlHeader = "HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Disposition: attachment; filename=\"redox_tv_backup.txt\"\r\nContent-Length: ${backupBytes.size}\r\nConnection: close\r\n\r\n"
                                val out = client.getOutputStream()
                                out.write(dlHeader.toByteArray(Charsets.UTF_8))
                                out.write(backupBytes)
                                out.flush()
                            } else {
                                val html = LocalTransferWebServer.buildMobileWebHtml()
                                val bytes = html.toByteArray(Charsets.UTF_8)
                                val header = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                                val out = client.getOutputStream()
                                out.write(header.toByteArray(Charsets.UTF_8))
                                out.write(bytes)
                                out.flush()
                            }
                        } catch (_: Exception) {
                        } finally {
                            try { client.close() } catch (_: Exception) {}
                        }
                    }
                }
            } catch (_: Exception) {
                _localWebServerUrl.value = "http://${LocalTransferWebServer.getLocalWifiIpAddress()}:8585"
            }
        }
    }

    private fun parseFormUrlEncoded(body: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        body.split("&").forEach { pair ->
            val parts = pair.split("=", limit = 2)
            if (parts.size == 2) {
                map[URLDecoder.decode(parts[0], "UTF-8")] = URLDecoder.decode(parts[1], "UTF-8")
            }
        }
        return map
    }

    fun selectTab(tab: MainNavTab) {
        _selectedTab.value = tab
        _selectedCategory.value = "Tümü"
        _showLandingScreen.value = false
        if (tab == MainNavTab.M3U_MANAGER) {
            refreshWifiTransferServer()
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun requestPlayChannel(channel: TvChannel, bypassPin: Boolean = false) {
        if (!bypassPin && _lockedIds.value.contains(channel.id)) {
            _pendingPinChannel.value = channel
            return
        }
        val currentActive = _activePlayingChannel.value
        if (currentActive != null && currentActive.id != channel.id) {
            _extraState.value = _extraState.value.copy(previousPlayingChannel = currentActive)
        }
        _showLandingScreen.value = false
        _pendingPinChannel.value = null
        _pipChannel.value = null
        _recentIds.value = repository.pushRecentChannelId(channel.id)
        _activePlayingChannel.value = channel
    }

    fun recallPreviousChannel() {
        val prev = _extraState.value.previousPlayingChannel
        if (prev != null) {
            requestPlayChannel(prev)
        } else {
            val secondRecentId = _recentIds.value.drop(1).firstOrNull()
            if (secondRecentId != null) {
                val all = _builtInChannels.value + _customM3uChannels.value
                all.find { it.id == secondRecentId }?.let { requestPlayChannel(it) }
            }
        }
    }

    fun saveChannelPreferredSource(channelId: String, sourceIndex: Int) {
        val updated = _extraState.value.preferredSources.toMutableMap()
        updated[channelId] = sourceIndex
        _extraState.value = _extraState.value.copy(preferredSources = updated)
        repository.savePreferredSources(updated)
    }

    fun saveChannelPreferredScale(channelId: String, scaleModeName: String) {
        val updated = _extraState.value.preferredScales.toMutableMap()
        updated[channelId] = scaleModeName
        _extraState.value = _extraState.value.copy(preferredScales = updated)
        repository.savePreferredScales(updated)
    }

    fun toggleAutoStartLastChannel() {
        val next = !_extraState.value.autoStartLastChannel
        _extraState.value = _extraState.value.copy(autoStartLastChannel = next)
        repository.saveAutoStartLastChannel(next)
        _statusMessage.value = if (next) {
            "⚡ Açılışta Son İzlenen Kanaldan Başla: AÇIK"
        } else {
            "⚡ Açılışta Son İzlenen Kanaldan Başla: KAPALI (Landing Page Açılır)"
        }
    }

    fun hideChannel(channelId: String) {
        val all = _builtInChannels.value + _customM3uChannels.value
        val chName = all.find { it.id == channelId }?.name ?: "Kanal"
        val updated = _extraState.value.hiddenChannelIds.toMutableSet()
        if (updated.add(channelId)) {
            _extraState.value = _extraState.value.copy(hiddenChannelIds = updated)
            repository.saveHiddenChannelIds(updated)
            if (_activePlayingChannel.value?.id == channelId) {
                zapChannel(+1)
            }
            _statusMessage.value = "👁️ $chName listeden gizlendi (M3U & Ayarlar'dan geri yüklenebilir)."
        }
    }

    fun restoreAllHiddenChannels() {
        val count = _extraState.value.hiddenChannelIds.size
        if (count == 0) {
            _statusMessage.value = "Gizlenmiş kanal bulunmuyor."
            return
        }
        _extraState.value = _extraState.value.copy(hiddenChannelIds = emptySet())
        repository.saveHiddenChannelIds(emptySet())
        _statusMessage.value = "👁️ Gizlenen $count kanal listeye geri yüklendi!"
    }

    fun toggleOnlyShowHealthy() {
        val next = !_extraState.value.onlyShowHealthy
        _extraState.value = _extraState.value.copy(onlyShowHealthy = next)
        _statusMessage.value = if (next) {
            "🟢 Sadece sağlık testinden geçen aktif kanallar gösteriliyor."
        } else {
            "Tüm kanallar gösteriliyor."
        }
    }

    fun runChannelHealthCheck() {
        if (_extraState.value.isScanningHealth) {
            healthCheckJob?.cancel()
            _extraState.value = _extraState.value.copy(
                isScanningHealth = false,
                healthScanProgress = ""
            )
            _statusMessage.value = "Kanal sağlık taraması durduruldu."
            return
        }
        healthCheckJob = viewModelScope.launch(Dispatchers.IO) {
            val hidden = _extraState.value.hiddenChannelIds
            val pool = (_builtInChannels.value + _customM3uChannels.value)
                .filterNot { hidden.contains(it.id) }
            if (pool.isEmpty()) return@launch

            _extraState.value = _extraState.value.copy(
                isScanningHealth = true,
                healthScanProgress = "0/${pool.size}"
            )
            _statusMessage.value = "🩺 Tüm kanallar (${pool.size} kanal) paralel olarak taranıyor..."

            val healthy = _extraState.value.healthyChannelIds.toMutableSet()
            val broken = _brokenIds.value.toMutableSet()
            var okCount = 0
            var failCount = 0
            var scannedCount = 0

            pool.chunked(16).forEach { batch ->
                val results = coroutineScope {
                    batch.map { ch ->
                        async(Dispatchers.IO) {
                            ch.id to repository.probeChannelStreamHealth(ch)
                        }
                    }.awaitAll()
                }
                results.forEach { (id, isOk) ->
                    if (isOk) {
                        healthy.add(id)
                        broken.remove(id)
                        okCount += 1
                    } else {
                        healthy.remove(id)
                        broken.add(id)
                        failCount += 1
                    }
                }
                scannedCount += batch.size
                _extraState.value = _extraState.value.copy(
                    healthyChannelIds = healthy.toSet(),
                    healthScanProgress = "$scannedCount/${pool.size}"
                )
                _brokenIds.value = broken.toSet()
            }
            repository.saveBrokenChannelIds(broken)
            _extraState.value = _extraState.value.copy(
                healthyChannelIds = healthy.toSet(),
                isScanningHealth = false,
                healthScanProgress = ""
            )
            _statusMessage.value = "🩺 Tüm kanallar tarandı (${pool.size}): 🟢 $okCount Aktif • ⚠️ $failCount Yanıt Vermiyor"
        }
    }

    fun backupSettingsToDevice() {
        viewModelScope.launch(Dispatchers.IO) {
            val savedPath = repository.saveBackupFileToDevice()
            if (savedPath != null) {
                _statusMessage.value = "💾 Favoriler, M3U ve kanal hafızası yedeklendi: $savedPath"
            } else {
                _statusMessage.value = "⚠️ Yedek dosyası oluşturulamadı."
            }
        }
    }

    fun restoreSettingsFromDevice() {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = repository.restoreBackupFileFromDevice()
            if (ok) {
                applyRestoredPreferences()
                _statusMessage.value = "♻️ Yedek başarıyla geri yüklendi!"
            } else {
                _statusMessage.value = "⚠️ Download klasöründe 'redox_tv_backup.txt' bulunamadı."
            }
        }
    }

    private fun restoreFromBackupString(raw: String): Boolean {
        val ok = repository.importBackupPayload(raw)
        if (ok) {
            applyRestoredPreferences()
        }
        return ok
    }

    private fun applyRestoredPreferences() {
        _favoriteOrder.value = repository.loadFavoriteOrderList()
        _lockedIds.value = repository.loadLockedChannelIds()
        _parentalPin.value = repository.loadParentalPin()
        _playlists.value = repository.loadSavedPlaylists()
        _extraState.value = _extraState.value.copy(
            preferredSources = repository.loadPreferredSources(),
            preferredScales = repository.loadPreferredScales(),
            autoStartLastChannel = repository.loadAutoStartLastChannel(),
            hiddenChannelIds = repository.loadHiddenChannelIds()
        )
        reloadAllChannelsAndEpg()
    }

    fun markChannelAsBroken(channelId: String) {
        val updated = _brokenIds.value.toMutableSet()
        if (updated.add(channelId)) {
            _brokenIds.value = updated
            repository.saveBrokenChannelIds(updated)
        }
    }

    fun clearChannelBrokenStatus(channelId: String) {
        val updated = _brokenIds.value.toMutableSet()
        if (updated.remove(channelId)) {
            _brokenIds.value = updated
            repository.saveBrokenChannelIds(updated)
        }
    }

    fun dismissPinDialog() {
        _pendingPinChannel.value = null
    }

    fun verifyPinAndPlay(enteredPin: String): Boolean {
        val target = _pendingPinChannel.value ?: return false
        return if (enteredPin == _parentalPin.value) {
            requestPlayChannel(target, bypassPin = true)
            true
        } else {
            _statusMessage.value = "Hatalı PIN! Mevcut PIN: ${_parentalPin.value}"
            false
        }
    }

    fun toggleChannelLock(channelId: String) {
        val updated = _lockedIds.value.toMutableSet()
        val lockedNow = if (updated.add(channelId)) true else {
            updated.remove(channelId)
            false
        }
        _lockedIds.value = updated
        repository.saveLockedChannelIds(updated)
        _statusMessage.value = if (lockedNow) {
            "Kanal PIN (${_parentalPin.value}) ile kilitlendi."
        } else {
            "Kanal kilidi kaldırıldı."
        }
    }

    fun updateParentalPin(newPin: String) {
        if (newPin.length == 4 && newPin.all { it.isDigit() }) {
            _parentalPin.value = newPin
            repository.saveParentalPin(newPin)
            _statusMessage.value = "Ebeveyn Kilidi PIN kodu '$newPin' olarak güncellendi."
        }
    }

    fun playChannelByNumber(channelNumber: Int) {
        val all = _builtInChannels.value + _customM3uChannels.value
        val match = all.find { it.number == channelNumber }
        if (match != null) {
            requestPlayChannel(match)
        } else {
            _statusMessage.value = "Kanal $channelNumber bulunamadı."
        }
    }

    fun minimizeToPip() {
        val current = _activePlayingChannel.value ?: return
        _activePlayingChannel.value = null
        _pipChannel.value = current
    }

    fun closePip() {
        _pipChannel.value = null
    }

    fun expandPipToFullScreen() {
        val pip = _pipChannel.value ?: return
        _pipChannel.value = null
        _activePlayingChannel.value = pip
    }

    fun openMultiViewFromPlayer() {
        val current = _activePlayingChannel.value
        _activePlayingChannel.value = null
        _pipChannel.value = null
        if (current != null) {
            val all = _builtInChannels.value + _customM3uChannels.value
            val others = all.filterNot { it.id == current.id }.take(3)
            _multiViewChannels.value = listOf(current) + others
        }
        _selectedTab.value = MainNavTab.MULTI_VIEW
    }

    fun toggleMultiViewMode() {
        _isMultiViewFourGrid.value = !_isMultiViewFourGrid.value
    }

    fun changeMultiViewSlotChannel(slotIndex: Int, delta: Int) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastZapElapsedMillis < 420L) return
        lastZapElapsedMillis = now

        val all = _builtInChannels.value + _customM3uChannels.value
        if (all.isEmpty()) return
        val currentSlots = (_multiViewChannels.value.ifEmpty { all.take(4) }).toMutableList()
        while (currentSlots.size < 4) {
            currentSlots.add(all[currentSlots.size % all.size])
        }
        val currentCh = currentSlots[slotIndex]
        val idx = all.indexOfFirst { it.id == currentCh.id }.coerceAtLeast(0)
        currentSlots[slotIndex] = all[(idx + delta).mod(all.size)]
        _multiViewChannels.value = currentSlots
    }

    fun closePlayer() {
        _activePlayingChannel.value = null
    }

    fun zapChannel(delta: Int) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastZapElapsedMillis < 450L) return
        lastZapElapsedMillis = now

        val current = _activePlayingChannel.value ?: return
        val currentList = when (_selectedTab.value) {
            MainNavTab.FAVORITES -> {
                val all = _builtInChannels.value + _customM3uChannels.value
                val byId = all.associateBy { it.id }
                _favoriteOrder.value.mapNotNull { byId[it] }.ifEmpty { _builtInChannels.value }
            }
            MainNavTab.CUSTOM_M3U -> _customM3uChannels.value.ifEmpty { _builtInChannels.value }
            else -> _builtInChannels.value
        }
        if (currentList.isEmpty()) return

        val currentIndex = currentList.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
        val nextIndex = (currentIndex + delta).mod(currentList.size)
        requestPlayChannel(currentList[nextIndex])
    }

    fun toggleFavorite(channelId: String) {
        val all = _builtInChannels.value + _customM3uChannels.value
        val chName = all.find { it.id == channelId }?.name ?: "Kanal"
        val updated = _favoriteOrder.value.toMutableList()
        if (updated.contains(channelId)) {
            updated.remove(channelId)
            _statusMessage.value = "☆ $chName favorilerden çıkarıldı."
        } else {
            updated.add(channelId)
            _statusMessage.value = "⭐ $chName favorilere eklendi (#${updated.size})."
        }
        _favoriteOrder.value = updated
        repository.saveFavoriteOrderList(updated)
    }

    fun moveFavoriteChannel(channelId: String, delta: Int) {
        val current = _favoriteOrder.value.toMutableList()
        val idx = current.indexOf(channelId)
        if (idx == -1) return
        val targetIdx = (idx + delta).coerceIn(0, current.lastIndex)
        if (idx == targetIdx) return
        val item = current.removeAt(idx)
        current.add(targetIdx, item)
        _favoriteOrder.value = current
        repository.saveFavoriteOrderList(current)
        val all = _builtInChannels.value + _customM3uChannels.value
        val chName = all.find { it.id == channelId }?.name ?: "Kanal"
        _statusMessage.value = "📌 $chName favori sırası #${targetIdx + 1} konumuna taşındı."
    }

    fun sortFavoritesBy(mode: String) {
        val all = _builtInChannels.value + _customM3uChannels.value
        val byId = all.associateBy { it.id }
        val currentFavChannels = _favoriteOrder.value.mapNotNull { byId[it] }
        if (currentFavChannels.size <= 1) return
        val sorted = when (mode) {
            "AZ" -> currentFavChannels.sortedBy { it.name.lowercase(Locale.getDefault()) }
            "NUMBER" -> currentFavChannels.sortedBy { it.number }
            "CATEGORY" -> currentFavChannels.sortedWith(compareBy<TvChannel> { it.category }.thenBy { it.number })
            else -> currentFavChannels
        }.map { it.id }
        _favoriteOrder.value = sorted
        repository.saveFavoriteOrderList(sorted)
        val modeLabel = when (mode) {
            "AZ" -> "İsme Göre (A-Z)"
            "NUMBER" -> "Kanal Numarasına Göre"
            else -> "Kategoriye Göre"
        }
        _statusMessage.value = "⭐ Favoriler $modeLabel otomatik sıralandı."
    }

    fun cycleSleepTimer() {
        val options = listOf(null, 30, 60, 90, 120)
        val currentIdx = options.indexOf(_sleepTimerMinutes.value)
        val next = options[(currentIdx + 1) % options.size]
        _sleepTimerMinutes.value = next
        sleepTimerJob?.cancel()

        if (next != null) {
            _statusMessage.value = "Uyku zamanlayıcısı: $next dk."
            sleepTimerJob = viewModelScope.launch {
                var remaining = next
                while (remaining > 0) {
                    delay(60_000L)
                    remaining -= 1
                    _sleepTimerMinutes.value = remaining
                }
                _sleepTimerMinutes.value = null
                _activePlayingChannel.value = null
                _pipChannel.value = null
            }
        } else {
            _statusMessage.value = "Uyku zamanlayıcısı kapatıldı."
        }
    }

    fun addRemoteM3uPlaylist(name: String, m3uUrl: String, epgUrl: String) {
        if (m3uUrl.isBlank()) return
        val cleanName = name.ifBlank { "Özel M3U Paketi (${_playlists.value.size + 1})" }
        val newSource = M3uPlaylistSource(
            name = cleanName.trim(),
            locationOrUrl = m3uUrl.trim(),
            epgXmlUrl = epgUrl.trim(),
            isLocalFile = false
        )
        val updated = _playlists.value + newSource
        _playlists.value = updated
        repository.savePlaylists(updated)
        reloadAllChannelsAndEpg()
    }

    fun addXtreamCodesAccount(name: String, serverUrl: String, username: String, password: String) {
        if (serverUrl.isBlank() || username.isBlank()) return
        val (m3uUrl, epgUrl) = M3uAndEpgParser.buildXtreamUrls(serverUrl, username, password)
        val displayName = name.ifBlank { "Xtream: $username" }
        addRemoteM3uPlaylist(displayName, m3uUrl, epgUrl)
    }

    fun importLocalM3uUri(uri: Uri) {
        viewModelScope.launch {
            _isLoadingM3u.value = true
            val imported = repository.importM3uFromUri(uri, "")
            if (imported != null) {
                val updated = _playlists.value + imported
                _playlists.value = updated
                repository.savePlaylists(updated)
                _statusMessage.value = "'${imported.name}' Özel M3U Kanalları menüsüne eklendi!"
                reloadAllChannelsAndEpg()
            } else {
                _isLoadingM3u.value = false
                _statusMessage.value = "Seçilen dosya geçerli bir M3U/M3U8 değil."
            }
        }
    }

    fun scanAndImportDownloadFolderM3u() {
        viewModelScope.launch {
            val foundFiles = repository.scanDownloadFolderForM3uFiles()
            if (foundFiles.isEmpty()) {
                _statusMessage.value = "Download klasöründe .m3u/.m3u8 bulunamadı."
                return@launch
            }
            val existingPaths = _playlists.value.map { it.locationOrUrl }.toSet()
            val newSources = foundFiles.filterNot { existingPaths.contains(it.absolutePath) }.map { file ->
                M3uPlaylistSource(
                    name = "Download: ${file.name}",
                    locationOrUrl = file.absolutePath,
                    isLocalFile = true
                )
            }
            if (newSources.isEmpty()) {
                _statusMessage.value = "Download klasöründeki M3U dosyaları zaten ekli."
            } else {
                val updated = _playlists.value + newSources
                _playlists.value = updated
                repository.savePlaylists(updated)
                _statusMessage.value = "${newSources.size} M3U dosyası eklendi!"
                reloadAllChannelsAndEpg()
            }
        }
    }

    fun removePlaylist(playlistId: String) {
        val updated = _playlists.value.filterNot { it.id == playlistId }
        _playlists.value = updated
        repository.savePlaylists(updated)
        reloadAllChannelsAndEpg()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun reloadAllChannelsAndEpg() {
        viewModelScope.launch(Dispatchers.Default) {
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val baseHourStart = cal.timeInMillis
            val currentHourOfDay = cal.get(Calendar.HOUR_OF_DAY)
            val oneHour = 60 * 60 * 1000L
            val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            val preformattedWindows = (-1..3).map { offset ->
                val s = baseHourStart + (offset * oneHour)
                val e = s + oneHour
                "${timeFmt.format(Date(s))} - ${timeFmt.format(Date(e))}"
            }

            val currentBuiltInBase = repository.getBuiltInChannels()
            val builtInList = ArrayList<TvChannel>(currentBuiltInBase.size)
            val combinedEpg = HashMap<String, List<EpgProgram>>(currentBuiltInBase.size + 64)

            for (ch in currentBuiltInBase) {
                val sched = repository.generateScheduleForChannelFast(ch, baseHourStart, currentHourOfDay, preformattedWindows)
                combinedEpg[ch.epgChannelId] = sched
                val cur = sched[1]
                val nxt = sched[2]
                builtInList.add(
                    ch.copy(
                        currentProgramTitle = cur.title,
                        currentProgramTime = cur.timeWindowText,
                        nextProgramTitle = nxt.title,
                        currentProgress = cur.progressFraction(now),
                        isCustomM3u = false
                    )
                )
            }

            _builtInChannels.value = builtInList
            _epgMap.value = combinedEpg

            // Arka planda https://aciptv.acvid.site/m3ulist/testchannel.m3u içindeki Türk kanallarını
            // çekip doğrudan Ana Kanal Listesine (Canlı TV) ve yedek akışlara birleştir
            launch(Dispatchers.IO) {
                val remoteTrM3u = repository.fetchAndCacheAciptvTurkishM3u()
                if (remoteTrM3u.isNotBlank()) {
                    val mergedBase = repository.mergeTurkishM3uIntoBuiltIn(repository.getStaticBuiltInChannels(), remoteTrM3u)
                    val refreshedBuiltIn = ArrayList<TvChannel>(mergedBase.size)
                    for (ch in mergedBase) {
                        val sched = repository.generateScheduleForChannelFast(ch, baseHourStart, currentHourOfDay, preformattedWindows)
                        combinedEpg[ch.epgChannelId] = sched
                        val cur = sched[1]
                        val nxt = sched[2]
                        refreshedBuiltIn.add(
                            ch.copy(
                                currentProgramTitle = cur.title,
                                currentProgramTime = cur.timeWindowText,
                                nextProgramTitle = nxt.title,
                                currentProgress = cur.progressFraction(now),
                                isCustomM3u = false
                            )
                        )
                    }
                    _builtInChannels.value = refreshedBuiltIn
                    _epgMap.value = combinedEpg.toMap()
                }
            }

            if (_playlists.value.isNotEmpty()) {
                _isLoadingM3u.value = true
                val customList = mutableListOf<TvChannel>()
                var nextCustomNumber = 1
                for (source in _playlists.value) {
                    try {
                        val rawM3u = if (source.isLocalFile) {
                            val f = File(source.locationOrUrl)
                            if (f.exists() && f.length() < 12_000_000L) {
                                withContext(Dispatchers.IO) { f.readText() }
                            } else ""
                        } else {
                            M3uAndEpgParser.downloadText(source.locationOrUrl)
                        }
                        if (rawM3u.isNotBlank()) {
                            val parsed = M3uAndEpgParser.parseM3uContent(rawM3u, source.name, nextCustomNumber, true)
                            for (ch in parsed) {
                                val sched = repository.generateScheduleForChannelFast(ch, baseHourStart, currentHourOfDay, preformattedWindows)
                                combinedEpg[ch.epgChannelId] = sched
                                val cur = sched[1]
                                customList.add(
                                    ch.copy(
                                        currentProgramTitle = cur.title,
                                        currentProgramTime = cur.timeWindowText,
                                        currentProgress = cur.progressFraction(now),
                                        isCustomM3u = true
                                    )
                                )
                            }
                            nextCustomNumber += parsed.size
                        }
                    } catch (_: Exception) {}
                }
                _customM3uChannels.value = customList
                _epgMap.value = combinedEpg.toMap()
                _isLoadingM3u.value = false
            }
        }
    }
}

class TvViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TvViewModel(TvRepository(context.applicationContext)) as T
    }
}

class MainActivity : ComponentActivity() {
    private var activeViewModel: TvViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            RedoXTvAppTheme {
                val context = LocalContext.current
                val viewModel: TvViewModel = viewModel(factory = TvViewModelFactory(context))
                SideEffect { activeViewModel = viewModel }
                RedoXTvRootScreen(
                    viewModel = viewModel,
                    onEnterSystemPip = { enterTvSystemPip() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && !isInPictureInPictureMode) {
            activeViewModel?.onAppBackgrounded()
        }
    }

    private fun enterTvSystemPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (_: Exception) {}
        }
    }
}

@Composable
fun RedoXTvAppTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = TvThemeColors.BullRedBright,
        onPrimary = Color.White,
        secondary = TvThemeColors.AccentCyan,
        background = TvThemeColors.BackgroundDeep,
        surface = TvThemeColors.SurfaceCard,
        onSurface = TvThemeColors.TextPrimary
    )
    MaterialTheme(
        colorScheme = darkColorScheme,
        content = content
    )
}

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
                        text = "Chromecast 4K • RedoX TV v1.5.2 Release",
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
    Card(
        onClick = onClick,
        scale = CardDefaults.scale(focusedScale = 1.05f),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.5.dp, Color.White),
                shape = RoundedCornerShape(18.dp)
            ),
            border = Border(
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(18.dp)
            )
        ),
        colors = CardDefaults.colors(
            containerColor = if (isPrimary) TvThemeColors.BullRedDark.copy(alpha = 0.50f) else TvThemeColors.SurfaceCard,
            focusedContainerColor = accentColor.copy(alpha = 0.30f)
        ),
        modifier = modifier
            .width(172.dp)
            .height(126.dp)
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

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importLocalM3uUri(uri)
        }
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
                                    try {
                                        filePickerLauncher.launch(arrayOf("*/*"))
                                    } catch (_: Exception) {
                                        viewModel.scanAndImportDownloadFolderM3u()
                                    }
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
        mutableStateOf(filtered.firstOrNull())
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
                items(categories, key = { "cat_$it" }) { category ->
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
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusable()
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
                columns = GridCells.Adaptive(minSize = 190.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(filtered, key = { it.id }) { channel ->
                    val isBroken = brokenIds.contains(channel.id) || channel.isBroken
                    TvChannelGridCard(
                        channel = channel,
                        isFavorite = favoriteIds.contains(channel.id),
                        isLocked = lockedIds.contains(channel.id),
                        isBroken = isBroken,
                        isHealthy = healthyIds.contains(channel.id),
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

@Composable
fun TvChannelGridCard(
    channel: TvChannel,
    isFavorite: Boolean,
    isLocked: Boolean = false,
    isBroken: Boolean = false,
    isHealthy: Boolean = false,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val catColor = if (isBroken) TvThemeColors.BrokenRed else TvThemeColors.categoryColor(channel.category)
    var cardFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        scale = CardDefaults.scale(focusedScale = 1.03f),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(
                    2.dp,
                    when {
                        isBroken -> TvThemeColors.BrokenRed
                        isFavorite -> TvThemeColors.FavoriteGold
                        else -> TvThemeColors.BullRedBright
                    }
                ),
                shape = RoundedCornerShape(14.dp)
            )
        ),
        colors = CardDefaults.colors(
            containerColor = TvThemeColors.SurfaceCard,
            focusedContainerColor = TvThemeColors.SurfaceElevated
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(118.dp)
            .onFocusChanged {
                cardFocused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .tvLongPressFavoriteModifier(
                itemKey = channel.id,
                onShortClick = onClick,
                onLongPress = onLongClick
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
                            cardFocused -> if (isFavorite) "⭐ Favoride • Uzun Bas: Çıkar" else "OK Basılı Tut: ⭐ Favoriye Ekle"
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
            items(results, key = { "search_${it.id}" }) { ch ->
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

/**
 * Gerçek ISO/IEC 18004 Version 2-L (25x25) Reed-Solomon QR Kod Üretici.
 * Telefon kamerası tutulduğunda doğrudan http://<TV-IP>:8585 adresini okur!
 */
object IsoQrCodeV2Generator {
    fun encodeUrlTo25x25Matrix(url: String): Array<IntArray> {
        val size = 25
        val matrix = Array(size) { IntArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        fun placeFinder(r0: Int, c0: Int) {
            for (r in 0..6) {
                for (c in 0..6) {
                    val isDark = r == 0 || r == 6 || c == 0 || c == 6 || (r in 2..4 && c in 2..4)
                    matrix[r0 + r][c0 + c] = if (isDark) 1 else 0
                    reserved[r0 + r][c0 + c] = true
                }
            }
            for (r in -1..7) {
                for (c in -1..7) {
                    val rr = r0 + r
                    val cc = c0 + c
                    if (rr in 0 until size && cc in 0 until size && !reserved[rr][cc]) {
                        matrix[rr][cc] = 0
                        reserved[rr][cc] = true
                    }
                }
            }
        }

        placeFinder(0, 0)
        placeFinder(0, 18)
        placeFinder(18, 0)

        // Alignment pattern at (16..20, 16..20)
        for (r in 0..4) {
            for (c in 0..4) {
                val isDark = r == 0 || r == 4 || c == 0 || c == 4 || (r == 2 && c == 2)
                matrix[16 + r][16 + c] = if (isDark) 1 else 0
                reserved[16 + r][16 + c] = true
            }
        }

        // Timing patterns (row 6 and col 6)
        for (i in 8..16) {
            val bit = if (i % 2 == 0) 1 else 0
            if (!reserved[6][i]) {
                matrix[6][i] = bit
                reserved[6][i] = true
            }
            if (!reserved[i][6]) {
                matrix[i][6] = bit
                reserved[i][6] = true
            }
        }

        // Format info reservation & Dark module
        for (i in 0..8) {
            if (i != 6) {
                reserved[8][i] = true
                reserved[i][8] = true
            }
        }
        for (i in 0..7) {
            reserved[8][17 + i] = true
            reserved[17 + i][8] = true
        }
        matrix[17][8] = 1

        // Encode payload bytes (up to 32 bytes for Version 2-L)
        val cleanUrl = url.ifBlank { "http://192.168.1.1:8585" }
        val rawBytes = cleanUrl.toByteArray(Charsets.UTF_8).take(32).map { it.toInt() and 0xFF }
        val bits = ArrayList<Int>(360)
        fun pushBits(value: Int, count: Int) {
            for (i in count - 1 downTo 0) {
                bits.add((value shr i) and 1)
            }
        }

        pushBits(0b0100, 4)
        pushBits(rawBytes.size, 8)
        for (b in rawBytes) pushBits(b, 8)
        val term = minOf(4, 272 - bits.size)
        pushBits(0, term)
        while (bits.size % 8 != 0) bits.add(0)
        val padBytes = intArrayOf(0xEC, 0x11)
        var padIdx = 0
        while (bits.size < 272) {
            pushBits(padBytes[padIdx % 2], 8)
            padIdx++
        }

        val dataCw = IntArray(34)
        for (i in 0 until 34) {
            var v = 0
            for (b in 0 until 8) {
                v = (v shl 1) or bits[i * 8 + b]
            }
            dataCw[i] = v
        }

        // Galois Field GF(256) with 0x11D
        val exp = IntArray(512)
        val log = IntArray(256)
        var x = 1
        for (i in 0 until 255) {
            exp[i] = x
            log[x] = i
            x = x shl 1
            if ((x and 0x100) != 0) x = x xor 0x11D
        }
        for (i in 255 until 512) exp[i] = exp[i - 255]

        var gen = intArrayOf(1)
        for (i in 0 until 10) {
            val nxt = IntArray(gen.size + 1)
            for (j in gen.indices) {
                nxt[j] = nxt[j] xor gen[j]
                if (gen[j] != 0) {
                    nxt[j + 1] = nxt[j + 1] xor exp[log[gen[j]] + i]
                }
            }
            gen = nxt
        }

        val ecc = IntArray(10)
        for (cw in dataCw) {
            val factor = cw xor ecc[0]
            for (k in 0 until 9) ecc[k] = ecc[k + 1]
            ecc[9] = 0
            if (factor != 0) {
                for (j in 0 until 10) {
                    ecc[j] = ecc[j] xor exp[log[gen[j + 1]] + log[factor]]
                }
            }
        }

        val fullBits = ArrayList<Int>(360)
        for (cw in dataCw) {
            for (i in 7 downTo 0) fullBits.add((cw shr i) and 1)
        }
        for (cw in ecc) {
            for (i in 7 downTo 0) fullBits.add((cw shr i) and 1)
        }
        repeat(7) { fullBits.add(0) }

        var bitIdx = 0
        var upward = true
        var c = size - 1
        while (c > 0) {
            if (c == 6) c -= 1
            val rowRange = if (upward) (size - 1 downTo 0) else (0 until size)
            for (r in rowRange) {
                for (dc in 0 downTo -1) {
                    val cc = c + dc
                    if (!reserved[r][cc]) {
                        var b = if (bitIdx < fullBits.size) fullBits[bitIdx++] else 0
                        if ((r + cc) % 2 == 0) b = b xor 1
                        matrix[r][cc] = b
                    }
                }
            }
            upward = !upward
            c -= 2
        }

        // Format info for Level L + Mask 0 -> 0x77C4
        val fmt = 0x77C4
        val tlRows = intArrayOf(8, 8, 8, 8, 8, 8, 8, 8, 7, 5, 4, 3, 2, 1, 0)
        val tlCols = intArrayOf(0, 1, 2, 3, 4, 5, 7, 8, 8, 8, 8, 8, 8, 8, 8)
        for (i in 0 until 15) {
            matrix[tlRows[i]][tlCols[i]] = (fmt shr (14 - i)) and 1
        }
        val trBlRows = intArrayOf(24, 23, 22, 21, 20, 19, 18, 8, 8, 8, 8, 8, 8, 8, 8)
        val trBlCols = intArrayOf(8, 8, 8, 8, 8, 8, 8, 17, 18, 19, 20, 21, 22, 23, 24)
        for (i in 0 until 15) {
            matrix[trBlRows[i]][trBlCols[i]] = (fmt shr (14 - i)) and 1
        }

        return matrix
    }
}

@Composable
fun QrMatrixVisualBox(seedText: String) {
    val qrMatrix = remember(seedText) {
        IsoQrCodeV2Generator.encodeUrlTo25x25Matrix(seedText)
    }
    Box(
        modifier = Modifier
            .size(104.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(6.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSize = 25
            val cellSize = size.width / gridSize
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    if (qrMatrix[r][c] == 1) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize * 1.02f, cellSize * 1.02f)
                        )
                    }
                }
            }
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
