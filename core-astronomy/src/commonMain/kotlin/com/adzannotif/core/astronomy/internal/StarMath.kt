package com.adzannotif.core.astronomy.internal

import com.adzannotif.core.astronomy.Star
import com.adzannotif.core.astronomy.StarPosition
import com.adzannotif.core.astronomy.internal.MathUtils.toDegrees
import com.adzannotif.core.astronomy.internal.MathUtils.toRadians
import com.adzannotif.core.astronomy.internal.MathUtils.unwindAngle
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

internal object StarMath {
    fun computeStarPosition(star: Star, lat: Double, lon: Double, epochMillis: Long): StarPosition {
        val lst = computeLST(lon, epochMillis)
        val (alt, az) = raDecToAltAz(star.ra, star.dec, lat, lst)
        return StarPosition(star, az, alt)
    }

    private fun raDecToAltAz(ra: Double, dec: Double, lat: Double, lst: Double): Pair<Double, Double> {
        val ha = (lst - ra).unwindAngle().toRadians()
        val decRad = dec.toRadians()
        val latRad = lat.toRadians()

        val altRad = asin(sin(latRad) * sin(decRad) + cos(latRad) * cos(decRad) * cos(ha))
        val azRad = atan2(sin(ha), cos(ha) * sin(latRad) - (sin(decRad) / cos(decRad)) * cos(latRad)) + kotlin.math.PI

        return Pair(altRad.toDegrees(), azRad.toDegrees().unwindAngle())
    }

    private fun computeLST(lon: Double, epochMillis: Long): Double {
        val jd = julianDay(epochMillis)
        val t = (jd - 2451545.0) / 36525.0
        val gmst0 = 280.46061837 + 360.98564736629 * (jd - 2451545.0) +
                t * t * 0.000387933 - (t * t * t) / 38710000.0
        return (gmst0.unwindAngle() + lon).unwindAngle()
    }

    private fun julianDay(epochMillis: Long): Double {
        return epochMillis / 86400000.0 + 2440587.5
    }
}
