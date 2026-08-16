Untuk aplikasi Android **pengingat waktu sholat + notifikasi + widget realtime seperti Muslim Pro**, saya menyarankan pendekatan **offline-first**: waktu sholat dihitung langsung di perangkat dari koordinat pengguna, bukan bergantung pada API setiap kali aplikasi dibuka. Dengan begitu aplikasi tetap berfungsi tanpa internet, lebih cepat, lebih privat, dan notifikasi tidak bergantung pada server.

Per **16 Agustus 2026**, stack yang paling masuk akal adalah Kotlin + Jetpack Compose + Material 3, Clean Architecture/UDF, Hilt, Coroutines/Flow, DataStore/Room, `adhan-kotlin`, AlarmManager, WorkManager, dan Jetpack Glance. Android sendiri merekomendasikan Compose untuk UI modern, arsitektur berlapis dengan data layer serta optional domain layer, dan Hilt untuk dependency injection. ([Android Developers][1])

## 1. Stack yang saya rekomendasikan

| Bagian                | Stack                                    | Fungsi                                       |
| --------------------- | ---------------------------------------- | -------------------------------------------- |
| Language              | **Kotlin**                               | Native Android                               |
| UI                    | **Jetpack Compose + Material 3**         | UI utama                                     |
| Architecture          | **Clean Architecture + MVVM/UDF**        | maintainable, testable                       |
| DI                    | **Hilt**                                 | Dependency Injection                         |
| Async                 | **Coroutines + Flow/StateFlow**          | reactive state                               |
| Navigation            | Navigation Compose                       | perpindahan screen                           |
| Prayer calculation    | **BatoulApps Adhan Kotlin**              | menghitung jadwal sholat offline             |
| Date/time             | `java.time` / `kotlinx-datetime`         | timezone + datetime                          |
| Location              | Fused Location Provider                  | latitude/longitude                           |
| Preferences           | **DataStore**                            | metode hisab, lokasi, alarm settings         |
| Database              | **Room**                                 | cache jadwal bulanan/history bila diperlukan |
| Exact prayer reminder | **AlarmManager**                         | alarm pada waktu sholat                      |
| Maintenance           | **WorkManager**                          | recalculate/cache/resync                     |
| Notifications         | NotificationManager / NotificationCompat | notifikasi adzan                             |
| Widget                | **Jetpack Glance**                       | home screen widget                           |
| Live countdown widget | Glance + RemoteViews `Chronometer`       | countdown tanpa polling                      |
| Audio adzan           | Android Media3                           | playback audio jika dibutuhkan               |
| Testing               | JUnit + Turbine + Compose UI Test        | unit/integration/UI                          |
| Static analysis       | Detekt + Ktlint/Spotless + Android Lint  | clean code                                   |
| Build                 | Gradle Kotlin DSL + Version Catalog      | dependency management                        |

Untuk kalkulasi waktu sholat, `batoulapps/adhan-kotlin` sangat layak digunakan. Repository-nya aktif dan saat ini mendokumentasikan artifact `com.batoulapps.adhan:adhan2:0.0.6`; library tersebut menerima koordinat, tanggal dan parameter metode kalkulasi lalu menghasilkan waktu sholat dalam `Instant`. ([GitHub][2])

---

# 2. Arsitektur yang saya rekomendasikan

Jangan membuat semua logic di `MainViewModel`.

Gunakan kira-kira:

```text
app
│
├── core
│   ├── common
│   ├── designsystem
│   ├── location
│   ├── database
│   ├── datastore
│   ├── notification
│   ├── alarm
│   └── widget
│
├── domain
│   ├── model
│   ├── repository
│   └── usecase
│
├── data
│   ├── repository
│   ├── local
│   └── prayer
│
└── feature
    ├── home
    ├── prayertimes
    ├── widget
    ├── location
    └── settings
```

Alur datanya:

```text
GPS / Manual Location
        ↓
LocationRepository
        ↓
PrayerTimeCalculator
        ↓
PrayerTimesRepository
        ↓
Room / DataStore
        ↓
UseCase
    ↙       ↘
ViewModel   AlarmScheduler
   ↓             ↓
Compose       Notification
   ↓
Glance Widget
```

Pendekatan tersebut selaras dengan rekomendasi Android mengenai **UI → domain opsional → data**, repository sebagai single source of truth, serta UDF antara ViewModel dan Compose. ([Android Developers][1])

---

# 3. Prayer Time Engine — offline

