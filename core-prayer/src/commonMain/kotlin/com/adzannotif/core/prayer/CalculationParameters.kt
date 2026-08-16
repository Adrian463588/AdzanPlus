package com.adzannotif.core.prayer

import kotlinx.serialization.Serializable

/**
 * Full configuration parameters required to calculate prayer times.
 *
 * @property method The calculation authority preset.
 * @property fajrAngle Twilight depression angle for Fajr (in degrees).
 * @property ishaAngle Twilight depression angle for Isha (in degrees, 0.0 if using interval).
 * @property ishaInterval Interval in minutes after Maghrib for Isha (0 if using angle).
 * @property maghribAngle Depression angle for Maghrib (0.0 if calculated from sunset).
 * @property madhab Juristic method for Asr shadow calculation.
 * @property highLatitudeRule Rule applied for higher latitude regions.
 * @property prayerAdjustments Per-prayer minute adjustments.
 * @property rounding Rounding mode applied to calculated timestamps.
 */
@Serializable
data class CalculationParameters(
    val method: CalculationMethod = CalculationMethod.KEMENAG_RI,
    val fajrAngle: Double = 20.0,
    val ishaAngle: Double = 18.0,
    val ishaInterval: Int = 0,
    val maghribAngle: Double = 0.0,
    val madhab: Madhab = Madhab.SHAFI,
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT,
    val prayerAdjustments: PrayerAdjustments = PrayerAdjustments.KEMENAG_DEFAULT_IHTIYATH,
    val rounding: RoundingType = RoundingType.NEAREST
)
