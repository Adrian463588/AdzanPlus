Untuk aplikasi Android **Moon/Sun photography planner** seperti Moon Pro/PhotoPills—menampilkan posisi Bulan dan Matahari berdasarkan waktu/lokasi, fase Bulan, moonrise/moonset, sunrise/sunset, golden hour, blue hour, notifikasi, dan widget—saya menyarankan arsitektur **offline-first astronomical engine**. Posisi Bulan/Matahari tidak perlu diambil dari API setiap detik; aplikasi dapat menghitungnya secara lokal dari `timestamp + latitude + longitude + elevation`. Astronomy Engine bahkan menyediakan posisi horizon/toposentrik, rise/set, twilight, fase Bulan, apogee/perigee, dan Kotlin/JVM API dengan klaim akurasi sekitar ±1 arcminute serta validasi terhadap JPL Horizons/NOVAS. ([GitHub][1])

## Stack yang saya rekomendasikan

| Layer                     | Stack                                    | Fungsi                      |
| ------------------------- | ---------------------------------------- | --------------------------- |
| Language                  | **Kotlin**                               | Native Android              |
| UI                        | **Jetpack Compose + Material 3**         | UI modern/adaptive          |
| Architecture              | **Clean Architecture + MVVM/UDF**        | separation of concerns      |
| DI                        | **Hilt**                                 | dependency injection        |
| Async                     | **Coroutines + Flow/StateFlow**          | realtime state              |
| Astronomy engine          | **Astronomy Engine Kotlin/JVM**          | Sun/Moon ephemeris utama    |
| Solar phase helper        | **Kastro**                               | golden/blue hour + twilight |
| Date/time                 | `java.time` / `kotlinx-datetime`         | timezone                    |
| Location                  | **FusedLocationProviderClient**          | observer coordinates        |
| Device direction          | `SensorManager` + `TYPE_ROTATION_VECTOR` | compass/AR direction        |
| Maps                      | Google Maps Compose atau MapLibre        | photography planning        |
| Local settings            | **DataStore**                            | preferences                 |
| Cached planner            | **Room**                                 | saved locations/plans       |
| Exact alerts              | **AlarmManager**                         | golden hour/moonrise alerts |
| Background reconciliation | **WorkManager**                          | rebuild schedule/cache      |
| Widget                    | **Jetpack Glance**                       | Sun/Moon widgets            |
| Camera overlay opsional   | **CameraX**                              | AR Sun/Moon finder          |
| Testing                   | JUnit + Turbine + Compose Test           | testing                     |
| Quality                   | Detekt + Ktlint/Spotless + Android Lint  | clean code                  |

Android sendiri saat ini merekomendasikan Compose, layered architecture, UDF, repositories, dan Hilt/manual DI sebagai pola aplikasi modern. ([Android Developers][2])

---

# 1. Core Astronomy Engine

Untuk aplikasi seperti ini saya **lebih memilih Astronomy Engine dibanding hanya SunCalc sederhana**.

Input:

```text
Observer
├── Latitude
├── Longitude
├── Elevation
└── Timezone

+
Timestamp
```

Output:

```text
SUN
├── azimuth
├── altitude
├── sunrise
├── sunset
├── solar noon
├── civil twilight
├── nautical twilight
└── astronomical twilight

MOON
├── azimuth
├── altitude
├── moonrise
├── moonset
├── illumination
├── phase
├── distance
├── libration
├── apogee
└── perigee
```

Astronomy Engine mendukung observer latitude/longitude/elevation, apparent horizon position, atmospheric refraction, rise/set, twilight serta Moon phases langsung di Kotlin/JVM. ([GitHub][1])

### Pilihan engine

Saya akan memakai:

```text
PRIMARY
Astronomy Engine
        ↓
high-accuracy Sun/Moon position

SECONDARY
Kastro
        ↓
photography phase abstraction
Golden Hour
Blue Hour
Civil Twilight
etc.
```

