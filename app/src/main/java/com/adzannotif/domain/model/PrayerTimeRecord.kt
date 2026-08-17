package com.adzannotif.domain.model

import com.adzannotif.core.prayer.Coordinates
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import com.adzannotif.core.prayer.Prayer

/**
 * Domain model representing computed prayer times for a single day.
 */
data class PrayerTimeRecord(
    val date: LocalDate,
    val coordinates: Coordinates,
    val imsak: Instant,
    val fajr: Instant,
    val sunrise: Instant,
    val dhuhr: Instant,
    val asr: Instant,
    val maghrib: Instant,
    val isha: Instant,
    val midnight: Instant,
    val firstThirdOfTheNight: Instant? = null,
    val lastThirdOfTheNight: Instant? = null,
) {
    fun getInstantForPrayer(prayer: Prayer): Instant? = when (prayer) {
        Prayer.IMSAK -> imsak
        Prayer.FAJR -> fajr
        Prayer.SUNRISE -> sunrise
        Prayer.DHUHR -> dhuhr
        Prayer.ASR -> asr
        Prayer.MAGHRIB -> maghrib
        Prayer.ISHA -> isha
        Prayer.MIDNIGHT -> midnight
        Prayer.TAHAJJUD -> lastThirdOfTheNight
    }

    /**
     * Determines the current active prayer and the next upcoming prayer relative to [now].
     */
    fun findNextPrayer(now: Instant): Pair<Prayer, Instant>? {
        val schedule = listOf(
            Prayer.FAJR to fajr,
            Prayer.SUNRISE to sunrise,
            Prayer.DHUHR to dhuhr,
            Prayer.ASR to asr,
            Prayer.MAGHRIB to maghrib,
            Prayer.ISHA to isha,
        )
        return schedule.firstOrNull { it.second > now }
    }

    fun findCurrentPrayer(now: Instant): Prayer? {
        return when {
            now < fajr -> null
            now < sunrise -> Prayer.FAJR
            now < dhuhr -> Prayer.SUNRISE
            now < asr -> Prayer.DHUHR
            now < maghrib -> Prayer.ASR
            now < isha -> Prayer.MAGHRIB
            else -> Prayer.ISHA
        }
    }
}
