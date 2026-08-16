# 🕌 AdzanPlus — Modern, Offline-First Islamic Prayer Times & Adhan Reminder

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20KMP-00C853?style=for-the-badge)](https://developer.android.com/topic/architecture)
[![Android Min SDK](https://img.shields.io/badge/Min%20SDK-API%2024%20(Nougat)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![DevSecOps](https://img.shields.io/badge/DevSecOps-Anti--Leakage%20Enforced-critical?style=for-the-badge&logo=shield&logoColor=white)](#-devsecops--security-best-practices)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](LICENSE)

---

## 📖 Overview

**AdzanPlus** adalah aplikasi jadwal sholat dan pengingat adzan modern berbasis Android yang mengedepankan privasi pengguna, performa tinggi, efisiensi baterai, dan kemampuan **100% Offline-First**. 

Berbeda dengan aplikasi konvensional yang bergantung pada REST API pihak ketiga, AdzanPlus menghitung seluruh waktu sholat astronomis langsung di perangkat menggunakan modul **Kotlin Multiplatform (KMP)** murni, sehingga tetap akurat tanpa memerlukan koneksi internet aktif.

---

## ✨ Fitur Unggulan

- 🕋 **Kalkulasi Astronomis On-Device (100% Offline)**:
  - Mendukung standar hisab **Kemenag RI (Kementerian Agama Republik Indonesia)** dengan koreksi ihtiyath (+2 menit).
  - Pilihan metode internasional: **Muslim World League (MWL)**, **Umm Al-Qura (Makkah)**, **Egyptian General Authority**, **Karachi**, **ISNA**, **MUIS (Singapura)**, dan **Custom Method**.
  - Pilihan Madhab (Syafi'i/Hambali/Maliki vs Hanafi) dan aturan lintang tinggi (*High Latitude Rules*).
  - Waktu sholat komprehensif: Subuh, Terbit (Syuruq), Dzuhur, Ashar, Maghrib, Isya, Imsak, Tengah Malam (*Midnight*), dan Sepertiga Malam Terakhir (*Tahajjud*).

- ⏰ **Sistem Alarm & Notifikasi Presisi (Doze-Resistant)**:
  - Memanfaatkan `AlarmManager.setExactAndAllowWhileIdle()` untuk ketepatan waktu alarm bahkan saat perangkat dalam mode *Doze*.
  - Pemulihan otomatis setelah *reboot* (`BootReceiver`) dan deteksi pergantian zona waktu (`TimeChangeReceiver`).
  - Rekonsiliasi harian mandiri via `WorkManager`.
  - Audio playback adzan autentik dan layar fullscreen dismiss/snooze ketika alarm berbunyi dalam keadaan layar terkunci.

- 📱 **Real-Time Home Screen Widget (Jetpack Compose Glance)**:
  - Menampilkan hitung mundur waktu sholat berikutnya secara *live* tanpa menguras baterai menggunakan `RemoteViews.Chronometer`.
  - Tersedia varian *Compact* (2x2) dan *Detailed* (4x2 / 4x3) dengan tema adaptif *Material You*.

- 🧭 **Kompas Kiblat Presisi**:
  - Perhitungan arah Ka'bah (21.4225° N, 39.8262° E) menggunakan algoritma *Great-Circle Bearing*.
  - Integrasi sensor geomagnetik dan akselerometer perangkat dengan visualisasi kompas yang mulus.

- 🎨 **Modern Islamic Elegance (Material 3)**:
  - Palet warna eksklusif *Deep Emerald & Warm Gold* dengan dukungan tema Terang (*Light*) dan Gelap (*Dark*).
  - Tata letak responsif (*Adaptive Layouts*) yang dioptimalkan untuk smartphone compact, perangkat lipat (*foldables*), dan tablet.

---

## 🛠️ Tech Stack & Arsitektur

AdzanPlus dibangun dengan prinsip **Clean Architecture**, **SOLID**, **DRY**, dan **Unidirectional Data Flow (UDF)**:

```
┌─────────────────────────────────────────────────────────────┐
│                      :app (Android)                         │
│  ┌────────────────────────┐    ┌──────────────────────────┐ │
│  │   Jetpack Compose M3   │    │  Jetpack Glance Widgets  │ │
│  │      (Presentation)    │    │      (Home Screen)       │ │
│  └───────────┬────────────┘    └────────────┬─────────────┘ │
│              │                              │               │
│              ▼                              ▼               │
│  ┌────────────────────────────────────────────────────────┐ │
│  │             ViewModels & UI State Holders              │ │
│  └───────────────────────────┬────────────────────────────┘ │
│                              │                              │
│                              ▼                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │     Data Layer (Room DB, DataStore, AlarmManager)      │ │
│  └───────────────────────────┬────────────────────────────┘ │
└──────────────────────────────┼──────────────────────────────┘
                               │ (Domain Contract)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                  :core-prayer (KMP Shared)                  │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Pure Kotlin Astronomical Calculation Math & Algorithms│ │
│  │   (AstronomicalMath, SolarCoordinates, PrayerTimes)    │ │
│  │             * Zero Android SDK Dependencies *          │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Komponen Teknologi:
- **Language**: Kotlin 2.0+ & Kotlin Multiplatform (KMP)
- **UI Framework**: Jetpack Compose, Material 3, Compose Navigation
- **Widget**: Jetpack Compose Glance + RemoteViews Chronometer
- **Asynchronous**: Kotlin Coroutines & Reactive StateFlow / SharedFlow
- **Dependency Injection**: Dagger Hilt
- **Storage & Caching**: Jetpack Room Database & Jetpack DataStore Preferences
- **System Background Services**: Android `AlarmManager`, `BroadcastReceiver`, `WorkManager`
- **Audio & Media**: AndroidX Media3 ExoPlayer

---

## 📂 Struktur Direktori

```plaintext
AdzanPlus/
├── app/                                 # Modul Android Application
│   ├── src/main/
│   │   ├── java/com/adzannotif/
│   │   │   ├── data/                    # Repositories, DAOs, DataStore Preferences
│   │   │   ├── di/                      # Hilt DI Dependency Injection Modules
│   │   │   ├── domain/                  # UseCases & Business Contracts
│   │   │   ├── platform/                # AlarmManager, Receivers, Audio & Notifications
│   │   │   ├── presentation/            # Compose Screens (Home, Schedule, Qibla, Settings)
│   │   │   ├── ui/                      # Material 3 Theme, Typography, Colors
│   │   │   └── widget/                  # Jetpack Compose Glance Home Widget
│   │   └── res/                         # Vector drawables, Audio assets, Layouts
│   └── build.gradle.kts
│
├── core-prayer/                         # Modul KMP Pure Domain & Calculation Engine
│   ├── src/
│   │   ├── commonMain/                  # Algoritma Astronomi, Solar Times, Qibla (Pure Kotlin)
│   │   └── commonTest/                  # Unit Tests (>95% coverage kalkulasi waktu sholat)
│   └── build.gradle.kts
│
├── docs/                                # Dokumentasi teknis & spesifikasi sprint
├── .github/                             # GitHub Actions CI/CD workflows
├── .gitignore                           # DevSecOps Anti-Leakage rules
├── .gitattributes                       # Line normalization & binary assets handling
├── DESIGN.md                            # Design System & UI Specification
├── PRD.md                               # Product Requirements Document
├── AGENTS.md                            # Multi-Agent Operating Model
└── settings.gradle.kts
```

---

## 🚀 Memulai (Getting Started)

### Prasyarat
- **Android Studio**: Ladybug (2024.2.1) / Meerkat atau versi lebih baru.
- **JDK**: Java Development Kit 17 atau 21.
- **Android SDK**: Min SDK `API 24` (Android 7.0), Target SDK `API 35` (Android 15).

### Langkah Instalasi & Menjalankan

1. **Clone Repositori**:
   ```bash
   git clone https://github.com/Adrian463588/AdzanPlus.git
   cd AdzanPlus
   ```

2. **Buka di Android Studio**:
   - Buka Android Studio -> `Open Project` -> Pilih direktori `AdzanPlus`.
   - Biarkan Gradle melakukan sync dependensi secara otomatis.

3. **Build & Jalankan via Terminal / CLI**:
   ```bash
   # Menjalankan unit test modul kalkulasi
   ./gradlew :core-prayer:test

   # Build APK Debug
   ./gradlew assembleDebug

   # Install langsung ke perangkat/emulator yang terhubung
   ./gradlew installDebug
   ```

---

## 🔒 DevSecOps & Security Best Practices

Proyek ini menerapkan standar keamanan **DevSecOps** dan **Anti-Credential Leakage**:
1. **Zero Secret Policy**: Seluruh file konfigurasi lokal (`local.properties`, `key.properties`, `secrets.properties`) dikecualikan dari version control melalui [`.gitignore`](.gitignore).
2. **Keystore Protection**: Kunci tanda tangan aplikasi (`*.jks`, `*.keystore`, `*.p12`) tidak pernah disimpan di dalam repositori publik.
3. **Pure Domain Isolation**: Modul perhitungan waktu sholat (`:core-prayer`) diisolasi penuh dari dependensi eksternal untuk menjamin integritas data astronomis.
4. **Privacy-First**: Aplikasi tidak memerlukan izin akses internet wajib untuk menghitung waktu sholat.

---

## 👨‍💻 Penulis & Signature

Dikonsep, dirancang, dan dikembangkan dengan dedikasi oleh:

**Adrian Syah Abidin**  
*Lead Software Engineer & Android Specialist*  
GitHub: [@Adrian463588](https://github.com/Adrian463588)

---

## 📄 Lisensi

Proyek ini dilisensikan di bawah [MIT License](LICENSE) — Bebas digunakan, dipelajari, dan dikembangkan untuk kebaikan umat.
