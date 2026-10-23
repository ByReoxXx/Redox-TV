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
