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
