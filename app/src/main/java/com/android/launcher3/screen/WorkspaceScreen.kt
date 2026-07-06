package com.android.launcher3.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.android.launcher3.model.AppInfo
import com.android.launcher3.model.FolderInfo
import com.android.launcher3.model.HomeScreenPage
import com.android.launcher3.model.LauncherItem
import com.android.launcher3.model.WidgetInfo
import com.android.launcher3.state.LauncherPage
import com.android.launcher3.state.LauncherViewModel
import com.android.launcher3.theme.AppIcon
import com.android.launcher3.theme.FolderIcon
import com.android.launcher3.util.DropResult
import kotlin.math.roundToInt

@Composable
fun WorkspaceScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = uiState.workspacePageIndex
    )
    val dragState = viewModel.dragDropState
    val dp = viewModel.deviceProfile
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val cellWidth = with(density) {
        (configuration.screenWidthDp.dp / dp.columns)
    }
    val cellHeight = with(density) {
        ((configuration.screenHeightDp - 220).dp / dp.rows)
    }

    LaunchedEffect(uiState.workspacePageIndex) {
        listState.animateScrollToItem(uiState.workspacePageIndex)
    }

    Box(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.isEditMode) {
                EditModeToolbar(
                    onAddPage = { viewModel.addPage() },
                    onSettings = { viewModel.navigateTo(LauncherPage.SETTINGS) },
                    onWidgets = { viewModel.navigateTo(LauncherPage.WIDGET_PICKER) },
                    onExitEditMode = { viewModel.toggleEditMode() }
                )
            } else {
                if (viewModel.showSearchBar) {
                    SearchBar(
                        onClick = { viewModel.navigateTo(LauncherPage.ALL_APPS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 12.dp)
                    )
                }
            }

            Box(
                modifier = Modifier.weight(1f)
            ) {
                LazyRow(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    userScrollEnabled = uiState.currentPage == LauncherPage.WORKSPACE && !dragState.isDragging
                ) {
                    itemsIndexed(
                        items = viewModel.pages,
                        key = { _, page -> page.id }
                    ) { pageIndex, page ->
                        WorkspacePage(
                            page = page,
                            pageIndex = pageIndex,
                            viewModel = viewModel,
                            cellWidth = cellWidth,
                            cellHeight = cellHeight,
                            isEditMode = uiState.isEditMode,
                            modifier = Modifier.fillParentMaxSize()
                        )
                    }
                }

                if (uiState.isEditMode && viewModel.pages.size > 1) {
                    val currentPage = uiState.workspacePageIndex
                    if (currentPage < viewModel.pages.size) {
                        IconButton(
                            onClick = { viewModel.removePage(currentPage) },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                                .size(32.dp)
                                .background(
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove page",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            PageIndicator(
                pageCount = viewModel.pages.size,
                currentPage = uiState.workspacePageIndex,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 4.dp)
            )
        }

        if (dragState.isDragging && dragState.dragData != null) {
            val data = dragState.dragData!!
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete zone",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        CircleShape
                    )
                    .padding(8.dp)
            )
        }

        if (uiState.contextMenuAppInfo is AppInfo) {
            val appInfo = uiState.contextMenuAppInfo as AppInfo
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            uiState.contextMenuX.roundToInt(),
                            uiState.contextMenuY.roundToInt()
                        )
                    }
                    .padding(8.dp)
            ) {
                AppContextMenu(
                    appInfo = appInfo,
                    viewModel = viewModel,
                    onDismiss = { viewModel.dismissContextMenu() }
                )
            }
        }
    }
}

