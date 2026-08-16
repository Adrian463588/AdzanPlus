package com.adzannotif.domain.repository

import com.adzannotif.domain.model.LocationInfo
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    val currentOrSelectedLocation: Flow<LocationInfo>
    val favoriteLocations: Flow<List<LocationInfo>>
    
    suspend fun getDeviceLocation(): Result<LocationInfo>
    suspend fun searchOfflineCities(query: String): List<LocationInfo>
    suspend fun getAllOfflineCities(): List<LocationInfo>
    suspend fun saveLocation(location: LocationInfo)
    suspend fun deleteLocation(locationId: String)
}
