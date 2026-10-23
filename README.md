# 🐂 RedoX TV — Android TV, Chromecast 4K, Tablet & Mobil Açık Kaynak IPTV Oynatıcı (v1.6.0)

RedoX TV, **Google TV / Chromecast 4K / Android TV Box / Android Tablet ve Telefon** cihazları için Jetpack Compose ve Media3 ExoPlayer ile geliştirilmiş, **13 dosyalı modüler mimariye**, **Hem Kumanda (D-Pad) Hem Dokunmatik (Touch) Desteğine** ve **GitHub üzerinden dinamik güncellenen `channel/channels.json` & `channel/channels.m3u`** kanal altyapısına sahip açık kaynaklı bir canlı TV uygulamasıdır.

## ⚡ v1.6.0 Yenilikleri ve Çözülen Sorunlar
- **Child Pencere (Dialog) D-Pad Kilidi:** `Dosyadan M3U Seç` ve `Ebeveyn Kilidi (PIN)` pencereleri gerçek Android `Dialog` alt penceresine alındı; kumanda odağı asla arka plana kaçmaz, her dosya/klasör satırı doğrudan OK veya dokunuşla seçilir.
- **Özel M3U Klasörü Çökme Sorunu Giderildi:** Aynı `tvg-id` veya aynı isimli kanallara sahip M3U dosyalarında oluşan `LazyVerticalGrid` duplicate-key çökmesi tamamen engellendi; aynı isimli kanallar otomatik olarak alternatif akış (`Akış 1, 2, 3`) şeklinde birleştirilir.
- **Kanaldan Çıkınca Son İzlenen Kanala Odaklanma:** Kanal izlerken Geri tuşuna basıldığında odak artık sol menüdeki *Canlı TV* sekmesine kaçmaz; doğrudan izlediğiniz kanalın listedeki kartına döner.
- **4'lü Çoklu Ekran (2x2 Multi-View) Alt Ekran Geçişi ve Siyah Ekran Çözümü:** Üstteki 2 ekrandan alttaki 2 ekrana D-Pad geçişi (`FocusRequester` 2x2 matrisi) açıldı; `TextureView` + `setEnableDecoderFallback(true)` + Multi-View ABR optimizasyonu ile Android TV Box'larda alttaki ekranların siyah kalması önlendi.
- **Android Tablet ve Telefon Dokunmatik Desteği:** Tüm kartlar (`combinedClickable`), menüler ve oynatıcı kontrolleri tek dokunuşla çalışacak şekilde evrensel hale getirildi.
- **KOZA TV Haber Kategorisine Taşındı & Yayın Kaynakları Optimize Edildi:** Tüm ulusal ve haber kanallarında adaptif ana yayınlar 1. sıraya alındı, ölü IP bağlantıları temizlendi.
