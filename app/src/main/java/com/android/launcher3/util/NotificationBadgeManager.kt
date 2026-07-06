package com.android.launcher3.util

import android.app.NotificationManager
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationBadgeManager(private val context: Context) {

    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val _badgeCounts = MutableStateFlow(mapOf<String, Int>())
    val badgeCounts: StateFlow<Map<String, Int>> = _badgeCounts.asStateFlow()

    private val selfUser = Process.myUserHandle()

    fun refreshBadges() {
        try {
            val counts = mutableMapOf<String, Int>()
            val profiles = launcherApps.getProfiles()
            for (profile in profiles) {
                val activityList = launcherApps.getActivityList(null, profile)
                for (activity in activityList) {
                    val pkg = activity.componentName.packageName
                    val notificationListeners = notificationManager.getActiveNotifications()
                    var count = 0
                    for (ns in notificationListeners) {
                        if (ns.packageName == pkg) {
                            count += ns.notification.number.coerceAtLeast(1)
                        }
                    }
                    if (count > 0) {
                        val key = activity.componentName.flattenToShortString() + "_" + profile.hashCode()
                        counts[key] = count
                    }
                }
            }
            _badgeCounts.value = counts
        } catch (_: Exception) { }
    }
}

@Composable
fun rememberNotificationBadgeManager(): NotificationBadgeManager {
    val context = LocalContext.current
    return remember { NotificationBadgeManager(context) }
}
