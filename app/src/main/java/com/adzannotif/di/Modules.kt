package com.adzannotif.di

import android.content.Context
import androidx.room.Room
import com.adzannotif.data.datastore.AppDataStore
import com.adzannotif.data.local.PrayerDatabase
import com.adzannotif.data.local.city.OfflineCityDatabase
import com.adzannotif.data.local.dao.PrayerScheduleDao
import com.adzannotif.data.local.dao.SavedLocationDao
import com.adzannotif.data.repository.AlarmRepositoryImpl
import com.adzannotif.data.repository.LocationRepositoryImpl
import com.adzannotif.data.repository.PrayerTimesRepositoryImpl
import com.adzannotif.core.astronomy.AstronomyEngine
import com.adzannotif.core.astronomy.AstronomyResourceLoader
import com.adzannotif.data.repository.SettingsRepositoryImpl
import com.adzannotif.domain.repository.AlarmRepository
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.repository.PrayerTimesRepository
import com.adzannotif.domain.repository.SettingsRepository
import com.adzannotif.platform.network.NetworkMonitor
import com.adzannotif.platform.network.NetworkMonitorImpl
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
        ).fallbackToDestructiveMigration().build()
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
}
