package crocalert.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import crocalert.app.feature.alerts.ui.AlertDetailScreen
import crocalert.app.ui.dashboard.DashboardScreen
import crocalert.app.ui.dashboard.DashboardTab

@Composable
fun App() {
    val navController = rememberNavController()
    // Track which tab should appear selected on the detail screen
    var detailSelectedTab by remember { mutableStateOf(DashboardTab.Alerts) }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            DashboardScreen(
                onAlertClick = { alertId ->
                    detailSelectedTab = DashboardTab.Alerts
                    navController.navigate("alert_detail/$alertId")
                }
            )
        }
        composable("alert_detail/{alertId}") { backStackEntry ->
            val alertId = backStackEntry.arguments?.getString("alertId") ?: return@composable
            AlertDetailScreen(
                alertId = alertId,
                onBack = { navController.popBackStack() },
                selectedTab = detailSelectedTab,
                onTabSelect = { tab ->
                    detailSelectedTab = tab
                    navController.popBackStack()
                },
            )
        }
    }
}
