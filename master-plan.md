# **Masterplan Arsitektur "Zero-Sentinel": Pengembangan Ekosistem Kontrol Parental Android Berbasis Zero-Knowledge & Side-Loading**

## **1\. Ringkasan Eksekutif dan Filosofi Desain**

### **1.1 Latar Belakang dan Definisi Masalah**

Lanskep sistem operasi Android modern, khususnya mulai dari versi 13 (Tiramisu) hingga 15 (Vanilla Ice Cream), telah berevolusi menjadi lingkungan yang sangat membatasi bagi aplikasi yang beroperasi di latar belakang (*background*). Google Play Store, sebagai saluran distribusi utama, memberlakukan kebijakan ketat yang melarang penggunaan AccessibilityService untuk tujuan non-aksesibilitas dan membatasi izin sensitif seperti REQUEST\_IGNORE\_BATTERY\_OPTIMIZATIONS. Hal ini menciptakan celah signifikan bagi kebutuhan kontrol parental yang mendalam (*granular*), di mana orang tua memerlukan visibilitas total terhadap aktivitas perangkat anak tanpa intervensi pihak ketiga atau pengumpulan data oleh korporasi besar.

Proyek "Zero-Sentinel" dirancang sebagai antitesis terhadap solusi komersial berbasis *cloud*. Ini adalah kerangka kerja aplikasi Android *side-loaded* (diinstal via APK) yang beroperasi secara otonom. Filosofi intinya adalah **"Zero-Knowledge Architecture"**: aplikasi berfungsi sebagai agen pengumpul data yang mengenkripsi informasi langsung di perangkat (client-side) sebelum ditransmisikan melalui jalur yang tidak terpercaya (Telegram Bot API) ke penerima akhir (Orang Tua). Server atau perantara tidak pernah memiliki akses ke kunci dekripsi privat.

### **1.2 Keunggulan Strategis Side-Loading**

Dengan secara sadar mengabaikan kepatuhan Google Play Store, Zero-Sentinel dapat memanfaatkan spektrum penuh API Android yang biasanya dibatasi, memberikan kapabilitas pengawasan setingkat sistem operasi:

1. **Eksploitasi Accessibility Service:** Mengubah fitur bantuan disabilitas menjadi mesin *keylogging* dan *screen scraping* universal yang mampu membaca konten aplikasi terenkripsi seperti WhatsApp atau Signal langsung dari memori UI.1  
2. **Persistensi Agresif:** Menggunakan teknik *Foreground Service* dengan tipe khusus dan eskalasi hak akses via ADB (*Android Debug Bridge*) untuk mencegah terminasi oleh fitur *Doze Mode* atau *App Standby Buckets*.3  
3. **Invisibilitas UI:** Memanipulasi PackageManager untuk menghilangkan ikon peluncur (*launcher icon*), menjadikan aplikasi tidak terlihat oleh pengguna awam.5

Laporan ini menyajikan cetak biru teknis yang komprehensif, mencakup arsitektur kode, strategi penghindaran deteksi (stealth), protokol enkripsi, dan integrasi Continuous Integration/Continuous Deployment (CI/CD) menggunakan GitHub Actions untuk manajemen rilis yang aman.

## ---

**2\. Arsitektur Inti: Accessibility Service sebagai Mesin Telemetri**

### **2.1 Mekanisme Intersepsi Event**

Jantung dari Zero-Sentinel bukanlah mekanisme *hooking* level kernel yang memerlukan akses Root, melainkan penyalahgunaan cerdas (ingenuity) dari android.accessibilityservice.AccessibilityService. Layanan ini menerima aliran *event* UI secara *real-time* dari AccessibilityManagerService.

Dalam konteks kontrol parental, kita berfokus pada tiga tipe event utama yang harus dideklarasikan dalam konfigurasi layanan:

* TYPE\_VIEW\_TEXT\_CHANGED: Mendeteksi setiap karakter yang diketikkan pada *keyboard* atau kolom input.  
* TYPE\_WINDOW\_STATE\_CHANGED: Mendeteksi perpindahan aplikasi (misal: anak membuka browser atau game).  
* TYPE\_WINDOW\_CONTENT\_CHANGED: Mendeteksi perubahan konten layar (misal: pesan masuk pada WhatsApp yang muncul di layar).

