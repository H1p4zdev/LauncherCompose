package com.android.launcher3

import android.app.Application
import android.appwidget.AppWidgetManager
import androidx.profileinstaller.ProfileInstallerInitializer
import com.android.launcher3.widget.LauncherAppWidgetHost

class LauncherApp : Application() {
    lateinit var appWidgetHost: LauncherAppWidgetHost
        private set

    override fun onCreate() {
        super.onCreate()
        appWidgetHost = LauncherAppWidgetHost(this)
        appWidgetHost.startListening()
    }

    override fun onTerminate() {
        super.onTerminate()
        if (::appWidgetHost.isInitialized) {
            appWidgetHost.stopListening()
        }
    }
}
