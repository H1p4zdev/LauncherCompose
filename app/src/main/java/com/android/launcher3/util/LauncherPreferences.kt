package com.android.launcher3.util

import android.content.ComponentName
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.android.launcher3.model.AppInfo
import com.android.launcher3.model.DeviceProfile
import com.android.launcher3.model.FolderInfo
import com.android.launcher3.model.HomeScreenPage
import com.android.launcher3.model.HotseatInfo
import com.android.launcher3.model.LauncherItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "launcher_prefs")

object PreferencesKeys {
    val GRID_ROWS = intPreferencesKey("grid_rows")
    val GRID_COLUMNS = intPreferencesKey("grid_columns")
    val HOTSEAT_COUNT = intPreferencesKey("hotseat_count")
    val SHOW_NOTIFICATION_DOTS = booleanPreferencesKey("show_notification_dots")
    val SHOW_SEARCH_BAR = booleanPreferencesKey("show_search_bar")
    val WORKSPACE_PAGE_COUNT = intPreferencesKey("workspace_page_count")
    val WORKSPACE_LAYOUT = stringPreferencesKey("workspace_layout")
    val HOTSEAT_LAYOUT = stringPreferencesKey("hotseat_layout")
    val FOLDER_DATA = stringPreferencesKey("folder_data")
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

    val savedLayout: Flow<WorkspaceLayoutData> = context.dataStore.data.map { prefs ->
        val wsJson = prefs[PreferencesKeys.WORKSPACE_LAYOUT] ?: ""
        val hsJson = prefs[PreferencesKeys.HOTSEAT_LAYOUT] ?: ""
        val folderJson = prefs[PreferencesKeys.FOLDER_DATA] ?: ""
        WorkspaceLayoutData(
            workspaceJson = wsJson,
            hotseatJson = hsJson,
            folderJson = folderJson
        )
    }

    suspend fun saveWorkspaceLayout(
        pages: List<HomeScreenPage>,
        hotseat: HotseatInfo,
        folders: Map<Long, FolderInfo>
    ) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.WORKSPACE_LAYOUT] = serializePages(pages)
            prefs[PreferencesKeys.HOTSEAT_LAYOUT] = serializeHotseat(hotseat)
            prefs[PreferencesKeys.FOLDER_DATA] = serializeFolders(folders)
        }
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

    private fun serializePages(pages: List<HomeScreenPage>): String {
        val arr = JSONArray()
        for (page in pages) {
            val pageObj = JSONObject()
            pageObj.put("id", page.id)
            val itemsArr = JSONArray()
            for (item in page.items) {
                itemsArr.put(serializeItem(item))
            }
            pageObj.put("items", itemsArr)
            arr.put(pageObj)
        }
        return arr.toString()
    }

    private fun serializeHotseat(hotseat: HotseatInfo): String {
        val arr = JSONArray()
        for (item in hotseat.items) {
            arr.put(serializeItem(item))
        }
        return arr.toString()
    }

    private fun serializeFolders(folders: Map<Long, FolderInfo>): String {
        val obj = JSONObject()
        for ((id, folder) in folders) {
            val folderObj = JSONObject()
            folderObj.put("id", folder.id)
            folderObj.put("screenId", folder.screenId)
            folderObj.put("cellX", folder.cellX)
            folderObj.put("cellY", folder.cellY)
            folderObj.put("title", folder.title)
            val itemsArr = JSONArray()
            for (child in folder.items) {
                itemsArr.put(serializeItem(child))
            }
            folderObj.put("items", itemsArr)
            obj.put(id.toString(), folderObj)
        }
        return obj.toString()
    }

    private fun serializeItem(item: LauncherItem): JSONObject {
        val obj = JSONObject()
        obj.put("id", item.id)
        obj.put("screenId", item.screenId)
        obj.put("cellX", item.cellX)
        obj.put("cellY", item.cellY)
        obj.put("spanX", item.spanX)
        obj.put("spanY", item.spanY)
        when (item) {
            is AppInfo -> {
                obj.put("type", "app")
                obj.put("componentName", item.componentName.flattenToString())
                obj.put("title", item.title)
                obj.put("userHash", item.user.hashCode())
            }
            is FolderInfo -> {
                obj.put("type", "folder")
                obj.put("title", item.title)
            }
            is com.android.launcher3.model.WidgetInfo -> {
                obj.put("type", "widget")
                obj.put("widgetProviderName", item.providerName.flattenToString())
                obj.put("title", item.label)
            }
        }
        return obj
    }

    fun parseSavedLayout(json: String): List<JSONObject> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getJSONObject(it) }
        } catch (_: Exception) { emptyList() }
    }
}

data class WorkspaceLayoutData(
    val workspaceJson: String,
    val hotseatJson: String,
    val folderJson: String
)
