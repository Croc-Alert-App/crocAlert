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
import androidx.compose.runtime.rememberCoroutineScope
import crocalert.app.ui.auth.AuthGateway
import crocalert.app.ui.auth.AuthResult
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import crocalert.app.ui.auth.SetupMfaScreen
import crocalert.app.ui.auth.TotpEnrollmentInfo
import crocalert.app.ui.auth.EnrollMfaScreen

@Composable
fun App(
    authGateway: AuthGateway,
         ) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var detailSelectedTab by remember { mutableStateOf(DashboardTab.Alerts) }
    var showRegisterSuccessDialog by remember { mutableStateOf(false) }
    var loginErrorMessage by remember { mutableStateOf<String?>(null) }
    var totpEnrollmentInfo by remember { mutableStateOf<TotpEnrollmentInfo?>(null) }


    if (showRegisterSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showRegisterSuccessDialog = false },
            title = {
                Text("Cuenta creada")
            },
            text = {
                Text("Tu registro fue exitoso. Ahora puedes iniciar sesión.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRegisterSuccessDialog = false
                        navController.popBackStack()
                    },
                ) {
                    Text("Aceptar")
                }
            },
        )
    }

    if (loginErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { loginErrorMessage = null },
            title = {
                Text("No se pudo iniciar sesión")
            },
            text = {
                Text(loginErrorMessage ?: "")
            },
            confirmButton = {
                TextButton(
                    onClick = { loginErrorMessage = null },
                ) {
                    Text("Aceptar")
                }
            },
        )
    }


    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onSessionChecked = { isDeviceRemembered ->
                val hasFirebaseSession = authGateway.currentUser() != null
                val dest = if (hasFirebaseSession && isDeviceRemembered) "home" else "login"

                navController.navigate(dest) {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        composable("login") {
            LoginScreen(
                onLogin = { email, password, rememberDevice ->
                    scope.launch {
                        when (val result = authGateway.signIn(email, password)) {
                            is AuthResult.Success -> {
                                if (rememberDevice) {
                                    SessionManager.rememberDevice()
                                } else {
                                    SessionManager.forgetDevice()
                                }

                                val nextRoute = if (authGateway.isTotpEnrolled()) "mfa" else "setup_mfa"

                                navController.navigate(nextRoute) {
                                    popUpTo("login") { inclusive = false }
                                }
                            }

                            AuthResult.MfaRequired -> {
                                navController.navigate("mfa") {
                                    popUpTo("login") { inclusive = false }
                                }
                            }

                            is AuthResult.Error -> {
                                loginErrorMessage = result.message
                            }
                        }
                    }
                },
                onRegister = { navController.navigate("register") },
                onForgotPassword = { navController.navigate("forgot_password") },
            )
        }
        composable("register") {
            RegisterScreen(
                onRegister = { nombre, apellidos, email, rol, password ->
                    scope.launch {
                        when (
                            authGateway.register(
                                nombre = nombre,
                                apellidos = apellidos,
                                email = email,
                                rol = rol,
                                password = password,
                            )
                        ) {
                            is AuthResult.Success -> {
                                authGateway.signOut()
                                showRegisterSuccessDialog = true
                            }

                            is AuthResult.Error -> {

                            }

                            AuthResult.MfaRequired -> Unit

                        }
                    }
                },
            )
        }
        composable("forgot_password") {
            ForgotPasswordScreen(
                authGateway = authGateway,
                onBack = { navController.popBackStack() },
            )
        }

        composable("setup_mfa") {
            SetupMfaScreen(
                onStartSetup = {
                    scope.launch {
                        when (val result = authGateway.startTotpEnrollment()) {
                            is AuthResult.Success -> {
                                totpEnrollmentInfo = result.data
                                navController.navigate("enroll_mfa")
                            }

                            is AuthResult.Error -> {
                                loginErrorMessage = result.message
                            }

                            AuthResult.MfaRequired -> Unit
                        }
                    }
                },
                onSkip = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
            )
        }

        composable("enroll_mfa") {
            val info = totpEnrollmentInfo ?: return@composable

            EnrollMfaScreen(
                enrollmentInfo = info,
                onConfirmCode = { code ->
                    scope.launch {
                        when (authGateway.finalizeTotpEnrollment(code)) {
                            is AuthResult.Success -> {
                                totpEnrollmentInfo = null
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }

                            is AuthResult.Error -> {
                                loginErrorMessage = "No se pudo confirmar MFA."
                            }

                            AuthResult.MfaRequired -> Unit
                        }
                    }
                },
                onCancel = {
                    totpEnrollmentInfo = null
                    navController.popBackStack()
                },
            )
        }

        composable("mfa") {
            MfaScreen(
                onVerify = { code ->
                    scope.launch {
                        when (val result = authGateway.finalizeTotpSignIn(code)) {
                            is AuthResult.Success -> {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }

                            is AuthResult.Error -> {
                                loginErrorMessage = result.message
                            }

                            AuthResult.MfaRequired -> Unit
                        }
                    }
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
                    authGateway.signOut()
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
