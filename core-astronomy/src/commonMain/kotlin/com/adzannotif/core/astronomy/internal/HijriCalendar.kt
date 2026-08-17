package com.adzannotif.core.astronomy.internal

import com.adzannotif.core.astronomy.HijriDate
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.floor

/**
 * Offline Umm al-Qura conversion for the published 1356 AH–1500 AH table.
 *
 * Umm al-Qura is a civil calendar table, not an observation claim. Keeping the
 * table bounded is intentional: dates outside the published range are rejected
 * instead of silently falling back to a tabular approximation.
 */
public object HijriCalendar {
    private const val FIRST_YEAR = 1356
    private const val LAST_YEAR = 1500
    private const val MILLIS_PER_DAY = 86_400_000L
    private const val UNIX_EPOCH_JULIAN_DAY = 2_440_587.5

    /** Gregorian epoch-day at 1 Muharram for each year in [FIRST_YEAR]..[LAST_YEAR]. */
    private val yearStartEpochDays = intArrayOf(
        -11981, -11626, -11272, -10918, -10564, -10210, -9855, -9501, -9146, -8792,
        -8438, -8084, -7729, -7375, -7020, -6665, -6311, -5957, -5603, -5249,
        -4894, -4539, -4185, -3830, -3476, -3122, -2768, -2414, -2059, -1704,
        -1350, -996, -642, -288, 67, 422, 776, 1131, 1485, 1839,
        2193, 2547, 2902, 3256, 3611, 3965, 4319, 4673, 5028, 5382,
        5737, 6092, 6446, 6800, 7154, 7508, 7863, 8218, 8572, 8927,
        9281, 9635, 9989, 10344, 10698, 11053, 11407, 11761, 12115, 12469,
        12824, 13179, 13533, 13888, 14242, 14596, 14950, 15304, 15659, 16013,
        16368, 16722, 17076, 17430, 17785, 18139, 18494, 18848, 19203, 19557,
        19911, 20265, 20620, 20975, 21329, 21683, 22038, 22392, 22746, 23101,
        23456, 23810, 24165, 24519, 24873, 25227, 25581, 25936, 26291, 26645,
        26999, 27353, 27708, 28062, 28417, 28771, 29126, 29480, 29834, 30188,
        30542, 30897, 31252, 31606, 31960, 32314, 32668, 33023, 33378, 33732,
        34087, 34441, 34795, 35149, 35504, 35858, 36213, 36567, 36922, 37276,
        37630, 37984, 38339, 38693, 39048,
    )

    /** Bit m-1 is set when month m has 30 days. */
    private val thirtyDayMonthMasks = intArrayOf(
        3754, 3732, 3370, 3158, 1198, 2669, 1386, 3413, 3402, 2707,
        1323, 2651, 1338, 1717, 3753, 3410, 3369, 2645, 1197, 1389,
        2794, 1764, 3793, 3490, 2730, 2394, 730, 1465, 2994, 1892,
        1737, 1365, 683, 1243, 2746, 1460, 3497, 3410, 2725, 2349,
        621, 2285, 730, 2773, 2725, 2635, 1175, 2359, 694, 2421,
        3433, 3410, 3221, 2347, 603, 1243, 2517, 1490, 3493, 3402,
        2709, 1357, 2733, 938, 3026, 3012, 2953, 2709, 1325, 1453,
        2922, 1748, 3529, 3474, 2726, 2390, 686, 1389, 874, 2901,
        2730, 2381, 1181, 2397, 698, 1461, 1450, 3413, 2714, 2350,
        622, 1373, 2778, 1748, 1701, 2855, 2637, 1197, 1389, 2906,
        1876, 3913, 3730, 3366, 2646, 854, 1717, 2986, 2962, 2853,
        1675, 2715, 1370, 2778, 1460, 3497, 2898, 2714, 1334, 630,
        1397, 2802, 1748, 1705, 1365, 685, 1213, 2490, 1396, 2921,
        2898, 2709, 1325, 2653, 1242, 2777, 1714, 3733, 3626, 3222,
        2350, 2733, 1386, 3429, 3402,
    )

    init {
        check(yearStartEpochDays.size == LAST_YEAR - FIRST_YEAR + 1)
        check(thirtyDayMonthMasks.size == yearStartEpochDays.size)
    }

    public fun toHijri(
        gregorianEpochMillis: Long,
        timeZone: TimeZone = TimeZone.UTC,
    ): HijriDate {
        val localDate = Instant.fromEpochMilliseconds(gregorianEpochMillis)
            .toLocalDateTime(timeZone)
            .date
        val epochDay = gregorianToEpochDay(localDate)
        val yearIndex = yearIndexFor(epochDay)
        val dayOfYear = epochDay - yearStartEpochDays[yearIndex]
        require(dayOfYear in 0 until yearLength(yearIndex)) {
            "Umm al-Qura data is unavailable outside $FIRST_YEAR-$LAST_YEAR AH"
        }

        var remaining = dayOfYear
        var month = 1
        while (month <= 12) {
            val length = monthLength(yearIndex, month)
            if (remaining < length) break
            remaining -= length
            month += 1
        }

        return HijriDate(
            year = FIRST_YEAR + yearIndex,
            month = month,
            day = remaining + 1,
            monthName = HijriDate.MONTH_NAMES[month - 1],
        )
    }

    /** Returns UTC midnight for the requested Umm al-Qura date. */
    public fun fromHijri(year: Int, month: Int, day: Int): Long {
        require(year in FIRST_YEAR..LAST_YEAR) {
            "Umm al-Qura data is unavailable outside $FIRST_YEAR-$LAST_YEAR AH"
        }
        require(month in 1..12) { "Hijri month must be between 1 and 12" }
        val yearIndex = year - FIRST_YEAR
        require(day in 1..monthLength(yearIndex, month)) {
            "Invalid day $day for Umm al-Qura month $year-$month"
        }

        var epochDay = yearStartEpochDays[yearIndex]
        for (monthIndex in 1 until month) {
            epochDay += monthLength(yearIndex, monthIndex)
        }
        epochDay += day - 1
        return epochDay.toLong() * MILLIS_PER_DAY
    }

    private fun yearIndexFor(epochDay: Int): Int {
        if (epochDay < yearStartEpochDays.first()) {
            throw IllegalArgumentException(
                "Umm al-Qura data is unavailable before $FIRST_YEAR AH",
            )
        }
        var index = yearStartEpochDays.lastIndex
        while (index > 0 && epochDay < yearStartEpochDays[index]) index -= 1
        return index
    }

    private fun monthLength(yearIndex: Int, month: Int): Int =
        if ((thirtyDayMonthMasks[yearIndex] and (1 shl (month - 1))) != 0) 30 else 29

    private fun yearLength(yearIndex: Int): Int {
        var total = 0
        for (month in 1..12) total += monthLength(yearIndex, month)
        return total
    }

    private fun gregorianToEpochDay(date: LocalDate): Int {
        val julianDay = gregorianToJulianDay(date)
        return floor(julianDay - UNIX_EPOCH_JULIAN_DAY).toInt()
    }

    private fun gregorianToJulianDay(date: LocalDate): Double {
        var year = date.year
        var month = date.monthNumber
        if (month <= 2) {
            year -= 1
            month += 12
        }

        val century = floor(year / 100.0)
        return floor(365.25 * (year + 4716)) +
            floor(30.6001 * (month + 1)) +
            date.dayOfMonth +
            2.0 - century +
            floor(century / 4.0) -
            1524.5
    }
}