Kastro sudah Kotlin Multiplatform, tersedia melalui Maven Central, serta secara eksplisit menyediakan solar phases seperti **blue hour, golden hour, civil/nautical/astronomical twilight, sunrise dan sunset**. Dokumentasinya menyatakan perhitungan umumnya berada dalam kisaran sekitar satu menit, dengan event lunar tertentu bisa meleset beberapa menit. ([GitHub][3])

Jadi untuk aplikasi fotografi saya tidak akan menjadikan Kastro sebagai satu-satunya ephemeris presisi. Gunakan Astronomy Engine sebagai sumber posisi utama dan Kastro/policy Anda sendiri untuk klasifikasi photographic light phase.

---

# 2. Realtime Sun & Moon

"Realtime" sebaiknya berarti:

```text
System clock
    ↓
Instant.now()
    ↓
AstronomyEngine
    ↓
SunPosition + MoonPosition
    ↓
StateFlow
    ↓
Compose
```

Saat aplikasi **foreground**, update misalnya setiap:

```text
1 second
```

untuk:

```text
Sun Azimuth       278.4°
Sun Altitude        8.2°

Moon Azimuth      113.7°
Moon Altitude      32.8°
```

Tetapi jangan menjalankan foreground/background service terus-menerus hanya untuk menghitung posisi celestial object.

Semua posisi bisa direkonstruksi dari waktu saat itu.

---

# 3. Sun Path

Screen utama bisa menggunakan grafik horizon:

```text
             ☀
          ╱     ╲
       ╱           ╲
─────🌅─────────────🌇─────
     Sunrise      Sunset

06:04    12:01      17:58
```

Dengan timeline:

```text
05:00 ─────●──────────────── 19:00
           ↑
         06:30
```

Saat user drag slider:

```text
06:30
06:35
06:40
...
17:30
```

Anda calculate ulang:

```kotlin
SunPosition(time)
MoonPosition(time)
```

tanpa network request.

Ini adalah fitur yang sangat penting untuk photography planner.

---

# 4. Moon Path

Moon screen:

```text
Waxing Crescent
Illumination 23%

                 🌙
              ╱
           ╱
─────────╱─────────────────

Moonrise       09:43
Transit        16:22
Moonset        22:58

Azimuth        241°
Altitude       37°
Distance       392,412 km
```

Astronomy Engine juga menyediakan phase search, rise/set, apogee/perigee serta libration, sehingga engine yang sama bisa dipakai untuk fitur lunar lanjutan. ([GitHub][1])

---

# 5. Photography Planner — fitur utama

Menurut saya ini justru bagian terpenting aplikasinya.

User memilih:

```text
Photography Location
        ↓
Date
        ↓
Time
        ↓
Target direction
```

Misalnya:

```text
📍 Bromo

18 August 2026

Golden Hour
05:31 – 06:16

Sunrise
05:45

Blue Hour
05:08 – 05:31

Moonrise
21:42
```

Kemudian map:

```text
                Moon
                 🌙
                 │ 118°
                 │
                 │
                 ● Photographer
                ╱
               ╱ 73°
              ☀
             Sun
```

Buat dua bearing ray:

```text
Sun azimuth  = 73°
Moon azimuth = 118°
```

Ini memungkinkan fotografer memilih **arah kamera**, bukan hanya mengetahui jam sunrise.

---

# 6. Golden Hour & Blue Hour Engine

Jangan menyimpan jam seperti:

```text
Golden hour = 06:00 ❌
```

Karena bergantung pada:

```text
latitude
longitude
date
timezone
solar elevation
```

Gunakan:

```text
SolarPosition(time)
        ↓
Solar altitude
        ↓
PhotographyPhasePolicy
```

Contohnya domain model:

