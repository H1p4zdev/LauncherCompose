package com.android.launcher3.widget

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import com.android.launcher3.model.WidgetInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LauncherAppWidgetHost(
    context: Context,
    hostId: Int = APPWIDGET_HOST_ID
) : AppWidgetHost(context, hostId) {

    companion object {
        const val APPWIDGET_HOST_ID = 1024
        const val REQUEST_PICK_APPWIDGET = 1
        const val REQUEST_CREATE_APPWIDGET = 2
    }

    private val appWidgetManager = AppWidgetManager.getInstance(context)

    suspend fun getAllWidgetProviders(): List<AppWidgetProviderInfo> = withContext(Dispatchers.IO) {
        try {
            appWidgetManager.getInstalledProvidersForProfile(null, null)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun createWidgetBinding(
        providerInfo: AppWidgetProviderInfo,
        activity: Activity,
        requestCode: Int
    ): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val config = providerInfo.configure
                    if (config != null) {
                        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                            .setComponent(config)
                            .putExtra(
                                AppWidgetManager.EXTRA_APPWIDGET_ID,
                                allocateAppWidgetId()
                            )
                        activity.startActivityForResult(intent, requestCode)
                    }
                }
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun createWidgetView(context: Context, widgetInfo: WidgetInfo): AppWidgetHostView? {
        try {
            val hostView = createView(context, widgetInfo.id.toInt(), widgetInfo.providerName)
            val appWidgetInfo = appWidgetManager.getAppWidgetInfo(widgetInfo.id.toInt())
            if (appWidgetInfo != null) {
                hostView.setAppWidget(widgetInfo.id.toInt(), appWidgetInfo)
                return hostView
            }
        } catch (_: Exception) { }
        return null
    }
}
