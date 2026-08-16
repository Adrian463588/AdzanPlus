Membangun aplikasi pengingat waktu sholat dengan notifikasi dan widget yang reliable membutuhkan fondasi teknologi yang tepat. Berikut adalah rekomendasi **stack best practice** dan **5 repository referensi** yang bisa Anda pelajari.

### 📱 Stack Best Practice

Untuk mencapai fungsionalitas seperti Muslim Pro (real-time, notifikasi, widget), arsitektur yang disarankan adalah **Modern Android Development** dengan fokus pada stabilitas dan efisiensi baterai.

#### 🧮 1. Perhitungan Waktu Sholat
*   **Jangan andalkan API selalu online**: Gunakan library yang bisa menghitung secara *offline* agar aplikasi tetap akurat meski tanpa internet dan lebih hemat baterai.
*   **Pustaka yang direkomendasikan**: `Prayer-Times-KMM` (Kotlin Multiplatform) atau `OfflinePrayerTimes`. Keduanya support berbagai metode kalkulasi (Kemenag, MWL, Umm Al-Qura, dll) .
*   **Contoh penggunaan**:
    ```kotlin
    // Menggunakan Prayer-Times-KMM
    val coordinates = Coordinates(latitude = -6.2088, longitude = 106.8456)
    val params = CalculationParameters(method = CalculationMethod.MUSLIM_WORLD_LEAGUE)
    val prayerTimes = PrayerTimes(coordinates, dateComponents, params)
    val waktuDzuhur = prayerTimes.dhuhr
    ```

#### ⏰ 2. Notifikasi & Pengingat (Background Task)
Ini adalah bagian paling krusial. Hindari `AlarmManager` untuk setExact yang ketat karena sering bermasalah di OEM China (Xiaomi, Oppo) dan Android Doze mode.

*   **Solusi yang Tepat**: Gunakan **WorkManager** dari Android Jetpack .
    *   **Keunggulan**: Lebih tahan terhadap pembunuhan aplikasi di background, kompatibel dengan Doze mode, dan tidak memerlukan izin `SCHEDULE_EXACT_ALARM` yang sulit .
    *   **Strategi**: Schedule `OneTimeWorkRequest` untuk setiap waktu sholat berikutnya. Setelah notifikasi muncul, hitung waktu sholat selanjutnya dan schedule ulang.
*   **Izin Notifikasi**: Wajib minta izin `POST_NOTIFICATIONS` (Android 13+) secara eksplisit.

#### 🧩 3. Widget Real-time
*   **Teknologi**: Gunakan **Jetpack Compose Glance** untuk membuat widget modern dengan kode deklaratif.
*   **Update Data**: Widget diperbarui via `WorkManager` yang dijadwalkan secara periodik (misal, setiap 5 menit atau setiap kali waktu sholat berubah) menggunakan ` PeriodicWorkRequest`.

#### 🗺️ 4. Lokasi
*   **Izin**: Gunakan `ACCESS_FINE_LOCATION` atau `ACCESS_COARSE_LOCATION` secara efisien.
*   **Update**: Perbarui lokasi secara berkala di background (sesuaikan dengan interval tertentu) agar waktu sholat mengikuti posisi pengguna . Kombinasikan dengan `WorkManager` untuk mengambil lokasi terakhir.

#### 📐 5. Arsitektur UI
*   **Framework**: **Jetpack Compose** (standar baru Android) .
*   **Pattern**: **MVVM** (Model-View-ViewModel) dengan `StateFlow`/`LiveData` untuk memisahkan UI dan logika bisnis .
*   **Dependency Injection**: **Hilt** untuk manajemen dependency yang rapi.

---

### 🏆 Top 5 Repository Android Referensi

Berikut adalah 5 proyek open-source terbaik yang bisa Anda jadikan referensi untuk melihat implementasi nyata dari stack di atas.

| Nama & Fitur Utama | Alasan Direkomendasikan | Teknologi Kunci |
| :--- | :--- | :--- |
| **[Al-Azan Compose]** <br>• **No Internet Permission** (Full Offline)<br>• **Widget & Notifikasi**<br>• **Qibla Finder** | **Arsitektur modern terbaik.** Ini adalah contoh sempurna aplikasi Adzan yang ditulis ulang dari React Native ke native Android dengan arsitektur terkini. Sangat privat dan bersih. | **Kotlin, Jetpack Compose, Material You** |
| **[Prayer-Times-KMM]** <br>• **Library Perhitungan**<br>• **Multiplatform** (Android/iOS)<br>• **11+ Metode Kalkulasi** | **Inti perhitungan yang paling baik.** Jika Anda ingin membuat library atau komponen inti untuk menghitung waktu, ini adalah referensi terbaik. Kodenya sangat clean dan sesuai standar. | **Kotlin Multiplatform (KMP)** |
| **[OfflinePrayerTimes]** <br>• **Library Offline**<br>• **Fokus pada Kalkulasi** | **Library Perhitungan Lokal.** Sangat cocok untuk integrasi cepat jika Anda tidak ingin memusingkan rumus astronomi. Dokumentasi dan contoh implementasinya jelas. | **Kotlin, Coroutines** |
| **[Solat-Kuy-Android-MVVM]** <br>• **Implementasi MVVM**<br>• **Dagger Hilt**<br>• **Coroutine & Testing** | **Referensi Arsitektur.** Repository ini didesain untuk menunjukkan implementasi pola MVVM yang bersih dengan dependency injection dan unit testing, yang sangat baik untuk skala proyek besar. | **Kotlin, MVVM, Dagger Hilt, Coroutines** |
| **[Mihrab]**<br>• **Aplikasi Open Source di F-Droid**<br>• **Widget & Quran Reader**<br>• **Zero Tracker** | **Referensi Fitur Lengkap.** Aplikasi ini sudah matang dan memiliki banyak fitur (Quran, Dua, Tasbih). Anda bisa belajar bagaimana mengorganisir kode untuk aplikasi dengan banyak modul. | **Open Source (AGPL-3.0)** |

### 💡 Tips Implementasi

1.  **Mulai dari Inti**: Fokus dulu pada library perhitungan (`Prayer-Times-KMM`). Pastikan aplikasi Anda bisa menampilkan 5 waktu sholat dengan akurat berdasarkan lokasi.
2.  **Tambahkan Notifikasi**: Implementasikan `WorkManager` untuk menjadwalkan notifikasi. Uji coba di berbagai merek HP (terutama Xiaomi/Samsung) untuk memastikan notifikasi tidak mati .
3.  **Kembangkan Widget**: Setelah notifikasi stabil, buat widget sederhana dengan `Glance` yang menampilkan waktu sholat berikutnya.
4.  **Optimasi Baterai**: Beri edukasi atau panduan di dalam aplikasi agar pengguna menonaktifkan "Battery Optimization" untuk aplikasi Anda, sehingga widget dan notifikasi lebih akurat .

Semoga panduan dan referensi ini membantu Anda memulai proyek. Selamat coding!