```kotlin
enum class SolarPhase {
    NIGHT,
    ASTRONOMICAL_TWILIGHT,
    NAUTICAL_TWILIGHT,
    BLUE_HOUR,
    CIVIL_TWILIGHT,
    GOLDEN_HOUR,
    DAY
}
```

Kastro sudah menyediakan konsep blue dan golden hour secara langsung, sementara Suntimes juga menghitung blue/golden hour bersamaan dengan civil, nautical, dan astronomical twilight. ([GitHub][3])

Untuk production, definisikan sendiri `PhotographyPhasePolicy`, supaya threshold bisa diuji dan nantinya diubah tanpa mengganti astronomy engine.

---

# 7. AR Sun/Moon Finder

Ini fitur yang akan membuat aplikasi Anda terasa seperti aplikasi photography planner premium.

```text
Camera Preview

            🌙
             │
             │
       predicted moon
           location

────────────────── Horizon

                ☀
          predicted Sun
```

Stack:

```text
CameraX
+
SensorManager
+
TYPE_ROTATION_VECTOR
+
AstronomyEngine
```

Flow:

```text
Astronomy Engine
        ↓
Celestial azimuth/altitude
        ↓

Rotation Vector
        ↓
Phone orientation
        ↓

Coordinate Transformation
        ↓
Compose/Canvas overlay
        ↓
CameraX Preview
```

Android menyediakan rotation-vector sensor untuk merepresentasikan orientasi perangkat; dokumentasinya juga mengingatkan sensor yang tidak diperlukan harus dihentikan ketika Activity pause karena sensor aktif terus dapat memboroskan baterai. ([Android Developers][4])

**Jangan mulai dengan ARCore** untuk MVP. Untuk Sun/Moon finder sederhana, CameraX + orientation sensor biasanya sudah cukup. ARCore bisa ditambahkan kemudian bila Anda membutuhkan world anchoring/3D yang lebih kompleks.

---

# 8. Location

Gunakan:

```text
FusedLocationProviderClient
```

Hanya ambil:

```text
latitude
longitude
altitude/elevation
timezone
```

dan simpan lokasi favorit:

```text
Bromo
Borobudur
Kuta
Raja Ampat
Jakarta
```

Fused Location Provider memang direkomendasikan Android untuk location-aware apps dan mengelola underlying location technology dengan mempertimbangkan akurasi serta konsumsi daya. ([Android Developers][5])

Tidak perlu meminta background location untuk MVP.

---

# 9. Notification System

User misalnya mengaktifkan:

```text
Golden Hour
✓ notify 30 min before

Blue Hour
✓ notify 15 min before

Sunrise
✓ notify

Sunset
✓ notify

Moonrise
✓ notify

Full Moon
✓ notify one day before
```

Architecture:

```text
AstronomyEngine
      ↓
EventGenerator
      ↓
PhotographyEvent
      ↓
AlarmScheduler
      ↓
AlarmManager
      ↓
BroadcastReceiver
      ↓
Notification
```

Gunakan **AlarmManager** untuk event yang memang dipilih user dan perlu terjadi pada waktu yang tepat. Android menyatakan exact alarms ditujukan untuk aksi/notifikasi yang waktunya presisi dan user-intentioned. ([Android Developers][6])

Contoh event:

```text
17:08
Golden Hour akan dimulai 30 menit lagi

17:38
Golden Hour dimulai

18:02
Sunset dalam 10 menit

18:12
Sunset

18:19
Blue Hour dimulai
```

Pada Android modern, periksa:

```kotlin
alarmManager.canScheduleExactAlarms()
```

dan berikan fallback jika exact alarm permission belum tersedia. ([Android Developers][7])

---

# 10. WorkManager bukan timer realtime

WorkManager hanya untuk maintenance:

```text
DailyWorker
     ↓
calculate next 7 days
     ↓
cache photography events
     ↓
verify alarms
     ↓
refresh widget
```

Jangan:

```text
WorkManager every minute ❌
```

