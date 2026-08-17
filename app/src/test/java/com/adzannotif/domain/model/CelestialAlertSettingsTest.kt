package com.adzannotif.domain.model

import com.adzannotif.domain.model.astronomy.SkyEventType
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CelestialAlertSettingsTest {

    @Test
    fun disabledByDefaultDoesNotScheduleCelestialEvents() {
        val settings = CelestialAlertSettings()

        assertFalse(settings.isEnabled(SkyEventType.MOONRISE))
        assertFalse(settings.isEnabled(SkyEventType.FULL_MOON))
        assertFalse(settings.isEnabled(SkyEventType.SUNRISE))
    }

    @Test
    fun enabledAlertMapsToBothMorningAndEveningStartEvents() {
        val settings = CelestialAlertSettings(goldenHourStart = true)

        assertTrue(settings.isEnabled(SkyEventType.GOLDEN_HOUR_MORNING_START))
        assertTrue(settings.isEnabled(SkyEventType.GOLDEN_HOUR_EVENING_START))
        assertFalse(settings.isEnabled(SkyEventType.GOLDEN_HOUR_MORNING_END))
    }

    @Test
    fun updateKeepsOtherAlertPreferences() {
        val settings = CelestialAlertSettings(moonrise = true)

        val updated = settings.withEnabled(CelestialAlertType.NEW_MOON, true)

        assertTrue(updated.isEnabled(SkyEventType.MOONRISE))
        assertTrue(updated.isEnabled(SkyEventType.NEW_MOON))
    }
}
