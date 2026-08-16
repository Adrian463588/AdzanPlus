package com.adzannotif.core.prayer

import kotlinx.serialization.Serializable

/**
 * Rounding mode applied to the calculated prayer timestamps.
 */
@Serializable
enum class RoundingType {
    /**
     * Round to nearest whole minute (e.g. 04:30:31 -> 04:31, 04:30:29 -> 04:30).
     */
    NEAREST,

    /**
     * Always round up / ceiling to the next minute (adds 1 minute if seconds > 0).
     */
    UP,

    /**
     * Always round down / truncate seconds.
     */
    DOWN,

    /**
     * Keep exact second precision without rounding.
     */
    NONE
}
