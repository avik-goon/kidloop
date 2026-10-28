package com.kidloop.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kidloop.app.data.model.DailyUsage
import com.kidloop.app.data.model.KidLoopSettings
import com.kidloop.app.data.model.PlaylistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kidloop_preferences")

interface SettingsRepository {
    val settingsFlow: Flow<KidLoopSettings>
    val dailyUsageFlow: Flow<DailyUsage>
    val currentItemIndexFlow: Flow<Int>

    suspend fun savePin(salt: String, hash: String)
    suspend fun updateSessionLimit(minutes: Int)
    suspend fun updateDailyLimit(minutes: Int)
    suspend fun setAllowVolumeKeys(allow: Boolean)
    suspend fun setKidModeActive(active: Boolean)
    suspend fun saveItems(items: List<PlaylistItem>)
    suspend fun addItem(item: PlaylistItem)
    suspend fun removeItem(itemId: String)
    suspend fun reorderItems(fromIndex: Int, toIndex: Int)
    suspend fun saveDailyUsage(dateString: String, usedSeconds: Long)
    suspend fun resetDailyUsage(todayDateString: String)
    suspend fun saveCurrentItemIndex(index: Int)
}

class DataStoreSettingsRepository(
    private val context: Context
) : SettingsRepository {

    private object PreferencesKeys {
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val SESSION_LIMIT_MINUTES = intPreferencesKey("session_limit_minutes")
        val DAILY_LIMIT_MINUTES = intPreferencesKey("daily_limit_minutes")
        val ALLOW_VOLUME_KEYS = booleanPreferencesKey("allow_volume_keys")
        val IS_KID_MODE_ACTIVE = booleanPreferencesKey("is_kid_mode_active")
        val PLAYLIST_ITEMS_RAW = stringPreferencesKey("playlist_items_raw")
        val LAST_USAGE_DATE = stringPreferencesKey("last_usage_date")
        val USED_SECONDS_TODAY = longPreferencesKey("used_seconds_today")
        val CURRENT_ITEM_INDEX = intPreferencesKey("current_item_index")
    }

    override val settingsFlow: Flow<KidLoopSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val salt = prefs[PreferencesKeys.PIN_SALT].orEmpty()
            val hash = prefs[PreferencesKeys.PIN_HASH].orEmpty()
            val sessionLimit = prefs[PreferencesKeys.SESSION_LIMIT_MINUTES] ?: 20
            val dailyLimit = prefs[PreferencesKeys.DAILY_LIMIT_MINUTES] ?: 60
            val allowVolume = prefs[PreferencesKeys.ALLOW_VOLUME_KEYS] ?: true
            val isKidModeActive = prefs[PreferencesKeys.IS_KID_MODE_ACTIVE] ?: false
            val itemsRaw = prefs[PreferencesKeys.PLAYLIST_ITEMS_RAW].orEmpty()
            val items = if (itemsRaw.isBlank()) {
                emptyList()
            } else {
                itemsRaw.split(";;").mapNotNull { PlaylistItem.fromSerializedString(it) }
            }

            KidLoopSettings(
                pinSalt = salt,
                pinHash = hash,
                sessionLimitMinutes = sessionLimit,
                dailyLimitMinutes = dailyLimit,
                allowVolumeKeys = allowVolume,
                isKidModeActive = isKidModeActive,
                items = items
            )
        }

    override val dailyUsageFlow: Flow<DailyUsage> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            DailyUsage(
                dateString = prefs[PreferencesKeys.LAST_USAGE_DATE].orEmpty(),
                usedSecondsToday = prefs[PreferencesKeys.USED_SECONDS_TODAY] ?: 0L
            )
        }

    override val currentItemIndexFlow: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            prefs[PreferencesKeys.CURRENT_ITEM_INDEX] ?: 0
        }

    override suspend fun savePin(salt: String, hash: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.PIN_SALT] = salt
            prefs[PreferencesKeys.PIN_HASH] = hash
        }
    }

    override suspend fun updateSessionLimit(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SESSION_LIMIT_MINUTES] = minutes.coerceIn(1, 480)
        }
    }

    override suspend fun updateDailyLimit(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DAILY_LIMIT_MINUTES] = minutes.coerceIn(1, 1440)
        }
    }

    override suspend fun setAllowVolumeKeys(allow: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.ALLOW_VOLUME_KEYS] = allow
        }
    }

    override suspend fun setKidModeActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_KID_MODE_ACTIVE] = active
        }
    }

    override suspend fun saveItems(items: List<PlaylistItem>) {
        val serialized = items.joinToString(";;") { it.toSerializedString() }
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.PLAYLIST_ITEMS_RAW] = serialized
        }
    }

    override suspend fun addItem(item: PlaylistItem) {
        val currentSettings = settingsFlow.first()
        val updated = currentSettings.items + item
        saveItems(updated)
    }

    override suspend fun removeItem(itemId: String) {
        val currentSettings = settingsFlow.first()
        val updated = currentSettings.items.filterNot { it.id == itemId }
        saveItems(updated)
    }

    override suspend fun reorderItems(fromIndex: Int, toIndex: Int) {
        val currentSettings = settingsFlow.first()
        val currentList = currentSettings.items.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices && fromIndex != toIndex) {
            val moved = currentList.removeAt(fromIndex)
            currentList.add(toIndex, moved)
            saveItems(currentList)
        }
    }

    override suspend fun saveDailyUsage(dateString: String, usedSeconds: Long) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.LAST_USAGE_DATE] = dateString
            prefs[PreferencesKeys.USED_SECONDS_TODAY] = usedSeconds
        }
    }

    override suspend fun resetDailyUsage(todayDateString: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.LAST_USAGE_DATE] = todayDateString
            prefs[PreferencesKeys.USED_SECONDS_TODAY] = 0L
        }
    }

    override suspend fun saveCurrentItemIndex(index: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.CURRENT_ITEM_INDEX] = index
        }
    }
}
