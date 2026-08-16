package com.adzannotif.domain.usecase

import com.adzannotif.core.prayer.CalculationMethod
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.UserSettings
import com.adzannotif.domain.repository.LocationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DomainUseCasesTest {

    @Test
    fun testGetQiblaDirectionCalculation() = runTest {
        val fakeLocation = LocationInfo(
            id = "test_jakarta",
            name = "Jakarta",
            country = "Indonesia",
            latitude = -6.2088,
            longitude = 106.8456,
            timeZoneId = "Asia/Jakarta"
        )
        val fakeLocationRepo = object : LocationRepository {
            override val currentOrSelectedLocation = flowOf(fakeLocation)
            override val favoriteLocations = flowOf(listOf(fakeLocation))
            override suspend fun getDeviceLocation() = Result.success(fakeLocation)
            override suspend fun searchOfflineCities(query: String) = listOf(fakeLocation)
            override suspend fun getAllOfflineCities() = listOf(fakeLocation)
            override suspend fun saveLocation(location: LocationInfo) {}
            override suspend fun deleteLocation(locationId: String) {}
        }

        val useCase = GetQiblaDirectionUseCase(fakeLocationRepo)
        val qibla = useCase().first()

        assertNotNull(qibla)
        // Jakarta Qibla is roughly 295° Northwest
        assertTrue(qibla.directionDegrees in 290.0..300.0, "Jakarta Qibla angle should be ~295°")
        // Distance from Jakarta to Kaaba is ~7900 km
        assertTrue(qibla.distanceKm in 7800.0..8000.0, "Jakarta distance to Kaaba should be ~7900 km")
    }
}
