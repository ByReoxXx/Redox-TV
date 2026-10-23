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
