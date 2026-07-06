package com.android.launcher3.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.android.launcher3.model.AppInfo
import com.android.launcher3.state.LauncherViewModel
import com.android.launcher3.theme.AppIcon
import dev.chrisbanes.haze.HazeState

@Composable
fun HotseatScreen(
    viewModel: LauncherViewModel,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val hotseat = viewModel.hotseat
    val isEditMode by viewModel.uiState.collectAsState { it.isEditMode }
    val dragState = viewModel.dragDropState

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = hotseat.items.toMutableList()
            while (items.size < hotseat.maxCount) {
                items.add(
                    AppInfo(
                        id = -(items.size + 1).toLong(),
                        screenId = -1,
                        cellX = items.size,
                        cellY = 0,
                        componentName = android.content.ComponentName("", ""),
                        title = "",
                        user = android.os.Process.myUserHandle()
                    )
                )
            }

            items.forEach { appInfo ->
                if (appInfo.componentName.packageName.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .pointerInput(appInfo.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        dragState.startDrag(
                                            item = appInfo,
                                            sourceScreenId = -1,
                                            sourceCellX = appInfo.cellX,
                                            sourceCellY = 0
                                        )
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragState.updatePosition(
                                            dragState.dragX + dragAmount.x,
                                            dragState.dragY + dragAmount.y
                                        )
                                    },
                                    onDragEnd = {
                                        val data = dragState.endDrag()
                                        if (data != null) {
                                            viewModel.removeItem(data.item.id)
                                        }
                                    },
                                    onDragCancel = { dragState.cancelDrag() }
                                )
                            }
                    ) {
                        AppIcon(
                            appInfo = appInfo,
                            modifier = Modifier.size(48.dp),
                            badgeCount = if (viewModel.showNotificationDots) {
                                val key = appInfo.componentName.flattenToShortString() + "_" + appInfo.user.hashCode()
                                viewModel.notificationBadgeManager.badgeCounts.value[key]
                            } else null
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Transparent)
                    )
                }
            }
        }
    }
}