Saya justru **tidak merekomendasikan menjadikan API jadwal sholat sebagai sumber utama**.

Lebih bagus:

```text
latitude
longitude
date
timezone
calculation method
madhab
minute adjustment
        ↓
Adhan Kotlin
        ↓
PrayerTimes
```

Contohnya secara konseptual:

```kotlin
val coordinates = Coordinates(
    latitude,
    longitude
)

val parameters =
    CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters

val prayerTimes = PrayerTimes(
    coordinates,
    dateComponents,
    parameters
)
```

Adhan sudah menyediakan beberapa metode perhitungan dan juga `PrayerAdjustments`, sehingga aplikasi Anda dapat mendukung koreksi beberapa menit tanpa memodifikasi algoritma utama. ([GitHub][2])

### Khusus Indonesia

Saya menyarankan membuat profile tersendiri:

```text
CalculationProfile

Indonesia / Kemenag
Muslim World League
Umm Al-Qura
Karachi
Egyptian
Custom
```

Lalu sediakan:

```text
Subuh     +/- minute
Dzuhur    +/- minute
Ashar     +/- minute
Maghrib   +/- minute
Isya      +/- minute
```

Ini penting karena jadwal resmi tidak hanya terkait sudut astronomis tetapi juga bisa memakai **ihtiyath/koreksi waktu**. Materi Kemenag mengenai hisab waktu sholat juga membahas penambahan ihtiyath beberapa menit, sementara Bimas Islam menyediakan jadwal sholat resmi yang dapat digunakan sebagai dataset pembanding pengujian. ([Kantor Kementerian Agama Kab. Temanggung][3])

Jadi strategi terbaik untuk aplikasi Indonesia adalah:

```text
Adhan calculation
        +
Indonesia calculation profile
        +
manual minute correction
        +
automated regression test
        ↓
compare against Kemenag reference dates/cities
```

Bukan scraping halaman Kemenag setiap hari.

---

# 4. Location

Untuk MVP cukup gunakan:

```text
FusedLocationProviderClient
        ↓
latitude + longitude
        ↓
PrayerTimeCalculator
```

Saya tidak menyarankan meminta **background location** hanya untuk jadwal sholat. Android sendiri menyarankan membatasi background location kecuali benar-benar menjadi fungsi inti. ([Android Developers][4])

Berikan dua pilihan:

```text
Use Current Location

atau

Choose City Manually
```

Contoh UX:

```text
Lokasi
──────────────
📍 Jakarta Selatan
Terakhir diperbarui: 18:41

[ Gunakan lokasi saya ]

atau

[ Pilih kota manual ]
```

Untuk pengguna yang menolak permission, aplikasi tetap harus berfungsi dengan lokasi manual.

---

# 5. Sistem notifikasi sholat

Ini bagian yang **jangan menggunakan WorkManager sebagai alarm utama**.

Gunakan:

```text
PrayerTimes
      ↓
AlarmScheduler
      ↓
AlarmManager
      ↓
BroadcastReceiver
      ↓
Notification
```

Misalnya:

```text
04:35 Subuh
11:58 Dzuhur
15:20 Ashar
17:55 Maghrib
19:06 Isya
```

Anda jadwalkan 5 alarm terpisah.

Untuk reminder yang harus tepat waktu, exact alarm adalah mekanisme Android yang tepat ketika tindakan memang user-facing dan timing presisi merupakan kebutuhan utama. ([Android Developers][5])

### Android 12+

Periksa:

```kotlin
alarmManager.canScheduleExactAlarms()
```

dan bila diizinkan gunakan misalnya:

```text
setExactAndAllowWhileIdle()
```

Android 14 tidak lagi memberikan `SCHEDULE_EXACT_ALARM` secara default kepada sebagian besar fresh install, jadi aplikasi harus menangani kondisi permission tidak tersedia. ([Android Developers][6])

Saya akan memilih:

```xml
SCHEDULE_EXACT_ALARM
```

untuk versi awal Play Store.

Jangan langsung memakai:

```xml
USE_EXACT_ALARM
```

karena Google Play memperlakukan `USE_EXACT_ALARM` sebagai restricted permission dan membatasinya pada use case inti seperti aplikasi alarm/timer/calendar. ([Google Support][7])

---

# 6. Notification Channel

Minimal buat dua channel:

```text
Prayer Alerts
Importance: HIGH

Prayer Reminders
Importance: DEFAULT
```

Contohnya:

```text
10 menit lagi waktu Maghrib
17:45
```

kemudian:

```text
Waktu Maghrib telah tiba
17:55
```

