package com.homeping.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "homeping_prefs",
)

class PreferencesRepository(private val context: Context) {

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            deviceId = prefs[Keys.DEVICE_ID].orEmpty(),
            displayName = prefs[Keys.DISPLAY_NAME].orEmpty(),
            homePin = prefs[Keys.HOME_PIN].orEmpty(),
            setupComplete = prefs[Keys.SETUP_COMPLETE] ?: false,
            pairedPeerId = prefs[Keys.PAIRED_PEER_ID].orEmpty(),
            pairedPeerName = prefs[Keys.PAIRED_PEER_NAME].orEmpty(),
            alertSoundId = prefs[Keys.ALERT_SOUND_ID] ?: "default",
        )
    }

    suspend fun ensureDeviceId(): String {
        val current = preferences.first().deviceId
        if (current.isNotBlank()) return current
        val id = UUID.randomUUID().toString()
        context.dataStore.edit { it[Keys.DEVICE_ID] = id }
        return id
    }

    suspend fun setDisplayName(name: String) {
        context.dataStore.edit { it[Keys.DISPLAY_NAME] = name.trim() }
    }

    suspend fun setHomePin(pin: String) {
        context.dataStore.edit { it[Keys.HOME_PIN] = pin.trim() }
    }

    suspend fun completeSetup(displayName: String, homePin: String) {
        ensureDeviceId()
        context.dataStore.edit { prefs ->
            prefs[Keys.DISPLAY_NAME] = displayName.trim()
            prefs[Keys.HOME_PIN] = homePin.trim()
            prefs[Keys.SETUP_COMPLETE] = true
        }
    }

    suspend fun setSetupComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.SETUP_COMPLETE] = complete }
    }

    suspend fun clearPairedPeer() {
        context.dataStore.edit { prefs ->
            prefs[Keys.PAIRED_PEER_ID] = ""
            prefs[Keys.PAIRED_PEER_NAME] = ""
        }
    }

    private object Keys {
        val DEVICE_ID = stringPreferencesKey("device_id")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val HOME_PIN = stringPreferencesKey("home_pin")
        val SETUP_COMPLETE = booleanPreferencesKey("setup_complete")
        val PAIRED_PEER_ID = stringPreferencesKey("paired_peer_id")
        val PAIRED_PEER_NAME = stringPreferencesKey("paired_peer_name")
        val ALERT_SOUND_ID = stringPreferencesKey("alert_sound_id")
    }

    companion object {
        @Volatile
        private var instance: PreferencesRepository? = null

        fun getInstance(context: Context): PreferencesRepository {
            return instance ?: synchronized(this) {
                instance ?: PreferencesRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