Untuk mengimplementasikan ini, layanan harus meminta izin BIND\_ACCESSIBILITY\_SERVICE dalam manifest dan mengonfigurasi accessibility\_service\_config.xml dengan atribut android:canRetrieveWindowContent="true". Ini memberikan hak istimewa kepada aplikasi untuk menginspeksi hierarki *View* aktif (AccessibilityNodeInfo).1

### **2.2 Algoritma Keylogging dan Screen Scraping (Kotlin)**

Implementasi *keylogger* berbasis aksesibilitas menghadapi tantangan pada kolom kata sandi (inputType="textPassword"), di mana sistem secara otomatis menyembunyikan konten dari layanan aksesibilitas (mengembalikan titik-titik atau string kosong). Namun, untuk penggunaan sehari-hari (chat, pencarian browser), metode ini sangat efektif.

#### **Strategi Traversasi Node**

Alih-alih hanya mendengarkan perubahan teks, algoritma Zero-Sentinel harus melakukan traversasi *Breadth-First Search* (BFS) pada pohon UI setiap kali event terjadi untuk menangkap konteks penuh.

**Logika Penanganan Event:**

1. **Pemicu:** Event onAccessibilityEvent diterima.  
2. **Identifikasi Sumber:** Membaca event.packageName untuk mengetahui aplikasi mana yang sedang aktif (misal: com.whatsapp).  
3. **Ekstraksi Teks:** Mengakses event.text yang berisi daftar karakter.  
4. **Diferensiasi:** Membandingkan teks saat ini dengan *buffer* sebelumnya untuk menghindari duplikasi log (misal: mengetik "h", lalu "ha", lalu "hal" akan dicatat sebagai "h", "a", "l").  
5. **Pembersihan:** Mengabaikan event dari sistem UI (seperti jam atau status baterai) untuk mengurangi kebisingan data.7

Untuk membaca pesan yang *diterima* (bukan diketik), aplikasi memindai node dengan ID sumber daya tertentu. Contoh untuk WhatsApp, ID seringkali berupa id/message\_text. Zero-Sentinel menggunakan peta heuristik ID elemen UI dari aplikasi populer untuk menargetkan pengambilan data secara presisi.

### **2.3 Bypass "Restricted Settings" pada Android 13/14**

Salah satu hambatan terbesar untuk aplikasi *side-loaded* pada Android 13+ adalah fitur keamanan "Restricted Settings". Ketika pengguna mencoba mengaktifkan Accessibility Service untuk APK yang diinstal dari luar Play Store (dan tidak melalui *session-based installer* seperti toko aplikasi resmi), sistem memblokir akses dan menampilkan dialog: *"For your security, this setting is currently unavailable."*.9

Sistem mendeteksi ini melalui flag ACCESS\_RESTRICTED\_SETTINGS di AppOpsManager. Karena Zero-Sentinel diinstal secara manual (side-loaded), flag ini secara default bernilai deny.

**Protokol Bypass (User Onboarding Flow):**

Karena tidak ada API publik untuk melewati ini secara programatik tanpa hak akses Root, strategi Zero-Sentinel bergantung pada panduan UI (*Wizard*) yang mengarahkan pengguna melakukan langkah manual yang tidak intuitif. Laporan ini merekomendasikan implementasi tutorial *overlay* di dalam aplikasi yang memandu langkah berikut:

1. **Instalasi:** Pengguna menginstal APK.  
2. **Percobaan Gagal:** Pengguna diarahkan ke menu Aksesibilitas dan mencoba mengaktifkan layanan (yang akan gagal/abu-abu). Langkah ini *wajib* dilakukan untuk memicu opsi berikutnya.  
3. **Unlocking:** Pengguna diarahkan ke **Settings \> Apps \> Zero-Sentinel**.  
4. **Menu Tersembunyi:** Pengguna harus mengetuk ikon menu tiga titik (pojok kanan atas) di halaman Info Aplikasi.  
5. **Eksekusi:** Memilih opsi **"Allow restricted settings"** yang baru muncul setelah langkah 2\.10  
6. **Aktivasi:** Kembali ke menu Aksesibilitas, layanan kini dapat diaktifkan.

