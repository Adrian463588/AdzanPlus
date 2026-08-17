package com.adzannotif.presentation.ui

import com.adzannotif.presentation.home.formatCountdown
import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownTextTest {
    @Test
    fun formatsAndClampsCountdownWithoutChangingTheTargetShape() {
        assertEquals("-00:00:00", formatCountdown(-1))
        assertEquals("-01:01:01", formatCountdown(3_661))
    }
}
