package com.sprint.runner.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.sprint.runner.presentation.history.HistoryScreen
import com.sprint.runner.presentation.progress.ProgressScreen
import com.sprint.runner.presentation.settings.SettingsScreen
import com.sprint.runner.presentation.timer.TimerScreen

object Destinations {
    const val TIMER_ROUTE = "timer"
    const val HISTORY_ROUTE = "history"
    const val PROGRESS_ROUTE = "progress"
    const val SETTINGS_ROUTE = "settings"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberSwipeDismissableNavController(),
    modifier: Modifier = Modifier
) {
    SwipeDismissableNavHost(
        navController = navController,
        startDestination = Destinations.TIMER_ROUTE,
        modifier = modifier
    ) {
        composable(Destinations.TIMER_ROUTE) {
            TimerScreen(
                onNavigateToHistory = {
                    navController.navigate(Destinations.HISTORY_ROUTE)
                },
                onNavigateToProgress = {
                    navController.navigate(Destinations.PROGRESS_ROUTE)
                },
                onNavigateToSettings = {
                    navController.navigate(Destinations.SETTINGS_ROUTE)
                }
            )
        }

        composable(Destinations.SETTINGS_ROUTE) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Destinations.HISTORY_ROUTE) {
            HistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Destinations.PROGRESS_ROUTE) {
            ProgressScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}