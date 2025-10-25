package com.humblecoders.plantmanagement.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.humblecoders.plantmanagement.data.DayHistory
import com.humblecoders.plantmanagement.data.HistoryTransaction
import com.humblecoders.plantmanagement.data.HistoryTransactionType
import com.humblecoders.plantmanagement.repositories.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class HistoryState(
    val selectedDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val dayHistory: DayHistory? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class HistoryViewModel(
    private val historyRepository: HistoryRepository
) {
    var historyState by mutableStateOf(HistoryState())
        private set

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    fun selectDate(date: String) {
        historyState = historyState.copy(selectedDate = date)
        loadHistoryForDate(date)
    }

    fun loadHistoryForDate(date: String) {
        historyState = historyState.copy(isLoading = true, error = null)
        
        coroutineScope.launch {
            try {
                val result = historyRepository.getTransactionsForDate(date)
                result.fold(
                    onSuccess = { dayHistory ->
                        historyState = historyState.copy(
                            dayHistory = dayHistory,
                            isLoading = false,
                            error = null
                        )
                    },
                    onFailure = { exception ->
                        historyState = historyState.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load history"
                        )
                    }
                )
            } catch (e: Exception) {
                historyState = historyState.copy(
                    isLoading = false,
                    error = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun refreshHistory() {
        loadHistoryForDate(historyState.selectedDate)
    }

    fun clearError() {
        historyState = historyState.copy(error = null)
    }

    fun clearSuccessMessage() {
        historyState = historyState.copy(successMessage = null)
    }

    fun getFilteredTransactions(): List<HistoryTransaction> {
        return historyState.dayHistory?.transactions ?: emptyList()
    }

    fun getTransactionsByType(type: HistoryTransactionType): List<HistoryTransaction> {
        return getFilteredTransactions().filter { it.transactionType == type }
    }

    fun getSalesTransactions(): List<HistoryTransaction> {
        return getTransactionsByType(HistoryTransactionType.SALE)
    }

    fun getPurchaseTransactions(): List<HistoryTransaction> {
        return getTransactionsByType(HistoryTransactionType.PURCHASE)
    }

    fun getCashInTransactions(): List<HistoryTransaction> {
        return getFilteredTransactions().filter { 
            it.transactionType == HistoryTransactionType.CASH_IN_CUSTOMER ||
            it.transactionType == HistoryTransactionType.CASH_IN_SALES ||
            it.transactionType == HistoryTransactionType.DIFFERENCE_CASH_IN
        }
    }

    fun getCashOutTransactions(): List<HistoryTransaction> {
        return getFilteredTransactions().filter { 
            it.transactionType == HistoryTransactionType.CASH_OUT_CUSTOMER ||
            it.transactionType == HistoryTransactionType.CASH_OUT_PURCHASES ||
            it.transactionType == HistoryTransactionType.CASH_OUT_GENERAL ||
            it.transactionType == HistoryTransactionType.DIFFERENCE_CASH_OUT
        }
    }

    fun getDifferenceTransactions(): List<HistoryTransaction> {
        return getFilteredTransactions().filter { 
            it.transactionType == HistoryTransactionType.DIFFERENCE_CASH_IN ||
            it.transactionType == HistoryTransactionType.DIFFERENCE_CASH_OUT
        }
    }

    fun getCustomerTransactions(): List<HistoryTransaction> {
        return getFilteredTransactions().filter { 
            it.transactionType == HistoryTransactionType.CASH_IN_CUSTOMER ||
            it.transactionType == HistoryTransactionType.CASH_OUT_CUSTOMER
        }
    }

    fun getSalesModuleTransactions(): List<HistoryTransaction> {
        return getFilteredTransactions().filter { 
            it.transactionType == HistoryTransactionType.CASH_IN_SALES
        }
    }

    fun getPurchaseModuleTransactions(): List<HistoryTransaction> {
        return getFilteredTransactions().filter { 
            it.transactionType == HistoryTransactionType.CASH_OUT_PURCHASES
        }
    }

    // Debug method to see all transactions
    fun getAllTransactions(): List<HistoryTransaction> {
        return historyState.dayHistory?.transactions ?: emptyList()
    }

    // Debug method to see transaction count by type
    fun getTransactionCountByType(): Map<HistoryTransactionType, Int> {
        val transactions = getAllTransactions()
        return transactions.groupBy { it.transactionType }
            .mapValues { it.value.size }
    }
}
