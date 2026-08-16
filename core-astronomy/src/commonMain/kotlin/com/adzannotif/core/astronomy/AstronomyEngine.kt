package com.adzannotif.core.astronomy

import com.adzannotif.core.astronomy.internal.HijriCalendar
import com.adzannotif.core.astronomy.internal.MoonMath
import com.adzannotif.core.astronomy.internal.PhotoPhasePolicy
import com.adzannotif.core.astronomy.internal.StarMath
import com.adzannotif.core.astronomy.internal.SunMath
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Platform-agnostic resource loader. Implement this interface on the Android side
 * to provide the star catalog and constellation JSON bundled as assets.
 * This keeps :core-astronomy 100% free of android.* imports.
 */
fun interface AstronomyResourceLoader {
    /** Returns the UTF-8 text content of a bundled resource by name. */
    fun loadResource(name: String): String
}

@Serializable
private data class StarJson(
    val hipId: Int,
    val name: String? = null,
    val ra: Double,
    val dec: Double,
    val magnitude: Double
)

@Serializable
private data class ConstellationLineJson(val fromHipId: Int, val toHipId: Int)

@Serializable
private data class ConstellationJson(
    val abbreviation: String,
    val name: String,
    val lines: List<ConstellationLineJson>
)

/**
 * Main entry point for astronomical computations.
 *
 * @param resourceLoader Platform-specific loader for bundled JSON assets.
 *   Pass `null` on platforms that do not need star map functionality.
 */
