package com.adzannotif.core.prayer

import kotlinx.serialization.Serializable

/**
 * Minute adjustments to add or subtract from calculated prayer times.
 * Useful for local mosque calibration or standard ihtiyath.
 *
 * @property imsak Minute offset for Imsak (default: 0)
 * @property fajr Minute offset for Fajr (default: 0)
 * @property sunrise Minute offset for Sunrise (default: 0)
 * @property dhuhr Minute offset for Dhuhr (default: 0)
 * @property asr Minute offset for Asr (default: 0)
 * @property maghrib Minute offset for Maghrib (default: 0)
 * @property isha Minute offset for Isha (default: 0)
 */
@Serializable
data class PrayerAdjustments(
    val imsak: Int = 0,
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0
) {
    /**
     * Returns the adjustment value for a given [Prayer].
     */
    fun forPrayer(prayer: Prayer): Int {
        return when (prayer) {
            Prayer.IMSAK -> imsak
            Prayer.FAJR -> fajr
            Prayer.SUNRISE -> sunrise
            Prayer.DHUHR -> dhuhr
            Prayer.ASR -> asr
            Prayer.MAGHRIB -> maghrib
            Prayer.ISHA -> isha
            Prayer.MIDNIGHT, Prayer.TAHAJJUD -> 0
        }
    }

    companion object {
        val ZERO = PrayerAdjustments()

        /**
         * Standard Indonesian Kemenag Ihtiyath (safety buffer of +2 minutes for all 5 prayers).
         */
        val KEMENAG_DEFAULT_IHTIYATH = PrayerAdjustments(
            imsak = 2,
            fajr = 2,
            sunrise = -2, // Sunrise usually subtracted 2 minutes or 0
            dhuhr = 2,
            asr = 2,
            maghrib = 2,
            isha = 2
        )
    }
}
