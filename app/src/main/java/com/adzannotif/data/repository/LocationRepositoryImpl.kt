package com.adzannotif.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.adzannotif.data.datastore.AppDataStore
import com.adzannotif.data.local.city.OfflineCityDatabase
import com.adzannotif.data.local.dao.SavedLocationDao
import com.adzannotif.data.local.entity.SavedLocationEntity
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.platform.network.NetworkMonitor
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDataStore: AppDataStore,
    private val savedLocationDao: SavedLocationDao,
    private val offlineCityDatabase: OfflineCityDatabase,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val networkMonitor: NetworkMonitor,
) : LocationRepository {

    /**
     * DataStore is the selected-location source, while Room is the durable
     * offline backup written by every selection path. A corrupted or partial
     * preference write must not discard a real saved location.
     */
    override val currentOrSelectedLocation: Flow<LocationInfo?> = combine(
        appDataStore.userSettingsFlow,
        savedLocationDao.getAllLocations(),
    ) { settings, savedLocations ->
        LocationSelectionResolver.resolve(
            selected = settings.selectedLocation,
            saved = savedLocations.map { it.toDomain() },
        )
    }.map { it?.normalizeAutoDetectedTimeZone() }
        .distinctUntilChanged()

    override val favoriteLocations: Flow<List<LocationInfo>> = savedLocationDao.getAllLocations().map { list ->
        list.map { it.toDomain() }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getDeviceLocation(): Result<LocationInfo> {
        if (!hasLocationPermission()) {
            return Result.failure(LocationPermissionRequiredException())
        }

        return withContext(Dispatchers.IO) {
            try {
                // 1. Try High Accuracy first with timeout
                val highAccuracyLocation = getCurrentLocation(
                    priority = Priority.PRIORITY_HIGH_ACCURACY,
                    timeoutMillis = 6000L,
                )

                val finalLocation: Location? = highAccuracyLocation
                    ?: getCurrentLocation(
                        priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        timeoutMillis = 3000L,
                    )
                    ?: withTimeoutOrNull(1500L) {
                        awaitTask(fusedLocationClient.lastLocation)
                    }

                if (finalLocation != null && finalLocation.isUsable()) {
                    val lat = finalLocation.latitude
                    val lng = finalLocation.longitude
                    // Elevation is optional in Android Location. Prayer and
                    // astronomy engines accept sea-level when the provider
                    // has no altitude measurement; coordinates remain real.
                    val elevation = finalLocation.altitude.takeIf {
                        finalLocation.hasAltitude() && it.isFinite()
                    } ?: 0.0
                    val timeZoneId = TimeZone.getDefault().id

                    // Try geocoding online if connected
                    val addressName = resolveLocationName(lat, lng)

                    val locationInfo = LocationInfo(
                        id = "gps_${lat.hashCode()}_${lng.hashCode()}",
                        name = addressName.name,
                        country = addressName.country,
                        latitude = lat,
                        longitude = lng,
                        elevation = elevation,
                        timeZoneId = timeZoneId,
                        isAutoDetected = true
                    )
                    Result.success(locationInfo)
                } else if (finalLocation != null) {
                    Result.failure(InvalidLocationException())
                } else {
                    Result.failure(LocationUnavailableException())
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    class LocationUnavailableException : IllegalStateException(
        "Device location is unavailable; choose an offline city or enter coordinates manually",
    )

    class LocationPermissionRequiredException : SecurityException(
        "Location permission is required to refresh GPS location",
    )

    class InvalidLocationException : IllegalStateException(
        "The location provider returned invalid coordinates",
    )

    private data class ResolvedName(val name: String, val country: String)

    /**
     * GPS locations inherit the device's current zone. Re-evaluate that zone
     * whenever the selected GPS snapshot is consumed so a travel/time-zone
     * change cannot render or schedule events in a stale zone.
     */
    private fun LocationInfo.normalizeAutoDetectedTimeZone(): LocationInfo {
        if (!isAutoDetected) return this
        val currentTimeZoneId = TimeZone.getDefault().id
        return if (timeZoneId == currentTimeZoneId) this else copy(timeZoneId = currentTimeZoneId)
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(priority: Int, timeoutMillis: Long): Location? {
        val cancellationTokenSource = CancellationTokenSource()
        return try {
            withTimeoutOrNull(timeoutMillis) {
                awaitTask(
                    fusedLocationClient.getCurrentLocation(
                        priority,
                        cancellationTokenSource.token,
                    ),
                )
            }
        } finally {
            cancellationTokenSource.cancel()
        }
    }

    private fun Location.isUsable(): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0

    private suspend fun resolveLocationName(lat: Double, lng: Double): ResolvedName {
        val closest = offlineCityDatabase.findClosestCity(lat, lng)
        if (networkMonitor.isCurrentlyOnline() && Geocoder.isPresent()) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = withTimeoutOrNull(4000L) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suspendCancellableCoroutine<List<Address>> { cont ->
                            geocoder.getFromLocation(lat, lng, 1) { resultList ->
                                cont.resume(resultList)
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(lat, lng, 1) ?: emptyList()
                    }
                }

                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val locality = addr.subLocality ?: addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: closest.name
                    val country = addr.countryName ?: closest.country
                    return ResolvedName(locality, country)
                }
            } catch (_: Exception) {
                // Fallback to offline proximity
            }
        }

        // Offline spatial proximity fallback
        return ResolvedName("${closest.name} (GPS)", closest.country)
    }

    override suspend fun searchLocations(query: String): List<LocationInfo> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) offlineCityDatabase.allCities
        else offlineCityDatabase.searchCities(trimmed)
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

    override suspend fun setSelectedLocation(location: LocationInfo) {
        saveLocation(location)
        appDataStore.updateUserSettings { current ->
            current.copy(selectedLocation = location)
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
}
