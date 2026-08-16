package com.adzannotif.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.adzannotif.domain.model.AllAlarmSettings
import com.adzannotif.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings_pref")
private val Context.alarmDataStore: DataStore<Preferences> by preferencesDataStore(name = "alarm_settings_pref")

@Singleton
class AppDataStore @Inject constructor(
    private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private val userSettingsKey = stringPreferencesKey("key_user_settings_json")
    private val alarmSettingsKey = stringPreferencesKey("key_alarm_settings_json")

    val userSettingsFlow: Flow<UserSettings> = context.settingsDataStore.data.map { preferences ->
        val rawJson = preferences[userSettingsKey]
        if (rawJson != null) {
            try {
                json.decodeFromString<UserSettings>(rawJson)
            } catch (e: Exception) {
                UserSettings()
            }
        } else {
            UserSettings()
        }
    }

    suspend fun updateUserSettings(transform: (UserSettings) -> UserSettings) {
        context.settingsDataStore.edit { preferences ->
            val currentRaw = preferences[userSettingsKey]
            val current = if (currentRaw != null) {
                try {
                    json.decodeFromString<UserSettings>(currentRaw)
                } catch (e: Exception) {
                    UserSettings()
                }
            } else {
                UserSettings()
            }
            val updated = transform(current)
            preferences[userSettingsKey] = json.encodeToString(updated)
        }
    }

    val alarmSettingsFlow: Flow<AllAlarmSettings> = context.alarmDataStore.data.map { preferences ->
        val rawJson = preferences[alarmSettingsKey]
        if (rawJson != null) {
            try {
                json.decodeFromString<AllAlarmSettings>(rawJson)
            } catch (e: Exception) {
                AllAlarmSettings()
            }
        } else {
            AllAlarmSettings()
        }
    }

    suspend fun updateAlarmSettings(transform: (AllAlarmSettings) -> AllAlarmSettings) {
        context.alarmDataStore.edit { preferences ->
            val currentRaw = preferences[alarmSettingsKey]
            val current = if (currentRaw != null) {
                try {
                    json.decodeFromString<AllAlarmSettings>(currentRaw)
                } catch (e: Exception) {
                    AllAlarmSettings()
                }
            } else {
                AllAlarmSettings()
            }
            val updated = transform(current)
            preferences[alarmSettingsKey] = json.encodeToString(updated)
        }
    }
}
