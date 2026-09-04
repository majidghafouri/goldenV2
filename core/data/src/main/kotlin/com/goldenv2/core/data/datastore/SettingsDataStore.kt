package com.goldenv2.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.goldenv2.core.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    private val context: Context
) {
    private object Keys {
        val SETTINGS_JSON = stringPreferencesKey("settings_json")
        val FIRST_RUN = booleanPreferencesKey("first_run")
        val LAST_SELECTED_SERVER_ID = stringPreferencesKey("last_selected_server_id")
    }

    private val defaultSettings = AppSettings()

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .map { prefs ->
            val json = prefs[Keys.SETTINGS_JSON] ?: Json.encodeToString(AppSettings.serializer(), defaultSettings)
            Json.decodeFromString<AppSettings>(json)
        }

    val firstRunFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[Keys.FIRST_RUN] ?: true }

    val lastSelectedServerIdFlow: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[Keys.LAST_SELECTED_SERVER_ID] }

    suspend fun saveSettings(settings: AppSettings) {
        val json = Json.encodeToString(AppSettings.serializer(), settings)
        context.dataStore.edit { prefs ->
            prefs[Keys.SETTINGS_JSON] = json
        }
    }

    suspend fun setFirstRunComplete() {
        context.dataStore.edit { prefs ->
            prefs[Keys.FIRST_RUN] = false
        }
    }

    suspend fun saveLastSelectedServerId(serverId: String?) {
        context.dataStore.edit { prefs ->
            if (serverId != null) {
                prefs[Keys.LAST_SELECTED_SERVER_ID] = serverId
            } else {
                prefs.remove(Keys.LAST_SELECTED_SERVER_ID)
            }
        }
    }

    suspend fun getSettings(): AppSettings {
        val prefs = context.dataStore.data.first()
        val json = prefs[Keys.SETTINGS_JSON] ?: return defaultSettings
        return Json.decodeFromString<AppSettings>(json)
    }

    suspend fun isFirstRun(): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[Keys.FIRST_RUN] ?: true
    }
}
