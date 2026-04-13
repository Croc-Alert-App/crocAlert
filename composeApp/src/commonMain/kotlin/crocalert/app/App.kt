package crocalert.app

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import crocalert.app.ui.auth.SessionCheckResult
import crocalert.app.ui.auth.SessionManager
import crocalert.app.ui.auth.SplashScreen
import crocalert.app.ui.dashboard.DashboardScreen
import crocalert.app.ui.dashboard.DashboardTab

@Composable
fun App() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var detailSelectedTab by rememberSaveable { mutableStateOf(DashboardTab.Alerts) }
    var sessionExpired by rememberSaveable { mutableStateOf(false) }

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
            SplashScreen(onSessionChecked = { result ->
                sessionExpired = result == SessionCheckResult.Expired
                val dest = if (result == SessionCheckResult.Active) "home" else "login"
                navController.navigate(dest) { popUpTo("splash") { inclusive = true } }
            })
        }
        composable("login") {
            var savedEmail by rememberSaveable { mutableStateOf("") }
            LaunchedEffect(Unit) { savedEmail = SessionManager.getSavedEmail() ?: "" }
            LoginScreen(
                isLoading = isLoading,
                error = loginError,
                initialEmail = savedEmail,
                sessionExpired = sessionExpired,
                onLogin = { email, password, rememberDevice ->
                    sessionExpired = false
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
            var showSessionExpiredDialog by remember { mutableStateOf(false) }

            // Wait exactly until the session expires, then surface the dialog.
            // LaunchedEffect is cancelled automatically when leaving this destination.
            LaunchedEffect(Unit) {
                val remainingMs = SessionManager.sessionRemainingMs()
                if (remainingMs != null) {
                    if (remainingMs > 0L) delay(remainingMs)
                    showSessionExpiredDialog = true
                }
            }

            if (showSessionExpiredDialog) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("Sesión expirada") },
                    text = { Text("Tu sesión ha expirado. Por favor, inicia sesión nuevamente.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showSessionExpiredDialog = false
                            sessionExpired = true
                            scope.launch {
                                SessionManager.logout()
                                navController.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        }) { Text("Aceptar") }
                    },
                )
            }

            DashboardScreen(
                onAlertClick = { alertId ->
                    detailSelectedTab = DashboardTab.Alerts
                    navController.navigate("alert_detail/$alertId")
                },
                onLogout = {
                    scope.launch {
                        SessionManager.logout()
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
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
