package com.adzannotif.domain.model.astronomy

data class StarMapData(
    val visibleStars: List<VisibleStar>,  // altitude > -5°
    val constellations: List<ConstellationData>,
    val sunAzimuth: Double,
    val sunAltitude: Double,
    val moonAzimuth: Double,
    val moonAltitude: Double,
    val observerLatitude: Double,
    val observerLongitude: Double,
    val epochMillis: Long
)

data class VisibleStar(
    val hipId: Int,
    val name: String?,
    val azimuth: Double,
    val altitude: Double,
    val magnitude: Double
)

data class ConstellationData(
    val name: String,
    val abbreviation: String,
    val lines: List<Pair<Int, Int>>  // hipId pairs
)