@Composable
private fun SearchBar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .pointerInput(Unit) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Search apps",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EditModeToolbar(
    onAddPage: () -> Unit,
    onSettings: () -> Unit,
    onWidgets: () -> Unit,
    onExitEditMode: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
        }
        Text(
            text = "Edit mode",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row {
            IconButton(onClick = onWidgets) {
                Icon(Icons.Default.Widgets, contentDescription = "Widgets", tint = MaterialTheme.colorScheme.onBackground)
            }
            IconButton(onClick = onAddPage) {
                Icon(Icons.Default.Add, contentDescription = "Add page", tint = MaterialTheme.colorScheme.onBackground)
            }
            IconButton(onClick = onExitEditMode) {
                Icon(Icons.Default.Close, contentDescription = "Done", tint = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Composable
private fun WorkspacePage(
    page: HomeScreenPage,
    pageIndex: Int,
    viewModel: LauncherViewModel,
    cellWidth: androidx.compose.ui.unit.Dp,
    cellHeight: androidx.compose.ui.unit.Dp,
    isEditMode: Boolean,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val dragState = viewModel.dragDropState

    Box(modifier = modifier) {
        GridLayout(
            rows = viewModel.deviceProfile.rows,
            columns = viewModel.deviceProfile.columns,
            modifier = Modifier.fillMaxSize()
        ) { _, _ ->
            Box(
                modifier = Modifier
                    .size(cellWidth, cellHeight)
                    .padding(4.dp)
            )
        }

        page.items.forEach { item ->
            val posModifier = Modifier
                .offset(
                    x = (item.cellX * (cellWidth.value + 8)).dp,
                    y = (item.cellY * (cellHeight.value + 8)).dp
                )
                .size(cellWidth - 8.dp, cellHeight - 8.dp)

            when (item) {
                is AppInfo -> {
                    if (isEditMode || dragState.isDragging) {
                        val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
                        val jiggleRotation by infiniteTransition.animateFloat(
                            initialValue = -2f,
                            targetValue = 2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(150, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "jiggleRotation"
                        )
                        AppIconItem(
                            appInfo = item,
                            modifier = posModifier
                                .rotate(if (isEditMode) jiggleRotation else 0f),
                            viewModel = viewModel,
                            isEditMode = isEditMode
                        )
                    } else {
                        AppIconItem(
                            appInfo = item,
                            modifier = posModifier,
                            viewModel = viewModel,
                            isEditMode = false
                        )
                    }
                }
                is FolderInfo -> {
                    val infiniteTransition = rememberInfiniteTransition(label = "folderJiggle")
                    val jiggleRotation by infiniteTransition.animateFloat(
                        initialValue = -2f,
                        targetValue = 2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(150, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "folderJiggleRotation"
                    )
                    FolderItem(
                        folderInfo = item,
                        modifier = posModifier.rotate(if (isEditMode) jiggleRotation else 0f),
                        viewModel = viewModel,
                        isEditMode = isEditMode
                    )
                }
                is WidgetInfo -> {
                    Box(
                        modifier = posModifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun AppIconItem(
    appInfo: AppInfo,
    modifier: Modifier = Modifier,
    viewModel: LauncherViewModel,
    isEditMode: Boolean = false
) {
    val dragState = viewModel.dragDropState
    var itemPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Column(
        modifier = modifier
            .onGloballyPositioned {
                itemPosition = it.positionInRoot()
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (isEditMode) {
                            viewModel.showContextMenu(appInfo, itemPosition.x, itemPosition.y)
                        } else {
                            viewModel.launchApp(appInfo)
                        }
                    },
                    onLongPress = {
                        if (!isEditMode) {
                            dragState.startDrag(
                                item = appInfo,
                                sourceScreenId = appInfo.screenId,
                                sourceCellX = appInfo.cellX,
                                sourceCellY = appInfo.cellY
                            )
                        }
                    }
                )
            }
            .then(
                if (isEditMode) {
                    Modifier.pointerInput(appInfo.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                dragState.startDrag(
                                    item = appInfo,
                                    sourceScreenId = appInfo.screenId,
                                    sourceCellX = appInfo.cellX,
                                    sourceCellY = appInfo.cellY
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
                } else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppIcon(
            appInfo = appInfo,
            modifier = Modifier.size(48.dp),
            badgeCount = if (viewModel.showNotificationDots) {
                val key = appInfo.componentName.flattenToShortString() + "_" + appInfo.user.hashCode()
                viewModel.notificationBadgeManager.badgeCounts.value[key]
            } else null
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = appInfo.title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier = Modifier.widthIn(max = 56.dp)
        )
    }
}

@Composable
private fun FolderItem(
    folderInfo: FolderInfo,
    modifier: Modifier = Modifier,
    viewModel: LauncherViewModel,
    isEditMode: Boolean = false
) {
    val dragState = viewModel.dragDropState

    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (isEditMode) {
                            // folder options
                        } else {
                            viewModel.openFolder(folderInfo.id)
                        }
                    }
                )
            }
            .then(
                if (isEditMode) {
                    Modifier.pointerInput(folderInfo.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { },
                            onDrag = { change, dragAmount ->
                                change.consume()
                            },
                            onDragEnd = { },
                            onDragCancel = { }
                        )
                    }
                } else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        FolderIcon(folderInfo = folderInfo, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = folderInfo.title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier = Modifier.widthIn(max = 56.dp)
        )
    }
}

@Composable
private fun GridLayout(
    rows: Int,
    columns: Int,
    modifier: Modifier = Modifier,
    content: @Composable (row: Int, col: Int) -> Unit
) {
    Column(modifier = modifier) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.weight(1f)) {
                for (col in 0 until columns) {
                    Box(modifier = Modifier.weight(1f)) {
                        content(row, col)
                    }
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    if (pageCount <= 1) return
    Row(
        modifier = modifier.navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(
                        width = if (index == currentPage) 16.dp else 6.dp,
                        height = 6.dp
                    )
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (index == currentPage)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
            )
        }
    }
}