atau:

```text
Foreground Service 24/7 ❌
```

---

# 11. Widget

Gunakan **Jetpack Glance**. Glance adalah framework Compose-based untuk AppWidgets. ([Android Developers][8])

Saya sarankan 3 widget.

### Sun Widget

```text
┌──────────────────────┐
│ ☀ SUN                │
│                      │
│ Sunset        18:12  │
│ Golden        17:38  │
│ Blue          18:19  │
│                      │
│ ↓ 32 min             │
└──────────────────────┘
```

### Moon Widget

```text
┌──────────────────────┐
│ 🌙 MOON              │
│                      │
│ Waxing Crescent      │
│ Illumination     27% │
│ Moonrise       21:43 │
│ Azimuth          118°│
└──────────────────────┘
```

### Photography Widget

```text
┌────────────────────────┐
│ 📸 BEST LIGHT          │
│                        │
│ Golden Hour            │
│ 17:38 – 18:12          │
│                        │
│ Starts in 01:24        │
└────────────────────────┘
```

Jangan mencoba redraw widget setiap detik. Android membatasi `updatePeriodMillis` menjadi minimal 30 menit dan secara eksplisit memperingatkan agar widget tidak diperbarui setiap menit di background karena konsumsi baterai. ([Android Developers][9])

Jika membutuhkan countdown yang terlihat bergerak tanpa redraw terus-menerus, Glance dapat meng-embed `RemoteViews`, sementara `RemoteViews` menyediakan `setChronometer`. ([Android Developers][10])

---

# 12. Arsitektur project

Saya sarankan:

```text
app
│
├── core
│   ├── astronomy
│   ├── location
│   ├── sensors
│   ├── map
│   ├── database
│   ├── datastore
│   ├── notifications
│   └── widget
│
├── domain
│   ├── model
│   ├── repository
│   └── usecase
│
├── data
│   ├── astronomy
│   ├── location
│   ├── local
│   └── repository
│
└── feature
    ├── dashboard
    ├── sun
    ├── moon
    ├── planner
    ├── map
    ├── ar
    ├── calendar
    └── settings
```

Core model:

```text
ObserverLocation

CelestialPosition
├── azimuth
├── altitude
└── distance

SunState
MoonState

SolarPhase

PhotographyWindow

AstronomyEvent
├── Sunrise
├── Sunset
├── Moonrise
├── Moonset
├── GoldenHourStart
├── GoldenHourEnd
├── BlueHourStart
├── BlueHourEnd
├── FullMoon
└── NewMoon
```

Ini membuat astronomy math **tidak bergantung kepada Compose, Activity, ViewModel maupun Android UI**, sehingga mudah di-unit-test.

---

# 13. Top 5 GitHub repository untuk referensi

| Rank  | Repository                    | Fokus                                          |
| ----- | ----------------------------- | ---------------------------------------------- |
| **1** | `forrestguice/SuntimesWidget` | Sun/Moon + golden/blue hour + alarms + widgets |
| **2** | `Hamza417/Positional`         | Sun/Moon position + compass + widgets          |
| **3** | `davemorrissey/sundroid`      | Sun/Moon tracker + map + widgets               |
| **4** | `sky-map-team/stardroid`      | realtime sky/device orientation                |
| **5** | `phototime/solarized-android` | golden/blue hour calculation                   |

### #1 — `forrestguice/SuntimesWidget`

**Referensi paling dekat dengan kebutuhan Anda.**

Sudah memiliki:

```text
Sunrise
Sunset
Golden hour
Blue hour

Civil twilight
Nautical twilight
Astronomical twilight

Moonrise
Moonset
Moon phases
Moon illumination

Sun position
Moon position

Countdown
Alarm
Notification
Widgets
```

Bahkan ada proyeksi current sunlight/moonlight ke world map serta custom rise/set event berdasarkan angle. ([GitHub][11])

