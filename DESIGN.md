# Design System & UI Specifications (DESIGN.md) — AdzanNotif v2

## 1. Design Philosophy
- **Modern Islamic Elegance**: Clean, serene aesthetic combining deep forest emerald tones, soft gold/sand accents, and crisp typography.
- **Content-First Simplicity**: Immediate focus on the next upcoming prayer and countdown time.
- **Anti-Cliché Discipline**: No tacky purple-on-dark, no glowing border accents, no bloated bento boxes, no unnecessary decorative clutter.

---

## 2. Color Palette & Theming (Material 3 Tokens)

### 2.1. Light Theme
- `md_theme_light_primary`: `#1A6B51` (Deep Emerald Green)
- `md_theme_light_onPrimary`: `#FFFFFF`
- `md_theme_light_primaryContainer`: `#A4F2D2`
- `md_theme_light_onPrimaryContainer`: `#002117`
- `md_theme_light_secondary`: `#7A5900` (Warm Amber/Gold)
- `md_theme_light_onSecondary`: `#FFFFFF`
- `md_theme_light_secondaryContainer`: `#FFE089`
- `md_theme_light_onSecondaryContainer`: `#261900`
- `md_theme_light_background`: `#F8FAF8`
- `md_theme_light_onBackground`: `#191C1B`
- `md_theme_light_surface`: `#F8FAF8`
- `md_theme_light_onSurface`: `#191C1B`
- `md_theme_light_surfaceVariant`: `#DBE5DF`
- `md_theme_light_onSurfaceVariant`: `#3F4945`
- `md_theme_light_outline`: `#6F7975`

### 2.2. Dark Theme
- `md_theme_dark_primary`: `#88D6B7` (Luminous Sage Emerald)
- `md_theme_dark_onPrimary`: `#003829`
- `md_theme_dark_primaryContainer`: `#00513C`
- `md_theme_dark_onPrimaryContainer`: `#A4F2D2`
- `md_theme_dark_secondary`: `#FAC248` (Soft Gold)
- `md_theme_dark_onSecondary`: `#402D00`
- `md_theme_dark_secondaryContainer`: `#5C4300`
- `md_theme_dark_onSecondaryContainer`: `#FFE089`
- `md_theme_dark_background`: `#0F1513`
- `md_theme_dark_onBackground`: `#E1E3DF`
- `md_theme_dark_surface`: `#0F1513`
- `md_theme_dark_onSurface`: `#E1E3DF`
- `md_theme_dark_surfaceVariant`: `#3F4945`
- `md_theme_dark_onSurfaceVariant`: `#BFC9C3`
- `md_theme_dark_outline`: `#89938E`

---

## 3. Typography (Google Fonts: Plus Jakarta Sans / Roboto)
- **Display Large**: 57sp, tracking -0.25sp, weight Bold (used for huge Countdown Clock).
- **Headline Medium**: 28sp, tracking 0sp, weight SemiBold (used for Next Prayer Name).
- **Title Large**: 22sp, tracking 0sp, weight Medium (Section Headers, Card Titles).
- **Body Large**: 16sp, tracking 0.5sp, weight Regular (Prayer row text, times).
- **Label Large / Medium**: 14sp / 12sp, tracking 0.1sp, weight Medium (Badges, Buttons, Tabs).

---

## 4. Responsive Breakpoints & Adaptive Layouts
- **Compact (`width < 600dp`)**: Standard smartphone portrait. Single column layout with stacked hero card and vertical prayer list.
- **Medium (`600dp <= width < 840dp`)**: Foldables and small tablets. Dual-column or side-by-side hero card and monthly calendar snippet.
- **Expanded (`width >= 840dp`)**: Large tablets and desktop/landscape mode. Multi-pane layout: Navigation Rail + Hero card with interactive Qibla compass side-by-side with full schedule matrix.

---

## 5. Component Specifications

