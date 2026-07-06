package com.android.launcher3.state

enum class LauncherPage {
    WORKSPACE,
    ALL_APPS,
    WIDGET_PICKER,
    SETTINGS
}

data class LauncherUiState(
    val currentPage: LauncherPage = LauncherPage.WORKSPACE,
    val workspacePageIndex: Int = 0,
    val openFolderId: Long? = null,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val isEditMode: Boolean = false,
    val isDragOver: Boolean = false,
    val contextMenuAppInfo: Any? = null,
    val contextMenuX: Float = 0f,
    val contextMenuY: Float = 0f,
    val showJiggle: Boolean = false
)
