package com.adzannotif.astronomy

import com.adzannotif.core.astronomy.HijriDate
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.astronomy.CalendarDay
import com.adzannotif.domain.model.astronomy.MoonInfo
import com.adzannotif.domain.model.astronomy.SkyEvent
import com.adzannotif.domain.model.astronomy.StarMapData
import com.adzannotif.domain.model.astronomy.SunInfo
import com.adzannotif.domain.repository.AstronomyRepository
import com.adzannotif.domain.usecase.GetMoonInfoUseCase
import com.adzannotif.domain.usecase.GetSunInfoUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AstronomyUseCaseTest {
    private val location = LocationInfo(
        id = "manual-test",
        name = "Test location",
        country = "Test",
        latitude = -6.2,
        longitude = 106.8,
        elevation = 0.0,
        timeZoneId = "Asia/Jakarta",
    )

    @Test
    fun moonUseCasePreservesLiveNextRiseAndPhaseValues() = runTest {
        val expected = MoonInfo(
            riseMillis = 1_000L,
            setMillis = null,
            transitMillis = null,
            azimuth = 120.0,
            altitude = 15.0,
            azimuthAtRise = 90.0,
            phaseName = "Waxing Crescent",
            phaseOrdinal = 1,
            illuminationPercent = 23.0,
            ageInDays = 4.0,
            distanceKm = 385_000.0,
            isApogee = false,
            isPerigee = false,
            nextRiseMillis = 1_000L,
        )

        val actual = GetMoonInfoUseCase(FakeAstronomyRepository(moon = expected))
            .invoke(location, 500L)
            .first()

        assertEquals(expected, actual)
    }

    @Test
    fun sunUseCasePreservesNextEventAndCivilTwilightValues() = runTest {
        val expected = SunInfo(
            riseMillis = 2_000L,
            setMillis = 10_000L,
            noonMillis = 6_000L,
            azimuth = 90.0,
            altitude = -2.0,
            azimuthAtRise = 80.0,
            azimuthAtSet = 280.0,
            currentPhase = "Civil Twilight",
            civilDawnMillis = 1_500L,
            civilDuskMillis = 10_500L,
            nauticalDawnMillis = null,
            nauticalDuskMillis = null,
            astronomicalDawnMillis = null,
            astronomicalDuskMillis = null,
            morningGoldenHourStartMillis = null,
            morningGoldenHourEndMillis = null,
            eveningGoldenHourStartMillis = null,
            eveningGoldenHourEndMillis = null,
            morningBlueHourStartMillis = null,
            morningBlueHourEndMillis = null,
            eveningBlueHourStartMillis = null,
            eveningBlueHourEndMillis = null,
            nextEventMillis = 1_500L,
            nextEventName = "Civil Twilight fajar",
        )

        val actual = GetSunInfoUseCase(FakeAstronomyRepository(sun = expected))
            .invoke(location, 500L)
            .first()

        assertEquals(expected, actual)
    }

    private class FakeAstronomyRepository(
        private val moon: MoonInfo? = null,
        private val sun: SunInfo? = null,
    ) : AstronomyRepository {
        override fun getSunInfo(location: LocationInfo, epochMillis: Long): Flow<SunInfo> =
            flowOf(sun ?: error("Sun fixture is not configured"))
        override fun getMoonInfo(location: LocationInfo, epochMillis: Long): Flow<MoonInfo> =
            flowOf(moon ?: error("Moon fixture is not configured"))
        override fun getStarMapData(location: LocationInfo, epochMillis: Long): Flow<StarMapData> = flowOf(
            StarMapData(emptyList(), emptyList(), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, epochMillis),
        )
        override suspend fun getHijriDate(gregorianEpochMillis: Long, timeZoneId: String): HijriDate =
            error("Not used by this test")
        override suspend fun getMonthCalendar(location: LocationInfo, year: Int, month: Int): List<CalendarDay> =
            emptyList()
        override suspend fun getUpcomingEvents(location: LocationInfo, fromMillis: Long, days: Int): List<SkyEvent> =
            emptyList()
    }
}
