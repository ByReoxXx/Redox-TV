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
import android.util.Base64
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * GitHub Üzerinden AES-256 Şifreli Kanal Listesi (`channel/channels.enc`) Yöneticisi.
 *
 * - GitHub deposunda açık `.m3u` veya `.json` tutulmaz; yalnızca `AES-256-CBC` ile şifrelenmiş
 *   `channel/channels.enc` dosyası bulunur (`REDOX_ENC_V1:<base64(iv + ciphertext)>`).
 * - Uygulama açılışta `assets/channels.enc` (veya önbellekteki `github_channels_v17_cached.enc`)
 *   dosyasını bellekte çözer, arka planda ise GitHub'daki güncel `channel/channels.enc`
 *   dosyasını indirip çözer.
 * - Şifrelenen içerik ister `.m3u` (`#EXTM3U`) ister `.json` formatında olsun otomatik tanınır!
 */
object DynamicChannelsJsonService {
    const val DEFAULT_GITHUB_ENC_URL = "https://raw.githubusercontent.com/ByReoxXx/Redox-TV/main/channel/channels.enc"
    const val FALLBACK_GITHUB_ENC_URL = "https://raw.githubusercontent.com/ByReoxXx/Redox-TV/master/channel/channels.enc"
    private const val CACHED_ENC_FILENAME = "github_channels_v17_cached.enc"
    private const val ASSET_ENC_FILENAME = "channels.enc"
    private const val DEFAULT_AES_SECRET = "RedoX_TV_2026_Secret_Key_4K"

