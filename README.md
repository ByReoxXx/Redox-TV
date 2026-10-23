# 🐂 RedoX TV — Android TV, Google TV, Tablet & Mobil Canlı TV / IPTV Oynatıcı

![Platform](https://img.shields.io/badge/Platform-Android%20TV%20%7C%20Google%20TV%20%7C%20Tablet%20%7C%20Mobil-E11D48?style=for-the-badge&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-TV%20%26%20Mobile-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Media3 ExoPlayer](https://img.shields.io/badge/Media3-ExoPlayer-10B981?style=for-the-badge)
![AES-256](https://img.shields.io/badge/Kanal%20Altyap%C4%B1s%C4%B1-AES--256%20%C5%9Eifreli-FACC15?style=for-the-badge)

**RedoX TV**, başta **Chromecast with Google TV (4K/HD)**, **Android TV Box**, **Akıllı TV'ler** olmak üzere **Android Tablet ve Telefonlarda** da kesintisiz çalışmak üzere geliştirilmiş, açık kaynaklı, modern ve yüksek performanslı bir Canlı TV & IPTV oynatıcısıdır.

Hem **TV kumandası (D-Pad)** hem de **dokunmatik ekran (Touch)** için özel olarak optimize edilmiş arayüzü, çoklu yayın kaynağı yedekleme (Auto-Failover) motoru, 4'lü çoklu ekran (Multi-View) desteği ve telefondan QR kod ile kablosuz liste yönetimi gibi gelişmiş özellikler sunar.

---

## 📸 Uygulama İçi Ekran Görüntüleri

<img width="1917" height="1078" alt="Ekran görüntüsü 2026-10-23 212649" src="https://github.com/user-attachments/assets/0e0b72d7-c446-4667-a536-b1f4c2291024" />
<img width="1917" height="1078" alt="Ekran görüntüsü 2026-10-23 212814" src="https://github.com/user-attachments/assets/ddbcfaeb-71d6-4b3c-bb5d-71ac132878ed" />
<img width="1917" height="1078" alt="Ekran görüntüsü 2026-10-23 213222" src="https://github.com/user-attachments/assets/7053e3f2-bd33-4d7c-b206-b3eb00c41c6e" />
<img width="1917" height="1078" alt="Ekran görüntüsü 2026-10-23 213152" src="https://github.com/user-attachments/assets/4a2eb33f-927c-414f-82ef-b23d09972631" />
<img width="1917" height="1078" alt="Ekran görüntüsü 2026-10-23 214506" src="https://github.com/user-attachments/assets/0c35f25f-a7b0-4937-b9fb-8c9434bc7bd1" />
<img width="1917" height="1078" alt="Ekran görüntüsü 2026-10-23 214355" src="https://github.com/user-attachments/assets/04bc7e5f-5463-4855-8dcd-b21fe36093c9" />


## ✨ Genel Özellikler

### 📺 1. Akıllı Oynatıcı ve Otomatik Yedek Akış Geçişi (Auto-Failover)
- **Çoklu Yayın Kaynağı (Multi-Source):** Tek bir kanal altında birden fazla alternatif yayın adresi (`Akış 1`, `Akış 2`, `Akış 3`...) barındırabilir.
- **Otomatik Yedek Geçişi:** Ana yayın açılmazsa veya yanıt vermezse oynatıcı kullanıcı müdahalesine gerek kalmadan saniyeler içinde bir sonraki alternatif kaynağa otomatik geçer.
- **Çalışan Kaynağı Hafızaya Alma:** Bir kanalda sorunsuz açılan alternatif kaynak otomatik olarak cihaz hafızasına kaydedilir; bir sonraki açılışta doğrudan o kaynaktan başlar.
- **Canlı Yayın Kontrolü (Timeshift):** Canlı yayını duraklatma, 15 saniye geri sarma ve tek tuşla canlı yayına dönme desteği.
- **Görüntü Oranı ve Ses/Altyazı Seçimi:** Kanal bazlı kaydedilebilen `16:9 Sığdır`, `Ekranı Doldur`, `Sinema Zoom` modları ile çoklu ses ve altyazı izi seçimi.

### 🎛️ 2. Evrensel Kumanda (Chromecast 4K D-Pad) ve Dokunmatik Uyumluluğu
- **Chromecast 4K Kumanda Optimizasyonu:** Üzerinde `CH+/CH-` veya `0-9` rakam tuşları bulunmayan minimalist kumandalar için tüm fonksiyonlar 4 yön tuşu (`▲/▼/◄/►`), `OK` ve `Geri` tuşlarıyla eksiksiz yönetilir.
- **Ekran Üstü Sanal Numaratör (`0-9`):** Fiziksel rakam tuşu olmayan kumandalarda hem oynatıcı içinden hem de arama ekranından sanal tuş takımıyla kanal numarası tuşlayarak doğrudan kanala gitme imkanı.
- **Tam Dokunmatik (Tablet & Telefon) Desteği:** Tüm kanal kartları, kategori çipleri, yan menüler ve oynatıcı OSD ekranı tek dokunuş ve uzun basma hareketleriyle tablet ve telefonlarda doğal bir mobil uygulama gibi çalışır.

### 🔲 3. 4'lü Çoklu Ekran (2x2 Multi-View) ve Resim İçinde Resim (PiP)
- **2x2 ve 2'li Yan Yana Çoklu Ekran:** Aynı anda 4 farklı canlı kanalı tek ekranda izleme imkanı.
- **Otomatik Ses Odaklama:** Kumanda ile hangi ekranın üzerine gelirseniz (veya dokunursanız) o ekranın sesi otomatik açılır, diğerleri sessize alınır.
- **Hücre İçi Kontrol Çubuğu:** Çoklu ekranda herhangi bir hücrenin üzerinde `OK` tuşuna basıldığında o hücreye özel `Tam Ekran`, `Kanal+`, `Kanal-` ve `Yedek Akış` menüsü açılır.
- **Mini PiP Penceresi:** Kanal izlerken yayını sağ alt köşeye küçültüp kanal listesinde veya ayarlarda gezinmeye devam edebilme.

### 📱 4. QR Kod İle Telefondan Kablosuz M3U / Xtream / Yedek Gönderimi
- **Yerleşik Wi-Fi Web Sunucusu (`:8585`):** TV ekranında uzun URL veya şifre yazma zahmetini ortadan kaldırır.
- **ISO/IEC 18004 Reed-Solomon QR Üretici:** Ekrandaki QR kodu telefon kamerasıyla okuttuğunuzda açılan mobil web arayüzünden:
  - Telefonunuzdaki `.m3u` / `.m3u8` dosyasını veya URL'sini,
  - **Xtream Codes** (`Sunucu URL`, `Kullanıcı Adı`, `Şifre`) hesabınızı,
  - Veya daha önce aldığınız uygulama yedeğini (`redox_tv_backup.txt`) tek tıkla TV'ye gönderebilirsiniz.

### 📂 5. Dahili TV Dosya Gezgini ve Özel M3U Yönetimi
- **Uygulama İçi Dosya Seçici:** Android TV / Google TV cihazlarında sistem dosya yöneticisi bulunmasa bile RedoX TV kendi dahili dosya gezginiyle `Download`, `Ana Dizin (/sdcard)` ve takılı **USB Bellekleri** tarayarak `.m3u` / `.m3u8` dosyalarını doğrudan yükler.
- **Akıllı M3U Birleştirici:** Eklediğiniz özel M3U listelerinde aynı isimli kanalların alternatif yayınlarını tek kanal altında birleştirir ve otomatik kategorize eder.

### ⭐ 6. Gelişmiş Favori Sıralama, Ebeveyn Kilidi ve Kanal Sağlığı Tarayıcısı
- **Tut & Taşı Favori Sıralama:** Favoriler sekmesinde herhangi bir kanalın üzerindeyken `► (Sağ)` tuşuna basarak kanalı yakalayıp `▲/▼` ile istediğiniz sıraya taşıyabilir veya `A-Z`, `Kanal No` ve `Kategori` bazlı otomatik sıralayabilirsiniz.
- **PIN Korumalı Ebeveyn Kilidi & Kanal Gizleme:** İstediğiniz kanalları 4 haneli PIN koduyla kilitleyebilir veya listeden gizleyip dilediğiniz zaman geri yükleyebilirsiniz.
- **Paralel Kanal Sağlık Testi:** Tek tuşla tüm kanalların yayın kaynaklarını paralel olarak test edip yalnızca aktif çalışan kanalları filtreleyebilirsiniz.

---

## 🎮 Kumanda ve Dokunmatik Kontrol Haritası

| Ekran | Tuş / Hareket | İşlev |
| :--- | :--- | :--- |
| **Kanal Listesi & Izgara** | `OK` / Tek Dokunuş | Seçili kanalı tam ekran açar |
| **Kanal Listesi & Izgara** | `OK Basılı Tut` / Uzun Dokunuş | Kanalı açmadan **⭐ Favorilere Ekler / Çıkarır** |
| **Kanal Listesi & Izgara** | `Geri` (Oynatıcıdan Çıkış) | Sol menüye kaçmadan doğrudan son izlenen kanal kartına odaklanır |
| **Tam Ekran Oynatıcı** | `▲ Yukarı` / `▼ Aşağı` | Sonraki / Önceki kanala geçer (450ms zap korumalı) |
| **Tam Ekran Oynatıcı** | `◄ Sol Yön Tuşu` | Sol Hızlı Kanal Çekmecesini açar ve aktif kanala odaklanır |
| **Tam Ekran Oynatıcı** | `► Sağ Yön Tuşu` | Bir sonraki alternatif yayın kaynağına (`Akış 1 -> 2 -> 3`) geçer |
| **Tam Ekran Oynatıcı** | `OK` / Ekrana Dokunuş | Alt bilgi ve kontrol çubuğunu (OSD) açar / gizler |
| **Tam Ekran Oynatıcı** | `0` Tuşu veya OSD `🔢 Kanal No` | Önceki izlenen kanala döner (Recall) / Sanal Numaratörü açar |
| **Favoriler Ekranı** | `► Sağ` -> `▲/▼` -> `OK` | Kanalı tutar, yukarı/aşağı taşır ve yeni sırasına bırakır |
| **4'lü Çoklu Ekran (2x2)** | `◄ / ► / ▲ / ▼` ve `OK` | 4 ekran arasında gezinir; `OK` ile seçili ekranın kontrol menüsünü açar |

---

## 🛠️ Teknik Detaylar ve Mimari

### 1. Teknoloji Yığını (Tech Stack)
- **Dil & Derleyici:** Kotlin 2.0 (JVM Target 17)
- **Arayüz (UI):** Jetpack Compose (`androidx.tv:tv-material`, `androidx.compose.foundation`, Donanım Hızlandırmalı `graphicsLayer` animasyonları)
- **Medya Motoru:** AndroidX Media3 ExoPlayer (`media3-exoplayer`, `media3-exoplayer-hls`, `media3-exoplayer-dash`, `media3-datasource`)
- **Durum Yönetimi:** MVVM + Kotlin Coroutines + `StateFlow` (`SharingStarted.Eagerly` ile `0 ms` anında açılış)
- **Görsel Yükleme:** Coil Compose (`AsyncImage`) + 26 katmanlı vektörel Cubic Bézier `Canvas` çizimleri

### 2. Katmanlı Modüler Proje Yapısı (`app/src/main/java/com/example/chromecasttv/`)
Proje; okunabilirlik, kolay hata ayıklama ve yeni özellik geliştirme için 13 bağımsız modüle ayrılmıştır:

```text
app/src/main/java/com/example/chromecasttv/
├── MainActivity.kt                         # Activity yaşam döngüsü, WAKE_LOCK, PiP ve tam ekran yönetimi
├── model/
│   └── TvModels.kt                         # TvChannel, StreamSource, EpgProgram, M3uPlaylistSource, TvUiState
├── theme/
│   └── TvTheme.kt                          # TvThemeColors, 3D Kırmızı Boğa (RedBullEmblemBox) ve tema tanımları
├── player/
│   └── ExoPlayerFactory.kt                 # Media3 ExoPlayer kurulumu, DecoderFallback, CDN Header & SSL yönetimi
├── data/
│   ├── DynamicChannelsJsonService.kt       # AES-256 şifreli kanal listesi (channels.enc) çözücü ve senkronizasyon
│   ├── M3uAndEpgParser.kt                  # M3U/M3U8 ayrıştırıcı, mükerrer kanal birleştirici ve Xtream URL üretici
│   └── TvRepository.kt                     # SharedPreferences, yedekleme, paralel kanal sağlık tarayıcısı ve EPG
├── network/
│   └── LocalTransferWebServer.kt           # Yerel HTTP sunucusu (:8585) ve ISO Reed-Solomon QR kod matris üretici
├── viewmodel/
│   └── TvViewModel.kt                      # StateFlow yönetimi, 450ms zap debounce, uyku zamanlayıcısı ve senkronizasyon
└── ui/
    ├── components/
    │   └── TvCommonComponents.kt           # MultiViewPlayerCell, PiP, PIN Dialog, Dahili Dosya Seçici ve ortak butonlar
    ├── screens/
    │   ├── LandingAndChannelScreens.kt     # Karşılama ekranı, Canlı TV vitrini, kanal ızgarası ve sanal numaratör
    │   └── FavoritesEpgAndSettingsScreens.kt # Favori sıralama, 2x2 Multi-View, EPG rehberi ve M3U/Ayarlar ekranı
    └── player/
        └── RedoXPlayerScreen.kt            # Tam ekran OSD oynatıcı, otomatik yedek akış geçişi ve sol kanal çekmecesi
```

### 3. AES-256 Şifreli Dinamik Kanal Altyapısı (`channel/channels.enc`)
- Depoda açık `.m3u` veya `.json` yayın linkleri tutulmaz; tüm yerleşik kanal veritabanı `SHA-256` anahtar türetmeli **AES-256-CBC (`REDOX_ENC_V1:...`)** formatında şifrelenerek `channel/channels.enc` içinde saklanır.
- Uygulama açılışta ilk milisaniyede cihaz önbelleğindeki veya `assets/channels.enc` içindeki paketi bellekte çözer; arka planda ise GitHub üzerindeki güncel `channel/channels.enc` dosyasını kontrol ederek APK güncellemeye gerek kalmadan kanal listesini canlı olarak yeniler.

### 4. Donanım ve Oynatıcı Optimizasyonları
- **Çoklu Ekran Kodek Koruması (`DecoderFallback` & `TextureView`):** Android TV Box'larda aynı anda 4 yüksek çözünürlüklü `SurfaceView` açıldığında oluşan donanım dekoder sınırı ve siyah ekran sorununu önlemek için çoklu ekranda `TextureView` tabanlı `PlayerView`, `DefaultRenderersFactory.setEnableDecoderFallback(true)` ve akıllı çözünürlük/bitrate dengelemesi kullanılır.
- **Kesin Zaman Aşımlı Sağlık Taraması:** SHOUTcast/Icecast radyo akışları veya yanıt vermeyen sunucularda taramanın takılmasını önlemek için her kanal `CompletableDeferred` + bağımsız daemon iş parçacığı ve `1.4 sn` kesin zaman aşımıyla (`Range: bytes=0-128`) denetlenir.
- **CDN & Bölgesel Yayın Uyumluluğu:** Ulusal yayıncı CDN'leri için otomatik `Referer`/`Origin` başlık eşleştirmesi ve yerel TV sunucularındaki özel SSL sertifikaları için esnek TLS el sıkışma desteği içerir.

---

## 🚀 Kurulum ve Derleme

### Hazır APK İndirme
Deponun **[Releases](https://github.com/ByReoxXx/Redox-TV/releases)** sayfasından en güncel imzalı `Release.apk` dosyasını indirip Android TV, Google TV (Chromecast 4K), TV Box, Tablet veya Telefonunuza doğrudan kurabilirsiniz.

### Kaynak Koddan Derleme
```bash
git clone https://github.com/ByReoxXx/Redox-TV.git
cd Redox-TV
gradle assembleRelease
```

---

## 📚 Detaylı Dokümantasyon (Wiki)
Kurulum adımları, kumanda kısayolları, M3U/Xtream/QR yükleme rehberi ve geliştirici notları için **[RedoX TV Wiki (Depo İçi Kılavuz)](./wiki/Home.md)** veya **[GitHub Wiki Sekmesi](https://github.com/ByReoxXx/Redox-TV/wiki)** sayfalarını ziyaret edebilirsiniz.

---

## 📄 Lisans
Bu proje açık kaynaklı olarak geliştirilmektedir. Öneri, hata bildirimi ve katkılarınız için **Issues** veya **Pull Requests** bölümlerini kullanabilirsiniz.
