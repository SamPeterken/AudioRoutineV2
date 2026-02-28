package com.sam.audioroutine.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sam.audioroutine.data.bundled.BundledMediaCatalog
import com.sam.audioroutine.feature.player.ActivePlaybackScreen
import com.sam.audioroutine.feature.background.AppBackgroundViewModel
import com.sam.audioroutine.feature.player.PlayerViewModel
import com.sam.audioroutine.feature.player.shouldForceActivePlaybackRoute
import com.sam.audioroutine.feature.routine.RoutineEditorScreen
import com.sam.audioroutine.feature.schedule.ScheduleScreen
import coil.compose.AsyncImage

private data class RootDestination(val route: String, val label: String)

@Composable
fun AudioRoutineRoot(
    openPlaybackRequestNonce: Int = 0,
    backgroundViewModel: AppBackgroundViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val backgroundState by backgroundViewModel.uiState.collectAsStateWithLifecycle()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val destinations = listOf(
        RootDestination("routine", "Routine"),
        RootDestination("schedule", "Schedule")
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val isPlaybackRoute = currentRoute == "player_playback"
    val isRoutinePlaying = playerState.playbackProgress.isRunning
    val showBottomBar = !isPlaybackRoute && !isRoutinePlaying
    val navTextColor = MaterialTheme.colorScheme.onSurface
    val navSecondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val navOutlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.34f)

    PlaybackSystemUiEffect(enabled = isPlaybackRoute)

    LaunchedEffect(isRoutinePlaying, currentRoute) {
        if (shouldForceActivePlaybackRoute(isRoutinePlaying, currentRoute)) {
            navController.navigate("player_playback") {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(openPlaybackRequestNonce) {
        if (openPlaybackRequestNonce > 0) {
            navController.navigate("player_playback") {
                launchSingleTop = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackgroundLayer(backgroundUri = backgroundState.backgroundUri)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                if (!showBottomBar) {
                    return@Scaffold
                }
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.64f)
                ) {
                    Column {
                        HorizontalDivider(color = navOutlineColor)
                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp
                        ) {
                            destinations.forEach { destination ->
                                val isSelected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        if (isRoutinePlaying) return@NavigationBarItem
                                        navController.navigate(destination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    label = {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                destination.label.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) {
                                                    navTextColor
                                                } else {
                                                    navSecondaryTextColor
                                                }
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.4f)
                                                    .background(
                                                        color = if (isSelected) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                    .padding(vertical = 1.5.dp)
                                            )
                                        }
                                    },
                                    icon = {},
                                    alwaysShowLabel = true
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            val navModifier = if (showBottomBar) {
                Modifier.padding(paddingValues)
            } else {
                Modifier
            }
            NavHost(
                navController = navController,
                startDestination = "routine",
                modifier = navModifier
            ) {
                composable("routine") {
                    RoutineEditorScreen(
                        onOpenActivePlayback = { navController.navigate("player_playback") }
                    )
                }
                composable("player_playback") {
                    ActivePlaybackScreen(
                        onExitPlayback = {
                            navController.popBackStack(route = "routine", inclusive = false)
                        }
                    )
                }
                composable("schedule") { ScheduleScreen() }
            }
        }
    }
}

@Composable
private fun PlaybackSystemUiEffect(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        val activity = view.context.findActivity()
        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }

        if (window != null && insetsController != null) {
            WindowCompat.setDecorFitsSystemWindows(window, !enabled)
            if (enabled) {
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }

        onDispose {
            if (window != null && insetsController != null) {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@Composable
private fun AppBackgroundLayer(backgroundUri: String?) {
    val fallbackModel = BundledMediaCatalog.defaultBackgroundAssetUri()
    val resolvedModel = backgroundUri?.takeIf { it.isNotBlank() } ?: fallbackModel

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    )

    AsyncImage(
        model = resolvedModel,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .blur(8.dp)
    )
}
