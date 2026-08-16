package com.adzannotif.data.repository

import android.annotation.SuppressLint
import android.location.Location
import com.adzannotif.data.datastore.AppDataStore
import com.adzannotif.data.local.city.OfflineCityDatabase
import com.adzannotif.data.local.dao.SavedLocationDao
import com.adzannotif.data.local.entity.SavedLocationEntity
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val appDataStore: AppDataStore,
    private val savedLocationDao: SavedLocationDao,
    private val offlineCityDatabase: OfflineCityDatabase,
    private val fusedLocationClient: FusedLocationProviderClient,
) : LocationRepository {

    override val currentOrSelectedLocation: Flow<LocationInfo> = appDataStore.userSettingsFlow.map { settings ->
        settings.selectedLocation
    }

    override val favoriteLocations: Flow<List<LocationInfo>> = savedLocationDao.getAllLocations().map { list ->
        if (list.isEmpty()) {
            listOf(LocationInfo.JAKARTA)
        } else {
            list.map { it.toDomain() }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getDeviceLocation(): Result<LocationInfo> {
        return try {
            val cts = CancellationTokenSource()
            val location: Location? = awaitTask(
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cts.token
                )
            )

            if (location != null) {
                val tzId = TimeZone.getDefault().id
                val locationInfo = LocationInfo(
                    id = "current_gps",
                    name = "Lokasi Saya",
                    country = "Indonesia",
                    latitude = location.latitude,
                    longitude = location.longitude,
                    elevation = location.altitude,
                    timeZoneId = tzId,
                    isAutoDetected = true
                )
                Result.success(locationInfo)
            } else {
                // Fallback to last known location or Jakarta
                val lastLoc: Location? = awaitTask(fusedLocationClient.lastLocation)
                if (lastLoc != null) {
                    val locationInfo = LocationInfo(
                        id = "last_known_gps",
                        name = "Lokasi Terakhir",
                        country = "Indonesia",
                        latitude = lastLoc.latitude,
                        longitude = lastLoc.longitude,
                        elevation = lastLoc.altitude,
                        timeZoneId = TimeZone.getDefault().id,
                        isAutoDetected = true
                    )
                    Result.success(locationInfo)
                } else {
                    Result.success(LocationInfo.JAKARTA)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun <T> awaitTask(task: Task<T>): T? = suspendCancellableCoroutine { cont ->
        task.addOnSuccessListener { result ->
            cont.resume(result)
        }.addOnFailureListener { exception ->
            cont.resumeWithException(exception)
        }.addOnCanceledListener {
            cont.cancel()
        }
    }

    override suspend fun searchOfflineCities(query: String): List<LocationInfo> {
        return offlineCityDatabase.searchCities(query)
    }

    override suspend fun getAllOfflineCities(): List<LocationInfo> {
        return offlineCityDatabase.allCities
    }

    override suspend fun saveLocation(location: LocationInfo) {
        savedLocationDao.insertLocation(SavedLocationEntity.fromDomain(location))
    }

    override suspend fun deleteLocation(locationId: String) {
        savedLocationDao.deleteLocationById(locationId)
    }
}
