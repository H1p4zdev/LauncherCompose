package com.android.launcher3.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bgColor = if (isDark) Color(0x99000000) else Color(0xE6F2F2F7)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor, RoundedCornerShape(20.dp))
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
                    AppIcon(
                        appInfo = appInfo,
                        modifier = Modifier.size(48.dp)
                    )
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
