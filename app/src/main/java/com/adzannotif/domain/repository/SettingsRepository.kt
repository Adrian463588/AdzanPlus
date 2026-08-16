package com.adzannotif.domain.repository

import com.adzannotif.domain.model.AllAlarmSettings
import com.adzannotif.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val userSettings: Flow<UserSettings>
    val alarmSettings: Flow<AllAlarmSettings>
    
    suspend fun updateUserSettings(transform: (UserSettings) -> UserSettings)
    suspend fun updateAlarmSettings(transform: (AllAlarmSettings) -> AllAlarmSettings)
}
