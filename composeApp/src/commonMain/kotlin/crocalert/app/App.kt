package crocalert.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import crocalert.app.feature.alerts.ui.AlertDetailScreen
import crocalert.app.ui.auth.ForgotPasswordScreen
import crocalert.app.ui.auth.LoginScreen
import crocalert.app.ui.auth.MfaScreen
import crocalert.app.ui.auth.RegisterScreen
import crocalert.app.ui.auth.SessionManager
import crocalert.app.ui.auth.SplashScreen
import crocalert.app.ui.dashboard.DashboardScreen
import crocalert.app.ui.dashboard.DashboardTab

@Composable
fun App() {
    val navController = rememberNavController()
    var detailSelectedTab by remember { mutableStateOf(DashboardTab.Alerts) }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onSessionChecked = { isLoggedIn ->
                val dest = if (isLoggedIn) "home" else "login"
                navController.navigate(dest) { popUpTo("splash") { inclusive = true } }
            })
        }
        composable("login") {
            LoginScreen(
                onLogin = { _, _, rememberDevice ->
                    if (rememberDevice) SessionManager.rememberDevice()
                    navController.navigate("mfa") { popUpTo("login") { inclusive = false } }
                },
                onRegister = { navController.navigate("register") },
                onForgotPassword = { navController.navigate("forgot_password") },
            )
        }
        composable("register") {
            RegisterScreen(
                onRegister = { _, _, _, _, _ -> navController.popBackStack() },
            )
        }
        composable("forgot_password") {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
        composable("mfa") {
            MfaScreen(
                onVerify = { _ ->
                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                },
                onResend = {},
                onUseBackupCode = {},
            )
        }
        composable("home") {
            DashboardScreen(
                onAlertClick = { alertId ->
                    detailSelectedTab = DashboardTab.Alerts
                    navController.navigate("alert_detail/$alertId")
                },
                onLogout = {
                    SessionManager.forgetDevice()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
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
