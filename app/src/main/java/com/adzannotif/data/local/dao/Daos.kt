package com.adzannotif.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.adzannotif.data.local.entity.PrayerScheduleEntity
import com.adzannotif.data.local.entity.SavedLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerScheduleDao {
    @Query("SELECT * FROM prayer_schedules WHERE dateString = :dateString AND locationId = :locationId LIMIT 1")
    fun getScheduleForDate(dateString: String, locationId: String): Flow<PrayerScheduleEntity?>

    @Query("SELECT * FROM prayer_schedules WHERE dateString LIKE :yearMonthPrefix || '%' AND locationId = :locationId ORDER BY dateString ASC")
    fun getMonthlySchedules(yearMonthPrefix: String, locationId: String): Flow<List<PrayerScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<PrayerScheduleEntity>)

    @Query(
        "DELETE FROM prayer_schedules " +
            "WHERE locationId = :locationId AND (dateString < :firstDate OR dateString > :lastDate)",
    )
    suspend fun deleteOutsideWindow(locationId: String, firstDate: String, lastDate: String)

    @Query("DELETE FROM prayer_schedules WHERE locationId = :locationId")
    suspend fun clearSchedulesForLocation(locationId: String)
}

@Dao
interface SavedLocationDao {
    @Query("SELECT * FROM saved_locations ORDER BY lastUpdatedEpochMs DESC")
    fun getAllLocations(): Flow<List<SavedLocationEntity>>

    @Query("SELECT * FROM saved_locations WHERE id = :id LIMIT 1")
    suspend fun getLocationById(id: String): SavedLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: SavedLocationEntity)

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteLocationById(id: String)
}

@Dao
interface AstronomyCacheDao {
    @androidx.room.Upsert
    suspend fun upsert(entity: com.adzannotif.data.local.entity.AstronomyCacheEntity)

    @Query("SELECT * FROM astronomy_cache WHERE cacheKey = :key")
    suspend fun queryByKey(key: String): com.adzannotif.data.local.entity.AstronomyCacheEntity?

    @Query("DELETE FROM astronomy_cache WHERE cachedAtMillis < :cutoffMillis")
    suspend fun deleteStale(cutoffMillis: Long)

    @Query("DELETE FROM astronomy_cache WHERE dateEpochMillis < :firstMillis OR dateEpochMillis > :lastMillis")
    suspend fun deleteOutsideWindow(firstMillis: Long, lastMillis: Long)
}
