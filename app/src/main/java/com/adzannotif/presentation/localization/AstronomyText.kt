package com.adzannotif.presentation.localization

import android.content.Context
import com.adzannotif.R

/**
 * Maps stable engine labels to localized UI resources.
 *
 * The calculation modules intentionally expose stable enum labels and no
 * Android resources. Presentation and widget surfaces localize those labels
 * at the platform boundary.
 */
internal fun solarPhaseLabel(context: Context, phase: String?): String = when (phase) {
    "Night" -> context.getString(R.string.solar_phase_night)
    "Astronomical Twilight" -> context.getString(R.string.solar_phase_astronomical_twilight)
    "Nautical Twilight" -> context.getString(R.string.solar_phase_nautical_twilight)
    "Blue Hour" -> context.getString(R.string.solar_phase_blue_hour)
    "Civil Twilight" -> context.getString(R.string.solar_phase_civil_twilight)
    "Golden Hour" -> context.getString(R.string.solar_phase_golden_hour)
    "Day" -> context.getString(R.string.solar_phase_day)
    else -> phase ?: context.getString(R.string.value_unavailable)
}

internal fun moonPhaseLabel(context: Context, phase: String?): String = when (phase) {
    "New Moon" -> context.getString(R.string.moon_phase_new_moon)
    "Waxing Crescent" -> context.getString(R.string.moon_phase_waxing_crescent)
    "First Quarter" -> context.getString(R.string.moon_phase_first_quarter)
    "Waxing Gibbous" -> context.getString(R.string.moon_phase_waxing_gibbous)
    "Full Moon" -> context.getString(R.string.moon_phase_full_moon)
    "Waning Gibbous" -> context.getString(R.string.moon_phase_waning_gibbous)
    "Last Quarter" -> context.getString(R.string.moon_phase_last_quarter)
    "Waning Crescent" -> context.getString(R.string.moon_phase_waning_crescent)
    else -> phase ?: context.getString(R.string.value_unavailable)
}

/** Resolves stable astronomy event keys at the UI/platform boundary. */
internal fun astronomyEventLabel(context: Context, key: String?): String = when (key) {
    "CIVIL_TWILIGHT_DAWN" -> context.getString(R.string.celestial_event_civil_dawn)
    "CIVIL_TWILIGHT_DUSK" -> context.getString(R.string.celestial_event_civil_dusk)
    "SUNRISE" -> context.getString(R.string.calendar_sunrise_label)
    "SUNSET" -> context.getString(R.string.calendar_sunset_label)
    "MOONRISE" -> context.getString(R.string.settings_celestial_moonrise)
    "MOONSET" -> context.getString(R.string.settings_celestial_moonset)
    "FULL_MOON" -> context.getString(R.string.settings_celestial_full_moon)
    "NEW_MOON" -> context.getString(R.string.settings_celestial_new_moon)
    "GOLDEN_HOUR_MORNING_START",
    "GOLDEN_HOUR_EVENING_START" -> context.getString(R.string.celestial_event_golden_start)
    "GOLDEN_HOUR_MORNING_END",
    "GOLDEN_HOUR_EVENING_END" -> context.getString(R.string.celestial_event_golden_end)
    "BLUE_HOUR_MORNING_START",
    "BLUE_HOUR_EVENING_START" -> context.getString(R.string.celestial_event_blue_start)
    "BLUE_HOUR_MORNING_END",
    "BLUE_HOUR_EVENING_END" -> context.getString(R.string.celestial_event_blue_end)
    else -> context.getString(R.string.value_unavailable)
}
