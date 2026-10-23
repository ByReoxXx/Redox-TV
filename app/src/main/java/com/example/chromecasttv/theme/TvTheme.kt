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

