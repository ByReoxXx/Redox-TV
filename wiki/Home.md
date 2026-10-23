# 🐂 RedoX TV Wiki — Resmi Kullanım ve Geliştirici Kılavuzu

**RedoX TV** resmi Wiki sayfasına hoş geldiniz! Bu dokümantasyon; **Google TV (Chromecast 4K/HD)**, **Android TV Box**, **Akıllı TV'ler**, **Android Tablet** ve **Telefonlarda** RedoX TV'yi en verimli şekilde kurmanız, kullanmanız ve geliştirmeniz için hazırlanmıştır.

---

## 📚 Wiki İçindekiler

1. **[🚀 Kurulum Rehberi](Kurulum-Rehberi)**
   - Android TV / Chromecast 4K / TV Box kurulumu (Downloader, USB ve ADB yöntemleri)
   - Android Tablet ve Telefon kurulumu
   - Depolama ve dosya erişim izinlerinin açılması
2. **[🎮 Kumanda (D-Pad) ve Dokunmatik Kontroller](Kumanda-ve-Dokunmatik-Kontroller)**
   - Chromecast 4K kumandası (rakamsız ve `CH+/CH-` tuşsuz kumandalar) için tam rehber
   - Ekran üstü sanal numaratör (`0-9`) kullanımı
   - Tablet ve telefon dokunmatik hareketleri
3. **[📱 M3U, Xtream Codes ve QR Web Yükleme Rehberi](M3U-Xtream-ve-QR-Yukleme)**
   - Telefondan QR kod ile kablosuz M3U/Xtream/Yedek gönderimi (`:8585`)
   - Dahili TV Dosya Gezgini ile `Download` ve USB bellekten `.m3u` seçimi
   - Yedek alma ve geri yükleme (`redox_tv_backup.txt`)
4. **[📺 Akıllı Oynatıcı, Otomatik Yedek Geçişi ve 2x2 Çoklu Ekran](Coklu-Ekran-ve-Oynatici)**
   - Çoklu yayın kaynağı (Multi-Source) ve Auto-Failover nasıl çalışır?
   - 4'lü Çoklu Ekran (2x2 Multi-View) ve PiP (Resim İçinde Resim) kullanımı
   - Favori sıralama (Tut & Taşı) ve PIN korumalı Ebeveyn Kilidi
5. **[🛠️ Teknik Mimari ve Geliştirici Rehberi](Teknik-Mimari-ve-Gelistirici-Rehberi)**
   - 13 dosyalı katmanlı Kotlin & Jetpack Compose mimarisi
   - AES-256-CBC şifreli kanal altyapısı (`channel/channels.enc`)
   - Media3 ExoPlayer, `TextureView` ve `DecoderFallback` optimizasyonları
6. **[❓ Sıkça Sorulan Sorular ve Sorun Giderme (SSS)](Sikca-Sorulan-Sorular-SSS)**
   - Yayın açılmama, siyah ekran, dosya seçici izni ve kumanda ipuçları

---

## 🌟 Öne Çıkan Özellikler Özeti

| Özellik | Açıklama |
| :--- | :--- |
| **Evrensel Platform Desteği** | Android TV, Google TV, TV Box, Android Tablet ve Telefonlarda tek APK ile çalışır |
| **Çoklu Kaynak & Auto-Failover** | Her kanal altında 3-6 yedek akış; ana yayın yanıt vermezse saniyeler içinde otomatik yedek yayına geçer |
| **Chromecast 4K Kumanda Uyumu** | `CH+/CH-` veya `0-9` tuşu olmayan kumandalar için sanal numaratör ve hücre içi kontrol menüleri |
| **QR Web Kumandası (`:8585`)** | Telefondan QR kod okutarak kablosuz M3U dosyası, URL, Xtream hesabı ve yedek gönderimi |
| **4'lü Çoklu Ekran (2x2)** | `TextureView` ve `DecoderFallback` destekli, aynı anda 4 canlı kanalı izleme modu |
| **AES-256 Şifreli Kanal Paketi** | GitHub üzerinden dinamik güncellenen `channel/channels.enc` şifreli kanal veritabanı |
