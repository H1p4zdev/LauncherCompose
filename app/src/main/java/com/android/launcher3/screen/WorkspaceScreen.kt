package com.android.launcher3.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import com.android.launcher3.theme.launcherHazeStyle
import com.android.launcher3.theme.rememberLauncherHazeState
import dev.chrisbanes.haze.HazeEffect
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeSource

@Composable
fun WorkspaceScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val hazeState = rememberLauncherHazeState()
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = uiState.workspacePageIndex
    )

    LaunchedEffect(uiState.workspacePageIndex) {
        listState.animateScrollToItem(uiState.workspacePageIndex)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            userScrollEnabled = uiState.currentPage == LauncherPage.WORKSPACE
        ) {
            itemsIndexed(
                items = viewModel.pages,
                key = { _, page -> page.id }
            ) { pageIndex, page ->
                WorkspacePage(
                    page = page,
                    pageIndex = pageIndex,
                    viewModel = viewModel,
                    modifier = Modifier.fillParentMaxSize()
                )
            }
        }

        PageIndicator(
            pageCount = viewModel.pages.size,
            currentPage = uiState.workspacePageIndex,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }
}

@Composable
private fun WorkspacePage(
    page: HomeScreenPage,
    pageIndex: Int,
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val density = androidx.compose.ui.platform.LocalDensity.current

    val cellWidth = with(density) {
        (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp /
            viewModel.deviceProfile.columns)
    }
    val cellHeight = with(density) {
        ((androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp - 180).dp /
            viewModel.deviceProfile.rows)
    }

    Box(modifier = modifier) {
        GridLayout(
            rows = viewModel.deviceProfile.rows,
            columns = viewModel.deviceProfile.columns,
            modifier = Modifier.fillMaxSize()
        ) { row, col ->
            Box(
                modifier = Modifier
                    .size(cellWidth, cellHeight)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder grid cell
            }
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
                    AppIconItem(
                        appInfo = item,
                        modifier = posModifier,
                        viewModel = viewModel
                    )
                }
                is FolderInfo -> {
                    FolderItem(
                        folderInfo = item,
                        modifier = posModifier,
                        viewModel = viewModel
                    )
                }
                is WidgetInfo -> {
                    // Widget placeholder
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
    viewModel: LauncherViewModel
) {
    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures {
                    viewModel.launchApp(appInfo)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppIcon(
            appInfo = appInfo,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        androidx.compose.material3.Text(
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
    viewModel: LauncherViewModel
) {
    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures {
                    viewModel.openFolder(folderInfo.id)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        FolderIcon(
            folderInfo = folderInfo,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        androidx.compose.material3.Text(
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
        modifier = modifier,
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
