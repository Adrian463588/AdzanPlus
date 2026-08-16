# Product Requirements Document (PRD) — AdzanNotif v2

## 1. Executive Summary
**AdzanNotif v2** is a modern, privacy-focused, offline-first Islamic prayer time and Adhan reminder mobile application built with Jetpack Compose, Kotlin Multiplatform (KMP), and Clean Architecture. It provides accurate prayer calculations, reliable exact alarms, notification alerts, and a real-time battery-efficient home screen widget.

---

## 2. Core Objectives & Value Proposition
- **100% Offline Capability**: Eliminates reliance on external prayer schedule APIs by computing astronomical solar positions locally on-device.
- **Accurate Adhan Alarm Engine**: Leverages Android `AlarmManager` exact alarms with Doze mode resistance, boot recovery, and audio playback.
- **Battery-Friendly Live Widget**: Uses Jetpack Compose Glance combined with RemoteViews `Chronometer` for real-time countdowns without background worker polling per second.
- **Responsive Material 3 Design**: Supports compact phones, foldable devices, and tablets with seamless adaptive UI layouts.
- **Privacy & Permissions**: Zero tracking, no mandatory internet permission, optional GPS location with offline city database fallback.

---

## 3. Target Audience & Personas
1. **Daily Muslim Commuters & Workers**: Need punctual notifications for all 5 daily prayers (Subuh, Dzuhur, Ashar, Maghrib, Isya) and pre-adhan reminders.
2. **Travelers / Offline Users**: Need instant schedule updates when changing locations or traveling in areas without internet connectivity.
3. **Widget Enthusiasts**: Want quick glanceable prayer countdowns directly on the home screen.

---

## 4. Functional Requirements

### 4.1. Prayer Time Calculation Engine (KMP Shared Core)
- **Calculation Methods**:
  - **Kemenag RI (Indonesia Standard)**: Fajr 20°, Isha 18°, with configurable default ihtiyath (+2 mins).
  - **Muslim World League (MWL)**: Fajr 18°, Isha 17°.
  - **Umm Al-Qura (Makkah)**: Fajr 18.5°, Isha 90 min interval after Maghrib (120 min in Ramadan).
  - **Egyptian General Authority of Survey**: Fajr 19.5°, Isha 17.5°.
  - **University of Islamic Sciences, Karachi**: Fajr 18°, Isha 18°.
  - **Islamic Society of North America (ISNA)**: Fajr 15°, Isha 15°.
  - **Majlis Ugama Islam Singapura (MUIS)**: Fajr 20°, Isha 18°.
  - **Custom Method**: Custom Fajr & Isha angles and intervals.
- **Madhab Configuration**: Shafi/Hanbali/Maliki (standard shadow factor = 1) vs Hanafi (shadow factor = 2).
- **High Latitude Adjustments**: Middle of Night, 1/7th of Night, Angle-based.
- **Additional Times**: Sunrise (Syuruq), Sunset (Ghurub), Imsak (-10 min before Fajr), Midnight, and Tahajjud (last third of the night).
- **Per-Prayer Minute Adjustments**: Ability to add/subtract minutes per prayer for local mosque calibration.
- **Qibla Direction**: Great-circle bearing calculation from any coordinates to the Kaaba (21.4225° N, 39.8262° E).

### 4.2. Location & Coordinate Resolution
- **GPS / Fused Location Provider**: One-shot coordinate fetch when enabled.
- **Offline City Database**: Embedded offline database containing major cities across Indonesian provinces and global capitals.
- **Manual Coordinate Input**: Latitude and longitude manual override.

### 4.3. Notification & Alarm System
- **Notification Channels**:
  - `Adzan Alerts` (High Importance, Fullscreen Intent, Custom Adhan sound / vibration).
  - `Prayer Reminders` (Default Importance, 10/15 min pre-adhan notice).
- **Exact Alarm Manager**:
  - Uses `setExactAndAllowWhileIdle()` to fire accurately at the exact prayer minute.
  - Proper runtime handling of `SCHEDULE_EXACT_ALARM` on Android 12/13/14+.
