package com.sam.audioroutine.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sam.audioroutine.feature.player.ActivePlaybackScreen
import com.sam.audioroutine.feature.routine.RoutineEditorScreen
import com.sam.audioroutine.feature.schedule.ScheduleScreen

private data class RootDestination(val route: String, val label: String)

@Composable
fun AudioRoutineRoot() {
    val navController = rememberNavController()
    val destinations = listOf(
        RootDestination("routine", "Routine"),
        RootDestination("schedule", "Schedule")
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route != "player_playback"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!showBottomBar) {
                return@Scaffold
            }
            Surface(shadowElevation = 8.dp) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        destinations.forEach { destination ->
                            val isSelected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
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
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                        androidx.compose.foundation.layout.Box(
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
