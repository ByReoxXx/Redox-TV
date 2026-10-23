# 🐂 RedoX TV — Android TV, Chromecast 4K, Tablet & Mobil Açık Kaynak IPTV Oynatıcı (v1.7.0)

RedoX TV, **Google TV / Chromecast 4K / Android TV Box / Android Tablet ve Telefon** cihazları için Jetpack Compose ve Media3 ExoPlayer ile geliştirilmiş, **13 dosyalı modüler mimariye**, **AES-256 Şifreli Kanal Listesi Altyapısına (`channel/channels.enc`)** ve **Chromecast 4K Kumandası (D-Pad) + Dokunmatik Desteğine** sahip bir canlı TV uygulamasıdır.

## 🔒 Şifreli Kanal Listesi (`channel/channels.enc`) Nasıl Güncellenir?
GitHub üzerinde açık `.m3u` veya `.json` yayın linklerinin görünmemesi için tüm kanal listesi **AES-256-CBC (`REDOX_ENC_V1:...`)** ile şifrelenmiş olarak **`channel/channels.enc`** dosyasında tutulur.

Kanal listesini güncellemek için **2 kolay yöntem** kullanabilirsiniz:
1. **Görsel Tarayıcı Aracı İle (`tools/Redox_Kanal_Sifreleyici.html`):** Bilgisayar veya telefonunuzda `Redox_Kanal_Sifreleyici.html` dosyasını açın -> `.m3u`, `.json` veya mevcut `channels.enc` dosyanızı düzenleyin -> **"🔒 Şifrele ve `channels.enc` İndir"** butonuna basıp inen dosyayı GitHub'da `channel/channels.enc` üzerine yükleyin.
2. **GitHub Üzerinden Otomatik Şifreleme:** Deponuzdaki `channel/` klasörüne geçici olarak `channels.m3u` veya `channels.json` yüklerseniz, GitHub Actions iş akışı bu dosyayı otomatik olarak `channel/channels.enc` içine şifreler ve açık `.m3u` / `.json` dosyasını depodan otomatik siler!
