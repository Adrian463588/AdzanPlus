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
