package com.adzannotif.domain.model.astronomy

enum class SkyEventType {
    SUNRISE, SUNSET, MOONRISE, MOONSET,
    GOLDEN_HOUR_MORNING_START, GOLDEN_HOUR_MORNING_END,
    GOLDEN_HOUR_EVENING_START, GOLDEN_HOUR_EVENING_END,
    BLUE_HOUR_MORNING_START, BLUE_HOUR_MORNING_END,
    BLUE_HOUR_EVENING_START, BLUE_HOUR_EVENING_END,
    FULL_MOON, NEW_MOON
}

data class SkyEvent(
    val type: SkyEventType,
    val epochMillis: Long,
    val label: String
)