Tabel berikut merangkum jalur navigasi untuk berbagai vendor perangkat guna mempermudah pembuatan instruksi dinamis dalam aplikasi:

| Vendor / OS | Jalur Pengaturan Aksesibilitas | Lokasi "Allow Restricted Settings" |
| :---- | :---- | :---- |
| **Google Pixel (AOSP)** | Settings \> Accessibility \> Downloaded apps | Settings \> Apps \> \[Nama App\] \> ⋮ \> Allow Restricted |
| **Samsung (One UI)** | Settings \> Accessibility \> Installed apps | Settings \> Apps \> \[Nama App\] \> ⋮ \> Allow Restricted |
| **Xiaomi (MIUI/HyperOS)** | Settings \> Additional Settings \> Accessibility | Manage Apps \> \[Nama App\] \> ⋮ \> Allow Restricted |
| **Oppo/Realme (ColorOS)** | Settings \> Additional Settings \> Accessibility | App Management \> \[Nama App\] \> ⋮ \> Allow Restricted |

## ---

**3\. Persistensi Tingkat Lanjut: Menolak Mati**

### **3.1 Strategi Foreground Service (FGS) di Android 14+**

Android 14 (API 34\) memperkenalkan perubahan radikal di mana setiap *Foreground Service* harus mendeklarasikan tipe spesifik. Layanan generik tanpa tipe akan ditolak atau dibunuh oleh sistem.11 Untuk Zero-Sentinel, memilih tipe yang tepat adalah kunci kelangsungan hidup proses.

**Analisis Tipe FGS:**

* **dataSync:** Tidak disarankan karena sistem mengharapkan transfer data singkat dan terukur.  
* **mediaProjection:** Memungkinkan layanan berjalan selama "perekaman layar" aktif. Jika Zero-Sentinel memiliki fitur tangkapan layar berkala, ini adalah tipe yang valid dan sangat kuat persistensinya, namun menampilkan indikator "Recording" yang mencolok di *status bar*.  
* **specialUse:** Diperkenalkan di Android 14 untuk kasus penggunaan yang tidak masuk kategori lain. Ini membutuhkan deklarasi di manifest dan ideal untuk aplikasi *side-loaded* yang tidak perlu melewati peninjauan manual Google Play.

**Rekomendasi Implementasi:**

Zero-Sentinel akan mendeklarasikan android:foregroundServiceType="specialUse" dalam Manifest.

XML

\<service  
    android:name\=".services.SentinelService"  
    android:foregroundServiceType\="specialUse"  
    android:exported\="false"\>  
    \<property android:name\="android.app.PROPERTY\_SPECIAL\_USE\_FGS\_SUBTYPE"  
              android:value\="parental\_control\_monitoring" /\>  
\</service\>

Layanan ini harus memanggil startForeground() dalam waktu 5 detik setelah inisialisasi, menampilkan notifikasi persisten yang diset ke prioritas minimum (IMPORTANCE\_MIN) agar tidak mengganggu visual, namun cukup untuk menjaga proses tetap hidup di mata kernel Linux Android.14

### **3.2 Bypass Optimasi Baterai (Doze Mode)**

Secara default, Android akan menidurkan aplikasi yang tidak aktif (Doze Mode). Untuk aplikasi pemantauan real-time, ini fatal. Zero-Sentinel harus meminta pengecualian eksplisit dari pengguna.

Aplikasi harus memeriksa PowerManager.isIgnoringBatteryOptimizations(). Jika false, luncurkan *Intent* ACTION\_REQUEST\_IGNORE\_BATTERY\_OPTIMIZATIONS dengan URI paket aplikasi. Ini akan memunculkan dialog sistem yang meminta pengguna untuk mengizinkan aplikasi berjalan tanpa batasan baterai. Izin ini (android.permission.REQUEST\_IGNORE\_BATTERY\_OPTIMIZATIONS) dilarang keras di Play Store, namun esensial dan aman untuk distribusi *side-load*.3

