package com.android.launcher3.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.android.launcher3.model.FolderInfo
import com.android.launcher3.state.LauncherViewModel
import com.android.launcher3.theme.AppIcon

@Composable
fun FolderScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val folderId = uiState.openFolderId
    val folder = folderId?.let { viewModel.folders[it] }
    var isRenaming by remember { mutableStateOf(false) }
    var renameText by remember(folder) { mutableStateOf(folder?.title ?: "") }

    AnimatedVisibility(
        visible = folder != null,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        folder?.let { folderInfo ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(280.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            if (isRenaming) {
                                BasicTextField(
                                    value = renameText,
                                    onValueChange = { renameText = it },
                                    textStyle = MaterialTheme.typography.titleMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = {
                                    viewModel.renameFolder(folderInfo.id, renameText)
                                    isRenaming = false
                                }) {
                                    Text("Done")
                                }
                            } else {
                                Text(
                                    text = folderInfo.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { isRenaming = true }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Rename folder",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (folderInfo.items.isEmpty()) {
                            Text(
                                text = "Empty folder",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        } else {
                            val cols = when {
                                folderInfo.items.size <= 4 -> 2
                                folderInfo.items.size <= 9 -> 3
                                else -> 4
                            }
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(cols),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = folderInfo.items,
                                    key = { it.componentName.flattenToString() }
                                ) { appInfo ->
                                    Column(
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .pointerInput(appInfo.id) {
                                                detectTapGestures {
                                                    viewModel.launchApp(appInfo)
                                                }
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        AppIcon(
                                            appInfo = appInfo,
                                            modifier = Modifier.size(40.dp),
                                            badgeCount = if (viewModel.showNotificationDots) {
                                                val key = appInfo.componentName.flattenToShortString() + "_" + appInfo.user.hashCode()
                                                viewModel.notificationBadgeManager.badgeCounts.value[key]
                                            } else null
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = appInfo.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { viewModel.closeFolder() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}
