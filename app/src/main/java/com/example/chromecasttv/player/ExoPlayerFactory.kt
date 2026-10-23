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

private var sslInstalled = false

fun installPermissiveSslForRegionalStreams() {
    if (sslInstalled) return
    try {
        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        })
        val sc = SSLContext.getInstance("TLS")
        sc.init(null, trustAll, SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
        HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        sslInstalled = true
    } catch (_: Exception) {}
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun createTvExoPlayer(
    context: Context,
    forceMaxBitrate: Boolean = false,
    isMultiView: Boolean = false
): ExoPlayer {
    installPermissiveSslForRegionalStreams()

    val loadControl = if (isMultiView) {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(4_000, 12_000, 600, 1_200)
            .setBackBuffer(5_000, false)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    } else {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(10_000, 28_000, 650, 1_200)
            .setBackBuffer(30_000, true)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    val httpFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(4_500)
        .setReadTimeoutMs(5_500)

    val mediaSourceFactory = DefaultMediaSourceFactory(context)
        .setDataSourceFactory(httpFactory)

    // setEnableDecoderFallback(true): 4'lü (2x2) Çoklu Ekranda 3. ve 4. ekranın donanım kodek
    // sınırına takılıp siyah ekranda kalmasını tamamen engeller!
    val renderersFactory = DefaultRenderersFactory(context)
        .setEnableDecoderFallback(true)

    return ExoPlayer.Builder(context, renderersFactory)
        .setLoadControl(loadControl)
        .setMediaSourceFactory(mediaSourceFactory)
        .build()
        .apply {
            setWakeMode(C.WAKE_MODE_NETWORK)
            trackSelectionParameters = if (isMultiView) {
                trackSelectionParameters
                    .buildUpon()
                    .setMaxVideoSize(854, 480)
                    .setMaxVideoBitrate(1_250_000)
                    .setForceHighestSupportedBitrate(false)
                    .build()
            } else {
                trackSelectionParameters
                    .buildUpon()
                    .setForceHighestSupportedBitrate(forceMaxBitrate)
                    .setMaxVideoBitrate(Int.MAX_VALUE)
                    .build()
            }
        }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun buildStreamMediaSource(context: Context, source: StreamSource): MediaSource {
    installPermissiveSslForRegionalStreams()
    val url = source.url.trim()
    val lower = url.lowercase(Locale.US)

    val headers = HashMap<String, String>()
    when {
        lower.contains("trkvz") || lower.contains("atv") || lower.contains("ahaber") || lower.contains("aspor") -> {
            headers["Referer"] = "https://www.atv.com.tr/"
            headers["Origin"] = "https://www.atv.com.tr"
        }
        lower.contains("kanald") || lower.contains("duhnet") || lower.contains("demiroren") -> {
            headers["Referer"] = "https://www.kanald.com.tr/"
            headers["Origin"] = "https://www.kanald.com.tr"
        }
        lower.contains("showtv") || lower.contains("ciner") || lower.contains("haberturk") -> {
            headers["Referer"] = "https://www.showtv.com.tr/"
            headers["Origin"] = "https://www.showtv.com.tr"
        }
        lower.contains("startv") || lower.contains("ntv") || lower.contains("dogus") -> {
            headers["Referer"] = "https://www.startv.com.tr/"
            headers["Origin"] = "https://www.startv.com.tr"
        }
        lower.contains("tv8") -> {
            headers["Referer"] = "https://www.tv8.com.tr/"
            headers["Origin"] = "https://www.tv8.com.tr"
        }
        lower.contains("nowtv") -> {
            headers["Referer"] = "https://www.nowtv.com.tr/"
            headers["Origin"] = "https://www.nowtv.com.tr"
        }
        lower.contains("halktv") -> {
            headers["Referer"] = "https://halktv.com.tr/"
            headers["Origin"] = "https://halktv.com.tr"
        }
        lower.contains("trt.com.tr") -> {
            headers["Referer"] = "https://www.tabii.com/"
            headers["Origin"] = "https://www.tabii.com"
        }
    }

    val httpFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
        .setDefaultRequestProperties(headers)
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(5_000)
        .setReadTimeoutMs(6_500)

    val mediaItemBuilder = MediaItem.Builder().setUri(Uri.parse(url))
    if (source.type == StreamType.HLS && !lower.endsWith(".mp4") && !lower.endsWith(".mkv") && !lower.endsWith(";")) {
        mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
    } else if (source.type == StreamType.DASH || lower.contains(".mpd")) {
        mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
    }

    return DefaultMediaSourceFactory(context)
        .setDataSourceFactory(httpFactory)
        .createMediaSource(mediaItemBuilder.build())
}