### 5.1. Next Prayer Hero Card
- Elevated card with subtle subtle gradient surface (`primaryContainer` with 12% elevation tint).
- Top row: Location chip + Hijri date label.
- Center: Prayer Name (e.g. `MAGHRIB`), Prayer target time (e.g. `18:05 WIB`), and dynamic live countdown (`-00:24:18`).
- Bottom row: Time until Imsak/Syuruq or upcoming prayer notification status.

### 5.2. Daily Prayer Times List Item
- Row layout:
  - Left: Icon (Sun/Moon phase) + Prayer title (Subuh, Terbit, Dzuhur, Ashar, Maghrib, Isya).
  - Center: Highlight pill when this is the current active prayer window.
  - Right: Formatted 24-hour time (`04:35`) + Notification sound/vibrate toggle icon button.

### 5.3. Home Screen Glance Widget
- **Compact Layout (2x2)**:
  - Background: Rounded container (16dp radius) matching dynamic Material You color tokens.
  - Content: Prayer Icon, Next Prayer Title, Target Time, and `RemoteViews.Chronometer` counting down in realtime.
- **Detailed Layout (4x2 / 4x3)**:
  - Left column: Next prayer banner + live Chronometer countdown + location.
  - Right column / bottom row: Mini table showing Subuh, Dzuhur, Ashar, Maghrib, Isya with current prayer highlighted.

### 5.4. Fullscreen Alarm Screen
- Displayed when device wakes up for Adhan.
- Shows peaceful mosque silhouette background, large animated prayer call title, current time, and a swipe-to-dismiss / snooze gesture.

---

## 6. Sprint 2 — Astronomy Design Tokens & Component Specs

### 6.1. Night Sky Color Palette (Astronomy Feature Theme)
- `astronomy_background_deep`: `#050A14` (Deep Navy Night)
- `astronomy_background_mid`: `#0B1525` (Night Sky Mid)
- `astronomy_surface`: `#111D30` (Dark Panel)
- `astronomy_star_white`: `#F0F4FF` (Star White)
- `astronomy_moon_gold`: `#FAC248` (Moon Gold — matches dark theme secondary)
- `astronomy_sun_amber`: `#FF9A3C` (Solar Amber)
- `astronomy_golden_hour`: `#FFB347` (Golden Hour Orange)
- `astronomy_blue_hour`: `#5B8FD4` (Blue Hour Blue)
- `astronomy_twilight_civil`: `#7CA5E0` (Civil Twilight)
- `astronomy_twilight_nautical`: `#3D6B9E` (Nautical Twilight)
- `astronomy_twilight_astro`: `#1F3A5C` (Astronomical Twilight)
- `astronomy_constellation_line`: `#3A5A8A` (Constellation Line, 60% opacity)
- `astronomy_horizon`: `#2A4A2A` (Horizon Tint — slight green tint matches app primary)

> Anti-Cliché: No glowing neon borders, no purple-on-dark, no grid-line overlays on star map background.

### 6.2. Astronomy Dashboard Card
- Background: `astronomy_surface` with 8dp corner radius and 1dp divider at bottom.
- **Solar Phase Badge**: Pill shape, color-coded:
  - Golden Hour: `astronomy_golden_hour` background, dark text.
  - Day: `#FFFDE7` background (very light yellow), dark text.
  - Civil Twilight: `astronomy_twilight_civil` background, white text.
  - Night: `astronomy_background_mid` background, `astronomy_star_white` text.
- **Timeline Bar**: Horizontal bar from 00:00 to 24:00. Colored bands for each phase.
  - Night = `astronomy_background_deep`, Astro = `astronomy_twilight_astro`, Nautical = `astronomy_twilight_nautical`, Civil = `astronomy_twilight_civil`, Blue = `astronomy_blue_hour`, Golden = `astronomy_golden_hour`, Day = `#FFF8E1`.