    fun decryptPayloadIfNeeded(rawContent: String, secretKey: String = DEFAULT_AES_SECRET): String {
        val trimmed = rawContent.trim()
        if (!trimmed.startsWith("REDOX_ENC_V1:")) return trimmed
        return try {
            val b64Part = trimmed.removePrefix("REDOX_ENC_V1:").trim()
            val combined = Base64.decode(b64Part, Base64.DEFAULT)
            if (combined.size <= 16) return ""
            val keyBytes = MessageDigest.getInstance("SHA-256").digest(secretKey.toByteArray(Charsets.UTF_8))
            val ivSpec = IvParameterSpec(combined, 0, 16)
            val secretKeySpec = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec)
            val decryptedBytes = cipher.doFinal(combined, 16, combined.size - 16)
            String(decryptedBytes, Charsets.UTF_8).trim()
        } catch (_: Throwable) {
            ""
        }
    }

    fun parseDecryptedChannels(rawOrEncrypted: String): List<TvChannel> {
        val plain = decryptPayloadIfNeeded(rawOrEncrypted)
        if (plain.isEmpty()) return emptyList()
        val first = plain.firstOrNull()
        return if (first == '\u007B' || first == '[') {
            parseChannelsJson(plain)
        } else if (plain.contains("#EXTINF", ignoreCase = true)) {
            M3uAndEpgParser.parseM3uContent(plain, "RedoX Yerleşik Paket", 1, false)
        } else {
            emptyList()
        }
    }

    fun loadInstantChannels(context: Context): List<TvChannel> {
        val cachedFile = File(context.filesDir, CACHED_ENC_FILENAME)
        if (cachedFile.exists() && cachedFile.length() > 50L) {
            try {
                val parsed = parseDecryptedChannels(cachedFile.readText())
                if (parsed.isNotEmpty()) return parsed
            } catch (_: Throwable) {}
        }
        return loadBundledAssetChannels(context)
    }

    fun loadBundledAssetChannels(context: Context): List<TvChannel> {
        return try {
            val raw = context.assets.open(ASSET_ENC_FILENAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
            parseDecryptedChannels(raw)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun loadCachedRepoM3u(context: Context): String = ""

    suspend fun fetchAndCacheRemoteRepoM3u(context: Context): String = ""

    suspend fun fetchAndCacheRemoteChannelsJson(context: Context, customUrl: String = ""): List<TvChannel>? = withContext(Dispatchers.IO) {
        val candidateUrls = buildList {
            if (customUrl.isNotBlank()) add(customUrl.trim())
            add(DEFAULT_GITHUB_ENC_URL)
            add(FALLBACK_GITHUB_ENC_URL)
        }.distinct()

        val cacheBuster = System.currentTimeMillis() / 30_000L
        for (baseUrl in candidateUrls) {
            try {
                val sep = if (baseUrl.contains("?")) "&" else "?"
                val fullUrl = "$baseUrl${sep}cb=$cacheBuster"
                val conn = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 7500
                    readTimeout = 10000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "RedoXTV/1.7.2 (Android TV; EncryptedChannelSync)")
                    setRequestProperty("Cache-Control", "no-cache")
                }
                if (conn.responseCode in 200..299) {
                    val rawPayload = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val parsed = parseDecryptedChannels(rawPayload)
                    if (parsed.isNotEmpty()) {
                        try {
                            File(context.filesDir, CACHED_ENC_FILENAME).writeText(rawPayload)
                        } catch (_: Throwable) {}
                        return@withContext parsed
                    }
                }
            } catch (_: Throwable) {}
        }
        null
    }

    private fun parseHexColorLong(raw: String?, fallback: Long): Long {
        if (raw.isNullOrBlank()) return fallback
        val clean = raw.trim().removePrefix("0x").removePrefix("0X").removePrefix("#")
        return clean.toLongOrNull(16) ?: fallback
    }

    fun parseChannelsJson(rawJson: String): List<TvChannel> {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) return emptyList()

        val firstChar = trimmed.firstOrNull()
        val array: JSONArray = if (firstChar == '\u007B') {
            val rootObj = JSONObject(trimmed)
            rootObj.optJSONArray("channels") ?: return emptyList()
        } else if (firstChar == '[') {
            JSONArray(trimmed)
        } else {
            return emptyList()
        }

        val result = ArrayList<TvChannel>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val name = obj.optString("name", "").trim()
            if (name.isEmpty()) continue

            val number = obj.optInt("number", i + 1)
            val id = obj.optString("id", "").trim().ifEmpty {
                "ch_json_${number}_${name.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "").take(12)}"
            }
            val logoText = obj.optString("logoText", "").ifEmpty {
                name.take(5).uppercase(Locale.getDefault())
            }
            val c1 = parseHexColorLong(obj.optString("c1", "0xFF1E3A8A"), 0xFF1E3A8AL)
            val c2 = parseHexColorLong(obj.optString("c2", "0xFF3B82F6"), 0xFF3B82F6L)
            val cat = obj.optString("cat", obj.optString("category", "Ulusal")).ifBlank { "Ulusal" }
            val badge = obj.optString("badge", obj.optString("qualityBadge", "FHD")).ifBlank { "FHD" }
            val logoUrl = obj.optString("logoUrl", obj.optString("logo", "")).trim()

            val sourcesList = ArrayList<StreamSource>()
            val sourcesArr = obj.optJSONArray("sources")
            if (sourcesArr != null) {
                for (sIdx in 0 until sourcesArr.length()) {
                    val sObj = sourcesArr.optJSONObject(sIdx) ?: continue
                    val sUrl = sObj.optString("url", "").trim()
                    if (sUrl.isEmpty()) continue
                    val sLabel = sObj.optString("label", "Akış ${sIdx + 1}").ifBlank { "Akış ${sIdx + 1}" }
                    val sTypeName = sObj.optString("type", "HLS").uppercase(Locale.US)
                    val sType = when (sTypeName) {
                        "DASH" -> StreamType.DASH
                        "PROGRESSIVE" -> StreamType.PROGRESSIVE
                        else -> StreamType.HLS
                    }
                    sourcesList.add(StreamSource(sLabel, sUrl, sType))
                }
            }
            if (sourcesList.isEmpty()) {
                val singleUrl = obj.optString("url", obj.optString("streamUrl", "")).trim()
                if (singleUrl.isNotEmpty()) {
                    sourcesList.add(StreamSource("Ana Yayın", singleUrl, StreamType.HLS))
                }
            }
            if (sourcesList.isEmpty()) continue

            result.add(
                TvChannel(
                    id = id,
                    number = number,
                    name = name,
                    shortLogoText = logoText,
                    brandPrimaryColor = c1,
                    brandSecondaryColor = c2,
                    category = cat,
                    logoUrl = logoUrl,
                    sources = sourcesList,
                    qualityBadge = badge,
                    isCustomM3u = false
                )
            )
        }
        return result
    }
}