- **Boot & Time Change Rescheduling**:
  - `BootReceiver` listens to `ACTION_BOOT_COMPLETED` and `ACTION_LOCKED_BOOT_COMPLETED`.
  - `TimeChangeReceiver` listens to `ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED`.
- **WorkManager Reconciliation**: Periodic daily check (00:05) to re-verify scheduled alarms and update local database cache.
- **Audio Adhan Playback**:
  - Built-in authentic Adhan recordings (Makkah, Madinah, Al-Aqsa, Egyptian, Fajr special).
  - Device custom audio picker.
  - Auto-silence DND (Do Not Disturb) mode after prayer call.

### 4.4. Real-Time Home Screen Widget
- Built with **Jetpack Compose Glance**.
- Sizes:
  - **Compact (2x2)**: Next prayer name, target time, live count-down.
  - **Medium / Detailed (4x2 / 4x3)**: Location name, next prayer hero card, full 5-prayer schedule timeline.
- Countdown mechanism uses **RemoteViews `Chronometer` (countDown mode)** to avoid battery-draining per-second polling workers.

### 4.5. User Interface & Screen Flows
- **Home Screen**:
  - Header: Location name & Hijri/Gregorian date.
  - Next Prayer Hero Card with live ticking countdown.
  - Prayer Times List (Subuh, Terbit, Dzuhur, Ashar, Maghrib, Isya) with quick toggle for alarm/notification status.
- **Schedule Screen**: Monthly calendar view with 30-day offline computed prayer times.
- **Qibla Compass Screen**: Compass view showing compass heading and Kaaba direction.
- **Settings Screen**:
  - Calculation method, Madhab, per-prayer minute adjustments.
  - Audio tone selection per prayer.
  - Pre-adhan reminder offsets (e.g. 5, 10, 15 minutes before).
  - Theme selection (System / Light / Dark).
- **Fullscreen Alarm Activity**: Fullscreen dismiss/snooze dialog when Adhan alarm fires while device is locked.

---

## 5. Non-Functional Requirements
- **Performance**: Instant launch (< 500ms), prayer calculation execution (< 5ms per day).
- **Battery Efficiency**: Minimal battery consumption (< 0.5% daily), no continuous background CPU loops.
- **Architecture**: Clean Architecture + MVVM + Unidirectional Data Flow (UDF).
- **Code Quality**: SOLID, DRY, modular structure, strict linting, zero warnings.
- **Compatibility**: Android API 24 (Android 7.0) through Android 15+.

---

## 6. Sprint 2 — Celestial Astronomy Features

### 6.1. Astronomy Engine (`:core-astronomy` KMP Module)
- **Pure Kotlin, zero `android.*` imports** — fully testable on JVM/iOS.
- **Primary engine**: Ported astronomical algorithms from reference projects (Astronomy Engine / SuntimesWidget algorithms, MIT license) providing:
  - Sun azimuth, altitude, rise, set, solar noon for any observer lat/lon/date.
  - Moon azimuth, altitude, rise, set, transit, illumination, phase, distance, apogee/perigee.
  - Civil, nautical, and astronomical twilight begin/end times.
  - Golden Hour and Blue Hour windows (morning & evening) via `PhotoPhasePolicy` (altitude threshold classification).
  - Star positions (RA/Dec → azimuth/altitude for observer + sidereal time).
  - Hijri date conversion (Umm al-Qura algorithm).
- **Star catalog**: ~500 bright stars (magnitude ≤ 4.5) + 40 major constellations, embedded as JSON asset.
- **Accuracy targets**: Sun rise/set ≤ ±2 min of NOAA reference; Moon phase name exact; Hijri date matches Kemenag official calendar.

### 6.2. Solar Phase Classification
```
NIGHT → ASTRONOMICAL_TWILIGHT → NAUTICAL_TWILIGHT → BLUE_HOUR → CIVIL_TWILIGHT → GOLDEN_HOUR → DAY
```
- **Golden Hour**: Solar altitude between −4° and +6° (rise/set transitions).
- **Blue Hour**: Solar altitude between −6° and −4° (deeper twilight).
- Each threshold defined in `PhotoPhasePolicy` — fully unit-tested, independent of UI.

