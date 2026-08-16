package com.adzannotif.core.astronomy

data class TwilightTimes(
    val civilDawn: Long?,
    val nauticalDawn: Long?,
    val astronomicalDawn: Long?,
    val astronomicalDusk: Long?,
    val nauticalDusk: Long?,
    val civilDusk: Long?
)
