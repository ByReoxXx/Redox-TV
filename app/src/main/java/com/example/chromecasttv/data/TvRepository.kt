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


    @Volatile
    private var cachedStaticChannels: List<TvChannel>? = null

    fun loadCustomChannelsJsonUrl(): String =
        prefs.getString("custom_channels_json_url", "")?.trim().orEmpty()

    fun saveCustomChannelsJsonUrl(url: String) {
        prefs.edit().putString("custom_channels_json_url", url.trim()).apply()
    }

    fun getStaticBuiltInChannels(forceReload: Boolean = false): List<TvChannel> {
        val current = cachedStaticChannels
        if (!forceReload && !current.isNullOrEmpty()) return current
        val loaded = DynamicChannelsJsonService.loadInstantChannels(appContext)
        cachedStaticChannels = loaded
        return loaded
    }

    suspend fun syncRemoteGitHubChannelsJson(): List<TvChannel>? {
        val updated = DynamicChannelsJsonService.fetchAndCacheRemoteChannelsJson(
            context = appContext,
            customUrl = loadCustomChannelsJsonUrl()
        )
        if (!updated.isNullOrEmpty()) {
            cachedStaticChannels = updated
        }
        return updated
    }

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
