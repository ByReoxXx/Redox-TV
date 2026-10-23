# 🛠️ Teknik Mimari ve Geliştirici Rehberi

RedoX TV; **Kotlin 2.0**, **Jetpack Compose** ve **AndroidX Media3 ExoPlayer** kullanılarak 13 bağımsız modüler dosya halinde geliştirilmiştir.

---

## 1. Modüler Paket ve Dosya Yapısı

`app/src/main/java/com/example/chromecasttv/` dizini altındaki mimari katmanlar:

| Katman / Dosya | Sorumluluk Alanı |
| :--- | :--- |
| `MainActivity.kt` | Activity yaşam döngüsü, `WAKE_LOCK`, `FLAG_KEEP_SCREEN_ON`, PiP ve tam ekran pencere yönetimi |
| `model/TvModels.kt` | `@Immutable` veri modelleri (`TvChannel`, `StreamSource`, `EpgProgram`, `M3uPlaylistSource`, `TvUiState`) |
| `theme/TvTheme.kt` | Renk paleti (`TvThemeColors`), 26 katmanlı vektörel Kırmızı Boğa (`RedBullEmblemBox`) ve `ChannelBrandLogoBox` |
| `player/ExoPlayerFactory.kt` | Media3 ExoPlayer oluşturucu, `DefaultRenderersFactory.setEnableDecoderFallback(true)`, CDN `Referer`/`Origin` başlıkları ve esnek SSL |
| `data/DynamicChannelsJsonService.kt` | `channel/channels.enc` AES-256-CBC şifre çözücü, yerel önbellek ve GitHub dinamik senkronizasyon servisi |
| `data/M3uAndEpgParser.kt` | M3U/M3U8 ayrıştırıcı, mükerrer kanal birleştirici ve Xtream Codes bağlantı oluşturucu |
| `data/TvRepository.kt` | `SharedPreferences` yönetimi, yedekleme, `CompletableDeferred` tabanlı 1.4 sn zaman aşımlı kanal sağlık tarayıcısı |
| `network/LocalTransferWebServer.kt` | `:8585` yerel HTTP transfer sunucusu ve ISO/IEC 18004 Reed-Solomon QR kod üretici |
| `viewmodel/TvViewModel.kt` | Tekil `StateFlow<TvUiState>` yönetimi, 450ms zap debounce, uyku zamanlayıcısı ve arka plan senkronizasyonu |
| `ui/components/TvCommonComponents.kt` | `MultiViewPlayerCell`, `TvBuiltInM3uFilePickerDialog`, `ParentalPinDialog` ve GPU hızlandırmalı ortak butonlar |
| `ui/screens/LandingAndChannelScreens.kt` | Karşılama ekranı, Canlı TV vitrini, `combinedClickable` kanal ızgarası ve sanal numaratör |
| `ui/screens/FavoritesEpgAndSettingsScreens.kt` | Favori sıralama, 2x2 Multi-View yön matrisi, EPG zaman çizelgesi ve M3U/Ayarlar ekranı |
| `ui/player/RedoXPlayerScreen.kt` | Tam ekran OSD oynatıcı, otomatik yedek geçişi, sanal numaratör penceresi ve sol kanal çekmecesi |

---

## 2. AES-256-CBC Şifreli Kanal Veritabanı (`channel/channels.enc`)

Açık kaynak depoda yayın linklerinin düz metin olarak görünmemesi için kanal veritabanı `AES-256-CBC` (`PKCS5Padding`) ile şifrelenir:
* **Dinamik Senkronizasyon Adresi:** `https://raw.githubusercontent.com/ByReoxXx/Redox-TV/main/channel/channels.enc`
* **Anahtar Türetme:** `SHA-256(UTF8(passphrase))` -> 256-bit AES anahtarı.
* **Format:** `REDOX_ENC_V1:` + `Base64(16_Byte_Random_IV + AES_CBC_Ciphertext)`.
* **Çift Format Desteği:** Şifrelenen veri ister JSON (`{"channels": [...]}`) ister standart M3U (`#EXTM3U`) olsun `DynamicChannelsJsonService.parseDecryptedChannels()` tarafından otomatik tanınır ve çözümlenir.

---

## 3. Performans ve Donanım Optimizasyonları

1. **Anında Açılış (`0 ms` Cold Start):** `TvViewModel` başlangıçta `assets/channels.enc` veya yerel önbelleği senkronize okuyup `SharingStarted.Eagerly` ile ilk karede hazırlar; EPG saat pencereleri döngü dışında tek seferde hesaplanır.
2. **GPU Hızlandırmalı Hafif Animasyonlar:** Odaklanma efektlerinde yeniden yerleşim (relayout) tetiklemeyen `Modifier.graphicsLayer { scaleX; scaleY }` kullanılır; arka planda sürekli çalışan sonsuz animasyon döngüleri bulunmaz.
3. **2x2 Çoklu Ekran Donanım Kodek Koruması:** Android TV Box'larda aynı anda 4 adet 1080p `SurfaceView` açıldığında oluşan donanım dekoder sınırını aşmak için `multiview_player_view.xml` içinde `app:surface_type="texture_view"`, `DefaultRenderersFactory.setEnableDecoderFallback(true)` ve `854x480` / `1.25 Mbps` çoklu ekran akış sınırı uygulanır.

---
⬅️ **[Wiki Ana Sayfasına Dön](https://github.com/ByReoxXx/Redox-TV/blob/main/wiki/Home.md)**
