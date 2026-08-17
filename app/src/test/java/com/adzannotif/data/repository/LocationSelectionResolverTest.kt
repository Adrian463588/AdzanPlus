package com.adzannotif.data.repository

import com.adzannotif.domain.model.LocationInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocationSelectionResolverTest {
    private val savedLocation = LocationInfo(
        id = "saved-yogyakarta",
        name = "Yogyakarta",
        country = "Indonesia",
        latitude = -7.7956,
        longitude = 110.3695,
        elevation = 113.0,
        timeZoneId = "Asia/Jakarta",
    )

    private val selectedLocation = savedLocation.copy(
        id = "selected-gps",
        name = "GPS",
        isAutoDetected = true,
    )

    @Test
    fun selectedLocationWinsOverSavedOfflineBackup() {
        assertEquals(
            selectedLocation,
            LocationSelectionResolver.resolve(selectedLocation, listOf(savedLocation)),
        )
    }

    @Test
    fun savedLocationRestoresOfflineSessionWhenSelectionPreferenceIsMissing() {
        assertEquals(
            savedLocation,
            LocationSelectionResolver.resolve(null, listOf(savedLocation)),
        )
    }

    @Test
    fun emptySourcesRemainUnavailableInsteadOfUsingAdefault() {
        assertNull(LocationSelectionResolver.resolve(null, emptyList()))
    }
}
