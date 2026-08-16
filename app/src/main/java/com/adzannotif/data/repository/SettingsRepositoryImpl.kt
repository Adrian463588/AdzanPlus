package com.adzannotif.data.repository

import com.adzannotif.data.datastore.AppDataStore
import com.adzannotif.domain.model.AllAlarmSettings
import com.adzannotif.domain.model.UserSettings
import com.adzannotif.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val appDataStore: AppDataStore,
) : SettingsRepository {

    override val userSettings: Flow<UserSettings> = appDataStore.userSettingsFlow

    override val alarmSettings: Flow<AllAlarmSettings> = appDataStore.alarmSettingsFlow

    override suspend fun updateUserSettings(transform: (UserSettings) -> UserSettings) {
        appDataStore.updateUserSettings(transform)
    }

    override suspend fun updateAlarmSettings(transform: (AllAlarmSettings) -> AllAlarmSettings) {
        appDataStore.updateAlarmSettings(transform)
    }
}