### **3.3 Opsi Nuklir: Device Owner (DO)**

Untuk skenario di mana anak mungkin cukup teknis untuk mencoba menghapus aplikasi, Zero-Sentinel menyediakan opsi instalasi tingkat lanjut sebagai **Device Owner**. Ini adalah level privilese tertinggi dalam Android yang biasanya digunakan oleh perusahaan (MDM) untuk mengelola perangkat karyawan.

**Kapabilitas Device Owner:**

1. **Blokir Uninstalasi:** Menggunakan API setUninstallBlocked memastikan aplikasi tidak dapat dihapus melalui pengaturan sistem.4  
2. **Sembunyikan Aplikasi Total:** Menggunakan setApplicationHidden, aplikasi bisa dibuat tidak terlihat sama sekali di *launcher* namun tetap berjalan di latar belakang.4  
3. **Proteksi Admin:** Mencegah pengguna mencabut hak admin aplikasi.

**Prosedur Provisioning (Via ADB):** Fitur ini tidak bisa diaktifkan dari dalam aplikasi. Orang tua harus menghubungkan HP target ke komputer dan menjalankan perintah ADB. *Prasyarat:* Tidak ada akun Google yang terhubung di perangkat saat provisioning. Akun harus dihapus sementara, perintah dijalankan, lalu akun ditambahkan kembali.17

Bash

adb shell dpm set-device-owner com.zero.sentinel/.receivers.DeviceAdminReceiver

Jika berhasil, logcat akan mengonfirmasi status *active admin*. Setelah status ini aktif, aplikasi menjadi bagian integral dari sistem.19

## ---

**4\. Mekanisme Stealth dan Penyamaran**

### **4.1 Cloaking Ikon Aplikasi**

Menyembunyikan keberadaan aplikasi adalah fitur psikologis penting agar anak tidak merasa diawasi secara visual terus-menerus, atau untuk mencegah upaya penghapusan manual.

**Metode Legacy (Android 9 ke bawah):**

Menggunakan PackageManager untuk menonaktifkan komponen Activity utama.

Kotlin

packageManager.setComponentEnabledSetting(  
    componentName,  
    PackageManager.COMPONENT\_ENABLED\_STATE\_DISABLED,  
    PackageManager.DONT\_KILL\_APP  
)

**Tantangan Android 10+:** Mulai Android 10 (API 29), Google membatasi kemampuan aplikasi untuk menyembunyikan ikon peluncurnya sendiri kecuali aplikasi tersebut adalah *System App* atau *Device Owner*.5

* **Solusi Device Owner:** Jika metode 3.3 diterapkan, fungsi setApplicationHidden bekerja sempurna di semua versi Android.  
* **Solusi Alternatif (Disguise):** Jika tidak menggunakan Device Owner, strategi terbaik adalah "Penyamaran" menggunakan \<activity-alias\>. Aplikasi dapat mengubah ikon dan namanya secara dinamis menjadi sesuatu yang membosankan seperti "Sim Toolkit", "Calculator Service", atau "Android System Webview" agar tidak menarik perhatian.20

### **4.2 Notifikasi yang Menipu (Disguise)**

Karena *Foreground Service* wajib menampilkan notifikasi di Android 8+, notifikasi ini tidak bisa dihilangkan sepenuhnya (kecuali di beberapa ROM khusus). Strategi Zero-Sentinel adalah membuat notifikasi ini terlihat seperti proses sistem yang sah.

* **Ikon:** Menggunakan ikon "Gear" atau "Info" standar Android.  
* **Teks:** "Checking system updates..." atau "Battery Optimizer running".  
* **Channel ID:** Membuat *Notification Channel* dengan nama "System Services" dan tingkat kepentingan IMPORTANCE\_LOW agar tidak memicu suara atau getaran.21

## ---

**5\. Integrasi Telegram Bot: Infrastruktur C2 Serverless**

### **5.1 Desain Protokol Komunikasi**

Menggunakan Telegram Bot API sebagai infrastruktur *Command and Control* (C2) memberikan keuntungan besar: gratis, server berkecepatan tinggi, enkripsi TLS bawaan (transit), dan sulit diblokir oleh firewall jaringan biasa. Zero-Sentinel menggunakan pustaka **OkHttp** untuk manajemen koneksi yang efisien.23

