package com.adzannotif.core.prayer

import kotlinx.serialization.Serializable

/**
 * Represents geographic coordinates with latitude and longitude.
 *
 * @property latitude Latitude in decimal degrees (-90.0 to 90.0)
 * @property longitude Longitude in decimal degrees (-180.0 to 180.0)
 */
@Serializable
data class Coordinates(
    val latitude: Double,
    val longitude: Double
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90.0 and 90.0, was $latitude" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180.0 and 180.0, was $longitude" }
    }
}
