package com.adzannotif.core.prayer

import kotlinx.serialization.Serializable

/**
 * Adjustment rule for high-latitude locations where normal twilight angles may not occur.
 */
@Serializable
enum class HighLatitudeRule(val displayName: String) {
    /**
     * Fajr will not be earlier than the middle of the night, and Isha will not be later than the middle of the night.
     */
    MIDDLE_OF_THE_NIGHT("Middle of the Night"),

    /**
     * Fajr will not be earlier than 1/7th of the night, and Isha will not be later than 1/7th of the night.
     */
    SEVENTH_OF_THE_NIGHT("1/7th of the Night"),

    /**
     * Fajr and Isha are bounded by the twilight angle divided by 60 of the total night length.
     */
    TWILIGHT_ANGLE("Twilight Angle (Angle-Based)")
}
