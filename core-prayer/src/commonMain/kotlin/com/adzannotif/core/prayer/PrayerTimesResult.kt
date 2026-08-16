package com.adzannotif.core.prayer

/**
 * Outcome of a prayer-time calculation.
 *
 * A missing solar event is a valid astronomical condition at high latitudes;
 * it must be surfaced instead of being replaced with an invented timestamp.
 */
sealed interface PrayerTimesResult {
    data class Available(val value: PrayerTimes) : PrayerTimesResult

    data class Unavailable(val reason: PrayerTimesUnavailableReason) : PrayerTimesResult
}

enum class PrayerTimesUnavailableReason {
    SUNRISE_NOT_VISIBLE,
    SUNSET_NOT_VISIBLE,
    FAJR_NOT_COMPUTABLE,
    ASR_NOT_COMPUTABLE,
    MAGHRIB_NOT_COMPUTABLE,
    ISHA_NOT_COMPUTABLE,
    NEXT_DAY_SUNRISE_NOT_VISIBLE,
    NEXT_DAY_SUNSET_NOT_VISIBLE,
    NEXT_DAY_FAJR_NOT_COMPUTABLE,
}

class PrayerTimesUnavailableException(
    val reason: PrayerTimesUnavailableReason,
) : IllegalStateException("Prayer times are unavailable: $reason")
