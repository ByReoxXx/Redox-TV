# 🚀 Kurulum Rehberi

RedoX TV; **Android 9.0 (API 28)** ve üzeri tüm Android TV, Google TV (Chromecast 4K/HD), TV Box, Android Tablet ve Telefonlarda çalışır.

---

## 1. Android TV / Chromecast 4K / TV Box Kurulumu

### Yöntem A: İndirilen APK'yı Telefondan TV'ye Göndererek Kurulum (En Kolay)
1. Telefonunuza ve TV'nize Google Play Store'dan **Send Files to TV (SFTTV)** veya **LocalSend** uygulamasını kurun.
2. GitHub **[Releases](https://github.com/redox-baris/Redox-TV/releases)** sayfasından en güncel `RedoX-TV-Release.apk` dosyasını telefonunuza indirin.
3. APK dosyasını TV'nize gönderip kurulumu tamamlayın.

### Yöntem B: TV Üzerinden Downloader Uygulaması İle Kurulum
1. Google TV / Android TV cihazınızda **Downloader by AFTVnews** uygulamasını açın.
2. GitHub Releases sayfasındaki en güncel `.apk` indirme bağlantısını girerek doğrudan TV'ye indirin ve kurun.
3. İlk kurulumda Android TV sizden *"Bilinmeyen kaynaklardan uygulama yükleme izni"* isterse izin verin.

---

## 2. Android Tablet ve Telefon Kurulumu
1. GitHub **[Releases](https://github.com/redox-baris/Redox-TV/releases)** sayfasından en güncel `Release.apk` dosyasını indirin.
2. İndirilen APK dosyasına dokunarak kurulumu tamamlayın.
3. Uygulama tablet ve telefonlarda otomatik olarak yatay (`sensorLandscape`) modda açılır ve tüm kartlar/butonlar tek dokunuşla çalışır.

---

## 3. Dosya Erişim İzni (`Download` Klasöründen M3U Okuma İçin)
Android 11, 12, 13 ve 14 (Chromecast 4K dahil) işletim sistemlerinde güvenlik kısıtlaması nedeniyle bir uygulamanın `Download` klasöründeki `.m3u` dosyalarını görebilmesi için **Tüm Dosyalara Erişim İzni** gerekir:
1. Uygulama içinde **M3U & Dosya Yükle** -> **Dosya Seç** -> **Dosyadan M3U Seç** butonuna basın.
2. Açılan dahili dosya seçicinin üst kısmındaki **"🔓 İzni Şimdi Aç"** butonuna tıklayın ve listeden **RedoX TV** için dosya erişim iznini aktif edin.
