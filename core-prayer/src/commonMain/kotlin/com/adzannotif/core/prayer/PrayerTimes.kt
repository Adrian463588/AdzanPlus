package com.adzannotif.core.prayer

import com.adzannotif.core.prayer.internal.SolarTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Calculated Islamic prayer times for specific coordinates, date, and parameters.
 */
@Serializable
class PrayerTimes(
    val coordinates: Coordinates,
    val dateComponents: DateComponents,
    val calculationParameters: CalculationParameters,
    val fajr: Instant,
    val sunrise: Instant,
    val dhuhr: Instant,
    val asr: Instant,
    val maghrib: Instant,
    val isha: Instant,
    val imsak: Instant,
    val sunnahTimes: SunnahTimes = SunnahTimes.from(maghrib, fajr.plus(24.hours))
) {
    /**
     * Returns the [Instant] corresponding to a specific [Prayer].
     */
    fun timeForPrayer(prayer: Prayer): Instant {
        return when (prayer) {
            Prayer.IMSAK -> imsak
            Prayer.FAJR -> fajr
            Prayer.SUNRISE -> sunrise
            Prayer.DHUHR -> dhuhr
            Prayer.ASR -> asr
            Prayer.MAGHRIB -> maghrib
            Prayer.ISHA -> isha
            Prayer.MIDNIGHT -> sunnahTimes.middleOfTheNight
            Prayer.TAHAJJUD -> sunnahTimes.lastThirdOfTheNight
        }
    }

    /**
     * Determines which prayer time window is currently active at [now].
     */
    fun currentPrayer(now: Instant): Prayer? {
        val nowMs = now.toEpochMilliseconds()
        return when {
            nowMs >= isha.toEpochMilliseconds() -> Prayer.ISHA
            nowMs >= maghrib.toEpochMilliseconds() -> Prayer.MAGHRIB
            nowMs >= asr.toEpochMilliseconds() -> Prayer.ASR
            nowMs >= dhuhr.toEpochMilliseconds() -> Prayer.DHUHR
            nowMs >= sunrise.toEpochMilliseconds() -> Prayer.SUNRISE
            nowMs >= fajr.toEpochMilliseconds() -> Prayer.FAJR
            nowMs >= imsak.toEpochMilliseconds() -> Prayer.IMSAK
            else -> null // Before Fajr / Imsak (or still Isha from previous day)
        }
    }

    /**
     * Determines the next upcoming prayer after [now].
     */
    fun nextPrayer(now: Instant): Prayer? {
        val nowMs = now.toEpochMilliseconds()
        return when {
            nowMs < imsak.toEpochMilliseconds() -> Prayer.IMSAK
            nowMs < fajr.toEpochMilliseconds() -> Prayer.FAJR
            nowMs < sunrise.toEpochMilliseconds() -> Prayer.SUNRISE
            nowMs < dhuhr.toEpochMilliseconds() -> Prayer.DHUHR
            nowMs < asr.toEpochMilliseconds() -> Prayer.ASR
            nowMs < maghrib.toEpochMilliseconds() -> Prayer.MAGHRIB
            nowMs < isha.toEpochMilliseconds() -> Prayer.ISHA
            else -> null // Tomorrow's Fajr / Imsak
        }
    }

    companion object {
        /**
         * Calculates prayer times for the specified coordinates, date, and parameters.
         */
        operator fun invoke(
            coordinates: Coordinates,
            dateComponents: DateComponents,
            calculationParameters: CalculationParameters
        ): PrayerTimes {
            val solarTime = SolarTime(coordinates, dateComponents)

            // Dhuhr: solar transit
            val dhuhrHours = solarTime.transit

            // Sunrise & Sunset
            val sunriseHours = solarTime.sunrise ?: (dhuhrHours - 6.0)
            val sunsetHours = solarTime.sunset ?: (dhuhrHours + 6.0)

            // Fajr
            var fajrHours = solarTime.hourAngleForTwilight(calculationParameters.fajrAngle, isAfterTransit = false)
            if (fajrHours == null) {
                // High latitude fallback
                val nightPortion = when (calculationParameters.highLatitudeRule) {
                    HighLatitudeRule.MIDDLE_OF_THE_NIGHT -> 0.5
                    HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> 1.0 / 7.0
                    HighLatitudeRule.TWILIGHT_ANGLE -> calculationParameters.fajrAngle / 60.0
                }
                val nightLength = (24.0 + sunriseHours - sunsetHours) % 24.0
                fajrHours = (sunriseHours - (nightLength * nightPortion) + 24.0) % 24.0
            }

            // Asr
            val asrHours = solarTime.timeForAsr(calculationParameters.madhab.shadowFactor) ?: (dhuhrHours + 3.0)

            // Maghrib
            val maghribHours = if (calculationParameters.maghribAngle > 0.0) {
                solarTime.hourAngleForTwilight(calculationParameters.maghribAngle, isAfterTransit = true) ?: sunsetHours
            } else {
                sunsetHours
            }

            // Isha
            var ishaHours: Double
            if (calculationParameters.ishaInterval > 0) {
                ishaHours = (maghribHours + (calculationParameters.ishaInterval / 60.0)) % 24.0
            } else {
                val computedIsha = solarTime.hourAngleForTwilight(calculationParameters.ishaAngle, isAfterTransit = true)
                if (computedIsha != null) {
                    ishaHours = computedIsha
                } else {
                    // High latitude fallback
                    val nightPortion = when (calculationParameters.highLatitudeRule) {
                        HighLatitudeRule.MIDDLE_OF_THE_NIGHT -> 0.5
                        HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> 1.0 / 7.0
                        HighLatitudeRule.TWILIGHT_ANGLE -> calculationParameters.ishaAngle / 60.0
                    }
                    val nightLength = (24.0 + sunriseHours - sunsetHours) % 24.0
                    ishaHours = (sunsetHours + (nightLength * nightPortion)) % 24.0
                }
            }

            // Convert decimal UTC hours to Instant
            val baseDate = dateComponents.toLocalDate()
            val dhuhrInstant = toInstantWithAdjustments(baseDate, dhuhrHours, calculationParameters.prayerAdjustments.dhuhr, calculationParameters.rounding)

            // Fajr and Sunrise occur before Dhuhr on the observer's local day; if their UTC hours > dhuhrHours, they fall on baseDate - 1 day in UTC
            val fajrDate = if (fajrHours > dhuhrHours) baseDate.minus(DatePeriod(days = 1)) else baseDate
            val fajrInstant = toInstantWithAdjustments(fajrDate, fajrHours, calculationParameters.prayerAdjustments.fajr, calculationParameters.rounding)
            val rawFajrInstant = toInstantWithAdjustments(fajrDate, fajrHours, 0, calculationParameters.rounding)

            val sunriseDate = if (sunriseHours > dhuhrHours) baseDate.minus(DatePeriod(days = 1)) else baseDate
            val sunriseInstant = toInstantWithAdjustments(sunriseDate, sunriseHours, calculationParameters.prayerAdjustments.sunrise, calculationParameters.rounding)

            // Asr, Maghrib, Isha occur after Dhuhr on the observer's local day; if their UTC hours < dhuhrHours, they fall on baseDate + 1 day in UTC
            val asrDate = if (asrHours < dhuhrHours) baseDate.plus(DatePeriod(days = 1)) else baseDate
            val asrInstant = toInstantWithAdjustments(asrDate, asrHours, calculationParameters.prayerAdjustments.asr, calculationParameters.rounding)

            val maghribDate = if (maghribHours < dhuhrHours) baseDate.plus(DatePeriod(days = 1)) else baseDate
            val maghribInstant = toInstantWithAdjustments(maghribDate, maghribHours, calculationParameters.prayerAdjustments.maghrib, calculationParameters.rounding)

            val ishaDate = if (ishaHours < dhuhrHours) baseDate.plus(DatePeriod(days = 1)) else baseDate
            val ishaInstant = toInstantWithAdjustments(ishaDate, ishaHours, calculationParameters.prayerAdjustments.isha, calculationParameters.rounding)

            // Imsak is 10 minutes before Fajr + imsak adjustments
            val imsakInstant = rawFajrInstant.minus(10.minutes).plus(calculationParameters.prayerAdjustments.imsak.minutes)

            // Calculate next day's Fajr for exact Sunnah night prayers (Midnight & Tahajjud)
            val nextDateComponents = DateComponents.from(baseDate.plus(DatePeriod(days = 1)))
            val nextDaySolarTime = SolarTime(coordinates, nextDateComponents)
            val nextDhuhrHours = nextDaySolarTime.transit
            var nextDayFajrHours = nextDaySolarTime.hourAngleForTwilight(calculationParameters.fajrAngle, isAfterTransit = false)
            if (nextDayFajrHours == null) {
                val nightPortion = when (calculationParameters.highLatitudeRule) {
                    HighLatitudeRule.MIDDLE_OF_THE_NIGHT -> 0.5
                    HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> 1.0 / 7.0
                    HighLatitudeRule.TWILIGHT_ANGLE -> calculationParameters.fajrAngle / 60.0
                }
                val nextSunrise = nextDaySolarTime.sunrise ?: (nextDhuhrHours - 6.0)
                val nextSunset = nextDaySolarTime.sunset ?: (nextDhuhrHours + 6.0)
                val nightLength = (24.0 + nextSunrise - nextSunset) % 24.0
                nextDayFajrHours = (nextSunrise - (nightLength * nightPortion) + 24.0) % 24.0
            }
            val nextDayBaseDate = nextDateComponents.toLocalDate()
            val nextDayFajrDate = if (nextDayFajrHours > nextDhuhrHours) nextDayBaseDate.minus(DatePeriod(days = 1)) else nextDayBaseDate
            val nextDayFajrInstant = toInstantWithAdjustments(nextDayFajrDate, nextDayFajrHours, calculationParameters.prayerAdjustments.fajr, calculationParameters.rounding)

            val sunnahTimes = SunnahTimes.from(maghribInstant, nextDayFajrInstant)

            return PrayerTimes(
                coordinates = coordinates,
                dateComponents = dateComponents,
                calculationParameters = calculationParameters,
                fajr = fajrInstant,
                sunrise = sunriseInstant,
                dhuhr = dhuhrInstant,
                asr = asrInstant,
                maghrib = maghribInstant,
                isha = ishaInstant,
                imsak = imsakInstant,
                sunnahTimes = sunnahTimes
            )
        }

        private fun toInstantWithAdjustments(
            date: LocalDate,
            utcHours: Double,
            minuteAdjustment: Int,
            rounding: RoundingType
        ): Instant {
            val totalSeconds = (utcHours * 3600.0).roundToLong()
            val baseDayInstant = date.atTime(0, 0).toInstant(TimeZone.UTC)
            var instant = baseDayInstant.plus(totalSeconds.seconds).plus(minuteAdjustment.minutes)

            instant = when (rounding) {
                RoundingType.NEAREST -> {
                    val epochSec = instant.epochSeconds
                    val remainder = epochSec % 60
                    if (remainder >= 30) {
                        Instant.fromEpochSeconds(epochSec + (60 - remainder))
                    } else {
                        Instant.fromEpochSeconds(epochSec - remainder)
                    }
                }
                RoundingType.UP -> {
                    val epochSec = instant.epochSeconds
                    val remainder = epochSec % 60
                    if (remainder > 0) {
                        Instant.fromEpochSeconds(epochSec + (60 - remainder))
                    } else {
                        instant
                    }
                }
                RoundingType.DOWN -> {
                    val epochSec = instant.epochSeconds
                    val remainder = epochSec % 60
                    Instant.fromEpochSeconds(epochSec - remainder)
                }
                RoundingType.NONE -> instant
            }

            return instant
        }
    }
}

