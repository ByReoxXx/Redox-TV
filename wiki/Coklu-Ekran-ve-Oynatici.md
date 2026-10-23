# 📺 Akıllı Oynatıcı, Otomatik Yedek Geçişi ve 2x2 Çoklu Ekran

---

## 1. Çoklu Yayın Kaynağı ve Otomatik Yedek Geçişi (Auto-Failover)

RedoX TV'de her kanal birden fazla alternatif yayın kaynağı (`StreamSource`) barındırabilir:
* **Hızlı Otomatik Geçiş:** Bir kanalı açtığınızda `Akış 1` hata verirse (403/404/Bağlantı reddi) anında (`0 ms`), 2.5 saniye içinde görüntü vermezse otomatik olarak `Akış 2`, `Akış 3` ve diğer alternatif kaynaklara geçilir.
* **Akıllı Kaynak Hafızası:** Hangi alternatif akış sorunsuz açıldıysa o kanal için hafızaya alınır; kanalı tekrar açtığınızda bekletmeden doğrudan çalışan kaynaktan başlar.
* **Manuel Kaynak Seçimi:** Tam ekran oynatıcıda `► Sağ` yön tuşuna basarak veya OSD barının sağ üstündeki `Akış 1`, `Akış 2`, `Akış 3` çiplerinden istediğinizi seçerek kaynaklar arasında geçiş yapabilirsiniz.

---

## 2. Favori Sıralama (Tut & Taşı)

Sol menüdeki **Favorilerim** sekmesinde favori kanallarınızın sırasını istediğiniz gibi değiştirebilirsiniz:
1. Sırasını değiştirmek istediğiniz kanalın üzerine gelip kumandadan **`► (Sağ Yön Tuşu)`** tuşuna basın.
2. Kanal **📌 TAŞINIYOR** moduna geçer (altın sarısı çerçeveyle vurgulanır).
3. **`▲ Yukarı`** ve **`▼ Aşağı`** yön tuşlarıyla kanalı istediğiniz sıraya taşıyın (`► Sağ` tuşu 5'er sıra yukarı taşır).
4. **`OK`** tuşuna basarak kanalı yeni sırasına sabitleyin.
5. Dilerseniz üstteki **`A-Z Sırala`**, **`Kanal No Sırala`** veya **`Kategori Sırala`** butonlarıyla tek tuşla otomatik sıralama da yapabilirsiniz.

---

## 3. Ebeveyn Kilidi (PIN) ve Kanal Gizleme

* **Kanal Kilitleme:** Vitrin barındaki **Kilitle** butonuna bastığınızda kanal 4 haneli PIN koduyla (varsayılan: `0000`) kilitlenir. PIN kodunu **M3U & Dosya Yükle** ekranından değiştirebilirsiniz.
* **Kanal Gizleme ve Geri Yükleme:** İstemediğiniz kanalları vitrin barından veya oynatıcı içinden **Gizle** butonuyla listeden kaldırabilir, dilediğiniz zaman **Geri Yükle** butonuyla tekrar görünür yapabilirsiniz.
