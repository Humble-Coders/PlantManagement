package com.humblecoders.plantmanagement.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humblecoders.plantmanagement.data.CashTransaction
import com.humblecoders.plantmanagement.data.CashTransactionType
import com.humblecoders.plantmanagement.data.Entity
import com.humblecoders.plantmanagement.repositories.CashTransactionRepository
import kotlinx.coroutines.launch

data class CashTransactionState(
    val transactions: List<CashTransaction> = emptyList(),
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class CashTransactionViewModel(
    private val cashTransactionRepository: CashTransactionRepository
) : ViewModel() {

    var cashTransactionState by mutableStateOf(CashTransactionState())
        private set

    fun processCashTransaction(
        customerId: String,
        amount: Double,
        transactionType: CashTransactionType,
        note: String = ""
    ) {
        viewModelScope.launch {
            cashTransactionState = cashTransactionState.copy(isProcessing = true, error = null)
            
            val result = cashTransactionRepository.processCashTransaction(
                customerId = customerId,
                amount = amount,
                transactionType = transactionType,
                note = note
            )
            
            result.fold(
                onSuccess = { transactionId ->
                    cashTransactionState = cashTransactionState.copy(
                        isProcessing = false,
                        successMessage = "Cash transaction processed successfully"
                    )
                },
                onFailure = { error ->
                    cashTransactionState = cashTransactionState.copy(
                        isProcessing = false,
                        error = error.message ?: "Failed to process cash transaction"
                    )
                }
            )
        }
    }
    
    fun getCashTransactions(
        customerId: String? = null,
        transactionType: CashTransactionType? = null
    ) {
        viewModelScope.launch {
            cashTransactionState = cashTransactionState.copy(isLoading = true, error = null)
            
            try {
                val transactions = cashTransactionRepository.getCashTransactions(
                    customerId = customerId,
                    transactionType = transactionType
                )
                cashTransactionState = cashTransactionState.copy(
                    isLoading = false,
                    transactions = transactions
                )
            } catch (e: Exception) {
                cashTransactionState = cashTransactionState.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load cash transactions"
                )
            }
        }
    }
    
    fun listenToCashTransactions(
        customerId: String? = null,
        transactionType: CashTransactionType? = null
    ) {
        cashTransactionRepository.listenToCashTransactions(
            customerId = customerId,
            transactionType = transactionType
        ) { transactions ->
            cashTransactionState = cashTransactionState.copy(transactions = transactions)
        }
    }
    
    fun getCustomerDetails(customerId: String, onResult: (Result<Entity>) -> Unit) {
        viewModelScope.launch {
            val result = cashTransactionRepository.getCustomerDetails(customerId)
            onResult(result)
        }
    }
    
    fun clearMessages() {
        cashTransactionState = cashTransactionState.copy(
            error = null,
            successMessage = null
        )
    }
}
