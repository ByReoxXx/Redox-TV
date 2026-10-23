# 🐂 RedoX TV — Android TV & Chromecast 4K Açık Kaynak IPTV Oynatıcı (v1.5.3)

RedoX TV, **Google TV / Chromecast 4K / Android TV** cihazları için Jetpack Compose for TV ve Media3 ExoPlayer ile geliştirilmiş, **13 dosyalı modüler mimariye** ve **GitHub üzerinden dinamik güncellenen `channels.json` / `channels.m3u`** kanal altyapısına sahip açık kaynaklı bir canlı TV uygulamasıdır.

## 📂 Modüler Proje Mimarisi (`app/src/main/java/com/example/chromecasttv/`)

| Klasör / Dosya | Açıklama |
| :--- | :--- |
| `MainActivity.kt` | Android TV Activity yaşam döngüsü, `WAKE_LOCK`, `FLAG_KEEP_SCREEN_ON` ve PiP yönetimi (~173 satır) |
| `model/TvModels.kt` | `TvChannel`, `StreamSource`, `EpgProgram`, `M3uPlaylistSource`, `TvUiState` veri modelleri |
| `theme/TvTheme.kt` | `TvThemeColors`, 26 katmanlı 3D Kırmızı Boğa (`RedBullEmblemBox`) ve `ChannelBrandLogoBox` |
| `player/ExoPlayerFactory.kt` | Media3 ExoPlayer, HLS/DASH/Progressive akış oluşturucu ve düşük gecikmeli tampon ayarları |
| `data/DynamicChannelsJsonService.kt` | Açılışta GitHub üzerindeki `channels.json` ve `channels.m3u` dosyalarını dinamik çeken servis |
| `data/M3uAndEpgParser.kt` | M3U/M3U8 ayrıştırıcı ve Xtream Codes bağlantı oluşturucu |
| `data/TvRepository.kt` | Kullanıcı tercihleri, yedekleme, kanal sağlığı taraması ve EPG zaman çizelgesi üretici |
| `network/LocalTransferWebServer.kt` | Telefondan QR kod ile kablosuz M3U/Xtream/Yedek gönderimi sağlayan yerleşik HTTP sunucusu (`:8585`) |
| `viewmodel/TvViewModel.kt` | Kanal geçiş (450ms zap debounce), favori sıralama, çoklu ekran (Multi-View) ve dinamik kanal senkronizasyonu |
| `ui/components/TvCommonComponents.kt` | Kumanda (D-Pad) odaklı ortak TV bileşenleri, PiP penceresi, Ebeveyn Kilidi (PIN) ve sanal klavye kutusu |
| `ui/screens/LandingAndChannelScreens.kt` | Karşılama ekranı (Landing Page), Canlı TV vitrini, kanal ızgarası ve numaratör/arama ekranları |
| `ui/screens/FavoritesEpgAndSettingsScreens.kt` | Favori sıralama, 4'lü Multi-View, EPG rehberi ve M3U/Xtream yönetim ekranları |
| `ui/player/RedoXPlayerScreen.kt` | Tam ekran OSD oynatıcı, 3.2 sn hızlı otomatik alternatif kaynak geçişi (Auto-Failover) ve ses/altyazı seçici |

## 🌐 Dinamik Kanal Listesi (`channels.m3u` & `channels.json`) Nasıl Güncellenir?

Tüm yerleşik kanallar (299 kanal ve 1.184 yayın akışı) deponun ana dizinindeki **[`channels.m3u`](./channels.m3u)** ve **[`channels.json`](./channels.json)** dosyalarında yer alır:

1. Çalışmayan bir kanalın linkini düzeltmek, listeden kaldırmak veya yeni bir kanal eklemek için doğrudan GitHub üzerinden **`channels.m3u`** veya **`channels.json`** dosyasını düzenleyip kaydedin (Commit).
2. Uygulama her açılışta `https://raw.githubusercontent.com/redox-baris/Redox-TV/main/channels.json` ve `https://raw.githubusercontent.com/redox-baris/Redox-TV/main/channels.m3u` adreslerini arka planda kontrol eder ve **APK'yı yeniden derlemeye gerek kalmadan** kanal listesini otomatik günceller.