class AstronomyEngine(
    private val resourceLoader: AstronomyResourceLoader? = null
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Lazily parsed star catalog — loaded once on first use. */
    private val starCatalog: List<Star> by lazy {
        val loader = resourceLoader ?: return@lazy emptyList()
        val raw = loader.loadResource("star_catalog.json")
        json.decodeFromString<List<StarJson>>(raw).map { s ->
            Star(
                hipId = s.hipId,
                name = s.name,
                ra = s.ra,
                dec = s.dec,
                magnitude = s.magnitude
            )
        }
    }

    /** Lazily parsed constellation catalog — loaded once on first use. */
    private val constellationCatalog: List<Constellation> by lazy {
        val loader = resourceLoader ?: return@lazy emptyList()
        val raw = loader.loadResource("constellation_lines.json")
        json.decodeFromString<List<ConstellationJson>>(raw).map { c ->
            Constellation(
                abbreviation = c.abbreviation,
                name = c.name,
                lines = c.lines.map { l -> ConstellationLine(l.fromHipId, l.toHipId) }
            )
        }
    }

    fun getSunState(location: ObserverLocation, epochMillis: Long): SunState {
        val lat = location.latitude
        val lon = location.longitude
        val el = location.elevationMeters
        val pos = SunMath.computeSunPosition(lat, lon, el, epochMillis)
        val riseMs = SunMath.computeSunRise(lat, lon, epochMillis)
        val setMs = SunMath.computeSunSet(lat, lon, epochMillis)
        return SunState(
            position = pos,
            riseMillis = riseMs,
            setMillis = setMs,
            noonMillis = SunMath.computeSolarNoon(lat, lon, epochMillis),
            azimuthAtRise = riseMs?.let { SunMath.computeSunPosition(lat, lon, el, it).azimuth } ?: 90.0,
            azimuthAtSet = setMs?.let { SunMath.computeSunPosition(lat, lon, el, it).azimuth } ?: 270.0,
            twilight = SunMath.computeTwilightTimes(lat, lon, epochMillis),
            goldenBlueHour = PhotoPhasePolicy.computeGoldenBlueHour(lat, lon, epochMillis),
            currentPhase = PhotoPhasePolicy.classifySolarPhase(pos.altitude)
        )
    }

    fun getMoonState(location: ObserverLocation, epochMillis: Long): MoonState {
        val lat = location.latitude
        val lon = location.longitude
        val el = location.elevationMeters
        val riseMillis = MoonMath.computeMoonRise(lat, lon, epochMillis)
        val dist = MoonMath.computeMoonDistanceKm(epochMillis)
        return MoonState(
            position = MoonMath.computeMoonPosition(lat, lon, el, epochMillis),
            riseMillis = riseMillis,
            setMillis = MoonMath.computeMoonSet(lat, lon, epochMillis),
            transitMillis = MoonMath.computeMoonTransit(lat, lon, epochMillis),
            azimuthAtRise = riseMillis?.let { MoonMath.computeMoonPosition(lat, lon, el, it).azimuth } ?: 0.0,
            phase = MoonMath.computeMoonPhase(epochMillis),
            illuminationFraction = MoonMath.computeMoonIllumination(epochMillis),
            ageInDays = MoonMath.computeMoonAgeInDays(epochMillis),
            distanceKm = dist,
            isApogee = dist > 404000.0,
            isPerigee = dist < 365000.0
        )
    }

    fun getHijriDate(gregorianEpochMillis: Long): HijriDate {
        return HijriCalendar.toHijri(gregorianEpochMillis)
    }

    /**
     * Returns visible star positions (altitude > -5°) for the observer at the given time.
     * Loads star catalog from bundled JSON resource via [resourceLoader].
     * Returns empty list if no resource loader was provided.
     */
    fun getVisibleStars(location: ObserverLocation, epochMillis: Long): List<StarPosition> {
        return starCatalog
            .map { star -> StarMath.computeStarPosition(star, location.latitude, location.longitude, epochMillis) }
            .filter { it.altitude > -5.0 }
    }

    /**
     * Returns the constellation catalog (names + stick figure lines).
     * Required by StarMapScreen to draw constellation overlays.
     */
    fun getConstellations(): List<Constellation> = constellationCatalog

    fun getDayEvents(location: ObserverLocation, dateEpochMillis: Long): List<AstronomyEvent> {
        val events = mutableListOf<AstronomyEvent>()
        val sun = getSunState(location, dateEpochMillis)
        val moon = getMoonState(location, dateEpochMillis)

        sun.riseMillis?.let { events.add(AstronomyEvent.Sunrise(it, sun.azimuthAtRise)) }
        sun.setMillis?.let { events.add(AstronomyEvent.Sunset(it, sun.azimuthAtSet)) }

        moon.riseMillis?.let { events.add(AstronomyEvent.Moonrise(it, moon.azimuthAtRise)) }
        moon.setMillis?.let { events.add(AstronomyEvent.Moonset(it)) }

        sun.goldenBlueHour.morningGoldenHour?.startMillis?.let { events.add(AstronomyEvent.GoldenHourStart(it, true)) }
        sun.goldenBlueHour.morningGoldenHour?.endMillis?.let { events.add(AstronomyEvent.GoldenHourEnd(it, true)) }
        sun.goldenBlueHour.eveningGoldenHour?.startMillis?.let { events.add(AstronomyEvent.GoldenHourStart(it, false)) }
        sun.goldenBlueHour.eveningGoldenHour?.endMillis?.let { events.add(AstronomyEvent.GoldenHourEnd(it, false)) }

        sun.goldenBlueHour.morningBlueHour?.startMillis?.let { events.add(AstronomyEvent.BlueHourStart(it, true)) }
        sun.goldenBlueHour.morningBlueHour?.endMillis?.let { events.add(AstronomyEvent.BlueHourEnd(it, true)) }
        sun.goldenBlueHour.eveningBlueHour?.startMillis?.let { events.add(AstronomyEvent.BlueHourStart(it, false)) }
        sun.goldenBlueHour.eveningBlueHour?.endMillis?.let { events.add(AstronomyEvent.BlueHourEnd(it, false)) }

        if (moon.phase == MoonPhase.FULL_MOON) events.add(AstronomyEvent.FullMoon(dateEpochMillis))
        if (moon.phase == MoonPhase.NEW_MOON) events.add(AstronomyEvent.NewMoon(dateEpochMillis))

        return events.sortedBy { it.epochMillis }
    }
}
