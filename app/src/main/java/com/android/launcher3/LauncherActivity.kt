package com.android.launcher3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.launcher3.screen.AllAppsScreen
import com.android.launcher3.screen.FolderScreen
import com.android.launcher3.screen.HotseatScreen
import com.android.launcher3.screen.SettingsScreen
import com.android.launcher3.screen.WidgetPickerScreen
import com.android.launcher3.screen.WorkspaceScreen
import com.android.launcher3.state.LauncherPage
import com.android.launcher3.state.LauncherViewModel
import com.android.launcher3.theme.LauncherTheme
import com.android.launcher3.theme.rememberLauncherHazeState
import dev.chrisbanes.haze.HazeEffect
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeSource

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

                    // Hotseat overlay at the bottom (on workspace)
                    if (uiState.currentPage == LauncherPage.WORKSPACE) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .let { mod ->
                                    mod.then(
                                        Modifier.background(
                                            if (MaterialTheme.colorScheme.background.luminance() < 0.5f)
                                                Color(0x99000000)
                                            else
                                                Color(0xE6F2F2F7)
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            HazeEffect(
                                state = hazeState,
                                style = com.android.launcher3.theme.launcherHazeStyle(
                                    MaterialTheme.colorScheme.background.luminance() < 0.5f
                                ),
                                source = HazeSource.Behind(
                                    progressive = HazeProgressive.Immediate
                                )
                            )
                            HotseatScreen(
                                viewModel = viewModel,
                                hazeState = hazeState,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Folder overlay
                    if (uiState.openFolderId != null) {
                        FolderScreen(
                            viewModel = viewModel,
                            hazeState = hazeState,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Bottom navigation bar
                    BottomNavBar(
                        currentPage = uiState.currentPage,
                        onNavigate = { viewModel.navigateTo(it) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavBar(
    currentPage: LauncherPage,
    onNavigate: (LauncherPage) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(0.dp) // Invisible, navigation handled by swipe/gesture
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
        ) {
            // Navigation handled via swipe gestures instead
        }
    }
}
