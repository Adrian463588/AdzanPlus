package com.adzannotif.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "astronomy_cache")
data class AstronomyCacheEntity(
    @PrimaryKey val cacheKey: String,  // "${dateYYYYMMDD}_${lat4dp}_${lon4dp}"
    val dateEpochMillis: Long,
    val latitudeDeg: Double,
    val longitudeDeg: Double,
    val sunriseMillis: Long?,
    val sunsetMillis: Long?,
    val solarNoonMillis: Long?,
    val moonriseMillis: Long?,
    val moonsetMillis: Long?,
    val moonPhaseOrdinal: Int,
    val moonIlluminationPercent: Double,
    val moonDistanceKm: Double,
    val moonAgeInDays: Double,
    val goldenHourMorningStartMillis: Long?,
    val goldenHourMorningEndMillis: Long?,
    val goldenHourEveningStartMillis: Long?,
    val goldenHourEveningEndMillis: Long?,
    val blueHourMorningStartMillis: Long?,
    val blueHourMorningEndMillis: Long?,
    val blueHourEveningStartMillis: Long?,
    val blueHourEveningEndMillis: Long?,
    val civilDawnMillis: Long?,
    val civilDuskMillis: Long?,
    val cachedAtMillis: Long  // for cache invalidation (>24h = stale)
)
