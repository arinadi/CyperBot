# Usulan Fitur & Peningkatan untuk Zero-Sentinel

Dokumentasi ini merangkum fitur-fitur strategis yang disarankan untuk membedakan Zero-Sentinel dari aplikasi *parenting control* komersial biasa (seperti Google Family Link, Qustodio), dengan fokus utama pada **evasi deteksi (stealth)** dan **kemampuan intelijen mendalam (deep surveillance)**.

Tujuan utama: Mengelabui tinjauan manual/otomatis Google Play Store sambil mempertahankan kapabilitas monitoring tingkat lanjut.

## 1. Mode Penyamaran Dinamis (Dynamic Decoy Strategy)
Aplikasi berpura-pura menjadi aplikasi utilitas (Kalkulator, Notepad, Konverter Unit) yang berfungsi penuh. Fitur monitoring dinonaktifkan secara default (dormant state) hingga kondisi tertentu terpenuhi.

- **Mekanisme**:
  - Aplikasi dirilis sebagai "Simple Notes" atau "Unit Converter".
  - Kode berbahaya (monitoring) ada tetapi **mati total** saat instalasi awal.
  - Aktivasi melalui "Magic Trigger" (misal: ketik "1234=" di kalkulator atau buat catatan berjudul "SentinelConfig").
  - Atau aktivasi via *Remote Config* (server mengirim sinyal "ACTIVE" setelah aplikasi lolos review Play Store).

| Aspek | Detail |
| :--- | :--- |
| **Kelebihan** | - Kemungkinan lolos review Google Play sangat tinggi karena terlihat sebagai aplikasi bersih.<br>- Korban tidak curiga karena aplikasi benar-benar berfungsi sebagai utilitas. |
| **Kekurangan** | - Pengembangan lebih kompleks (harus membuat 2 UI: UI palsu dan UI admin).<br>- Menjelaskan *permissions* (izin) yang tidak relevan dengan "Notes App" bisa mencurigakan (contoh: Notes kok minta Notification Access?). |
| **Risiko** | - Jika reviewer menemukan trigger rahasia, akun developer bisa di-ban permanen.<br>- Perlu alasan kreatif untuk *permissions* (misal: "Notification Access" untuk fitur "Backup Notifikasi Catatan"). |

---

## 2. Eksfiltrasi File Pintar (Smart File Exfiltration)
Berbeda dengan aplikasi komersial yang biasanya hanya memblokir akses, fitur ini secara diam-diam menyalin file spesifik dari perangkat target ke server C2.

- **Mekanisme**:
  - Target file spesifik: Database WhatsApp (`msgstore.db`), foto di folder `DCIM/Camera` terbaru, dokumen PDF/DOCX di folder `Download`.
  - Dikirim via C2Worker saat WiFi terhubung untuk menghemat kuota data korban.

| Aspek | Detail |
| :--- | :--- |
| **Kelebihan** | - Mendapatkan data mentah (raw data) percakapan dan dokumen, bukan hanya metadata.<br>- Nilai intelijen sangat tinggi. |
| **Kekurangan** | - Penggunaan data internet besar.<br>- Butuh izin `MANAGE_EXTERNAL_STORAGE` (sangat sulit di Android 11+ tanpa alasan valid) atau harus menunggu user memberikan izin manual. |
| **Risiko** | - Aktivitas upload background yang besar bisa terdeteksi oleh fitur bawaan Android "Data Usage Alert". |

---

## 3. Perekaman Lingkungan Berbasis Event (Event-Triggered Ambient Recording)
Merekam audio sekitar (mic) bukan secara terus menerus, tetapi hanya pada momen kritikal.

- **Mekanisme**:
  - Rekam 15 detik audio saat layar menyala (Screen On).
  - Rekam saat aplikasi tertentu dibuka (misal: WhatsApp/Telegram dibuka).
  - File dikompresi (.amr/.m4a) dan dikirim di siklus C2 berikutnya.

| Aspek | Detail |
| :--- | :--- |
| **Kelebihan** | - Mendapatkan konteks situasi nyata (percakapan suara) di sekitar perangkat.<br>- Jauh lebih hemat baterai daripada merekam 24 jam. |
| **Kekurangan** | - Izin `RECORD_AUDIO` sangat sensitif dan memunculkan indikator hijau (Green Dot) di Android 12+. |
| **Risiko** | - Indikator privasi (titik hijau/oranye) di status bar saat mic aktif adalah *deal-breaker* untuk stealth. Pengguna akan tahu mic sedang digunakan. |

---

## 4. Keylogger Hantu (Ghost Keylogger via Accessibility)
Mengisi kekosongan fitur monitoring teks di aplikasi modern yang memiliki enkripsi end-to-end.

- **Mekanisme**:
  - Memanfaatkan Accessibility Service untuk mencatat *keystrokes* dan teks yang tampil di layar.
  - Hanya aktif pada aplikasi target (WA, Tele, IG) untuk menghindari spam log.
  - Tidak menyimpan password field untuk menghindari deteksi Play Protect otomatis.

| Aspek | Detail |
| :--- | :--- |
| **Kelebihan** | - Menangkap pesan draft yang belum dikirim.<br>- Menangkap pesan yang dihapus oleh pengirim.<br>- Bypasses end-to-end encryption. |
| **Kekurangan** | - Izin **Accessibility Service** adalah yang paling sulit ditembus di review Play Store. Google meminta video demo penggunaan yang valid. |
| **Risiko** | - Deteksi Play Protect sangat agresif terhadap Accessibility services yang mencurigakan. |

---

## 5. Screen Mirroring / Screenshot Berkala
Mengambil tangkapan layar secara diam-diam untuk melihat aktivitas visual korban.

- **Mekanisme**:
  - Mengambil screenshot setiap kali aplikasi sosial media dibuka.
  - Mengirim gambar resolusi rendah (thumbnail) untuk menghemat data.
  - Menggunakan *MediaProjection API* (tapi ini memunculkan dialog izin).
  - **Alternatif Stealth**: Menggunakan Accessibility Service untuk membaca *node hierarchy* (teks di layar) lalu merekonstruksi ulang tampilan secara virtual (Text-only mirroring).

| Aspek | Detail |
| :--- | :--- |
| **Kelebihan** | - Bukti visual aktivitas.<br>- Text-only mirroring sangat hemat data dan stealth. |
| **Kekurangan** | - Screenshot gambar asli (bitmap) sangat sulit dilakukan tanpa memicu notifikasi sistem atau izin yang terlihat. |
| **Risiko** | - Screenshot API yang membutuhkan izin pengguna setiap kali digunakan tidak viable untuk stealth. |

---

## 6. Lokasi "Jejak Pasif" (Passive Location Tracking)
Alih-alih menyalakan GPS (yang boros baterai dan muncul icon), gunakan data yang sudah ada.

- **Mekanisme**:
  - Membaca metadata lokasi dari foto baru yang diambil kamera user (EXIF data).
  - Membaca status WiFi SSID yang terhubung (bisa dipetakan ke lokasi fisik via database publik).
  - Mencatat Cell ID (menara seluler) untuk triangulasi kasar.

| Aspek | Detail |
| :--- | :--- |
| **Kelebihan** | - **Sangat Stealth**: Tidak ada icon GPS yang menyala.<br>- Hemat baterai. |
| **Kekurangan** | - Akurasi rendah (bisa meleset 100m - 1km).<br>- Butuh izin baca storage (untuk foto) atau lokasi background (untuk WiFi scanning). |
| **Risiko** | - Bergantung pada kebiasaan user mengambil foto atau menyalakan WiFi. |
