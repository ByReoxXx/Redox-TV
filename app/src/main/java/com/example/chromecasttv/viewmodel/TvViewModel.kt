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

            // [1] Arka planda GitHub üzerindeki dinamik `channels.json` dosyasını kontrol et ve güncelle
            // [2] Ardından ACIPTV Türk kanallarını ana listeye ve yedek akışlara birleştir
            launch(Dispatchers.IO) {
                val remoteJsonChannels = repository.syncRemoteGitHubChannelsJson()
                if (!remoteJsonChannels.isNullOrEmpty()) {
                    val jsonUpdatedList = ArrayList<TvChannel>(remoteJsonChannels.size)
                    for (ch in remoteJsonChannels) {
                        val sched = repository.generateScheduleForChannelFast(ch, baseHourStart, currentHourOfDay, preformattedWindows)
                        combinedEpg[ch.epgChannelId] = sched
                        val cur = sched[1]
                        val nxt = sched[2]
                        jsonUpdatedList.add(
                            ch.copy(
                                currentProgramTitle = cur.title,
                                currentProgramTime = cur.timeWindowText,
                                nextProgramTitle = nxt.title,
                                currentProgress = cur.progressFraction(now),
                                isCustomM3u = false
                            )
                        )
                    }
                    _builtInChannels.value = jsonUpdatedList
                    _epgMap.value = combinedEpg.toMap()
                }

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
