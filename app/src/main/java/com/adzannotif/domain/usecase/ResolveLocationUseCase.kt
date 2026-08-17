package com.adzannotif.domain.usecase

import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ResolveLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<LocationInfo?> {
        return locationRepository.currentOrSelectedLocation
    }

    suspend fun refreshDeviceLocation(): Result<LocationInfo> {
        val result = locationRepository.getDeviceLocation()
        if (result.isSuccess) {
            val location = result.getOrThrow()
            locationRepository.saveLocation(location)
            settingsRepository.updateUserSettings { current ->
                current.copy(selectedLocation = location, useAutoLocation = true)
            }
        }
        return result
    }

    suspend fun selectManualLocation(location: LocationInfo) {
        locationRepository.saveLocation(location)
        settingsRepository.updateUserSettings { current ->
            current.copy(selectedLocation = location, useAutoLocation = false)
        }
    }
}
