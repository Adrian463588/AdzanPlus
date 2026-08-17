package com.adzannotif.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.adzannotif.data.local.dao.PrayerScheduleDao
import com.adzannotif.data.local.dao.SavedLocationDao
import com.adzannotif.data.local.entity.PrayerScheduleEntity
import com.adzannotif.data.local.entity.SavedLocationEntity

@Database(
    entities = [
        PrayerScheduleEntity::class,
        SavedLocationEntity::class,
        com.adzannotif.data.local.entity.AstronomyCacheEntity::class,
    ],
    version = 2,
    exportSchema = false
)
abstract class PrayerDatabase : RoomDatabase() {
    abstract fun prayerScheduleDao(): PrayerScheduleDao
    abstract fun savedLocationDao(): SavedLocationDao
    abstract fun astronomyCacheDao(): com.adzannotif.data.local.dao.AstronomyCacheDao
}
