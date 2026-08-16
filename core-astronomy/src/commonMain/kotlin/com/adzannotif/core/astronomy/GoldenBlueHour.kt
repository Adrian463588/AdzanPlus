package com.adzannotif.core.astronomy

data class GoldenBlueHourWindow(val startMillis: Long, val endMillis: Long)
data class GoldenBlueHour(
    val morningBlueHour: GoldenBlueHourWindow?,
    val morningGoldenHour: GoldenBlueHourWindow?,
    val eveningGoldenHour: GoldenBlueHourWindow?,
    val eveningBlueHour: GoldenBlueHourWindow?
)