Pada Android 13+ Anda juga perlu menangani runtime permission:

```text
POST_NOTIFICATIONS
```

([Android Developers][8])

Di settings aplikasi beri kontrol:

```text
Subuh
✓ Reminder
✓ Adzan
10 menit sebelumnya

Dzuhur
✓ Reminder
✓ Adzan

Ashar
✓ Reminder
✓ Adzan

Maghrib
✓ Reminder
✓ Adzan

Isya
✓ Reminder
✓ Adzan
```

---

# 7. Jangan lupa reboot

Alarm yang dijadwalkan `AlarmManager` hilang setelah perangkat dimatikan/reboot. Android menyediakan `ACTION_BOOT_COMPLETED` untuk kasus seperti memasang ulang alarm setelah boot. ([Android Developers][9])

Implementasikan:

```text
BootCompletedReceiver
        ↓
Read location/settings
        ↓
Calculate PrayerTimes
        ↓
ReschedulePrayerAlarmsUseCase
```

Manifest:

```text
RECEIVE_BOOT_COMPLETED
```

Selain boot, lakukan rescheduling ketika:

```text
location berubah
timezone berubah
tanggal berubah
metode kalkulasi berubah
minute adjustment berubah
notification preference berubah
```

---

# 8. Widget seperti Muslim Pro

Untuk widget modern:

**Jetpack Glance**.

Glance memang dirancang Android untuk membuat AppWidget menggunakan API bergaya Compose. ([Android Developers][10])

Widget medium misalnya:

```text
┌────────────────────────────┐
│ Jakarta              18:20 │
│                            │
│ Maghrib                     │
│ 18:33                       │
│                            │
│ 13 menit lagi               │
│                            │
│ S  D  A  M  I              │
│ 04 12 15 18 19             │
└────────────────────────────┘
```

Widget besar:

```text
┌────────────────────────────┐
│ Minggu, 16 Agustus         │
│ Jakarta Selatan            │
│                            │
│ Selanjutnya                │
│        MAGHRIB             │
│         18:33              │
│       00:13:24             │
│                            │
│ Subuh       04:36          │
│ Dzuhur      11:59          │
│ Ashar       15:21          │
│ Maghrib     18:33    ←     │
│ Isya        19:43          │
└────────────────────────────┘
```

---

# 9. Cara membuat countdown widget benar-benar realtime

Ini penting.

**Jangan menjalankan Worker setiap menit.**

Android menyebut `updatePeriodMillis` tidak mendukung interval di bawah 30 menit, WorkManager periodic mempunyai minimum 15 menit, dan dokumentasi Glance secara eksplisit memperingatkan agar widget tidak diperbarui setiap menit di background karena boros baterai. ([Android Developers][11])

Gunakan hybrid:

```text
Jetpack Glance
     +
AndroidRemoteViews
     +
Chronometer
```

`RemoteViews` mendukung `Chronometer`, dan Glance sekarang menyediakan `AndroidRemoteViews` interoperability sehingga komponen RemoteViews dapat dimasukkan ke UI Glance. ([Android Developers][12])

Dengan demikian:

```text
18:33 Maghrib
      ↓
Chronometer base = Maghrib
      ↓
00:13:22
00:13:21
00:13:20
```

tanpa:

```text
Worker setiap 1 detik ❌
Service infinite ❌
Coroutine background infinite ❌
Alarm setiap menit ❌
```

Ini jauh lebih dekat dengan pola widget aplikasi production.

---

# 10. Fungsi WorkManager

WorkManager tetap dipakai, tetapi **bukan sebagai alarm sholat**.

Gunakan untuk:

```text
00:05 setiap hari
      ↓
calculate jadwal hari ini + besok
      ↓
cache database
      ↓
verify alarms
      ↓
update widget
```

dan:

```text
Periodic recovery
        ↓
Apakah alarm besok sudah ada?
        ↓
Tidak
        ↓
Schedule ulang
```

WorkManager memang dirancang untuk persistent background work dan otomatis menyimpan scheduled work agar bertahan terhadap reboot. ([Android Developers][13])

---

# 11. MVP yang saya sarankan

Jangan langsung membuat Quran, hadits, komunitas, AI, dan 30 fitur lain.

**MVP v1:**

```text
Home
├── waktu sholat hari ini
├── next prayer
├── countdown
└── lokasi

Prayer Schedule
└── jadwal 30 hari

Notifications
├── Subuh
├── Dzuhur
├── Ashar
├── Maghrib
└── Isya

Widget
├── Compact
└── Detailed

Settings
├── location
├── calculation method
├── madhab
├── minute adjustment
├── notification
└── theme
```

