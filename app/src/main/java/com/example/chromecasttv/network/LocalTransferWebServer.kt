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
