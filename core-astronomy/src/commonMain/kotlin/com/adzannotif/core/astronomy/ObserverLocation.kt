package com.adzannotif.core.astronomy

data class ObserverLocation(
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double,
    /** IANA timezone used for civil-day event windows. */
    val timeZoneId: String,
)
