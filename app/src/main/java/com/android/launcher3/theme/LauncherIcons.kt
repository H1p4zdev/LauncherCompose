package com.android.launcher3.theme

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.launcher3.model.AppInfo
import com.android.launcher3.model.FolderInfo

@Composable
fun AppIcon(
    appInfo: AppInfo,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val context = LocalContext.current
    val iconBitmap = remember(appInfo.componentName) {
        try {
            val pm = context.packageManager
            val drawable = pm.getActivityIcon(appInfo.componentName)
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (iconBitmap != null) {
            Image(
                painter = BitmapPainter(iconBitmap.asImageBitmap()),
                contentDescription = appInfo.title,
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(size * 0.22f))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(size * 0.22f))
                    .background(Color(0xFFC7C7CC)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text(
                    text = appInfo.title.take(1).uppercase(),
                    color = Color.White,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun FolderIcon(
    folderInfo: FolderInfo,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.22f))
                .background(Color(0xFFC7C7CC).copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            if (folderInfo.items.isNotEmpty()) {
                val subSize = size / 2.2f
                folderInfo.items.take(4).forEachIndexed { idx, _ ->
                    val offsetX = if (idx % 2 == 0) -subSize / 4 else subSize / 4
                    val offsetY = if (idx / 2 == 0) -subSize / 4 else subSize / 4
                    Box(
                        modifier = Modifier
                            .size(subSize)
                            .clip(RoundedCornerShape(subSize * 0.2f))
                            .background(Color(0xFF8E8E93).copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}