Setelah stabil baru tambahkan:

```text
Qibla
Hijri calendar
Imsak
Dhuha
Tahajjud
Quran
Dzikir
Mosque finder
```

---

# 12. Top 5 repository GitHub terbaik untuk referensi

| Rank  | Repository                            | Kenapa saya rekomendasikan                                  |
| ----- | ------------------------------------- | ----------------------------------------------------------- |
| **1** | `meypod/al-azan-compose`              | Referensi paling dekat dengan aplikasi yang Anda ingin buat |
| **2** | `BassamAlim/Hidaya`                   | Bagus untuk arsitektur Compose + alarm + widget             |
| **3** | `batoulapps/adhan-kotlin`             | Prayer calculation engine                                   |
| **4** | `olcayertas/prayer-time`              | Modern Compose + Glance + exact alarms + clean domain       |
| **5** | `yshalsager/SuntimesPrayerTimesAddon` | Referensi countdown/widget/alarm scheduling                 |

### 1. `meypod/al-azan-compose` — **paling saya rekomendasikan**

Ini hampir persis referensi yang Anda cari.

Sudah menggunakan native Android + Jetpack Compose dan memiliki:

```text
✓ prayer times
✓ GPS
✓ offline location
✓ custom Adhan
✓ Fajr-specific Adhan
✓ calculation methods
✓ reminders
✓ alarms
✓ homescreen widget
✓ notification widget
✓ Qibla
✓ Indonesian localization
✓ dark/light theme
```

Aplikasi juga menggunakan `adhan-kotlin` sebagai prayer calculation engine. ([GitHub][14])

Lisensi: **AGPL-3.0**, jadi perhatikan kewajiban lisensinya jika mengambil kode. ([GitHub][14])

**Nilai referensi: 10/10**

---

### 2. `BassamAlim/Hidaya`

Sangat bagus untuk mempelajari arsitektur.

Stack:

```text
Kotlin
Jetpack Compose
Hilt
Room
DataStore
Coroutines
Flow
GitHub Actions
```

Yang paling menarik adalah arsitekturnya sudah feature-oriented:

```text
XScreen
XUiState
XViewModel
XDomain
```

dan memiliki astronomical prayer calculator offline, alarm scheduling, boot rescheduling, serta dua home-screen widgets. ([GitHub][15])

**Nilai referensi: 9.5/10**

---

### 3. `batoulapps/adhan-kotlin`

Ini bukan aplikasi Muslim lengkap, tetapi menurut saya harus dijadikan **core calculation reference**.

Kelebihannya:

```text
✓ Kotlin Multiplatform
✓ high precision astronomical calculations
✓ calculation methods
✓ madhab
✓ custom adjustments
✓ high latitude rules
✓ SunnahTimes
✓ unit-test friendly
```

Repository ini menggunakan persamaan astronomis yang mengacu pada *Astronomical Algorithms* karya Jean Meeus. ([GitHub][2])

Lisensi **MIT**, jauh lebih fleksibel untuk dipakai sebagai dependency dibanding menyalin source aplikasi GPL/AGPL. ([GitHub][2])

**Nilai referensi: 10/10 untuk prayer engine**

---

### 4. `olcayertas/prayer-time`

Ini project 2026 yang menarik karena Android-nya sudah modern:

```text
Kotlin
Jetpack Compose
Material 3
Hilt
domain module
app module
exact alarms
boot reschedule
Glance Widget
automatic location
Qibla
R8
```

Bagian Android memisahkan `:domain` murni Kotlin/JVM dari `:app`, sehingga logic jadwal dan Qibla bisa dites tanpa Android framework. ([GitHub][16])

Lisensi **MIT**. ([GitHub][16])

**Nilai referensi: 9/10**

---

### 5. `yshalsager/SuntimesPrayerTimesAddon`

Saya terutama merekomendasikannya untuk mempelajari **widget + countdown + alarm event architecture**.

Repository ini mempunyai:

```text
prayer timeline
multiple locations
calculation profiles
alarm events
offset reminders
home widget
next-prayer highlighting
live remaining-time countdown
persistent prayer notification
Compose UI
RTL
```

Widget-nya bahkan menggunakan live `Chronometer` countdown pada launcher yang mendukung, yang sangat relevan dengan kebutuhan widget realtime Anda. ([GitHub][17])

