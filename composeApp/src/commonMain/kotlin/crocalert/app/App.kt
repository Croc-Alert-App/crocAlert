package crocalert.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import crocalert.app.feature.alerts.ui.AlertDetailScreen
import crocalert.app.ui.auth.AuthViewModel
import crocalert.app.ui.auth.ForgotPasswordScreen
import crocalert.app.ui.auth.LoginScreen
import crocalert.app.ui.auth.MfaScreen
import crocalert.app.ui.auth.MfaSetupScreen
import crocalert.app.ui.auth.RegisterScreen
import crocalert.app.ui.auth.SessionManager
import crocalert.app.ui.auth.SplashScreen
import crocalert.app.ui.dashboard.DashboardScreen
import crocalert.app.ui.dashboard.DashboardTab

@Composable
fun App() {
    val navController = rememberNavController()
    var detailSelectedTab by rememberSaveable { mutableStateOf(DashboardTab.Alerts) }

    val authViewModel: AuthViewModel = viewModel { AuthViewModel() }
    val loginError by authViewModel.loginError.collectAsState()
    val mfaError by authViewModel.mfaError.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val registerError by authViewModel.registerError.collectAsState()
    val registerSuccess by authViewModel.registerSuccess.collectAsState()
    val totpSetup by authViewModel.totpSetup.collectAsState()
    val totpSetupError by authViewModel.totpSetupError.collectAsState()
    val enrollError by authViewModel.enrollError.collectAsState()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onSessionChecked = { isLoggedIn ->
                val dest = if (isLoggedIn) "home" else "login"
                navController.navigate(dest) { popUpTo("splash") { inclusive = true } }
            })
        }
        composable("login") {
            LoginScreen(
                isLoading = isLoading,
                error = loginError,
                onLogin = { email, password, rememberDevice ->
                    authViewModel.login(
                        email = email,
                        password = password,
                        rememberDevice = rememberDevice,
                        onMfaRequired = {
                            navController.navigate("mfa") {
                                popUpTo("login") { inclusive = false }
                            }
                        },
                        onMfaEnrollmentRequired = {
                            navController.navigate("mfa_setup") {
                                popUpTo("login") { inclusive = false }
                            }
                        },
                        onSuccess = {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                    )
                },
                onRegister = { navController.navigate("register") },
                onForgotPassword = { navController.navigate("forgot_password") },
                onErrorDismiss = { authViewModel.clearLoginError() },
            )
        }
        composable("register") {
            RegisterScreen(
                isLoading = isLoading,
                registerError = registerError,
                registerSuccess = registerSuccess,
                onRegister = { nombre, apellidos, email, rol, password ->
                    authViewModel.register(nombre, apellidos, email, rol, password)
                },
                onSuccessDismiss = {
                    authViewModel.clearRegisterSuccess()
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onErrorDismiss = { authViewModel.clearRegisterError() },
            )
        }
        composable("forgot_password") {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
        composable("mfa") {
            MfaScreen(
                isLoading = isLoading,
                error = mfaError,
                onVerify = { otp ->
                    authViewModel.verifyTotp(otp) {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                },
                onResend = {},
                onUseBackupCode = {},
                onErrorDismiss = { authViewModel.clearMfaError() },
            )
        }
        composable("mfa_setup") {
            MfaSetupScreen(
                isLoading = isLoading,
                setupData = totpSetup,
                setupError = totpSetupError,
                enrollError = enrollError,
                onGenerateSetup = { authViewModel.generateTotpSetup() },
                onEnroll = { otp ->
                    authViewModel.enrollTotp(otp) {
                        authViewModel.clearTotpSetup()
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                },
                onEnrollErrorDismiss = { authViewModel.clearEnrollError() },
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
