package com.adzannotif.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.adzannotif.core.astronomy.AstronomyEngine
import com.adzannotif.core.astronomy.AstronomyResourceLoader
import com.adzannotif.data.local.PrayerDatabase
import com.adzannotif.data.local.dao.PrayerScheduleDao
import com.adzannotif.data.local.dao.SavedLocationDao
import com.adzannotif.data.repository.AlarmRepositoryImpl
import com.adzannotif.data.repository.LocationRepositoryImpl
import com.adzannotif.data.repository.PrayerTimesRepositoryImpl
import com.adzannotif.data.repository.SettingsRepositoryImpl
import com.adzannotif.domain.repository.AlarmRepository
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.repository.PrayerTimesRepository
import com.adzannotif.domain.repository.SettingsRepository
import com.adzannotif.platform.network.NetworkMonitor
import com.adzannotif.platform.network.NetworkMonitorImpl
import com.adzannotif.platform.alarm.AdhanScheduler
import com.adzannotif.platform.alarm.AlarmScheduler
import com.adzannotif.platform.audio.AdhanAudioPlayer
import com.adzannotif.platform.audio.AudioGateway
import com.adzannotif.platform.notification.NotificationGateway
import com.adzannotif.platform.notification.NotificationHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFusedLocationClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun providePrayerDatabase(@ApplicationContext context: Context): PrayerDatabase {
        return Room.databaseBuilder(
            context,
            PrayerDatabase::class.java,
            "prayer_times.db"
        )
            .addMigrations(PRAYER_DATABASE_MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun providePrayerScheduleDao(database: PrayerDatabase): PrayerScheduleDao {
        return database.prayerScheduleDao()
    }

    @Provides
    @Singleton
    fun provideSavedLocationDao(database: PrayerDatabase): SavedLocationDao {
        return database.savedLocationDao()
    }

    @Provides
    @Singleton
    fun provideAstronomyCacheDao(database: PrayerDatabase): com.adzannotif.data.local.dao.AstronomyCacheDao {
        return database.astronomyCacheDao()
    }

    @Provides
    @Singleton
    fun provideAstronomyEngine(@ApplicationContext context: Context): AstronomyEngine {
        val loader = AstronomyResourceLoader { name ->
            context.assets.open(name).bufferedReader().use { it.readText() }
        }
        return AstronomyEngine(resourceLoader = loader)
    }
}

private val PRAYER_DATABASE_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE prayer_schedules ADD COLUMN firstThirdOfNightEpochMs INTEGER"
        )
        database.execSQL(
            "ALTER TABLE prayer_schedules ADD COLUMN lastThirdOfNightEpochMs INTEGER"
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `astronomy_cache` (
                `cacheKey` TEXT NOT NULL,
                `dateEpochMillis` INTEGER NOT NULL,
                `latitudeDeg` REAL NOT NULL,
                `longitudeDeg` REAL NOT NULL,
                `sunriseMillis` INTEGER,
                `sunsetMillis` INTEGER,
                `solarNoonMillis` INTEGER,
                `moonriseMillis` INTEGER,
                `moonsetMillis` INTEGER,
                `moonPhaseOrdinal` INTEGER NOT NULL,
                `moonIlluminationPercent` REAL NOT NULL,
                `moonDistanceKm` REAL NOT NULL,
                `moonAgeInDays` REAL NOT NULL,
                `goldenHourMorningStartMillis` INTEGER,
                `goldenHourMorningEndMillis` INTEGER,
                `goldenHourEveningStartMillis` INTEGER,
                `goldenHourEveningEndMillis` INTEGER,
                `blueHourMorningStartMillis` INTEGER,
                `blueHourMorningEndMillis` INTEGER,
                `blueHourEveningStartMillis` INTEGER,
                `blueHourEveningEndMillis` INTEGER,
                `civilDawnMillis` INTEGER,
                `civilDuskMillis` INTEGER,
                `cachedAtMillis` INTEGER NOT NULL,
                PRIMARY KEY(`cacheKey`)
            )
            """.trimIndent()
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPrayerTimesRepository(impl: PrayerTimesRepositoryImpl): PrayerTimesRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    @Singleton
    abstract fun bindAlarmRepository(impl: AlarmRepositoryImpl): AlarmRepository

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: NetworkMonitorImpl): NetworkMonitor

    @Binds
    @Singleton
    abstract fun bindAstronomyRepository(impl: com.adzannotif.data.repository.AstronomyRepositoryImpl): com.adzannotif.domain.repository.AstronomyRepository

    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(impl: AdhanScheduler): AlarmScheduler

    @Binds
    @Singleton
    abstract fun bindNotificationGateway(impl: NotificationHelper): NotificationGateway

    @Binds
    @Singleton
    abstract fun bindAudioGateway(impl: AdhanAudioPlayer): AudioGateway
}
