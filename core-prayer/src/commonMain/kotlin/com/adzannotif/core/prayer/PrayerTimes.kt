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
    val sunnahTimes: SunnahTimes
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
            return when (val result = calculate(coordinates, dateComponents, calculationParameters)) {
                is PrayerTimesResult.Available -> result.value
                is PrayerTimesResult.Unavailable -> throw PrayerTimesUnavailableException(result.reason)
            }
        }

        /**
         * Calculates prayer times without inventing values when a solar event
         * cannot be observed or derived from the configured high-latitude rule.
         */
        fun calculate(
            coordinates: Coordinates,
            dateComponents: DateComponents,
            calculationParameters: CalculationParameters
        ): PrayerTimesResult {
            val solarTime = SolarTime(coordinates, dateComponents)

            // Dhuhr: solar transit
            val dhuhrHours = solarTime.transit

            // Sunrise & Sunset
            val sunriseHours = solarTime.sunrise
                ?: return PrayerTimesResult.Unavailable(PrayerTimesUnavailableReason.SUNRISE_NOT_VISIBLE)
            val sunsetHours = solarTime.sunset
                ?: return PrayerTimesResult.Unavailable(PrayerTimesUnavailableReason.SUNSET_NOT_VISIBLE)

            // Fajr
            val fajrHours = highLatitudeAdjustedTime(
                solarTime = solarTime,
                angle = calculationParameters.fajrAngle,
                isAfterTransit = false,
                sunriseHours = sunriseHours,
                sunsetHours = sunsetHours,
                rule = calculationParameters.highLatitudeRule,
            ) ?: return PrayerTimesResult.Unavailable(PrayerTimesUnavailableReason.FAJR_NOT_COMPUTABLE)

            // Asr
            val asrHours = solarTime.timeForAsr(calculationParameters.madhab.shadowFactor)
                ?: return PrayerTimesResult.Unavailable(PrayerTimesUnavailableReason.ASR_NOT_COMPUTABLE)

            // Maghrib
            val maghribHours = if (calculationParameters.maghribAngle > 0.0) {
                solarTime.hourAngleForTwilight(calculationParameters.maghribAngle, isAfterTransit = true)
                    ?: return PrayerTimesResult.Unavailable(PrayerTimesUnavailableReason.MAGHRIB_NOT_COMPUTABLE)
            } else {
                sunsetHours
            }

            // Isha
            val ishaHours = if (calculationParameters.ishaInterval > 0) {
                (maghribHours + (calculationParameters.ishaInterval / 60.0)) % 24.0
            } else {
                highLatitudeAdjustedTime(
                    solarTime = solarTime,
                    angle = calculationParameters.ishaAngle,
                    isAfterTransit = true,
                    sunriseHours = sunriseHours,
                    sunsetHours = sunsetHours,
                    rule = calculationParameters.highLatitudeRule,
                ) ?: return PrayerTimesResult.Unavailable(PrayerTimesUnavailableReason.ISHA_NOT_COMPUTABLE)
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
            val nextSunrise = nextDaySolarTime.sunrise
                ?: return PrayerTimesResult.Unavailable(PrayerTimesUnavailableReason.NEXT_DAY_SUNRISE_NOT_VISIBLE)
            val nextSunset = nextDaySolarTime.sunset
                ?: return PrayerTimesResult.Unavailable(PrayerTimesUnavailableReason.NEXT_DAY_SUNSET_NOT_VISIBLE)
            val nextDayFajrHours = highLatitudeAdjustedTime(
                solarTime = nextDaySolarTime,
                angle = calculationParameters.fajrAngle,
                isAfterTransit = false,
                sunriseHours = nextSunrise,
                sunsetHours = nextSunset,
                rule = calculationParameters.highLatitudeRule,
            ) ?: return PrayerTimesResult.Unavailable(PrayerTimesUnavailableReason.NEXT_DAY_FAJR_NOT_COMPUTABLE)
            val nextDayBaseDate = nextDateComponents.toLocalDate()
            val nextDayFajrDate = if (nextDayFajrHours > nextDhuhrHours) nextDayBaseDate.minus(DatePeriod(days = 1)) else nextDayBaseDate
            val nextDayFajrInstant = toInstantWithAdjustments(nextDayFajrDate, nextDayFajrHours, calculationParameters.prayerAdjustments.fajr, calculationParameters.rounding)

            val sunnahTimes = SunnahTimes.from(maghribInstant, nextDayFajrInstant)

            return PrayerTimesResult.Available(
                PrayerTimes(
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
            )
        }

        private fun highLatitudeAdjustedTime(
            solarTime: SolarTime,
            angle: Double,
            isAfterTransit: Boolean,
            sunriseHours: Double,
            sunsetHours: Double,
            rule: HighLatitudeRule,
        ): Double? {
            solarTime.hourAngleForTwilight(angle, isAfterTransit)?.let { return it }

            val nightLength = (24.0 + sunriseHours - sunsetHours) % 24.0
            if (nightLength <= 0.0) return null

            val nightPortion = when (rule) {
                HighLatitudeRule.MIDDLE_OF_THE_NIGHT -> 0.5
                HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> 1.0 / 7.0
                HighLatitudeRule.TWILIGHT_ANGLE -> angle / 60.0
            }
            return if (isAfterTransit) {
                (sunsetHours + (nightLength * nightPortion)) % 24.0
            } else {
                (sunriseHours - (nightLength * nightPortion) + 24.0) % 24.0
            }
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
