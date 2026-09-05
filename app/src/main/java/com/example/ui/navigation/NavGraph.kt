package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.auth.*
import com.example.ui.contractor.ContractorMainScreen
import com.example.ui.labour.LabourMainScreen
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.ContractorViewModel
import com.example.viewmodel.LabourViewModel

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Otp : Screen("otp")
    object RoleSelection : Screen("role_selection")
    object ContractorRegister : Screen("contractor_register")
    object LabourRegister : Screen("labour_register")
    object ContractorMain : Screen("contractor_main")
    object LabourMain : Screen("labour_main")
    object LabourPending : Screen("labour_pending")
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToContractor = {
                    navController.navigate(Screen.ContractorMain.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLabour = {
                    navController.navigate(Screen.LabourMain.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLabourPending = {
                    navController.navigate(Screen.LabourPending.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToOtp = {
                    navController.navigate(Screen.Otp.route)
                }
            )
        }

        composable(Screen.Otp.route) {
            OtpScreen(
                authViewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onExistingUserContractor = {
                    navController.navigate(Screen.ContractorMain.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onExistingUserLabour = {
                    navController.navigate(Screen.LabourMain.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onExistingUserLabourPending = {
                    navController.navigate(Screen.LabourPending.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNewUser = {
                    navController.navigate(Screen.RoleSelection.route)
                }
            )
        }

        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(
                authViewModel = authViewModel,
                onSelectContractor = {
                    navController.navigate(Screen.ContractorRegister.route)
                },
                onSelectLabour = {
                    navController.navigate(Screen.LabourRegister.route)
                }
            )
        }

        composable(Screen.ContractorRegister.route) {
            ContractorRegisterScreen(
                authViewModel = authViewModel,
                onRegistrationSuccess = {
                    navController.navigate(Screen.ContractorMain.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.LabourRegister.route) {
            LabourRegisterScreen(
                authViewModel = authViewModel,
                onSubmitSuccess = {
                    navController.navigate(Screen.LabourPending.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.LabourPending.route) {
            LabourPendingScreen(
                authViewModel = authViewModel,
                onApproved = {
                    navController.navigate(Screen.LabourMain.route) {
                        popUpTo(Screen.LabourPending.route) { inclusive = true }
                    }
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ContractorMain.route) {
            val contractorViewModel = ContractorViewModel(
                authViewModel.getApplication(),
                authViewModel.getRepository()
            )
            ContractorMainScreen(
                viewModel = contractorViewModel,
                onLogout = {
                    authViewModel.logout {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.LabourMain.route) {
            val labourViewModel = LabourViewModel(
                authViewModel.getApplication(),
                authViewModel.getRepository()
            )
            LabourMainScreen(
                viewModel = labourViewModel,
                onLogout = {
                    authViewModel.logout {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
