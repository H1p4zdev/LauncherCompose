package com.android.launcher3.screen

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.android.launcher3.model.AppInfo
import com.android.launcher3.state.LauncherViewModel

@Composable
fun AppContextMenu(
    appInfo: AppInfo,
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit,
    onUninstall: () -> Unit = {},
    onAddToHomeScreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .widthIn(min = 180.dp, max = 240.dp)
            .clip(RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = appInfo.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ContextMenuItem(
                icon = Icons.Default.Info,
                text = "App info",
                onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.parse("package:${appInfo.componentName.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (_: Exception) { }
                    onDismiss()
                }
            )

            if (onAddToHomeScreen != null) {
                ContextMenuItem(
                    icon = Icons.Default.Info,
                    text = "Add to Home screen",
                    onClick = {
                        onAddToHomeScreen()
                        onDismiss()
                    }
                )
            }

            ContextMenuItem(
                icon = Icons.Default.Delete,
                text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) "Uninstall" else "Remove",
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_DELETE)
                            .setData(Uri.parse("package:${appInfo.componentName.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (_: Exception) { }
                    onDismiss()
                },
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

sealed class ContextMenuAction {
    data object AppInfo : ContextMenuAction()
    data object Uninstall : ContextMenuAction()
    data class AddToHomeScreen(val appInfo: AppInfo) : ContextMenuAction()
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}
