# 🎮 Kumanda (D-Pad) ve Dokunmatik Kontroller

RedoX TV, hem klasik TV kumandaları hem **Chromecast 4K kumandası** (üzerinde `CH+/CH-` veya `0-9` rakam tuşları bulunmayan kumandalar) hem de **dokunmatik ekranlı tablet ve telefonlar** için özel olarak tasarlanmıştır.

---

## 1. Tam Ekran Oynatıcı Kontrolleri

| Tuş / Dokunmatik Hareket | İşlev |
| :--- | :--- |
| **`▲ Yukarı Yön Tuşu`** (OSD Kapalıyken) | Sonraki kanala geçer (`+1` — 450ms hızlı geçiş korumalı) |
| **`▼ Aşağı Yön Tuşu`** (OSD Kapalıyken) | Önceki kanala geçer (`-1`) |
| **`◄ Sol Yön Tuşu`** (OSD Kapalıyken) | Sol taraftaki **Hızlı Kanal Listesi Çekmecesini** açar ve doğrudan izlenen kanala odaklanır |
| **`► Sağ Yön Tuşu`** (OSD Kapalıyken) | Kanalın bir sonraki **Alternatif Yayın Kaynağına (`Akış 1 -> 2 -> 3`)** geçer |
| **`OK Tuşu`** veya **Ekrana Dokunma** | Alt Bilgi ve Kontrol Barını (**OSD**) açar veya kapatır |
| **`🔢 Kanal No (0-9)` Butonu** (OSD İçinde) | Chromecast 4K kumandaları için **Ekran Üstü Sanal Numaratör** penceresini açar |
| **`0` Rakam Tuşu** (Fiziksel Kumandada) | Son izlenen önceki kanala (Recall) anında geri döner |
| **`Geri (Back)` Tuşu** | Çekmece veya OSD açıksa önce onları kapatır; kapalıysa oynatıcıdan çıkıp **doğrudan kanal ızgarasındaki son izlenen kanalın üzerine** döner |

---

## 2. Kanal Izgarası ve Vitrin Kontrolleri

* **Kısa `OK` veya Tek Dokunuş:** Kanalı tam ekran oynatıcıda açar.
* **Uzun `OK` (~450ms Basılı Tutma) veya Uzun Dokunuş:** Kanalı açmadan anında **⭐ Favorilere Ekler veya Favorilerden Çıkarır**.
* **Kategori Çipleri:** Üstteki `Tümü`, `Ulusal`, `Haber`, `Spor`, `Belgesel`, `Müzik`, `Çocuk`, `Dini`, `Kıbrıs`, `Yerel`, `Dünya` çiplerine tıklayarak kanalları anında filtreleyebilirsiniz.

---

## 3. 4'lü Çoklu Ekran (2x2 Multi-View) Kontrolleri

Chromecast 4K kumandasında `CH+/CH-` tuşu bulunmadığı için Çoklu Ekran iki aşamalı akıllı kontrol sistemine sahiptir:

1. **Ekranlar Arası Gezinme Modu:**
   - Kumandanın `◄ Sol`, `► Sağ`, `▲ Yukarı`, `▼ Aşağı` yön tuşlarıyla **1. Ekran (Üst Sol)**, **2. Ekran (Üst Sağ)**, **3. Ekran (Alt Sol)** ve **4. Ekran (Alt Sağ)** arasında serbestçe gezinirsiniz.
   - Üzerine geldiğiniz ekranın sesi otomatik olarak açılır (`🔊 SES AÇIK`), diğer 3 ekran sessize alınır.
2. **Hücre İçi Kontrol Menüsü (`OK` Tuşu):**
   - Herhangi bir ekranın üzerindeyken **`OK`** tuşuna bastığınızda o ekranın altında **Hücre İçi Kontrol Çubuğu** aktifleşir:
     - **`📺 Tam Ekran`**: Seçili yayını tam ekran açar (varsayılan odak buradadır; arka arkaya iki kez `OK` basınca doğrudan tam ekran olur).
     - **`⏭ Kanal+` / `⏮ Kanal-`**: Sadece o küçük ekranın kanalını değiştirir.
     - **`🔄 Akış Değiştir`**: O ekranın yedek yayın kaynağını değiştirir.
     - **`✕` veya `Geri Tuşu`**: Hücre menüsünü kapatıp tekrar 4 ekran arasında gezinme moduna döner.

---
⬅️ **[Wiki Ana Sayfasına Dön](https://github.com/ByReoxXx/Redox-TV/blob/main/wiki/Home.md)**
