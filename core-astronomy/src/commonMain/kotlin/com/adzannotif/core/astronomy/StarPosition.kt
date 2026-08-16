package com.adzannotif.core.astronomy

import kotlinx.serialization.Serializable

@Serializable
data class Star(
    val hipId: Int,
    val name: String? = null,
    val ra: Double,
    val dec: Double,
    val magnitude: Double
)

data class StarPosition(
    val star: Star,
    val azimuth: Double,
    val altitude: Double
)

@Serializable
data class ConstellationLine(val fromHipId: Int, val toHipId: Int)

@Serializable
data class Constellation(
    val abbreviation: String,
    val name: String,
    val lines: List<ConstellationLine>
)
