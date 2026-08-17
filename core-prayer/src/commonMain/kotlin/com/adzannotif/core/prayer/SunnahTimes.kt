package com.adzannotif.core.prayer

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Calculations for Sunnah night prayers (First Third, Midnight, and Last Third / Tahajjud).
 *
 * @property middleOfTheNight The exact midpoint between Maghrib and the next day's Fajr.
 * @property lastThirdOfTheNight The beginning of the last third of the night (prime Tahajjud time).
 * @property firstThirdOfTheNight The end of the first third of the night.
 */
@Serializable
data class SunnahTimes(
    val middleOfTheNight: Instant,
    val lastThirdOfTheNight: Instant,
    val firstThirdOfTheNight: Instant?
) {
    companion object {
        /**
         * Computes Sunnah night prayer times given today's Maghrib and tomorrow's Fajr.
         */
        fun from(maghrib: Instant, nextDayFajr: Instant): SunnahTimes {
            val nightDuration = nextDayFajr.toEpochMilliseconds() - maghrib.toEpochMilliseconds()
            val firstThirdMs = maghrib.toEpochMilliseconds() + (nightDuration / 3L)
            val middleMs = maghrib.toEpochMilliseconds() + (nightDuration / 2L)
            val lastThirdMs = maghrib.toEpochMilliseconds() + (2L * nightDuration / 3L)

            return SunnahTimes(
                middleOfTheNight = Instant.fromEpochMilliseconds(middleMs),
                lastThirdOfTheNight = Instant.fromEpochMilliseconds(lastThirdMs),
                firstThirdOfTheNight = Instant.fromEpochMilliseconds(firstThirdMs)
            )
        }
    }
}