### 6.3. Astronomy Dashboard Screen
- Live solar phase badge (Golden Hour / Day / Twilight / Night) updated every 60 seconds (foreground only).
- Current Sun azimuth/altitude display.
- Current Moon phase icon + illumination % + moonrise/set times.
- Visual timeline bar showing morning/evening golden & blue hour windows for the day.
- Navigation tiles to: Moon Detail, Sun Detail, Star Map, Hijri Calendar.
- Fully offline — all data from `:core-astronomy`.

### 6.4. Moon Detail Screen
- **Animated Moon phase illustration** (Canvas-drawn crescent/gibbous/full shape, not a static image).
- Phase name (New Moon → Waxing Crescent → First Quarter → Waxing Gibbous → Full Moon → Waning Gibbous → Last Quarter → Waning Crescent).
- Illumination percentage, age in days since last New Moon.
- Moonrise, Transit, Moonset times with azimuth at rise/set.
- Current distance in km, Apogee/Perigee indicator.
- 30-day phase mini-calendar (phase icon per day for current month).

### 6.5. Sun Detail Screen
- **Sun arc visualization** (Canvas arc from rise azimuth through solar noon to set azimuth).
- Layered twilight band timeline: Golden Hour (morning & evening), Blue Hour (morning & evening), Civil, Nautical, Astronomical twilight.
- Key times: Sunrise, Solar Noon, Sunset, Civil/Nautical/Astronomical dusk.
- Live Sun altitude indicator and current solar phase badge.

### 6.6. Star Map / Constellation Viewer Screen
- **2D Sky Chart** (polar projection, Canvas-rendered):
  - ~500 bright stars drawn as dots (size proportional to magnitude).
  - 40 major constellation stick-figure lines.
  - Constellation labels (Orion, Scorpius, Leo, Ursa Major, etc.).
  - Sun & Moon positions marked with icons.
  - Horizon line with cardinal direction labels (N, E, S, W).
- **Interactive**: pinch-to-zoom, drag-to-pan, time slider (simulate sky at any hour).
- **Fully offline**: star catalog bundled as JSON asset in APK.

### 6.7. Hijri / Gregorian Dual Calendar Screen
- Month grid (7-column): each day cell shows Gregorian day + Hijri day + prayer dots + Moon phase mini-icon + Golden Hour indicator.
- Day-detail bottom sheet: full prayer times + Sun events (rise/set/golden hour) + Moon phase for selected day.
- Month navigation: swipe or arrow buttons (forward/back).
- Hijri month name displayed prominently alongside Gregorian month name.
- All data computed offline from `:core-prayer` + `:core-astronomy`.

### 6.8. Celestial Notification System
- New notification channel: `Celestial Events` (Default importance).
- User-configurable alerts:
  - Golden Hour Start (morning & evening) — notify N minutes before.
  - Blue Hour Start (morning & evening).
  - Moonrise / Moonset.
  - Full Moon (day-before reminder).
  - New Moon (day-before reminder).
- Uses `AlarmManager.setExactAndAllowWhileIdle()` (same pattern as prayer alarms).
- `CelestialAlarmReceiver` handles broadcast → `NotificationHelper.showCelestialEventNotification()`.

### 6.9. Moon Widget & Sun Widget (Jetpack Glance)
- **Moon Widget (2×2)**:
  - Moon phase icon + phase name.
  - Illumination %.
  - Next moonrise countdown (RemoteViews Chronometer).
- **Sun Widget (2×2)**:
  - Sun icon + current solar phase badge.
  - Next key event (Golden Hour / Sunset).
  - Countdown to next event (RemoteViews Chronometer).

### 6.10. Offline & Online Operation
- All celestial calculations: 100% offline via `:core-astronomy` KMP engine.
- No mandatory internet for any feature. GPS location optional (fallback to saved city or manual coordinates).
- Daily WorkManager job caches next 7 days of celestial events and refreshes widget data.
- When network is available (optional future): nothing changes — no online celestial data fetching is planned.
