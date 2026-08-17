package com.adzannotif.data.repository

import com.adzannotif.core.astronomy.AstronomyEngine
import com.adzannotif.core.astronomy.AstronomyEvent
import com.adzannotif.core.astronomy.HijriDate
import com.adzannotif.core.astronomy.MoonPhase
import com.adzannotif.core.astronomy.ObserverLocation
import com.adzannotif.data.local.dao.AstronomyCacheDao
import com.adzannotif.data.local.entity.AstronomyCacheEntity
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.astronomy.CalendarDay
import com.adzannotif.domain.model.astronomy.ConstellationData
import com.adzannotif.domain.model.astronomy.MoonInfo
import com.adzannotif.domain.model.astronomy.SkyEvent
import com.adzannotif.domain.model.astronomy.SkyEventType
import com.adzannotif.domain.model.astronomy.StarMapData
import com.adzannotif.domain.model.astronomy.SunInfo
import com.adzannotif.domain.model.astronomy.VisibleStar
import com.adzannotif.domain.repository.AstronomyRepository
import com.adzannotif.domain.repository.PrayerTimesRepository
import com.adzannotif.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AstronomyRepositoryImpl @Inject constructor(
    private val engine: AstronomyEngine,
    private val cacheDao: AstronomyCacheDao,
    private val prayerTimesRepository: PrayerTimesRepository,
    private val settingsRepository: SettingsRepository,
) : AstronomyRepository {

    override fun getSunInfo(locationInfo: LocationInfo, epochMillis: Long): Flow<SunInfo> = flow {
        val day = loadDay(locationInfo, epochMillis)
        val state = day.sunState
        val cache = day.cache
        val nextEvent = nextSunEvent(locationInfo, epochMillis)
        emit(
            SunInfo(
                calculationEpochMillis = epochMillis,
                riseMillis = cache?.sunriseMillis ?: state.riseMillis,
                setMillis = cache?.sunsetMillis ?: state.setMillis,
                noonMillis = cache?.solarNoonMillis ?: state.noonMillis,
                azimuth = state.position.azimuth,
                altitude = state.position.altitude,
                azimuthAtRise = state.azimuthAtRise,
                azimuthAtSet = state.azimuthAtSet,
                currentPhase = state.currentPhase.displayName,
                civilDawnMillis = cache?.civilDawnMillis ?: state.twilight.civilDawn,
                civilDuskMillis = cache?.civilDuskMillis ?: state.twilight.civilDusk,
                nauticalDawnMillis = state.twilight.nauticalDawn,
                nauticalDuskMillis = state.twilight.nauticalDusk,
                astronomicalDawnMillis = state.twilight.astronomicalDawn,
                astronomicalDuskMillis = state.twilight.astronomicalDusk,
                morningGoldenHourStartMillis = cache?.goldenHourMorningStartMillis
                    ?: state.goldenBlueHour.morningGoldenHour?.startMillis,
                morningGoldenHourEndMillis = cache?.goldenHourMorningEndMillis
                    ?: state.goldenBlueHour.morningGoldenHour?.endMillis,
                eveningGoldenHourStartMillis = cache?.goldenHourEveningStartMillis
                    ?: state.goldenBlueHour.eveningGoldenHour?.startMillis,
                eveningGoldenHourEndMillis = cache?.goldenHourEveningEndMillis
                    ?: state.goldenBlueHour.eveningGoldenHour?.endMillis,
                morningBlueHourStartMillis = cache?.blueHourMorningStartMillis
                    ?: state.goldenBlueHour.morningBlueHour?.startMillis,
                morningBlueHourEndMillis = cache?.blueHourMorningEndMillis
                    ?: state.goldenBlueHour.morningBlueHour?.endMillis,
                eveningBlueHourStartMillis = cache?.blueHourEveningStartMillis
                    ?: state.goldenBlueHour.eveningBlueHour?.startMillis,
                eveningBlueHourEndMillis = cache?.blueHourEveningEndMillis
                    ?: state.goldenBlueHour.eveningBlueHour?.endMillis,
                nextEventMillis = nextEvent?.epochMillis,
                nextEventName = nextEvent?.label,
            )
        )
    }

    override fun getMoonInfo(locationInfo: LocationInfo, epochMillis: Long): Flow<MoonInfo> = flow {
        val day = loadDay(locationInfo, epochMillis)
        val state = day.moonState
        val nextRiseMillis = nextMoonrise(locationInfo, epochMillis, state)
        emit(
            MoonInfo(
                // Moonrise is a live next-event value. If today's rise has
                // passed, continue through the next local civil day using the
                // same engine and cache path instead of fabricating a value.
                riseMillis = nextRiseMillis,
                nextRiseMillis = nextRiseMillis,
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

    private suspend fun nextMoonrise(
        locationInfo: LocationInfo,
        epochMillis: Long,
        currentState: com.adzannotif.core.astronomy.MoonState,
    ): Long? {
        val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone(locationInfo.timeZoneId)).apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        repeat(3) { dayOffset ->
            val dayState = if (dayOffset == 0) {
                currentState
            } else {
                loadDay(locationInfo, calendar.timeInMillis).moonState
            }
            val riseMillis = dayState.riseMillis?.takeIf { it > epochMillis }
            if (riseMillis != null) return riseMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return null
    }

    override fun getStarMapData(locationInfo: LocationInfo, epochMillis: Long): Flow<StarMapData> = flow {
        val location = locationInfo.toObserverLocation()
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
                observerLatitude = locationInfo.latitude,
                observerLongitude = locationInfo.longitude,
                epochMillis = epochMillis
            )
        )
    }

    override suspend fun getHijriDate(gregorianEpochMillis: Long, timeZoneId: String): HijriDate {
        return engine.getHijriDate(gregorianEpochMillis, kotlinx.datetime.TimeZone.of(timeZoneId))
    }

    override suspend fun getMonthCalendar(locationInfo: LocationInfo, year: Int, month: Int): List<CalendarDay> {
        val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone(locationInfo.timeZoneId)).apply {
            set(year, month - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val settings = settingsRepository.userSettings.first()

        return (1..daysInMonth).map { day ->
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dayMillis = cal.timeInMillis

            val cachedDay = loadDay(locationInfo, dayMillis)
            val sunState = cachedDay.sunState
            val moonState = cachedDay.moonState
            val hijri = engine.getHijriDate(
                dayMillis,
                kotlinx.datetime.TimeZone.of(locationInfo.timeZoneId),
            )
            val prayerTimes = runCatching {
                prayerTimesRepository.getPrayerTimesForDate(
                    date = kotlinx.datetime.LocalDate(year, month, day),
                    location = locationInfo,
                    settings = settings,
                ).firstOrNull()
            }.getOrNull()

            CalendarDay(
                gregorianEpochMillis = dayMillis,
                gregorianDay = day,
                gregorianMonth = month,
                gregorianYear = year,
                hijriDay = hijri.day,
                hijriMonth = hijri.month,
                hijriYear = hijri.year,
                hijriMonthName = hijri.monthName,
                prayerTimes = prayerTimes,
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
        locationInfo: LocationInfo,
        fromMillis: Long,
        days: Int,
    ): List<SkyEvent> {
        val location = locationInfo.toObserverLocation()
        val events = mutableListOf<SkyEvent>()
        val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone(locationInfo.timeZoneId)).apply {
            timeInMillis = fromMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        repeat(days) {
            val dayMillis = calendar.timeInMillis
            loadDay(locationInfo, dayMillis)
            engine.getDayEvents(location, dayMillis).forEach { astroEvent ->
                val type = when (astroEvent) {
                    is com.adzannotif.core.astronomy.AstronomyEvent.Sunrise ->
                        SkyEventType.SUNRISE
                    is com.adzannotif.core.astronomy.AstronomyEvent.Sunset ->
                        SkyEventType.SUNSET
                    is com.adzannotif.core.astronomy.AstronomyEvent.Moonrise ->
                        SkyEventType.MOONRISE
                    is com.adzannotif.core.astronomy.AstronomyEvent.Moonset ->
                        SkyEventType.MOONSET
                    is com.adzannotif.core.astronomy.AstronomyEvent.GoldenHourStart ->
                        if (astroEvent.isMorning) SkyEventType.GOLDEN_HOUR_MORNING_START
                        else SkyEventType.GOLDEN_HOUR_EVENING_START
                    is com.adzannotif.core.astronomy.AstronomyEvent.GoldenHourEnd ->
                        if (astroEvent.isMorning) SkyEventType.GOLDEN_HOUR_MORNING_END
                        else SkyEventType.GOLDEN_HOUR_EVENING_END
                    is com.adzannotif.core.astronomy.AstronomyEvent.BlueHourStart ->
                        if (astroEvent.isMorning) SkyEventType.BLUE_HOUR_MORNING_START
                        else SkyEventType.BLUE_HOUR_EVENING_START
                    is com.adzannotif.core.astronomy.AstronomyEvent.BlueHourEnd ->
                        if (astroEvent.isMorning) SkyEventType.BLUE_HOUR_MORNING_END
                        else SkyEventType.BLUE_HOUR_EVENING_END
                    is com.adzannotif.core.astronomy.AstronomyEvent.FullMoon ->
                        SkyEventType.FULL_MOON
                    is com.adzannotif.core.astronomy.AstronomyEvent.NewMoon ->
                        SkyEventType.NEW_MOON
                }
                events.add(SkyEvent(type = type, epochMillis = astroEvent.epochMillis, label = type.name))
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return events.sortedBy { it.epochMillis }
    }

    private suspend fun loadDay(locationInfo: LocationInfo, epochMillis: Long): CachedAstronomyDay {
        val key = cacheKey(locationInfo, epochMillis)
        val now = System.currentTimeMillis()
        val cacheEligible = epochMillis in (now - DAY_MILLIS)..(now + CACHE_RETENTION_MILLIS)
        val cached = if (cacheEligible) {
            cacheDao.queryByKey(key)?.takeIf {
                it.cachedAtMillis >= now - CACHE_RETENTION_MILLIS &&
                    it.dateEpochMillis.toCivilDayStart(locationInfo.timeZoneId) ==
                        epochMillis.toCivilDayStart(locationInfo.timeZoneId)
            }
        } else {
            null
        }
        val location = locationInfo.toObserverLocation()
        val sunState = engine.getSunState(location, epochMillis)
        val moonState = engine.getMoonState(location, epochMillis)
        if (cached == null && cacheEligible) {
            cacheDao.upsert(
                AstronomyCacheEntity(
                    cacheKey = key,
                    dateEpochMillis = epochMillis.toCivilDayStart(locationInfo.timeZoneId),
                    latitudeDeg = locationInfo.latitude,
                    longitudeDeg = locationInfo.longitude,
                    sunriseMillis = sunState.riseMillis,
                    sunsetMillis = sunState.setMillis,
                    solarNoonMillis = sunState.noonMillis,
                    moonriseMillis = moonState.riseMillis,
                    moonsetMillis = moonState.setMillis,
                    moonPhaseOrdinal = moonState.phase.ordinal,
                    moonIlluminationPercent = moonState.illuminationFraction * 100.0,
                    moonDistanceKm = moonState.distanceKm,
                    moonAgeInDays = moonState.ageInDays,
                    goldenHourMorningStartMillis = sunState.goldenBlueHour.morningGoldenHour?.startMillis,
                    goldenHourMorningEndMillis = sunState.goldenBlueHour.morningGoldenHour?.endMillis,
                    goldenHourEveningStartMillis = sunState.goldenBlueHour.eveningGoldenHour?.startMillis,
                    goldenHourEveningEndMillis = sunState.goldenBlueHour.eveningGoldenHour?.endMillis,
                    blueHourMorningStartMillis = sunState.goldenBlueHour.morningBlueHour?.startMillis,
                    blueHourMorningEndMillis = sunState.goldenBlueHour.morningBlueHour?.endMillis,
                    blueHourEveningStartMillis = sunState.goldenBlueHour.eveningBlueHour?.startMillis,
                    blueHourEveningEndMillis = sunState.goldenBlueHour.eveningBlueHour?.endMillis,
                    civilDawnMillis = sunState.twilight.civilDawn,
                    civilDuskMillis = sunState.twilight.civilDusk,
                    cachedAtMillis = now,
                )
            )
            cacheDao.deleteStale(now - CACHE_RETENTION_MILLIS)
            cacheDao.deleteOutsideWindow(
                firstMillis = now - DAY_MILLIS,
                lastMillis = now + CACHE_RETENTION_MILLIS,
            )
        }
        return CachedAstronomyDay(sunState, moonState, cached)
    }

    private fun cacheKey(location: LocationInfo, epochMillis: Long): String {
        val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone(location.timeZoneId)).apply {
            timeInMillis = epochMillis
        }
        return String.format(
            Locale.ROOT,
            "sun-moon-v2_%04d-%02d-%02d_%.4f_%.4f_%s",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
            location.latitude,
            location.longitude,
            location.timeZoneId,
        )
    }

    private suspend fun nextSunEvent(
        locationInfo: LocationInfo,
        fromMillis: Long,
    ): UpcomingSunEvent? {
        val location = locationInfo.toObserverLocation()
        val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone(locationInfo.timeZoneId)).apply {
            timeInMillis = fromMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val candidates = buildList {
            repeat(2) {
                val sunState = engine.getSunState(location, calendar.timeInMillis)
                sunState.twilight.civilDawn?.let {
                    if (it > fromMillis) add(UpcomingSunEvent(it, "CIVIL_TWILIGHT_DAWN"))
                }
                sunState.twilight.civilDusk?.let {
                    if (it > fromMillis) add(UpcomingSunEvent(it, "CIVIL_TWILIGHT_DUSK"))
                }
                engine.getDayEvents(location, calendar.timeInMillis).forEach { event ->
                    val label = event.sunEventKeyOrNull() ?: return@forEach
                    if (event.epochMillis > fromMillis) {
                        add(UpcomingSunEvent(event.epochMillis, label))
                    }
                }
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return candidates.minByOrNull { it.epochMillis }
    }

    private fun AstronomyEvent.sunEventKeyOrNull(): String? = when (this) {
        is AstronomyEvent.Sunrise -> SkyEventType.SUNRISE.name
        is AstronomyEvent.Sunset -> SkyEventType.SUNSET.name
        is AstronomyEvent.GoldenHourStart -> if (isMorning) {
            SkyEventType.GOLDEN_HOUR_MORNING_START.name
        } else {
            SkyEventType.GOLDEN_HOUR_EVENING_START.name
        }
        is AstronomyEvent.GoldenHourEnd -> if (isMorning) {
            SkyEventType.GOLDEN_HOUR_MORNING_END.name
        } else {
            SkyEventType.GOLDEN_HOUR_EVENING_END.name
        }
        is AstronomyEvent.BlueHourStart -> if (isMorning) {
            SkyEventType.BLUE_HOUR_MORNING_START.name
        } else {
            SkyEventType.BLUE_HOUR_EVENING_START.name
        }
        is AstronomyEvent.BlueHourEnd -> if (isMorning) {
            SkyEventType.BLUE_HOUR_MORNING_END.name
        } else {
            SkyEventType.BLUE_HOUR_EVENING_END.name
        }
        is AstronomyEvent.Moonrise,
        is AstronomyEvent.Moonset,
        is AstronomyEvent.FullMoon,
        is AstronomyEvent.NewMoon -> null
    }

    private fun Long.toCivilDayStart(timeZoneId: String): Long {
        val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone(timeZoneId)).apply {
            timeInMillis = this@toCivilDayStart
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun LocationInfo.toObserverLocation(): ObserverLocation = ObserverLocation(
        latitude = latitude,
        longitude = longitude,
        elevationMeters = elevation,
        timeZoneId = timeZoneId,
    )

    private data class CachedAstronomyDay(
        val sunState: com.adzannotif.core.astronomy.SunState,
        val moonState: com.adzannotif.core.astronomy.MoonState,
        val cache: AstronomyCacheEntity?,
    )

    private data class UpcomingSunEvent(val epochMillis: Long, val label: String)

    private companion object {
        const val CACHE_RETENTION_MILLIS = 7 * 24 * 60 * 60 * 1000L
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