### 6.3. Moon Phase Illustration (Canvas-drawn, NOT bitmap)
- **Shape technique**: Draw illuminated limb using two ellipses (outer terminator arc + disk boundary), filled with `astronomy_moon_gold`.
- **Background**: Dark circle `astronomy_background_deep` representing the full moon disk.
- **Phase mapping**:
  - New Moon: filled dark circle only.
  - Waxing Crescent: dark disk, thin bright right crescent.
  - First Quarter: right half bright.
  - Waxing Gibbous: right 3/4 bright.
  - Full Moon: entire disk `astronomy_moon_gold`.
  - Waning Gibbous: left 3/4 bright.
  - Last Quarter: left half bright.
  - Waning Crescent: thin bright left crescent.
- **Size**: 120dp diameter in hero section; 24dp in 30-day mini calendar row.

### 6.4. Sun Arc Visualization (Canvas-drawn)
- **Arc path**: Parabolic arc from rise azimuth to set azimuth, peak at solar noon altitude.
- **Color gradient**: Subtle amber-to-white gradient along the arc.
- **Current position dot**: 12dp circle at current time on arc, with vertical drop line to timeline.
- **Twilight bands**: Horizontal color bands beneath the arc (Blue Hour, Golden Hour bands visible at horizon).

### 6.5. Star Map (Canvas-rendered 2D Polar Chart)
- **Background**: `astronomy_background_deep` (#050A14).
- **Star dots**:
  - Magnitude ≤ 1: 5dp radius, opacity 100%.
  - Magnitude 1–2: 4dp radius, opacity 90%.
  - Magnitude 2–3: 3dp radius, opacity 80%.
  - Magnitude 3–4.5: 2dp radius, opacity 65%.
  - Color: `astronomy_star_white` (#F0F4FF).
- **Constellation lines**: 1.5dp stroke, `astronomy_constellation_line` (#3A5A8A, 60% opacity).
- **Constellation labels**: Label Medium 12sp, `astronomy_star_white` at 70% opacity.
- **Horizon ring**: Outer 320dp circle with `astronomy_horizon` tint (40% opacity), dashed stroke.
- **Cardinal labels**: N / E / S / W at 90° intervals on horizon ring edge, white.
- **Sun icon**: `☀` 24sp at computed azimuth/altitude position.
- **Moon icon**: Phase-appropriate crescent/circle 20sp at computed azimuth/altitude.
- **Controls**: Top-right FAB for time picker; bottom-left legend chip.

### 6.6. Hijri Calendar Cell Design
- Cell size: adaptive (auto-fit 7 columns, ~44dp min width).
- Top 60%: Gregorian day number — Title Large, `onBackground`.
- Bottom 40%: Hijri day number — Label Medium 10sp, `onSurfaceVariant`.
- **Today highlight**: `primaryContainer` background, rounded 8dp.
- **Prayer dots**: Row of 5 tiny dots (3dp diameter) at cell bottom:
  - Dot filled = prayer alarm enabled for that time.
  - Colors: Subuh=`primary`, Dzuhur=`secondary`, Ashar=`tertiary`, Maghrib=`error`, Isya=`outline`.
- **Moon phase icon**: 12dp Moon icon top-right corner of cell (if New/Quarter/Full phase day).
- **Golden Hour indicator**: 2dp top border in `astronomy_golden_hour` color (if sunrise golden hour is significant).

### 6.7. Moon Widget (Glance)
- Background: Rounded container 16dp radius, `astronomy_surface` color.
- Row 1: Moon phase icon (24dp vector) + phase name (Label Large).
- Row 2: Illumination % + distance (Label Medium, `onSurfaceVariant`).
- Row 3: "Moonrise" label + time (Body Large).
- Row 4: Countdown Chronometer (Display Small, `astronomy_moon_gold`).

### 6.8. Sun Widget (Glance)
- Background: Rounded container 16dp radius, gradient `astronomy_background_mid` → `#1A2A1A` (slight emerald tint for brand).
- Row 1: Sun/phase icon (24dp) + solar phase badge (Label Medium pill).
- Row 2: Next event label + time (Body Large).
- Row 3: Countdown Chronometer (Display Small, `astronomy_golden_hour`).
