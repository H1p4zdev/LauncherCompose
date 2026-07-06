package com.android.launcher3.state

import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.launcher3.model.AppInfo
import com.android.launcher3.model.DeviceProfile
import com.android.launcher3.model.FolderInfo
import com.android.launcher3.model.HomeScreenPage
import com.android.launcher3.model.HotseatInfo
import com.android.launcher3.model.LauncherItem
import com.android.launcher3.model.WidgetInfo
import com.android.launcher3.util.DragDropState
import com.android.launcher3.util.LauncherPreferences
import com.android.launcher3.util.NotificationBadgeManager
import com.android.launcher3.widget.LauncherAppWidgetHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val launcherApps: LauncherApps =
        application.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val prefs = LauncherPreferences(application)
    val dragDropState = DragDropState()
    val notificationBadgeManager = NotificationBadgeManager(application)
    val appWidgetHost = LauncherAppWidgetHost(application)
    private val appWidgetManager = AppWidgetManager.getInstance(application)

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    var deviceProfile by mutableStateOf(DeviceProfile())
        private set

    var pages by mutableStateOf(listOf<HomeScreenPage>())
        private set

    var hotseat by mutableStateOf(HotseatInfo())
        private set

    var allApps by mutableStateOf(listOf<AppInfo>())
        private set

    var folders by mutableStateOf(mapOf<Long, FolderInfo>())
        private set

    var allWidgetProviders by mutableStateOf(listOf<WidgetInfo>())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var showNotificationDots by mutableStateOf(true)
        private set

    var showSearchBar by mutableStateOf(true)
        private set

    var wallpaperDrawable by mutableStateOf<Drawable?>(null)
        private set

    private val currentUser: UserHandle = Process.myUserHandle()
    private var nextItemId = 1000L
    private val appInfoMap = mutableMapOf<Long, AppInfo>()

    init {
        viewModelScope.launch {
            deviceProfile = prefs.deviceProfile.first()
            showNotificationDots = prefs.showNotificationDots.first()
            showSearchBar = prefs.showSearchBar.first()
            loadWallpaper()
            loadApps()
        }
    }

    private fun loadWallpaper() {
        try {
            val wm = getApplication<Application>()
                .getSystemService(Context.WALLPAPER_SERVICE) as android.app.WallpaperManager
            val drawable = wm.drawable
            if (drawable is BitmapDrawable) {
                wallpaperDrawable = drawable
            } else if (drawable != null) {
                val bmp = Bitmap.createBitmap(
                    drawable.intrinsicWidth.coerceAtLeast(1),
                    drawable.intrinsicHeight.coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                wallpaperDrawable = BitmapDrawable(
                    getApplication<Application>().resources, bmp
                )
            }
        } catch (_: Exception) { }
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            try {
                val apps = mutableListOf<AppInfo>()
                val profiles = launcherApps.getProfiles()

                for (profile in profiles) {
                    val activityList = launcherApps.getActivityList(null, profile)
                    for (i in activityList.indices) {
                        val activity = activityList[i]
                        val cn = activity.componentName
                        if (cn.packageName == "com.android.launcher3") continue
                        apps.add(
                            AppInfo(
                                id = i.toLong(),
                                screenId = 0,
                                cellX = 0,
                                cellY = 0,
                                componentName = cn,
                                title = activity.label.toString(),
                                user = profile,
                                icon = null
                            )
                        )
                    }
                }

                allApps = apps.sortedBy { it.title.lowercase() }
                for (app in allApps) {
                    appInfoMap[app.id] = app
                }

                val layoutData = prefs.savedLayout.first()
                if (layoutData.workspaceJson.isNotEmpty()) {
                    restoreFromDataStore(layoutData)
                } else {
                    regenerateWorkspace()
                }

                loadWidgetProviders()
                notificationBadgeManager.refreshBadges()
            } catch (_: Exception) {
                pages = listOf(HomeScreenPage(0))
            } finally {
                isLoading = false
            }
        }
    }

    private fun loadWidgetProviders() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val providers = appWidgetManager.getInstalledProviders()
                allWidgetProviders = providers.mapIndexed { index, p ->
                    WidgetInfo(
                        id = index.toLong(),
                        screenId = 0,
                        cellX = 0,
                        cellY = 0,
                        spanX = minOf(p.minWidth / 72, 4).coerceAtLeast(1),
                        spanY = minOf(p.minHeight / 72, 3).coerceAtLeast(1),
                        providerName = p.provider,
                        label = p.loadLabel(getApplication<Application>().packageManager)
                    )
                }
            } catch (_: Exception) { }
        }
    }

    private fun restoreFromDataStore(data: com.android.launcher3.util.WorkspaceLayoutData) {
        try {
            val wsArr = JSONArray(data.workspaceJson)
            val hsArr = if (data.hotseatJson.isNotEmpty()) JSONArray(data.hotseatJson) else JSONArray()
            val folderObj = if (data.folderJson.isNotEmpty()) JSONObject(data.folderJson) else JSONObject()

            val restoredFolders = mutableMapOf<Long, FolderInfo>()
            val folderKeys = folderObj.keys()
            while (folderKeys.hasNext()) {
                val key = folderKeys.next()
                val fObj = folderObj.getJSONObject(key)
                val itemsArr = fObj.getJSONArray("items")
                val children = mutableListOf<AppInfo>()
                for (i in 0 until itemsArr.length()) {
                    val itemObj = itemsArr.getJSONObject(i)
                    val app = parseAppInfo(itemObj)
                    if (app != null) children.add(app)
                }
                val folder = FolderInfo(
                    id = fObj.getLong("id"),
                    screenId = fObj.getLong("screenId"),
                    cellX = fObj.getInt("cellX"),
                    cellY = fObj.getInt("cellY"),
                    title = fObj.optString("title", "Folder"),
                    items = children
                )
                restoredFolders[folder.id] = folder
            }
            folders = restoredFolders

            val restoredPages = mutableListOf<HomeScreenPage>()
            for (i in 0 until wsArr.length()) {
                val pageObj = wsArr.getJSONObject(i)
                val pageId = pageObj.getLong("id")
                val itemsArr = pageObj.getJSONArray("items")
                val items = mutableListOf<LauncherItem>()
                for (j in 0 until itemsArr.length()) {
                    val itemObj = itemsArr.getJSONObject(j)
                    val type = itemObj.optString("type", "app")
                    val item = when (type) {
                        "app" -> parseAppInfo(itemObj)
                        "folder" -> {
                            val fid = itemObj.getLong("id")
                            restoredFolders[fid]
                        }
                        else -> null
                    }
                    item?.let { items.add(it) }
                }
                restoredPages.add(HomeScreenPage(pageId, items))
            }
            if (restoredPages.isNotEmpty()) pages = restoredPages

            val restoredHotseat = mutableListOf<AppInfo>()
            for (i in 0 until hsArr.length()) {
                val itemObj = hsArr.getJSONObject(i)
                val app = parseAppInfo(itemObj)
                if (app != null) restoredHotseat.add(app)
            }
            hotseat = HotseatInfo(restoredHotseat, deviceProfile.hotseatCount)
        } catch (_: Exception) {
            regenerateWorkspace()
        }
    }

    private fun parseAppInfo(obj: JSONObject): AppInfo? {
        val cn = obj.optString("componentName", "")
        if (cn.isEmpty()) return null
        return AppInfo(
            id = obj.getLong("id"),
            screenId = obj.getLong("screenId"),
            cellX = obj.getInt("cellX"),
            cellY = obj.getInt("cellY"),
            spanX = obj.optInt("spanX", 1),
            spanY = obj.optInt("spanY", 1),
            componentName = ComponentName.unflattenFromString(cn) ?: return null,
            title = obj.optString("title", ""),
            user = Process.myUserHandle()
        )
    }

    private fun saveLayout() {
        viewModelScope.launch {
            prefs.saveWorkspaceLayout(pages, hotseat, folders)
        }
    }

    fun addAppToWorkspace(appInfo: AppInfo) {
        val cols = deviceProfile.columns
        val rows = deviceProfile.rows
        val firstPage = pages.firstOrNull() ?: HomeScreenPage(0)
        val slot = findEmptySlot(0, cols, rows, firstPage.items)
        if (slot != null) {
            val placed = appInfo.copy(
                id = nextItemId++,
                screenId = 0,
                cellX = slot.first,
                cellY = slot.second
            )
            appInfoMap[placed.id] = placed
            pages = listOf(firstPage.copy(items = firstPage.items + placed)) + pages.drop(1)
        } else {
            val newPageId = pages.size.toLong()
            val placed = appInfo.copy(
                id = nextItemId++,
                screenId = newPageId,
                cellX = 0,
                cellY = 0
            )
            appInfoMap[placed.id] = placed
            pages = pages + HomeScreenPage(newPageId, listOf(placed))
        }
        saveLayout()
    }

    fun addWidgetToWorkspace(widget: WidgetInfo) {
        val page = pages.firstOrNull() ?: HomeScreenPage(0)
        val placed = widget.copy(
            id = nextItemId++,
            screenId = page.id,
            cellX = 0,
            cellY = 0
        )
        pages = listOf(page.copy(items = page.items + placed))
        saveLayout()
    }

    fun moveItem(itemId: Long, targetScreenId: Long, targetCellX: Int, targetCellY: Int) {
        pages = pages.map { page ->
            if (page.id == targetScreenId) {
                val moving = page.items.find { it.id == itemId }
                val existing = page.items.find { it.cellX == targetCellX && it.cellY == targetCellY && it.id != itemId }
                if (moving != null && existing != null) {
                    page.copy(items = page.items.map {
                        when (it.id) {
                            itemId -> it.copy(cellX = targetCellX, cellY = targetCellY)
                            existing.id -> it.copy(cellX = moving.cellX, cellY = moving.cellY)
                            else -> it
                        }
                    })
                } else if (moving != null) {
                    page.copy(items = page.items.map {
                        if (it.id == itemId) it.copy(cellX = targetCellX, cellY = targetCellY) else it
                    })
                } else page
            } else {
                page.copy(items = page.items.filter { it.id != itemId })
            }
        }
        saveLayout()
    }

    fun removeItem(itemId: Long) {
        pages = pages.map { page ->
            page.copy(items = page.items.filter { it.id != itemId })
        }.filter { it.items.isNotEmpty() || it.id == 0L }
        if (pages.isEmpty()) pages = listOf(HomeScreenPage(0))
        hotseat = hotseat.copy(items = hotseat.items.filter { it.id != itemId })
        saveLayout()
    }

    fun createFolder(appInfos: List<AppInfo>, screenId: Long): FolderInfo {
        val folderId = nextItemId++
        val folder = FolderInfo(
            id = folderId,
            screenId = screenId,
            cellX = appInfos.first().cellX,
            cellY = appInfos.first().cellY,
            title = "Folder",
            items = appInfos
        )
        folders = folders + (folderId to folder)
        pages = pages.map { page ->
            if (page.id == screenId) {
                page.copy(items = page.items.filter { item -> appInfos.none { it.id == item.id } } + folder)
            } else page
        }
        saveLayout()
        return folder
    }

    fun addToFolder(folderId: Long, appInfo: AppInfo) {
        val existing = folders[folderId] ?: return
        folders = folders + (folderId to existing.copy(items = existing.items + appInfo))
        pages = pages.map { page ->
            page.copy(items = page.items.filter { it.id != appInfo.id })
        }
        saveLayout()
    }

    fun renameFolder(folderId: Long, newTitle: String) {
        val existing = folders[folderId] ?: return
        folders = folders + (folderId to existing.copy(title = newTitle))
        saveLayout()
    }

    fun addPage() {
        val newId = pages.size.toLong()
        pages = pages + HomeScreenPage(newId)
        saveLayout()
    }

    fun removePage(pageIndex: Int) {
        if (pages.size <= 1) return
        val removed = pages[pageIndex]
        val remaining = pages.toMutableList()
        remaining.removeAt(pageIndex)
        if (removed.items.isNotEmpty() && remaining.isNotEmpty()) {
            remaining[0] = remaining[0].copy(
                items = remaining[0].items + removed.items.map {
                    when (it) {
                        is AppInfo -> it.copy(screenId = remaining[0].id)
                        is FolderInfo -> it.copy(screenId = remaining[0].id)
                        else -> it
                    }
                }
            )
        }
        pages = remaining
        saveLayout()
    }

    private fun findEmptySlot(screenId: Long, cols: Int, rows: Int, current: List<LauncherItem>): Pair<Int, Int>? {
        val occupied = current.filter { it.screenId == screenId }.map { it.cellX to it.cellY }.toSet()
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                if ((x to y) !in occupied) return x to y
            }
        }
        return null
    }

    private fun regenerateWorkspace() {
        val dp = deviceProfile
        val cols = dp.columns
        val slotsPerScreen = cols * dp.rows
        val hotseatSlots = dp.hotseatCount

        val mainUserApps = allApps.filter {
            it.user == currentUser || it.user.hashCode() == currentUser.hashCode()
        }
        val totalSlots = slotsPerScreen + hotseatSlots
        val itemsForWorkspace = mainUserApps.take(totalSlots - hotseatSlots)
        val hotseatFromApps = mainUserApps.drop(
            (mainUserApps.size - hotseatSlots).coerceAtLeast(0)
        ).take(hotseatSlots)

        if (itemsForWorkspace.isNotEmpty()) {
            val pageItems = itemsForWorkspace.mapIndexed { index, app ->
                val pageIndex = index / slotsPerScreen
                val posInPage = index % slotsPerScreen
                app.copy(
                    id = nextItemId++,
                    screenId = pageIndex.toLong(),
                    cellX = posInPage % cols,
                    cellY = posInPage / cols
                )
            }
            val grouped = pageItems.groupBy { it.screenId }
            pages = grouped.map { (screenId, items) ->
                HomeScreenPage(screenId, items)
            }.ifEmpty { listOf(HomeScreenPage(0)) }
        } else {
            pages = listOf(HomeScreenPage(0))
        }

        hotseat = HotseatInfo(
            items = hotseatFromApps.mapIndexed { index, app ->
                app.copy(
                    id = nextItemId++,
                    screenId = -1,
                    cellX = index,
                    cellY = 0
                )
            },
            maxCount = hotseatSlots
        )
    }

    fun updateGrid(rows: Int, columns: Int) {
        viewModelScope.launch {
            prefs.setGridRows(rows)
            prefs.setGridColumns(columns)
            deviceProfile = deviceProfile.copy(rows = rows, columns = columns)
            pages = listOf(HomeScreenPage(0))
            hotseat = hotseat.copy(items = emptyList())
            saveLayout()
        }
    }

    fun updateHotseatCount(count: Int) {
        viewModelScope.launch {
            prefs.setHotseatCount(count)
            deviceProfile = deviceProfile.copy(hotseatCount = count)
            hotseat = hotseat.copy(maxCount = count)
            saveLayout()
        }
    }

    fun setShowNotificationDots(show: Boolean) {
        viewModelScope.launch {
            prefs.setShowNotificationDots(show)
            showNotificationDots = show
        }
    }

    fun setShowSearchBar(show: Boolean) {
        viewModelScope.launch {
            prefs.setShowSearchBar(show)
            showSearchBar = show
        }
    }

    fun navigateTo(page: LauncherPage) {
        _uiState.value = _uiState.value.copy(currentPage = page)
    }

    fun openFolder(folderId: Long) {
        _uiState.value = _uiState.value.copy(openFolderId = folderId)
    }

    fun closeFolder() {
        _uiState.value = _uiState.value.copy(openFolderId = null)
    }

    fun toggleEditMode() {
        _uiState.value = _uiState.value.copy(
            isEditMode = !_uiState.value.isEditMode,
            showJiggle = !_uiState.value.isEditMode
        )
    }

    fun setWorkspacePage(index: Int) {
        _uiState.value = _uiState.value.copy(workspacePageIndex = index)
    }

    fun showContextMenu(appInfo: Any, x: Float, y: Float) {
        _uiState.value = _uiState.value.copy(
            contextMenuAppInfo = appInfo,
            contextMenuX = x,
            contextMenuY = y
        )
    }

    fun dismissContextMenu() {
        _uiState.value = _uiState.value.copy(contextMenuAppInfo = null)
    }

    fun launchApp(appInfo: AppInfo) {
        viewModelScope.launch {
            try {
                val intent = Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(appInfo.componentName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                getApplication<Application>().startActivity(intent)
            } catch (_: Exception) { }
        }
    }

    fun getFilteredApps(query: String): List<AppInfo> {
        if (query.isBlank()) return allApps
        return allApps.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.componentName.packageName.contains(query, ignoreCase = true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try { appWidgetHost.stopListening() } catch (_: Exception) { }
    }
}
