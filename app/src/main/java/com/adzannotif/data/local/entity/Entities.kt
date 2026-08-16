package com.adzannotif.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.adzannotif.core.prayer.Coordinates
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.PrayerTimeRecord
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

@Entity(tableName = "prayer_schedules")
data class PrayerScheduleEntity(
    @PrimaryKey
    val id: String, // format: "YYYY-MM-DD_locationId"
    val dateString: String, // "YYYY-MM-DD"
    val locationId: String,
    val latitude: Double,
    val longitude: Double,
    val imsakEpochMs: Long,
    val fajrEpochMs: Long,
    val sunriseEpochMs: Long,
    val dhuhrEpochMs: Long,
    val asrEpochMs: Long,
    val maghribEpochMs: Long,
    val ishaEpochMs: Long,
    val midnightEpochMs: Long,
) {
    fun toDomain(): PrayerTimeRecord {
        return PrayerTimeRecord(
            date = LocalDate.parse(dateString),
            coordinates = Coordinates(latitude, longitude),
            imsak = Instant.fromEpochMilliseconds(imsakEpochMs),
            fajr = Instant.fromEpochMilliseconds(fajrEpochMs),
            sunrise = Instant.fromEpochMilliseconds(sunriseEpochMs),
            dhuhr = Instant.fromEpochMilliseconds(dhuhrEpochMs),
            asr = Instant.fromEpochMilliseconds(asrEpochMs),
            maghrib = Instant.fromEpochMilliseconds(maghribEpochMs),
            isha = Instant.fromEpochMilliseconds(ishaEpochMs),
            midnight = Instant.fromEpochMilliseconds(midnightEpochMs),
        )
    }

    companion object {
        fun fromDomain(record: PrayerTimeRecord, locationId: String): PrayerScheduleEntity {
            return PrayerScheduleEntity(
                id = "${record.date}_$locationId",
                dateString = record.date.toString(),
                locationId = locationId,
                latitude = record.coordinates.latitude,
                longitude = record.coordinates.longitude,
                imsakEpochMs = record.imsak.toEpochMilliseconds(),
                fajrEpochMs = record.fajr.toEpochMilliseconds(),
                sunriseEpochMs = record.sunrise.toEpochMilliseconds(),
                dhuhrEpochMs = record.dhuhr.toEpochMilliseconds(),
                asrEpochMs = record.asr.toEpochMilliseconds(),
                maghribEpochMs = record.maghrib.toEpochMilliseconds(),
                ishaEpochMs = record.isha.toEpochMilliseconds(),
                midnightEpochMs = record.midnight.toEpochMilliseconds(),
            )
        }
    }
}

@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    val timeZoneId: String,
    val isAutoDetected: Boolean,
    val lastUpdatedEpochMs: Long,
) {
    fun toDomain(): LocationInfo = LocationInfo(
        id = id,
        name = name,
        country = country,
        latitude = latitude,
        longitude = longitude,
        elevation = elevation,
        timeZoneId = timeZoneId,
        isAutoDetected = isAutoDetected,
    )

    companion object {
        fun fromDomain(location: LocationInfo, timestampMs: Long = System.currentTimeMillis()): SavedLocationEntity {
            return SavedLocationEntity(
                id = location.id,
                name = location.name,
                country = location.country,
                latitude = location.latitude,
                longitude = location.longitude,
                elevation = location.elevation,
                timeZoneId = location.timeZoneId,
                isAutoDetected = location.isAutoDetected,
                lastUpdatedEpochMs = timestampMs,
            )
        }
    }
}
