package com.android.launcher3.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.android.launcher3.model.WidgetInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerScreen(
    onBack: () -> Unit,
    onAddWidget: (WidgetInfo) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sampleWidgets = remember {
        listOf(
            WidgetInfo(1, 0, 0, 0, 2, 1, android.content.ComponentName("com.google.android.apps.nexuslauncher", "ClockWidget"), "Clock"),
            WidgetInfo(2, 0, 0, 0, 1, 1, android.content.ComponentName("com.google.android.calendar", "CalendarWidget"), "Calendar"),
            WidgetInfo(3, 0, 0, 0, 3, 1, android.content.ComponentName("com.google.android.googlequicksearchbox", "SearchWidget"), "Google Search"),
            WidgetInfo(4, 0, 0, 0, 4, 1, android.content.ComponentName("com.google.android.deskclock", "DigitalClock"), "Digital Clock"),
            WidgetInfo(5, 0, 0, 0, 2, 2, android.content.ComponentName("com.google.android.keep", "NoteWidget"), "Keep Notes"),
            WidgetInfo(6, 0, 0, 0, 3, 2, android.content.ComponentName("com.google.android.apps.photos", "PhotoWidget"), "Photos"),
            WidgetInfo(7, 0, 0, 0, 1, 2, android.content.ComponentName("com.google.android.deskclock", "AnalogClock"), "Analog Clock"),
            WidgetInfo(8, 0, 0, 0, 4, 2, android.content.ComponentName("com.google.android.deskclock", "WorldClock"), "World Clock"),
            WidgetInfo(9, 0, 0, 0, 2, 2, android.content.ComponentName("com.google.android.calendar", "AgendaWidget"), "Agenda"),
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery, sampleWidgets) {
        if (searchQuery.isBlank()) sampleWidgets
        else sampleWidgets.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                text = "Widgets",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search widgets",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No widgets found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(items = filtered, key = { it.id }) { widget ->
                    WidgetItem(
                        widget = widget,
                        onClick = { onAddWidget(widget) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetItem(
    widget: WidgetInfo,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Widgets,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = widget.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${widget.spanX} x ${widget.spanY}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
