package com.android.launcher3.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.launcher3.model.AppInfo
import com.android.launcher3.model.FolderInfo
import com.android.launcher3.model.LauncherItem

data class DragData(
    val item: AppInfo,
    val sourceScreenId: Long,
    val sourceCellX: Int,
    val sourceCellY: Int
)

class DragDropState {
    var isDragging by mutableStateOf(false)
        private set
    var dragData by mutableStateOf<DragData?>(null)
        private set
    var dragX by mutableStateOf(0f)
        private set
    var dragY by mutableStateOf(0f)
        private set
    var currentDropTarget by mutableStateOf<DropTarget?>(null)
        private set

    enum class DropTarget {
        WORKSPACE,
        HOTSEAT,
        FOLDER,
        DELETE,
        PAGE_INDICATOR
    }

    fun startDrag(
        item: AppInfo,
        sourceScreenId: Long,
        sourceCellX: Int,
        sourceCellY: Int
    ) {
        isDragging = true
        dragData = DragData(item, sourceScreenId, sourceCellX, sourceCellY)
    }

    fun updatePosition(x: Float, y: Float) {
        dragX = x
        dragY = y
    }

    fun setDropTarget(target: DropTarget?) {
        currentDropTarget = target
    }

    fun endDrag(): DragData? {
        val data = dragData
        isDragging = false
        dragData = null
        currentDropTarget = null
        dragX = 0f
        dragY = 0f
        return data
    }

    fun cancelDrag() {
        isDragging = false
        dragData = null
        currentDropTarget = null
        dragX = 0f
        dragY = 0f
    }
}

data class DropResult(
    val targetScreenId: Long,
    val targetCellX: Int,
    val targetCellY: Int,
    val createFolder: Boolean = false,
    val existingFolderId: Long? = null,
    val isHotseat: Boolean = false,
    val isDelete: Boolean = false
)
