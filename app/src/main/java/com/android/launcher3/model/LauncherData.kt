package com.android.launcher3.model

import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.os.UserHandle

sealed interface LauncherItem {
    val id: Long
    val screenId: Long
    val cellX: Int
    val cellY: Int
    val spanX: Int
    val spanY: Int
}

data class AppInfo(
    override val id: Long,
    override val screenId: Long,
    override val cellX: Int,
    override val cellY: Int,
    override val spanX: Int = 1,
    override val spanY: Int = 1,
    val componentName: ComponentName,
    val title: String,
    val user: UserHandle,
    val icon: Drawable? = null
) : LauncherItem

data class FolderInfo(
    override val id: Long,
    override val screenId: Long,
    override val cellX: Int,
    override val cellY: Int,
    override val spanX: Int = 1,
    override val spanY: Int = 1,
    val title: String,
    val items: List<AppInfo> = emptyList(),
    val icon: Drawable? = null
) : LauncherItem

data class WidgetInfo(
    override val id: Long,
    override val screenId: Long,
    override val cellX: Int,
    override val cellY: Int,
    override val spanX: Int,
    override val spanY: Int,
    val providerName: ComponentName,
    val label: String
) : LauncherItem

data class HomeScreenPage(
    val id: Long,
    val items: List<LauncherItem> = emptyList()
)

data class HotseatInfo(
    val items: List<AppInfo> = emptyList(),
    val maxCount: Int = 5
)

data class DeviceProfile(
    val rows: Int = 5,
    val columns: Int = 4,
    val hotseatCount: Int = 5,
    val iconSizeDp: Float = 48f,
    val gridPaddingDp: Float = 8f
)
