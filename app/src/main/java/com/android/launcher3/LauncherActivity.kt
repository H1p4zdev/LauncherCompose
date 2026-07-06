package com.android.launcher3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.launcher3.screen.AllAppsScreen
import com.android.launcher3.screen.FolderScreen
import com.android.launcher3.screen.HotseatScreen
import com.android.launcher3.screen.WidgetPickerScreen
import com.android.launcher3.screen.WorkspaceScreen
import com.android.launcher3.state.LauncherPage
import com.android.launcher3.state.LauncherViewModel
import com.android.launcher3.theme.LauncherTheme
import com.android.launcher3.theme.rememberLauncherHazeState
import dev.chrisbanes.haze.hazeEffect

class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LauncherTheme {
                val viewModel: LauncherViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val hazeState = rememberLauncherHazeState()

                Box(modifier = Modifier.fillMaxSize()) {
                    when (uiState.currentPage) {
                        LauncherPage.WORKSPACE -> {
                            WorkspaceScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        LauncherPage.ALL_APPS -> {
                            AllAppsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        LauncherPage.WIDGET_PICKER -> {
                            WidgetPickerScreen(
                                onBack = { viewModel.navigateTo(LauncherPage.WORKSPACE) }
                            )
                        }
                    }

                    if (uiState.currentPage == LauncherPage.WORKSPACE) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                        ) {
                            HotseatScreen(
                                viewModel = viewModel,
                                hazeState = hazeState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .hazeEffect(hazeState) {
                                        blurRadius = 32f
                                    }
                            )
                        }
                    }

                    if (uiState.openFolderId != null) {
                        FolderScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
