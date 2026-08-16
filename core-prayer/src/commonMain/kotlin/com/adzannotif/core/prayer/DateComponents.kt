package com.adzannotif.core.prayer

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Represents a calendar date (year, month, day) for prayer time calculations.
 *
 * @property year Calendar year (e.g. 2026)
 * @property month Month of the year (1-12)
 * @property day Day of the month (1-31)
 */
@Serializable
data class DateComponents(
    val year: Int,
    val month: Int,
    val day: Int
) {
    init {
        require(month in 1..12) { "Month must be between 1 and 12, was $month" }
        require(day in 1..31) { "Day must be between 1 and 31, was $day" }
    }

    companion object {
        /**
         * Creates a [DateComponents] instance from a [LocalDate].
         */
        fun from(localDate: LocalDate): DateComponents {
            return DateComponents(
                year = localDate.year,
                month = localDate.monthNumber,
                day = localDate.dayOfMonth
            )
        }
    }

    /**
     * Converts to a [LocalDate].
     */
    fun toLocalDate(): LocalDate {
        return LocalDate(year, month, day)
    }
}
