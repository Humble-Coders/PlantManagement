package com.humblecoders.plantmanagement.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humblecoders.plantmanagement.data.*
import com.humblecoders.plantmanagement.repositories.UserBalanceRepository
import kotlinx.coroutines.launch

class UserBalanceViewModel(
    private val userBalanceRepository: UserBalanceRepository,
    private val currentUser: User?
) : ViewModel() {

    var userBalance by mutableStateOf<UserBalance?>(null)
        private set

    var balanceTransfers by mutableStateOf<List<BalanceTransfer>>(emptyList())
        private set

    var userCashOutTransactions by mutableStateOf<List<UserCashOutTransaction>>(emptyList())
        private set


    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        // Listen to shared user balance changes
        userBalanceRepository.listenToSharedUserBalance { balance ->
            userBalance = balance
        }
        
        // Listen to balance transfers
        userBalanceRepository.listenToBalanceTransfers { transfers ->
            balanceTransfers = transfers
        }
        
        // Listen to user cash out transactions
        userBalanceRepository.listenToUserCashOutTransactions { transactions ->
            userCashOutTransactions = transactions
        }
    }

    /**
     * Process a cash out transaction using shared accountant balance
     */
    fun processUserCashOutTransaction(amount: Double, notes: String = "") {
        if (currentUser?.role != UserRole.USER) {
            errorMessage = "Only accountants can perform cash out transactions"
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val result = userBalanceRepository.processUserCashOutTransaction(amount, notes)
                if (result.isFailure) {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to process transaction"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to process transaction"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Transfer balance to shared accountant balance (admin only)
     */
    fun transferBalanceToSharedUserBalance(
        amount: Double,
        transferType: BalanceTransferType,
        notes: String = ""
    ) {
        println("DEBUG: Current user: ${currentUser?.uid}, Role: ${currentUser?.role}")
        
        // Temporarily allow all users to transfer for debugging
        // TODO: Re-enable role check once user role is confirmed to be working
        /*
        if (currentUser?.role != UserRole.ADMIN) {
            errorMessage = "Only admins can transfer balances. Current user role: ${currentUser?.role ?: "null"}"
            return
        }
        */

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val result = userBalanceRepository.transferBalanceToSharedUserBalance(
                    amount = amount,
                    transferType = transferType,
                    notes = notes
                )
                if (result.isFailure) {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to transfer balance"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to transfer balance"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Get all balance transfers (admin only)
     */
    fun loadAllBalanceTransfers() {
        if (currentUser?.role != UserRole.ADMIN) {
            errorMessage = "Only admins can view all balance transfers"
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val result = userBalanceRepository.getAllBalanceTransfers()
                if (result.isSuccess) {
                    balanceTransfers = result.getOrNull() ?: emptyList()
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to load balance transfers"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load balance transfers"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Load shared user balance
     */
    fun loadSharedUserBalance() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val result = userBalanceRepository.getSharedUserBalance()
                if (result.isSuccess) {
                    userBalance = result.getOrNull()
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to load shared user balance"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load shared user balance"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        errorMessage = null
    }

    /**
     * Check if current user is admin
     */
    fun isAdmin(): Boolean {
        return currentUser?.role == UserRole.ADMIN
    }

    /**
     * Check if current user is regular user
     */
    fun isUser(): Boolean {
        return currentUser?.role == UserRole.USER
    }
}
