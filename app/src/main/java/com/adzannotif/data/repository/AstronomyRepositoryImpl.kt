package com.adzannotif.data.repository

import com.adzannotif.core.astronomy.AstronomyEngine
import com.adzannotif.core.astronomy.HijriDate
import com.adzannotif.core.astronomy.MoonPhase
import com.adzannotif.core.astronomy.ObserverLocation
import com.adzannotif.data.local.dao.AstronomyCacheDao
import com.adzannotif.data.local.entity.AstronomyCacheEntity
import com.adzannotif.domain.model.astronomy.CalendarDay
import com.adzannotif.domain.model.astronomy.ConstellationData
import com.adzannotif.domain.model.astronomy.MoonInfo
import com.adzannotif.domain.model.astronomy.SkyEvent
import com.adzannotif.domain.model.astronomy.SkyEventType
import com.adzannotif.domain.model.astronomy.StarMapData
import com.adzannotif.domain.model.astronomy.SunInfo
import com.adzannotif.domain.model.astronomy.VisibleStar
import com.adzannotif.domain.repository.AstronomyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AstronomyRepositoryImpl @Inject constructor(
    private val engine: AstronomyEngine,
    private val cacheDao: AstronomyCacheDao
) : AstronomyRepository {

    override fun getSunInfo(latDeg: Double, lonDeg: Double, epochMillis: Long): Flow<SunInfo> = flow {
        val location = ObserverLocation(latDeg, lonDeg)
        val state = engine.getSunState(location, epochMillis)
        emit(
            SunInfo(
                riseMillis = state.riseMillis,
                setMillis = state.setMillis,
                noonMillis = state.noonMillis,
                azimuth = state.position.azimuth,
                altitude = state.position.altitude,
                azimuthAtRise = state.azimuthAtRise,
                azimuthAtSet = state.azimuthAtSet,
                currentPhase = state.currentPhase.displayName,
                civilDawnMillis = state.twilight.civilDawn,
                civilDuskMillis = state.twilight.civilDusk,
                nauticalDawnMillis = state.twilight.nauticalDawn,
                nauticalDuskMillis = state.twilight.nauticalDusk,
                astronomicalDawnMillis = state.twilight.astronomicalDawn,
                astronomicalDuskMillis = state.twilight.astronomicalDusk,
                morningGoldenHourStartMillis = state.goldenBlueHour.morningGoldenHour?.startMillis,
                morningGoldenHourEndMillis = state.goldenBlueHour.morningGoldenHour?.endMillis,
                eveningGoldenHourStartMillis = state.goldenBlueHour.eveningGoldenHour?.startMillis,
                eveningGoldenHourEndMillis = state.goldenBlueHour.eveningGoldenHour?.endMillis,
                morningBlueHourStartMillis = state.goldenBlueHour.morningBlueHour?.startMillis,
                morningBlueHourEndMillis = state.goldenBlueHour.morningBlueHour?.endMillis,
                eveningBlueHourStartMillis = state.goldenBlueHour.eveningBlueHour?.startMillis,
                eveningBlueHourEndMillis = state.goldenBlueHour.eveningBlueHour?.endMillis
            )
        )
    }

    override fun getMoonInfo(latDeg: Double, lonDeg: Double, epochMillis: Long): Flow<MoonInfo> = flow {
        val location = ObserverLocation(latDeg, lonDeg)
        val state = engine.getMoonState(location, epochMillis)
        emit(
            MoonInfo(
                riseMillis = state.riseMillis,
                setMillis = state.setMillis,
                transitMillis = state.transitMillis,
                azimuth = state.position.azimuth,
                altitude = state.position.altitude,
                azimuthAtRise = state.azimuthAtRise,
                phaseName = state.phase.displayName,
                phaseOrdinal = state.phase.ordinal,
                illuminationPercent = state.illuminationFraction * 100.0,
                ageInDays = state.ageInDays,
                distanceKm = state.distanceKm,
                isApogee = state.isApogee,
                isPerigee = state.isPerigee
            )
        )
    }

    override fun getStarMapData(latDeg: Double, lonDeg: Double, epochMillis: Long): Flow<StarMapData> = flow {
        val location = ObserverLocation(latDeg, lonDeg)
        val visibleStars = engine.getVisibleStars(location, epochMillis).map { sp ->
            VisibleStar(
                hipId = sp.star.hipId,
                name = sp.star.name,
                azimuth = sp.azimuth,
                altitude = sp.altitude,
                magnitude = sp.star.magnitude
            )
        }
        val constellations = engine.getConstellations().map { c ->
            ConstellationData(
                name = c.name,
                abbreviation = c.abbreviation,
                lines = c.lines.map { l -> Pair(l.fromHipId, l.toHipId) }
            )
        }
        val sunPos = engine.getSunState(location, epochMillis).position
        val moonPos = engine.getMoonState(location, epochMillis).position
        emit(
            StarMapData(
                visibleStars = visibleStars,
                constellations = constellations,
                sunAzimuth = sunPos.azimuth,
                sunAltitude = sunPos.altitude,
                moonAzimuth = moonPos.azimuth,
                moonAltitude = moonPos.altitude,
                observerLatitude = latDeg,
                observerLongitude = lonDeg,
                epochMillis = epochMillis
            )
        )
    }

    override suspend fun getHijriDate(gregorianEpochMillis: Long): HijriDate {
        return engine.getHijriDate(gregorianEpochMillis)
    }

    override suspend fun getMonthCalendar(latDeg: Double, lonDeg: Double, year: Int, month: Int): List<CalendarDay> {
        val cal = Calendar.getInstance().apply {
            set(year, month - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val location = ObserverLocation(latDeg, lonDeg)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        return (1..daysInMonth).map { day ->
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dayMillis = cal.timeInMillis

            val sunState = engine.getSunState(location, dayMillis)
            val moonState = engine.getMoonState(location, dayMillis)
            val hijri = engine.getHijriDate(dayMillis)

            CalendarDay(
                gregorianEpochMillis = dayMillis,
                gregorianDay = day,
                gregorianMonth = month,
                gregorianYear = year,
                hijriDay = hijri.day,
                hijriMonth = hijri.month,
                hijriYear = hijri.year,
                hijriMonthName = hijri.monthName,
                prayerTimes = null, // injected separately when needed
                moonPhaseName = moonState.phase.displayName,
                moonPhaseOrdinal = moonState.phase.ordinal,
                moonIlluminationPercent = moonState.illuminationFraction * 100.0,
                isNewMoon = moonState.phase == MoonPhase.NEW_MOON,
                isFullMoon = moonState.phase == MoonPhase.FULL_MOON,
                goldenHourMorningStartMillis = sunState.goldenBlueHour.morningGoldenHour?.startMillis,
                sunriseMillis = sunState.riseMillis,
                sunsetMillis = sunState.setMillis
            )
        }
    }

    override suspend fun getUpcomingEvents(
        latDeg: Double, lonDeg: Double, fromMillis: Long, days: Int
    ): List<SkyEvent> {
        val location = ObserverLocation(latDeg, lonDeg)
        val events = mutableListOf<SkyEvent>()
        for (i in 0 until days) {
            val dayMillis = fromMillis + i * 86_400_000L
            engine.getDayEvents(location, dayMillis).forEach { astroEvent ->
                val (type, label) = when (astroEvent) {
                    is com.adzannotif.core.astronomy.AstronomyEvent.Sunrise ->
                        SkyEventType.SUNRISE to "Matahari Terbit"
                    is com.adzannotif.core.astronomy.AstronomyEvent.Sunset ->
                        SkyEventType.SUNSET to "Matahari Terbenam"
                    is com.adzannotif.core.astronomy.AstronomyEvent.Moonrise ->
                        SkyEventType.MOONRISE to "Bulan Terbit"
                    is com.adzannotif.core.astronomy.AstronomyEvent.Moonset ->
                        SkyEventType.MOONSET to "Bulan Terbenam"
                    is com.adzannotif.core.astronomy.AstronomyEvent.GoldenHourStart ->
                        if (astroEvent.isMorning) SkyEventType.GOLDEN_HOUR_MORNING_START to "Golden Hour Pagi Dimulai"
                        else SkyEventType.GOLDEN_HOUR_EVENING_START to "Golden Hour Sore Dimulai"
                    is com.adzannotif.core.astronomy.AstronomyEvent.GoldenHourEnd ->
                        if (astroEvent.isMorning) SkyEventType.GOLDEN_HOUR_MORNING_END to "Golden Hour Pagi Berakhir"
                        else SkyEventType.GOLDEN_HOUR_EVENING_END to "Golden Hour Sore Berakhir"
                    is com.adzannotif.core.astronomy.AstronomyEvent.BlueHourStart ->
                        if (astroEvent.isMorning) SkyEventType.BLUE_HOUR_MORNING_START to "Blue Hour Pagi"
                        else SkyEventType.BLUE_HOUR_EVENING_START to "Blue Hour Sore"
                    is com.adzannotif.core.astronomy.AstronomyEvent.BlueHourEnd ->
                        if (astroEvent.isMorning) SkyEventType.BLUE_HOUR_MORNING_START to "Blue Hour Pagi Berakhir"
                        else SkyEventType.BLUE_HOUR_EVENING_START to "Blue Hour Sore Berakhir"
                    is com.adzannotif.core.astronomy.AstronomyEvent.FullMoon ->
                        SkyEventType.FULL_MOON to "Bulan Purnama"
                    is com.adzannotif.core.astronomy.AstronomyEvent.NewMoon ->
                        SkyEventType.NEW_MOON to "Bulan Baru"
                }
                events.add(SkyEvent(type = type, epochMillis = astroEvent.epochMillis, label = label))
            }
        }
        return events.sortedBy { it.epochMillis }
    }
}
