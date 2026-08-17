package com.adzannotif.presentation.home

import java.util.Locale

internal fun formatCountdown(secondsRemaining: Long): String {
    val safeSeconds = secondsRemaining.coerceAtLeast(0L)
    val hours = safeSeconds / 3_600L
    val minutes = (safeSeconds % 3_600L) / 60L
    val seconds = safeSeconds % 60L
    return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
}
