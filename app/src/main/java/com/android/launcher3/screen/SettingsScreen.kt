package com.android.launcher3.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.android.launcher3.state.LauncherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    text = "HOME SCREEN",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                )
            }

            item {
                SettingsItem(
                    title = "Grid rows",
                    summary = "${viewModel.deviceProfile.rows} rows",
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            for (i in 4..7) {
                                FilterChip(
                                    selected = viewModel.deviceProfile.rows == i,
                                    onClick = {
                                        viewModel.updateGrid(i, viewModel.deviceProfile.columns)
                                    },
                                    label = { Text("$i") },
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }
                )
            }

            item {
                SettingsItem(
                    title = "Grid columns",
                    summary = "${viewModel.deviceProfile.columns} columns",
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            for (i in 3..6) {
                                FilterChip(
                                    selected = viewModel.deviceProfile.columns == i,
                                    onClick = {
                                        viewModel.updateGrid(viewModel.deviceProfile.rows, i)
                                    },
                                    label = { Text("$i") },
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }
                )
            }

            item {
                SettingsItem(
                    title = "Show notification dots",
                    summary = "Badge icons with notification count",
                    trailing = {
                        Switch(
                            checked = viewModel.showNotificationDots,
                            onCheckedChange = { viewModel.setShowNotificationDots(it) }
                        )
                    }
                )
            }

            item {
                SettingsItem(
                    title = "Show search bar",
                    summary = "Display search bar on home screen",
                    trailing = {
                        Switch(
                            checked = viewModel.showSearchBar,
                            onCheckedChange = { viewModel.setShowSearchBar(it) }
                        )
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "DOCK",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                )
            }

            item {
                SettingsItem(
                    title = "Hotseat icons",
                    summary = "${viewModel.deviceProfile.hotseatCount} icons",
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            for (i in 3..7) {
                                FilterChip(
                                    selected = viewModel.deviceProfile.hotseatCount == i,
                                    onClick = { viewModel.updateHotseatCount(i) },
                                    label = { Text("$i") },
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    summary: String = "",
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (summary.isNotEmpty()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing()
    }
}
