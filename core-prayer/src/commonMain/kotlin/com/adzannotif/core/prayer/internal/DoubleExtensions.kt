package com.adzannotif.core.prayer.internal

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.round

internal object DoubleExtensions {
    fun Double.toRadians(): Double = this * (PI / 180.0)
    fun Double.toDegrees(): Double = this * (180.0 / PI)

    /**
     * Unwinds an angle to the range [0.0, 360.0).
     */
    fun Double.unwindAngle(): Double {
        val value = this % 360.0
        return if (value < 0) value + 360.0 else value
    }

    /**
     * Unwinds hours to the range [0.0, 24.0).
     */
    fun Double.unwindHours(): Double {
        val value = this % 24.0
        return if (value < 0) value + 24.0 else value
    }

    /**
     * Rounds a double to the nearest integer.
     */
    fun Double.roundToNearest(): Double = round(this)

    /**
     * Truncates / floors a double.
     */
    fun Double.floorValue(): Double = floor(this)
}
