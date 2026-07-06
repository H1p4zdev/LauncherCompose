package com.android.launcher3.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspace_items")
data class WorkspaceItemEntity(
    @PrimaryKey val id: Long,
    val screenId: Long,
    val cellX: Int,
    val cellY: Int,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val itemType: String,
    val componentName: String? = null,
    val title: String? = null,
    val folderId: Long? = null,
    val widgetProviderName: String? = null
)
