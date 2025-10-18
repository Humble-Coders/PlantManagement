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

// Update AppNavigation.kt
@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    entityViewModel: com.humblecoders.plantmanagement.viewmodels.EntityViewModel,
    purchaseViewModel: PurchaseViewModel,
    inventoryViewModel: com.humblecoders.plantmanagement.viewmodels.InventoryViewModel,
    cashTransactionViewModel: com.humblecoders.plantmanagement.viewmodels.CashTransactionViewModel,
    cashReportViewModel: com.humblecoders.plantmanagement.viewmodels.CashReportViewModel,
    expenseViewModel: com.humblecoders.plantmanagement.viewmodels.ExpenseViewModel,  // Add this line
    productionViewModel: com.humblecoders.plantmanagement.viewmodels.ProductionViewModel
) {
    val authState = authViewModel.authState
    var currentScreen by remember { mutableStateOf(Screen.LOGIN) }

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
                purchaseViewModel = purchaseViewModel,
                inventoryViewModel = inventoryViewModel,
                cashTransactionViewModel = cashTransactionViewModel,
                cashReportViewModel = cashReportViewModel,
                expenseViewModel = expenseViewModel,  // Add this line
                productionViewModel = productionViewModel
            )
        }
    }
}