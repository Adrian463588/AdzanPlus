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
