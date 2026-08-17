package com.adzannotif.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppContractsTest {
    @Test
    fun unavailableSnapshotDoesNotExposeComputedTime() {
        val snapshot = PrayerUiSnapshot()

        assertEquals(SnapshotAvailability.UNAVAILABLE, snapshot.availability)
        assertNull(snapshot.targetInstant())
        assertNull(snapshot.locationName)
    }

    @Test
    fun celestialSnapshotKeepsUnavailableStateExplicit() {
        val snapshot = CelestialUiSnapshot(kind = CelestialSnapshotKind.MOON)

        assertEquals(SnapshotAvailability.UNAVAILABLE, snapshot.availability)
        assertNull(snapshot.nextEventEpochMillis)
        assertNull(snapshot.phaseName)
    }

    @Test
    fun routeIdsRemainStableAcrossHosts() {
        assertEquals("home", SharedRoute.HOME.id)
        assertEquals("astronomy_dashboard", SharedRoute.ASTRONOMY.id)
    }
}
