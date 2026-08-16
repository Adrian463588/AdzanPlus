package com.adzannotif.core.astronomy.internal

import kotlin.math.PI

internal object MathUtils {
    fun Double.toRadians(): Double = this * PI / 180.0
    fun Double.toDegrees(): Double = this * 180.0 / PI

    fun Double.unwindAngle(): Double {
        var a = this % 360.0
        if (a < 0) a += 360.0
        return a
    }
}