**Nilai referensi: 10/10**

---

### #2 — `Hamza417/Positional`

Sangat bagus untuk referensi **realtime direction + compass + Sun/Moon**.

Fitur repository mencakup Sun azimuth/altitude, sunrise/sunset/twilight serta Moon position, moonrise/moonset, altitude, phase, illumination, dan widgets. Project Android ini mayoritas Kotlin dan menggunakan MVVM. ([GitHub][12])

Cocok untuk mempelajari:

```text
SensorManager
Compass
Azimuth
Altitude
Sun visualization
Moon visualization
Widgets
```

**Nilai: 9.5/10**

---

### #3 — `davemorrissey/sundroid`

Sundroid mempunyai:

```text
Sunrise / sunset
Moonrise / moonset
Moon phase
Moon illumination
Planet rise/set
Map tracker
Saved locations
Monthly calendar
4 widgets
```

dan mempunyai **Sun, Moon and planet tracker with map view**. ([GitHub][13])

Arsitekturnya tidak saya jadikan template utama untuk aplikasi baru karena project memiliki sejarah panjang/legacy, tetapi algoritma dan UX-nya sangat berguna sebagai referensi.

**Nilai: 8.5/10**

---

### #4 — `sky-map-team/stardroid`

Ini adalah **Sky Map**, awalnya Google Sky Map dan sekarang open-source serta masih dikelola komunitas. ([GitHub][14])

Gunakan repo ini terutama untuk belajar:

```text
device orientation
sensor fusion
sky coordinates
celestial projection
screen coordinate conversion
pointing/sky navigation
```

Bukan sebagai template Clean Architecture modern; maintainers sendiri menyebut arsitekturnya masih mencerminkan sejarah project dan sedang menuju modernisasi. ([GitHub][14])

**Nilai untuk AR/sky orientation: 9/10**

---

### #5 — `phototime/solarized-android`

Library Android Kotlin khusus untuk:

```text
Sun phases
Golden Hour
Blue Hour
Sunrise
Sunset
Twilight
```

Repository memang secara eksplisit dideskripsikan sebagai Android library untuk menghitung sun phases termasuk golden hour dan blue hour. ([GitHub][15])

Jangan jadikan engine astronomi utamanya karena repositorinya lebih lama, tetapi sangat bagus untuk mempelajari domain model photography-light calculation.

**Nilai: 8/10**

---

## Dua library tambahan yang justru paling penting

Walaupun bukan full Android app, saya akan menambahkan dua ini ke referensi utama development:

**`cosinekitty/astronomy` → engine utama.** Mendukung Kotlin/JVM, Sun/Moon topocentric positions, rise/set, twilight, lunar phase, libration, eclipse, apogee/perigee dan banyak fungsi lain. ([GitHub][1])

**`yoxjames/Kastro` → Kotlin helper.** API-nya jauh lebih Kotlin-friendly dan langsung mempunyai golden hour/blue hour serta Sun/Moon events. ([GitHub][3])

---

# Stack final yang saya pilih

Untuk project Anda, kombinasi terbaik menurut saya:

```text
Kotlin
+
Jetpack Compose Material 3
+
Clean Architecture
+
MVVM / UDF
+
Hilt
+
Coroutines / StateFlow

------------------------

Astronomy Engine
       ↓
Sun + Moon ephemeris

Kastro
       ↓
Photography light phases

------------------------

Fused Location Provider
+
SensorManager / Rotation Vector
+
Google Maps Compose / MapLibre
+
CameraX

------------------------

Room
+
DataStore

------------------------

AlarmManager
+
BroadcastReceiver
+
WorkManager

------------------------

Jetpack Glance
+
RemoteViews Chronometer

------------------------

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

Dan alur aplikasinya:

```text
                GPS LOCATION
                     │
                     ▼
             ASTRONOMY ENGINE
              ┌──────┴──────┐
              ▼             ▼
             SUN           MOON
              │             │
              ▼             ▼
          Azimuth       Azimuth
          Altitude      Altitude
          Sunrise       Moonrise
          Sunset        Moonset
              │         Phase
              │         Illumination
              │             │
              └──────┬──────┘
                     ▼
             PHOTOGRAPHY ENGINE
                     │
        ┌────────────┼────────────┐
        ▼            ▼            ▼
   Golden Hour    Blue Hour    Planner
        │            │            │
        └────────────┼────────────┘
                     ▼
             EVENT SCHEDULER
              ┌──────┴──────┐
              ▼             ▼
         AlarmManager     Widget
              │             │
              ▼             ▼
       Notification       Glance
