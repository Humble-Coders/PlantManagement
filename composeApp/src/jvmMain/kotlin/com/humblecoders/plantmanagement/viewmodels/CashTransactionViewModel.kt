package com.humblecoders.plantmanagement.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humblecoders.plantmanagement.data.CashTransaction
import com.humblecoders.plantmanagement.data.CashTransactionType
import com.humblecoders.plantmanagement.data.Entity
import com.humblecoders.plantmanagement.data.DifferenceTransactionType
import com.humblecoders.plantmanagement.repositories.CashTransactionRepository
import com.humblecoders.plantmanagement.repositories.CashOutRepository
import com.humblecoders.plantmanagement.repositories.CashInRepository
import kotlinx.coroutines.launch

data class CashTransactionState(
    val transactions: List<CashTransaction> = emptyList(),
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class CashTransactionViewModel(
    private val cashTransactionRepository: CashTransactionRepository,
    private val cashOutRepository: CashOutRepository,
    private val cashInRepository: CashInRepository
) : ViewModel() {

    var cashTransactionState by mutableStateOf(CashTransactionState())
        private set

    init {
        // Listen to all cash transactions from entity screen
        cashTransactionRepository.listenToCashTransactions { transactions ->
            val currentCashOuts = cashTransactionState.transactions.filter { 
                it.note.contains("Cash Out from Purchase Module") || it.note.contains("Difference Cash")
            }
            val entityTransactions = transactions.filter { 
                !it.note.contains("Cash Out from Purchase Module") && !it.note.contains("Difference Cash")
            }
            cashTransactionState = cashTransactionState.copy(
                transactions = entityTransactions + currentCashOuts
            )
        }
        
        // Listen to cash out transactions from purchase module
        cashOutRepository.listenToCashOutHistory { cashOuts ->
            val cashOutTransactions = cashOuts.flatMap { cashOut ->
                cashOut.purchaseAllocations.map { allocation ->
                    CashTransaction(
                        id = "${cashOut.id}_${allocation.purchaseId}",
                        userId = cashOut.userId,
                        customerId = allocation.customerId,
                        customerName = allocation.firmName,
                        amount = allocation.allocatedAmount,
                        transactionType = CashTransactionType.GIVE, // Cash out means we gave money to customer
                        note = "Cash Out from Purchase Module: ${cashOut.notes}",
                        previousBalance = 0.0, // Not tracked in cash out
                        newBalance = 0.0, // Not tracked in cash out
                        createdAt = cashOut.createdAt
                    )
                }
            }
            
            val entityTransactions = cashTransactionState.transactions.filter { 
                !it.note.contains("Cash Out from Purchase Module") && !it.note.contains("Difference Cash")
            }
            val currentDifferenceTransactions = cashTransactionState.transactions.filter { 
                it.note.contains("Difference Cash")
            }
            cashTransactionState = cashTransactionState.copy(
                transactions = entityTransactions + cashOutTransactions + currentDifferenceTransactions
            )
        }
        
        // Listen to difference cash transactions from sale module
        cashInRepository.listenToCashInOutDifferenceHistory { differenceTransactions ->
            val differenceCashTransactions = differenceTransactions.flatMap { diffTransaction ->
                diffTransaction.saleAllocations.map { allocation ->
                    CashTransaction(
                        id = "${diffTransaction.id}_${allocation.saleId}",
                        userId = diffTransaction.userId,
                        customerId = allocation.customerId,
                        customerName = allocation.firmName,
                        amount = allocation.allocatedAmount,
                        transactionType = when (diffTransaction.transactionType) {
                            DifferenceTransactionType.CASH_IN -> CashTransactionType.RECEIVE
                            DifferenceTransactionType.CASH_OUT -> CashTransactionType.GIVE
                        },
                        note = "Difference Cash ${diffTransaction.transactionType.name}: ${diffTransaction.notes}",
                        previousBalance = 0.0, // Not tracked in difference transactions
                        newBalance = 0.0, // Not tracked in difference transactions
                        createdAt = diffTransaction.createdAt
                    )
                }
            }
            
            val entityTransactions = cashTransactionState.transactions.filter { 
                !it.note.contains("Cash Out from Purchase Module") && !it.note.contains("Difference Cash")
            }
            val currentCashOutTransactions = cashTransactionState.transactions.filter { 
                it.note.contains("Cash Out from Purchase Module")
            }
            cashTransactionState = cashTransactionState.copy(
                transactions = entityTransactions + currentCashOutTransactions + differenceCashTransactions
            )
        }
    }

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
    
    /**
     * Get cash transactions by customer ID
     */
    fun getCashTransactionsByCustomerId(customerId: String) {
        viewModelScope.launch {
            cashTransactionState = cashTransactionState.copy(isLoading = true, error = null)
            
            val result = cashTransactionRepository.getCashTransactionsByCustomerId(customerId)
            
            result.fold(
                onSuccess = { transactions ->
                    cashTransactionState = cashTransactionState.copy(
                        isLoading = false,
                        transactions = transactions,
                        error = null
                    )
                },
                onFailure = { exception ->
                    cashTransactionState = cashTransactionState.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load cash transactions"
                    )
                }
            )
        }
    }
    
    /**
     * Manually refresh cash transactions data
     */
    fun refreshCashTransactions(customerId: String? = null, transactionType: CashTransactionType? = null) {
        viewModelScope.launch {
            cashTransactionState = cashTransactionState.copy(isLoading = true, error = null)
            
            val result = cashTransactionRepository.refreshCashTransactions(customerId, transactionType)
            
            cashTransactionState = if (result.isSuccess) {
                cashTransactionState.copy(
                    isLoading = false,
                    transactions = result.getOrNull() ?: emptyList(),
                    error = null
                )
            } else {
                cashTransactionState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to refresh cash transactions"
                )
            }
        }
    }
    
    fun clearMessages() {
        cashTransactionState = cashTransactionState.copy(
            error = null,
            successMessage = null
        )
    }
}
