package com.adzannotif.domain.model

import com.adzannotif.core.prayer.Prayer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AlarmConfigSettingsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun testDefaultAlarmSettings() {
        val allSettings = AllAlarmSettings()
        assertTrue(allSettings.fajr.isEnabled)
        assertEquals(AdhanSoundType.FULL_ADHAN, allSettings.fajr.soundType)
        assertFalse(allSettings.sunrise.isEnabled)
        assertEquals(AdhanSoundType.BEEP_NOTIFICATION, allSettings.sunrise.soundType)
    }

    @Test
    fun testUpdateSoundTypeToSilent() {
        val allSettings = AllAlarmSettings()
        val fajrConfig = allSettings.fajr
        val updatedFajr = fajrConfig.copy(soundType = AdhanSoundType.SILENT)

        val updatedAll = allSettings.updateConfig(updatedFajr)
        assertEquals(AdhanSoundType.SILENT, updatedAll.fajr.soundType)
        assertTrue(updatedAll.fajr.isEnabled)

        // Ensure other prayers remain unaffected
        assertEquals(AdhanSoundType.FULL_ADHAN, updatedAll.dhuhr.soundType)
        assertEquals(AdhanSoundType.FULL_ADHAN, updatedAll.maghrib.soundType)
    }

    @Test
    fun testSerializationWithSilentSoundType() {
        val original = AllAlarmSettings(
            fajr = AlarmConfig(Prayer.FAJR, soundType = AdhanSoundType.SILENT),
            dhuhr = AlarmConfig(Prayer.DHUHR, soundType = AdhanSoundType.BEEP_NOTIFICATION),
            maghrib = AlarmConfig(Prayer.MAGHRIB, soundType = AdhanSoundType.FULL_ADHAN),
        )

        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<AllAlarmSettings>(serialized)

        assertEquals(AdhanSoundType.SILENT, deserialized.fajr.soundType)
        assertEquals(AdhanSoundType.BEEP_NOTIFICATION, deserialized.dhuhr.soundType)
        assertEquals(AdhanSoundType.FULL_ADHAN, deserialized.maghrib.soundType)
    }
}