```

Untuk **MVP**, saya akan membangun hanya **Dashboard Sun/Moon → time slider → map Sun/Moon trajectory → golden/blue hour → moon phase → notification → 2–3 Glance widgets**. Setelah stabil, baru tambahkan **CameraX AR Finder**, Milky Way planner, weather/cloud-cover forecast, eclipse planner, dan advanced photography planning. Dengan begitu aplikasinya sudah sangat berguna tanpa menjadi over-engineered sejak versi pertama.

[1]: https://github.com/cosinekitty/astronomy "GitHub - cosinekitty/astronomy: Astronomy Engine: multi-language calculation of Sun, Moon, and planet positions. Predicts lunar phases, eclipses, transits, oppositions, conjunctions, equinoxes, solstices, rise/set times, and other events. Provides vector and angular coordinate transforms among equatorial, ecliptic, horizontal, and galactic orientations. · GitHub"
[2]: https://developer.android.com/topic/architecture?utm_source=chatgpt.com "Guide to app architecture  |  App architecture  |  Android Developers"
[3]: https://github.com/yoxjames/Kastro "GitHub - yoxjames/Kastro: A Kotlin Multiplatform library for calculating information about the sun and moon · GitHub"
[4]: https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion?utm_source=chatgpt.com "Motion sensors | Sensors and location"
[5]: https://developer.android.com/develop/sensors-and-location/location/retrieve-current?utm_source=chatgpt.com "Get the last known location | Sensors and location"
[6]: https://developer.android.com/develop/background-work/services/alarms?utm_source=chatgpt.com "Schedule alarms | Background work"
[7]: https://developer.android.com/about/versions/14/changes/schedule-exact-alarms?utm_source=chatgpt.com "Schedule exact alarms are denied by default"
[8]: https://developer.android.com/develop/ui/compose/glance/create-app-widget?utm_source=chatgpt.com "Create an app widget with Glance | Jetpack Compose"
[9]: https://developer.android.com/develop/ui/views/appwidgets/advanced?utm_source=chatgpt.com "Create an advanced widget  |  Views  |  Android Developers"
[10]: https://developer.android.com/reference/kotlin/androidx/glance/appwidget/AndroidRemoteViews.composable?utm_source=chatgpt.com "AndroidRemoteViews  |  API reference  |  Android Developers"
[11]: https://github.com/forrestguice/SuntimesWidget/blob/master/README.md "SuntimesWidget/README.md at master · forrestguice/SuntimesWidget · GitHub"
[12]: https://github.com/Hamza417/Positional?utm_source=chatgpt.com "GitHub - Hamza417/Positional: An elegant and colorful location information app for Android with Compass, Clock, Level, Sun, Moon, Trail Marker and many other features. · GitHub"
[13]: https://github.com/davemorrissey/sundroid "GitHub - davemorrissey/sundroid: Android sunrise/sunset app · GitHub"
[14]: https://github.com/sky-map-team/stardroid "GitHub - sky-map-team/stardroid: Sky Map (formerly Google Sky Map, open sourced in 2012) · GitHub"
[15]: https://github.com/topics/sunrise-sunset "sunrise-sunset · GitHub Topics · GitHub"
