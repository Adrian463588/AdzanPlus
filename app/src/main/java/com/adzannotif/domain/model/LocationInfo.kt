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
    val country: String = "Indonesia",
    val latitude: Double,
    val longitude: Double,
    val elevation: Double = 0.0,
    val timeZoneId: String = "Asia/Jakarta",
    val isAutoDetected: Boolean = false,
) {
    fun toCoordinates(): Coordinates = Coordinates(latitude, longitude)

    companion object {
        val JAKARTA = LocationInfo(
            id = "jakarta",
            name = "Jakarta",
            country = "Indonesia",
            latitude = -6.2088,
            longitude = 106.8456,
            elevation = 10.0,
            timeZoneId = "Asia/Jakarta",
            isAutoDetected = false,
        )
    }
}
