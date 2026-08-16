package com.adzannotif.domain.model

import com.adzannotif.core.prayer.CalculationMethod
import com.adzannotif.core.prayer.HighLatitudeRule
import com.adzannotif.core.prayer.Madhab
import com.adzannotif.core.prayer.PrayerAdjustments
import kotlinx.serialization.Serializable

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * Domain model representing calculation and general application settings.
 */
@Serializable
data class UserSettings(
    val calculationMethod: CalculationMethod = CalculationMethod.KEMENAG_RI,
    val madhab: Madhab = Madhab.SHAFI,
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT,
    val ihtiyatMinutes: Int = 2,
    val fajrAdjustment: Int = 2,
    val dhuhrAdjustment: Int = 2,
    val asrAdjustment: Int = 2,
    val maghribAdjustment: Int = 2,
    val ishaAdjustment: Int = 2,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useAutoLocation: Boolean = true,
    val selectedLocation: LocationInfo = LocationInfo.JAKARTA,
) {
    fun toPrayerAdjustments(): PrayerAdjustments = PrayerAdjustments(
        imsak = ihtiyatMinutes,
        fajr = fajrAdjustment,
        sunrise = 0,
        dhuhr = dhuhrAdjustment,
        asr = asrAdjustment,
        maghrib = maghribAdjustment,
        isha = ishaAdjustment,
    )
}
