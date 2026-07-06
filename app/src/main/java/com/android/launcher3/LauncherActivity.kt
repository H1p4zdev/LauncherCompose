package com.android.launcher3

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
                var swipeOffset by remember { mutableStateOf(0f) }
                val density = LocalDensity.current

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (uiState.currentPage == LauncherPage.WORKSPACE) {
                                Modifier.pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragEnd = {
                                            if (swipeOffset < -density.density * 100) {
                                                viewModel.navigateTo(LauncherPage.ALL_APPS)
                                            }
                                            swipeOffset = 0f
                                        },
                                        onVerticalDrag = { _, dragAmount ->
                                            swipeOffset += dragAmount
                                        }
                                    )
                                }
                            } else Modifier
                        )
                ) {
                    if (viewModel.wallpaperDrawable != null) {
                        val bitmap = remember(viewModel.wallpaperDrawable) {
                            val drawable = viewModel.wallpaperDrawable
                            if (drawable is BitmapDrawable) {
                                drawable.bitmap
                            } else {
                                val bmp = android.graphics.Bitmap.createBitmap(
                                    1080, 1920, android.graphics.Bitmap.Config.ARGB_8888
                                )
                                val canvas = android.graphics.Canvas(bmp)
                                drawable?.setBounds(0, 0, canvas.width, canvas.height)
                                drawable?.draw(canvas)
                                bmp
                            }
                        }
                        Image(
                            painter = BitmapPainter(bitmap.asImageBitmap()),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                        )
                    }

                    AnimatedContent(
                        targetState = uiState.currentPage,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            when {
                                targetState == LauncherPage.ALL_APPS -> slideInVertically(
                                    animationSpec = androidx.compose.animation.core.tween(300),
                                    initialOffsetY = { it }
                                ) + fadeIn() togetherWith
                                    slideOutVertically(
                                        animationSpec = androidx.compose.animation.core.tween(300),
                                        targetOffsetY = { it / 3 }
                                    ) + fadeOut()
                                targetState == LauncherPage.WORKSPACE -> slideInVertically(
                                    animationSpec = androidx.compose.animation.core.tween(300),
                                    initialOffsetY = { -it }
                                ) + fadeIn() togetherWith
                                    slideOutVertically(
                                        animationSpec = androidx.compose.animation.core.tween(300),
                                        targetOffsetY = { -it / 3 }
                                    ) + fadeOut()
                                else -> fadeIn() togetherWith fadeOut()
                            }
                        },
                        label = "pageTransition"
                    ) { page ->
                        when (page) {
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
                                    onBack = { viewModel.navigateTo(LauncherPage.WORKSPACE) },
                                    onAddWidget = { widget ->
                                        viewModel.addWidgetToWorkspace(widget)
                                        viewModel.navigateTo(LauncherPage.WORKSPACE)
                                    }
                                )
                            }
                            LauncherPage.SETTINGS -> {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateTo(LauncherPage.WORKSPACE) }
                                )
                            }
                        }
                    }

                    if (uiState.currentPage == LauncherPage.WORKSPACE) {
                        HotseatScreen(
                            viewModel = viewModel,
                            hazeState = hazeState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .hazeEffect(hazeState)
                                .navigationBarsPadding()
                        )
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
}
