package com.adzannotif.domain.model

import com.adzannotif.core.prayer.Coordinates
import kotlinx.serialization.Serializable

/**
 * Domain model representing a geographic location.
 */
@Serializable
data class LocationInfo(
    val id: String,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    val timeZoneId: String,
    val isAutoDetected: Boolean = false,
) {
    fun toCoordinates(): Coordinates = Coordinates(latitude, longitude)

}
