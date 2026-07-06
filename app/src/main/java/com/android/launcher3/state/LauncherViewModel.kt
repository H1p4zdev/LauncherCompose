package com.android.launcher3.state

import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
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
import com.android.launcher3.model.db.WorkspaceDatabase
import com.android.launcher3.model.db.WorkspaceItemEntity
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
import kotlinx.coroutines.withContext

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val launcherApps: LauncherApps =
        application.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val prefs = LauncherPreferences(application)
    private val db = WorkspaceDatabase.getInstance(application)
    private val dao = db.workspaceDao()
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
            val wm = getApplication<Application>().getSystemService(Context.WALLPAPER_SERVICE) as android.app.WallpaperManager
            wallpaperDrawable = wm.drawable
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
                        val componentName = activity.componentName
                        if (componentName.packageName == "com.android.launcher3") continue
                        apps.add(
                            AppInfo(
                                id = i.toLong(),
                                screenId = 0,
                                cellX = 0,
                                cellY = 0,
                                componentName = componentName,
                                title = activity.label.toString(),
                                user = profile,
                                icon = null
                            )
                        )
                    }
                }

                allApps = apps.sortedBy { it.title.lowercase() }

                val savedItems = mutableListOf<WorkspaceItemEntity>()
                try {
                    val items = dao.getWorkspaceItems().first()
                    savedItems.addAll(items)
                } catch (_: Exception) { }

                if (savedItems.isNotEmpty()) {
                    restoreFromDatabase(savedItems)
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
                        spanX = p.minWidth / 72,
                        spanY = p.minHeight / 72,
                        providerName = p.provider,
                        label = p.loadLabel(getApplication<Application>().packageManager)
                    )
                }
            } catch (_: Exception) { }
        }
    }

    private fun restoreFromDatabase(savedItems: List<WorkspaceItemEntity>) {
        val workspaceItems = savedItems.filter { it.screenId >= 0 && it.folderId == null && it.itemType != "widget" }
        val hotseatItems = savedItems.filter { it.screenId == -1L && it.folderId == null }
        val folderItems = savedItems.filter { it.itemType == "folder" }
        val widgetItemEntities = savedItems.filter { it.itemType == "widget" }

        val itemMap = mutableMapOf<Long, AppInfo>()
        for (app in allApps) {
            itemMap[app.id] = app
        }

        val restoredFolders = mutableMapOf<Long, FolderInfo>()
        for (f in folderItems) {
            val folderChildren = savedItems.filter { it.folderId == f.id }
            val childApps = folderChildren.mapNotNull { ent ->
                itemMap[ent.id]
            }
            val folder = FolderInfo(
                id = f.id,
                screenId = f.screenId,
                cellX = f.cellX,
                cellY = f.cellY,
                title = f.title ?: "Folder",
                items = childApps
            )
            restoredFolders[f.id] = folder
        }
        folders = restoredFolders

        val grouped = workspaceItems.groupBy { it.screenId }
        pages = grouped.map { (screenId, items) ->
            val launcherItems = items.mapNotNull { ent ->
                val app = itemMap[ent.id]
                if (app != null) {
                    app.copy(
                        screenId = ent.screenId,
                        cellX = ent.cellX,
                        cellY = ent.cellY,
                        spanX = ent.spanX,
                        spanY = ent.spanY
                    )
                } else {
                    null
                }
            }
            val folderItemsOnPage = restoredFolders.values.filter { it.screenId == screenId }
            HomeScreenPage(screenId, launcherItems + folderItemsOnPage)
        }.ifEmpty { listOf(HomeScreenPage(0)) }

        val hotseatApps = hotseatItems.mapNotNull { ent ->
            val app = itemMap[ent.id]
            app?.copy(screenId = -1, cellX = ent.cellX, cellY = 0)
        }
        val widgetList = widgetItemEntities.mapNotNull { ent ->
            ent.widgetProviderName?.let { name ->
                try {
                    WidgetInfo(
                        id = ent.id,
                        screenId = ent.screenId,
                        cellX = ent.cellX,
                        cellY = ent.cellY,
                        spanX = ent.spanX,
                        spanY = ent.spanY,
                        providerName = ComponentName.unflattenFromString(name) ?: return@let null,
                        label = ent.title ?: "Widget"
                    )
                } catch (_: Exception) { null }
            }
        }
        widgets = widgetList
        hotseat = HotseatInfo(hotseatApps, deviceProfile.hotseatCount)
    }

    private fun saveLayoutToDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            val entities = mutableListOf<WorkspaceItemEntity>()
            var idCounter = 1L
            for ((screenIdx, page) in pages.withIndex()) {
                for (item in page.items) {
                    when (item) {
                        is AppInfo -> {
                            entities.add(
                                WorkspaceItemEntity(
                                    id = item.id,
                                    screenId = screenIdx.toLong(),
                                    cellX = item.cellX,
                                    cellY = item.cellY,
                                    spanX = item.spanX,
                                    spanY = item.spanY,
                                    itemType = "app",
                                    componentName = item.componentName.flattenToString(),
                                    title = item.title
                                )
                            )
                        }
                        is FolderInfo -> {
                            entities.add(
                                WorkspaceItemEntity(
                                    id = item.id,
                                    screenId = screenIdx.toLong(),
                                    cellX = item.cellX,
                                    cellY = item.cellY,
                                    itemType = "folder",
                                    title = item.title
                                )
                            )
                            for (child in item.items) {
                                entities.add(
                                    WorkspaceItemEntity(
                                        id = child.id,
                                        screenId = -2,
                                        cellX = 0,
                                        cellY = 0,
                                        itemType = "app",
                                        componentName = child.componentName.flattenToString(),
                                        title = child.title,
                                        folderId = item.id
                                    )
                                )
                            }
                        }
                        is WidgetInfo -> {
                            entities.add(
                                WorkspaceItemEntity(
                                    id = item.id,
                                    screenId = screenIdx.toLong(),
                                    cellX = item.cellX,
                                    cellY = item.cellY,
                                    spanX = item.spanX,
                                    spanY = item.spanY,
                                    itemType = "widget",
                                    widgetProviderName = item.providerName.flattenToString(),
                                    title = item.label
                                )
                            )
                        }
                        else -> {}
                    }
                }
            }
            for ((idx, item) in hotseat.items.withIndex()) {
                entities.add(
                    WorkspaceItemEntity(
                        id = item.id,
                        screenId = -1,
                        cellX = idx,
                        cellY = 0,
                        itemType = "app",
                        componentName = item.componentName.flattenToString(),
                        title = item.title
                    )
                )
            }
            if (entities.isNotEmpty()) {
                dao.replaceAll(entities)
            }
        }
    }

    fun saveWidgetInstanceState(
        hostId: Int,
        oldWidgetId: Int,
        newWidgetId: Int,
        widgetInfo: AppWidgetProviderInfo
    ) {
        viewModelScope.launch {
            appWidgetHost.deleteAppWidgetId(oldWidgetId)
        }
    }

    private fun findEmptySlot(
        screenId: Long,
        cols: Int,
        rows: Int,
        currentItems: List<LauncherItem>
    ): Pair<Int, Int>? {
        val occupied = currentItems
            .filter { it.screenId == screenId }
            .map { it.cellX to it.cellY }
            .toSet()
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                if ((x to y) !in occupied) {
                    return x to y
                }
            }
        }
        return null
    }

    fun addAppToWorkspace(appInfo: AppInfo) {
        val cols = deviceProfile.columns
        val rows = deviceProfile.rows
        val firstPage = pages.firstOrNull() ?: HomeScreenPage(0)
        val currentItems = firstPage.items
        val slot = findEmptySlot(0, cols, rows, currentItems)
        if (slot != null) {
            val placed = appInfo.copy(
                id = nextItemId++,
                screenId = 0,
                cellX = slot.first,
                cellY = slot.second
            )
            val updatedItems = currentItems + placed
            pages = listOf(firstPage.copy(items = updatedItems)) + pages.drop(1)
            saveLayoutToDatabase()
        } else {
            val newPageId = pages.size.toLong()
            val placed = appInfo.copy(
                id = nextItemId++,
                screenId = newPageId,
                cellX = 0,
                cellY = 0
            )
            pages = pages + HomeScreenPage(newPageId, listOf(placed))
            saveLayoutToDatabase()
        }
    }

    fun addWidgetToWorkspace(widget: WidgetInfo) {
        val cols = deviceProfile.columns
        val firstPage = pages.firstOrNull() ?: HomeScreenPage(0)
        val currentItems = firstPage.items
        val slot = findEmptySlot(0, cols, deviceProfile.rows, currentItems)
        if (slot != null) {
            val placed = widget.copy(
                id = nextItemId++,
                screenId = 0,
                cellX = slot.first,
                cellY = slot.second
            )
            widgets = widgets + placed
            val updatedItems = currentItems + placed
            pages = listOf(firstPage.copy(items = updatedItems)) + pages.drop(1)
            nextItemId++
            saveLayoutToDatabase()
        }
    }

    fun moveItem(itemId: Long, targetScreenId: Long, targetCellX: Int, targetCellY: Int) {
        val newPages = pages.map { page ->
            if (page.id == targetScreenId) {
                val existing = page.items.find { it.cellX == targetCellX && it.cellY == targetCellY }
                if (existing != null) {
                    val movedItem = page.items.find { it.id == itemId } ?: return@map page
                    val swappedItems = page.items.map {
                        when {
                            it.id == itemId -> it.copy(cellX = targetCellX, cellY = targetCellY)
                            it.cellX == targetCellX && it.cellY == targetCellY -> it.copy(cellX = movedItem.cellX, cellY = movedItem.cellY)
                            else -> it
                        }
                    }
                    page.copy(items = swappedItems)
                } else {
                    val updated = page.items.map {
                        if (it.id == itemId) it.copy(cellX = targetCellX, cellY = targetCellY)
                        else it
                    }
                    page.copy(items = updated)
                }
            } else {
                page.copy(items = page.items.filter { it.id != itemId })
            }
        }
        val targetPage = newPages.find { it.id == targetScreenId }
        if (targetPage == null) {
            val movingItem = pages.flatMap { it.items }.find { it.id == itemId }
            if (movingItem != null) {
                pages = newPages.filter { it.id != movingItem.screenId } + HomeScreenPage(
                    targetScreenId,
                    listOf(movingItem.copy(screenId = targetScreenId, cellX = targetCellX, cellY = targetCellY))
                )
            } else {
                pages = newPages
            }
        } else {
            pages = newPages
        }
        saveLayoutToDatabase()
    }

    fun removeItem(itemId: Long) {
        pages = pages.map { page ->
            page.copy(items = page.items.filter { it.id != itemId })
        }.filter { it.items.isNotEmpty() || it.id == 0L }
        if (pages.isEmpty()) pages = listOf(HomeScreenPage(0))
        hotseat = hotseat.copy(items = hotseat.items.filter { it.id != itemId })
        saveLayoutToDatabase()
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
                val withoutItems = page.items.filter { item -> appInfos.none { it.id == item.id } }
                page.copy(items = withoutItems + folder)
            } else page
        }
        saveLayoutToDatabase()
        return folder
    }

    fun addToFolder(folderId: Long, appInfo: AppInfo) {
        val existing = folders[folderId] ?: return
        val updated = existing.copy(items = existing.items + appInfo)
        folders = folders + (folderId to updated)
        pages = pages.map { page ->
            page.copy(items = page.items.filter { it.id != appInfo.id || it.id == folderId })
        }
        saveLayoutToDatabase()
    }

    fun renameFolder(folderId: Long, newTitle: String) {
        val existing = folders[folderId] ?: return
        folders = folders + (folderId to existing.copy(title = newTitle))
        saveLayoutToDatabase()
    }

    fun addPage() {
        val newId = pages.size.toLong()
        pages = pages + HomeScreenPage(newId)
        saveLayoutToDatabase()
    }

    fun removePage(pageIndex: Int) {
        if (pages.size <= 1) return
        val removedPage = pages[pageIndex]
        val itemsToMove = removedPage.items
        val remainingPages = pages.toMutableList()
        remainingPages.removeAt(pageIndex)
        if (itemsToMove.isNotEmpty() && remainingPages.isNotEmpty()) {
            remainingPages[0] = remainingPages[0].copy(
                items = remainingPages[0].items + itemsToMove.map {
                    when (it) {
                        is AppInfo -> it.copy(screenId = remainingPages[0].id)
                        is FolderInfo -> it.copy(screenId = remainingPages[0].id)
                        else -> it
                    }
                }
            )
        }
        pages = remainingPages
        saveLayoutToDatabase()
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
            saveLayoutToDatabase()
        }
    }

    fun updateHotseatCount(count: Int) {
        viewModelScope.launch {
            prefs.setHotseatCount(count)
            deviceProfile = deviceProfile.copy(hotseatCount = count)
            hotseat = hotseat.copy(maxCount = count)
            saveLayoutToDatabase()
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

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            isSearchActive = query.isNotEmpty()
        )
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
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    )
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
        try {
            appWidgetHost.stopListening()
        } catch (_: Exception) { }
    }
}