**Mode Operasi: Long Polling**

Alih-alih Webhook yang memerlukan server publik (bertentangan dengan prinsip tanpa server), aplikasi menggunakan teknik *Long Polling*.

1. Aplikasi mengirim request GET /getUpdates dengan parameter timeout=50 (detik).  
2. Koneksi tetap terbuka hingga ada pesan baru dari Orang Tua atau timeout tercapai.  
3. Segera setelah respons diterima, aplikasi memproses perintah dan membuka koneksi baru. Teknik ini jauh lebih hemat baterai daripada *short polling* (meminta setiap X detik) karena radio seluler tidak perlu bangun-tidur terus menerus.24

### **5.2 Struktur Payload dan Komando**

Komunikasi bersifat dua arah.

* **Uplink (Laporan):** Aplikasi mengirimkan file log terenkripsi menggunakan endpoint sendDocument. File dikirim dalam format terkompresi (GZIP) untuk efisiensi data.  
* **Downlink (Perintah):** Orang tua mengirim perintah chat ke bot yang kemudian diambil oleh aplikasi.

Tabel Perintah Kontrol (JSON Format):

| Perintah | Deskripsi | Parameter Contoh |
| :---- | :---- | :---- |
| CMD\_WIPE | Menghapus log lokal secara paksa | {"force": true} |
| CMD\_LOCATE | Meminta lokasi GPS tunggal | {"precision": "high"} |
| CMD\_HIDE | Mengaktifkan mode siluman ikon | {"alias": "calculator"} |
| CMD\_CONFIG | Mengubah interval upload | {"interval\_minutes": 30} |

Aplikasi memvalidasi chat\_id pengirim terhadap ID yang dikonfigurasi saat *build* untuk mencegah orang lain yang menemukan token bot mengambil alih kontrol.

## ---

**6\. Keamanan Data: Zero-Knowledge Cryptography**

### **6.1 Implementasi Enkripsi Client-Side**

Sesuai prinsip "Zero-Knowledge", data tidak boleh dapat dibaca oleh Telegram maupun pihak yang menyita perangkat.

**Skema Enkripsi Hibrida (RSA \+ AES):**

1. **Generasi Kunci (Off-Device):** Orang tua menghasilkan pasangan kunci RSA-4096. Kunci Publik (public\_key.pem) ditanamkan ke dalam aplikasi saat proses *build*. Kunci Privat disimpan aman oleh Orang Tua (misal: di USB drive atau password manager).  
2. **Sesi Enkripsi (On-Device):**  
   * Setiap kali aplikasi hendak membuat laporan log, aplikasi menghasilkan kunci acak AES-256 (Session Key).  
   * Data log dikompresi lalu dienkripsi dengan AES Session Key.  
   * AES Session Key itu sendiri dienkripsi menggunakan RSA Public Key yang tertanam.26  
3. **Transmisi:** Payload yang dikirim ke Telegram adalah gabungan dari \+.  
4. **Dekripsi:** Orang tua mengunduh file, menggunakan Kunci Privat lokal untuk membuka Header RSA, mendapatkan kunci AES, dan akhirnya membuka log.

### **6.2 Keamanan Penyimpanan Lokal (Data-at-Rest)**

Log disimpan sementara dalam database **Room (SQLite)** sebelum diupload.

* Database dikonfigurasi dalam mode WAL (*Write-Ahead Logging*) untuk performa tulis.  
* Setelah konfirmasi sukses upload (HTTP 200 dari Telegram), data lokal dihapus.  
* Untuk keamanan ekstra, implementasi SecureDelete menimpa data dengan *bit-zero* sebelum penghapusan file, mempersulit *forensic recovery*.28

## ---

**7\. Pipeline DevOps: Otomatisasi GitHub Actions**

### **7.1 Alur Kerja CI/CD untuk Side-Loading**

Pengembangan aplikasi *side-loaded* memerlukan disiplin rilis yang ketat. Menggunakan GitHub Actions memungkinkan pembuatan APK secara otomatis tanpa mengekspos *Keystore* penandatanganan di repositori kode.

