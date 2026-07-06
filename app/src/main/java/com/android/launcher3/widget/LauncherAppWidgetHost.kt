package com.android.launcher3.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.UserHandle

class LauncherAppWidgetHost(
    context: Context,
    hostId: Int = APPWIDGET_HOST_ID
) : AppWidgetHost(context, hostId) {

    companion object {
        const val APPWIDGET_HOST_ID = 1024
    }

    private val appWidgetManager = AppWidgetManager.getInstance(context)

    suspend fun getAllWidgetProviders(): List<AppWidgetProviderInfo> {
        return try {
            appWidgetManager.getInstalledProvidersForProfile(null as UserHandle?)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun createWidgetView(context: Context, appWidgetId: Int, providerInfo: AppWidgetProviderInfo): AppWidgetHostView? {
        return try {
            val hostView = createView(context, appWidgetId, providerInfo)
            hostView.setAppWidget(appWidgetId, providerInfo)
            hostView
        } catch (_: Exception) {
            null
        }
    }
}
