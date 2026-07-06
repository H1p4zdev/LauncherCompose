package com.android.launcher3.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.android.launcher3.model.DeviceProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "launcher_prefs")

object PreferencesKeys {
    val GRID_ROWS = intPreferencesKey("grid_rows")
    val GRID_COLUMNS = intPreferencesKey("grid_columns")
    val HOTSEAT_COUNT = intPreferencesKey("hotseat_count")
    val SHOW_NOTIFICATION_DOTS = booleanPreferencesKey("show_notification_dots")
    val SHOW_SEARCH_BAR = booleanPreferencesKey("show_search_bar")
    val WORKSPACE_PAGE_COUNT = intPreferencesKey("workspace_page_count")
}

class LauncherPreferences(private val context: Context) {

    val gridRows: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.GRID_ROWS] ?: 5
    }

    val gridColumns: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.GRID_COLUMNS] ?: 4
    }

    val hotseatCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.HOTSEAT_COUNT] ?: 5
    }

    val showNotificationDots: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SHOW_NOTIFICATION_DOTS] ?: true
    }

    val showSearchBar: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SHOW_SEARCH_BAR] ?: true
    }

    val workspacePageCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.WORKSPACE_PAGE_COUNT] ?: 1
    }

    val deviceProfile: Flow<DeviceProfile> = context.dataStore.data.map { prefs ->
        DeviceProfile(
            rows = prefs[PreferencesKeys.GRID_ROWS] ?: 5,
            columns = prefs[PreferencesKeys.GRID_COLUMNS] ?: 4,
            hotseatCount = prefs[PreferencesKeys.HOTSEAT_COUNT] ?: 5,
            iconSizeDp = 48f,
            gridPaddingDp = 8f
        )
    }

    suspend fun setGridRows(rows: Int) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.GRID_ROWS] = rows }
    }

    suspend fun setGridColumns(columns: Int) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.GRID_COLUMNS] = columns }
    }

    suspend fun setHotseatCount(count: Int) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.HOTSEAT_COUNT] = count }
    }

    suspend fun setShowNotificationDots(show: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.SHOW_NOTIFICATION_DOTS] = show }
    }

    suspend fun setShowSearchBar(show: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.SHOW_SEARCH_BAR] = show }
    }

    suspend fun setWorkspacePageCount(count: Int) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.WORKSPACE_PAGE_COUNT] = count }
    }
}