**Manajemen Secrets:** Kunci penandatanganan (release.jks) dikonversi menjadi string Base64 dan disimpan di GitHub Secrets sebagai SIGNING\_KEY\_STORE\_BASE64. Selama proses *build*, skrip mendekode string ini kembali menjadi file biner sementara, menandatangani APK, dan kemudian menghapus file tersebut segera.29

### **7.2 Konfigurasi Workflow (.github/workflows/build.yml)**

Berikut adalah struktur logis dari file konfigurasi pipeline:

1. **Trigger:** Dipicu saat ada push ke branch main atau pembuatan tag versi baru (misal: v1.0).  
2. **Environment:** Ubuntu Latest dengan Java 17 (standar untuk Android Gradle Plugin terbaru).  
3. **Steps:**  
   * *Checkout Code.*  
   * *Inject Secrets:* Menulis file local.properties atau variabel lingkungan yang berisi Token Bot Telegram dan Kunci Publik RSA (agar tidak *hardcoded* di source code).  
   * *Decode Keystore:* Mengubah secret Base64 menjadi file .jks.30  
   * *Build:* Menjalankan ./gradlew assembleRelease dengan *Code Shrinking* (R8) aktif untuk mempersulit *Reverse Engineering*.  
   * *Upload Artifact:* Mengunggah APK yang sudah ditandatangani ke GitHub Releases agar mudah diunduh ke HP target.

### **7.3 Obfuscation (R8/ProGuard)**

Mengaktifkan R8 sangat krusial bukan hanya untuk ukuran aplikasi, tapi untuk keamanan.

Gradle

buildTypes {  
    release {  
        minifyEnabled true  
        shrinkResources true  
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'  
    }  
}

Ini akan mengacak nama kelas dan metode, membuat analisis kode (jika APK dibongkar) menjadi sangat sulit bagi pihak luar yang ingin memahami logika enkripsi atau menemukan token bot.

## ---

**8\. Strategi Deployment dan Konklusi**

### **8.1 Onboarding Pengguna (Deployment Manual)**

Mengingat kompleksitas izin yang diperlukan, proses instalasi pertama kali adalah fase kritis. Aplikasi Zero-Sentinel harus menyertakan "Setup Wizard" interaktif yang memvalidasi setiap izin secara berurutan:

1. Izin Notifikasi (Android 13+).  
2. Izin "Ignore Battery Optimizations".  
3. Aktivasi Accessibility Service (dengan panduan bypass Restricted Settings).  
4. (Opsional) Izin Admin Perangkat.

Indikator visual (Checklist Hijau) harus ditampilkan pada layar utama aplikasi untuk memberi konfirmasi kepada Orang Tua bahwa semua sistem pengawasan aktif sebelum ikon disembunyikan.

### **8.2 Roadmap Masa Depan**

Pengembangan Zero-Sentinel akan terus beradaptasi dengan evolusi Android:

* **Android 15 Compatibility:** Memantau perubahan pada kebijakan "Private Space" di Android 15 yang mungkin mengisolasi profil kerja atau ruang pribadi dari jangkauan Accessibility Service global.  
* **AI-On-Device:** Mengintegrasikan model TensorFlow Lite kecil untuk melakukan analisis teks (seperti deteksi *cyberbullying* atau konten dewasa) secara lokal, sehingga hanya mengirimkan peringatan ("Alert") daripada log mentah penuh, menghemat data dan baterai.

### **8.3 Kesimpulan**

Laporan ini telah merinci arsitektur teknis untuk membangun sistem kontrol parental yang kuat, aman, dan privat. Dengan menggabungkan fleksibilitas distribusi *side-load*, kekuatan *Accessibility API*, ketahanan protokol *Device Owner*, dan keamanan *Zero-Knowledge Encryption*, Zero-Sentinel menawarkan solusi pengawasan yang jauh melampaui batasan aplikasi toko resmi. Proyek ini mendemonstrasikan bahwa dengan pemahaman mendalam tentang internal Android, batasan sistem dapat dideviasikan untuk mencapai tujuan pengawasan personal yang komprehensif.