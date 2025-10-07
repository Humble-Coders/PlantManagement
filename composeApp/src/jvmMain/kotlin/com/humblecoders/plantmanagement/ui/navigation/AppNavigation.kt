package com.humblecoders.plantmanagement.ui.navigation

import androidx.compose.runtime.*
import com.humblecoders.plantmanagement.ui.LoginScreen
import com.humblecoders.plantmanagement.ui.MainScreen
import com.humblecoders.plantmanagement.ui.SignUpScreen
import com.humblecoders.plantmanagement.viewmodels.AuthViewModel
import com.humblecoders.plantmanagement.viewmodels.PurchaseViewModel

enum class Screen {
    LOGIN,
    SIGNUP,
    MAIN
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    entityViewModel: com.humblecoders.plantmanagement.viewmodels.EntityViewModel,
    purchaseViewModel: PurchaseViewModel

) {
    val authState = authViewModel.authState
    var currentScreen by remember { mutableStateOf(Screen.LOGIN) }

    // Navigate to main screen when authenticated
    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated) {
            currentScreen = Screen.MAIN
        } else {
            currentScreen = Screen.LOGIN
        }
    }

    when (currentScreen) {
        Screen.LOGIN -> {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToSignUp = { currentScreen = Screen.SIGNUP }
            )
        }

        Screen.SIGNUP -> {
            SignUpScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = { currentScreen = Screen.LOGIN }
            )
        }

        Screen.MAIN -> {
            MainScreen(
                authViewModel = authViewModel,
                entityViewModel = entityViewModel,
                purchaseViewModel = purchaseViewModel
            )
        }
    }
}