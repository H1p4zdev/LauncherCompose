package com.android.launcher3.state

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
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
import com.android.launcher3.util.LauncherPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val launcherApps: LauncherApps =
        application.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager: UserManager =
        application.getSystemService(Context.USER_SERVICE) as UserManager
    val prefs = LauncherPreferences(application)

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

    var widgets by mutableStateOf(listOf<WidgetInfo>())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var showNotificationDots by mutableStateOf(true)
        private set

    var showSearchBar by mutableStateOf(true)
        private set

    private val currentUser: UserHandle = Process.myUserHandle()

    init {
        viewModelScope.launch {
            deviceProfile = prefs.deviceProfile.first()
            showNotificationDots = prefs.showNotificationDots.first()
            showSearchBar = prefs.showSearchBar.first()
            loadApps()
        }
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

                regenerateWorkspace()
            } catch (_: Exception) {
                pages = listOf(HomeScreenPage(0))
            } finally {
                isLoading = false
            }
        }
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
                app.copy(screenId = -1, cellX = index, cellY = 0)
            },
            maxCount = hotseatSlots
        )
    }

    fun updateGrid(rows: Int, columns: Int) {
        viewModelScope.launch {
            prefs.setGridRows(rows)
            prefs.setGridColumns(columns)
            deviceProfile = deviceProfile.copy(rows = rows, columns = columns)
            regenerateWorkspace()
        }
    }

    fun updateHotseatCount(count: Int) {
        viewModelScope.launch {
            prefs.setHotseatCount(count)
            deviceProfile = deviceProfile.copy(hotseatCount = count)
            regenerateWorkspace()
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
            isEditMode = !_uiState.value.isEditMode
        )
    }

    fun setWorkspacePage(index: Int) {
        _uiState.value = _uiState.value.copy(workspacePageIndex = index)
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
    }
}
