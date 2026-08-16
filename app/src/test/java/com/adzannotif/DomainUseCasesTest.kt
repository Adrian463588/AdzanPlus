package com.adzannotif

import com.adzannotif.core.prayer.CalculationMethod
import com.adzannotif.core.prayer.Prayer
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.UserSettings
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.repository.PrayerTimesRepository
import com.adzannotif.domain.repository.SettingsRepository
import com.adzannotif.domain.usecase.GetNextPrayerUseCase
import com.adzannotif.domain.usecase.GetQiblaDirectionUseCase
import com.adzannotif.domain.usecase.GetTodayPrayerTimesUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainUseCasesTest {

    private val fakeLocation = LocationInfo(
        id = "test_jakarta",
        name = "Jakarta",
        country = "Indonesia",
        latitude = -6.2088,
        longitude = 106.8456,
        timeZoneId = "Asia/Jakarta"
    )

    private val fakeSettings = UserSettings(
        calculationMethod = CalculationMethod.KEMENAG_RI
    )

    @Test
    fun testGetQiblaDirectionCalculation() = runTest {
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
        assertTrue("Jakarta Qibla angle should be ~295°", qibla.directionDegrees in 290.0..300.0)
        // Distance from Jakarta to Kaaba is ~7900 km
        assertTrue("Jakarta distance to Kaaba should be ~7900 km", qibla.distanceKm in 7800.0..8000.0)
    }
}