Lisensi **GPL-3.0-only**. ([GitHub][17])

**Nilai referensi: 8.5/10**

---

# Rekomendasi stack final

Kalau saya yang membangun project Anda, saya akan menggunakan:

```text
Kotlin
+
Jetpack Compose Material 3
+
MVVM + UDF + Clean Architecture
+
Hilt
+
Coroutines / Flow
+
Adhan Kotlin
+
FusedLocationProviderClient
+
DataStore
+
Room
+
AlarmManager
+
BroadcastReceiver
+
WorkManager
+
NotificationCompat
+
Jetpack Glance
+
RemoteViews Chronometer
+
Media3
+
JUnit
+
Turbine
+
Compose UI Test
+
Detekt
+
Ktlint / Spotless
+
Android Lint
```

Dengan alur production:

```text
LOCATION
   ↓
PRAYER CALCULATOR
   ↓
PRAYER REPOSITORY
   ↓
┌─────────────┬───────────────┐
↓             ↓               ↓
UI        ALARM MANAGER     WIDGET
↓             ↓               ↓
Compose   Notification    Glance
              ↓               ↓
           Adhan       Chronometer
```

**Repository pertama yang saya sarankan Anda clone dan pelajari adalah `meypod/al-azan-compose`, kemudian kombinasikan pola arsitektur `Hidaya`, prayer engine `adhan-kotlin`, dan pendekatan Glance/Alarm dari `olcayertas/prayer-time`.** Dengan kombinasi ini Anda sudah mempunyai fondasi yang sangat kuat untuk membuat aplikasi jadwal sholat Android modern tanpa perlu membuat arsitektur terlalu kompleks. ([GitHub][14])

[1]: https://developer.android.com/topic/architecture?utm_source=chatgpt.com "Guide to app architecture"
[2]: https://github.com/batoulapps/adhan-kotlin "GitHub - batoulapps/adhan-kotlin: High precision Islamic prayer time library for Java · GitHub"
[3]: https://temanggung.kemenag.go.id/bimbingan-masyarakat-islam/standar-baku-hisab-rukyat-dalam-pelatihan-perhitungan-jadwal-shalat/?utm_source=chatgpt.com "Standar Baku Hisab Rukyat Dalam Pelatihan Perhitungan ..."
[4]: https://developer.android.com/develop/sensors-and-location/location/permissions?utm_source=chatgpt.com "Request location permissions | Sensors and location"
[5]: https://developer.android.com/develop/background-work/services/alarms?utm_source=chatgpt.com "Schedule alarms | Background work"
[6]: https://developer.android.com/about/versions/14/changes/schedule-exact-alarms?utm_source=chatgpt.com "Schedule exact alarms are denied by default"
[7]: https://support.google.com/googleplay/android-developer/answer/16909972?hl=en&utm_source=chatgpt.com "Permissions and APIs that Access Sensitive Information"
[8]: https://developer.android.com/develop/ui/compose/notifications/notification-permission?utm_source=chatgpt.com "Notification runtime permission | Jetpack Compose"
[9]: https://developer.android.com/reference/kotlin/android/app/AlarmManager?utm_source=chatgpt.com "AlarmManager | API reference"
[10]: https://developer.android.com/develop/ui/compose/glance?utm_source=chatgpt.com "Jetpack Glance | Jetpack Compose"
[11]: https://developer.android.com/develop/ui/views/appwidgets/advanced?utm_source=chatgpt.com "Create an advanced widget | Views"
[12]: https://developer.android.com/reference/kotlin/android/widget/Chronometer?utm_source=chatgpt.com "Chronometer | API reference"
[13]: https://developer.android.com/develop/background-work/background-tasks/persistent?utm_source=chatgpt.com "Task scheduling | Background work"
[14]: https://github.com/meypod/al-azan-compose "GitHub - meypod/al-azan-compose: Privacy focused ad-free open-source muslim Adhan (islamic prayer times) and qibla app · GitHub"
[15]: https://github.com/BassamAlim/Hidaya "GitHub - BassamAlim/Hidaya: An Islamic Android application with a wide range of features · GitHub"
[16]: https://github.com/olcayertas/prayer-time "GitHub - olcayertas/prayer-time: Prayer time app for masOS · GitHub"
[17]: https://github.com/yshalsager/SuntimesPrayerTimesAddon "GitHub - yshalsager/SuntimesPrayerTimesAddon: Prayer times, prohibited (makruh) windows, and night portions as a SuntimesWidget Android app addon. · GitHub"
