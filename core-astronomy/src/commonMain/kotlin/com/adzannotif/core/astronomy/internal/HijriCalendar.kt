package com.adzannotif.core.astronomy.internal

import com.adzannotif.core.astronomy.HijriDate
import kotlin.math.floor

internal object HijriCalendar {
    fun toHijri(gregorianEpochMillis: Long): HijriDate {
        val jd = (gregorianEpochMillis / 86400000.0) + 2440587.5
        // Approximation using Kuwati algorithm / standard tabular Islamic calendar
        val jdFloor = floor(jd).toLong()
        val epochAstro = 1948084L
        val shift1 = 8.01066
        var z = jdFloor - epochAstro
        val cyc = floor(z / 10631.0)
        z -= (cyc * 10631L).toLong()
        val j = floor((z - shift1) / 354.367)
        val iy = (cyc * 30 + j).toInt()
        z -= floor(j * 354.367).toLong()
        var im = floor((z + 29.0) / 29.5).toInt()
        if (im == 13) im = 12
        val id = (z - floor(29.5001 * im - 29.0)).toInt()
        
        val monthIdx = (im - 1) % 12
        return HijriDate(
            year = iy,
            month = im,
            day = id,
            monthName = HijriDate.MONTH_NAMES[if (monthIdx < 0) 0 else monthIdx]
        )
    }

    fun fromHijri(year: Int, month: Int, day: Int): Long {
        val jd = floor((11.0 * year + 3.0) / 30.0) + 354 * year + 30 * month - floor((month - 1.0) / 2.0) + day + 1948440 - 385
        return ((jd - 2440587.5) * 86400000.0).toLong()
    }
}
