package com.humblecoders.plantmanagement.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.humblecoders.plantmanagement.data.DailyTransactionSummary
import com.humblecoders.plantmanagement.data.HistoryTransaction
import com.humblecoders.plantmanagement.data.HistoryTransactionType
import com.humblecoders.plantmanagement.repositories.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class HistoryState(
    val selectedDate: String = "",
    val dailySummary: DailyTransactionSummary? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val filteredTransactions: List<HistoryTransaction> = emptyList(),
    val selectedTransactionType: HistoryTransactionType? = null,
    val showDateRange: Boolean = false,
    val startDate: String = "",
    val endDate: String = "",
    val dateRangeSummaries: List<DailyTransactionSummary> = emptyList()
)

class HistoryViewModel(
    private val historyRepository: HistoryRepository
) {
    var historyState by mutableStateOf(HistoryState())
        private set

    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        // Set default date to today
        val today = dateFormat.format(Date())
        historyState = historyState.copy(selectedDate = today)
        loadTransactionsForDate(today)
    }

    /**
     * Load transactions for a specific date
     */
    fun loadTransactionsForDate(date: String) {
        viewModelScope.launch {
            historyState = historyState.copy(
                isLoading = true,
                error = null,
                selectedDate = date
            )

            val result = historyRepository.getTransactionsByDate(date)

            historyState = if (result.isSuccess) {
                val summary = result.getOrNull()!!
                historyState.copy(
                    isLoading = false,
                    dailySummary = summary,
                    filteredTransactions = summary.transactions,
                    error = null
                )
            } else {
                historyState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to load transactions"
                )
            }
        }
    }

    /**
     * Load transactions for a date range
     */
    fun loadTransactionsForDateRange(startDate: String, endDate: String) {
        viewModelScope.launch {
            historyState = historyState.copy(
                isLoading = true,
                error = null,
                startDate = startDate,
                endDate = endDate
            )

            val result = historyRepository.getTransactionsByDateRange(startDate, endDate)

            historyState = if (result.isSuccess) {
                val summaries = result.getOrNull()!!
                historyState.copy(
                    isLoading = false,
                    dateRangeSummaries = summaries,
                    error = null
                )
            } else {
                historyState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to load transactions"
                )
            }
        }
    }

    /**
     * Filter transactions by type
     */
    fun filterTransactionsByType(transactionType: HistoryTransactionType?) {
        historyState = historyState.copy(selectedTransactionType = transactionType)
        
        val summary = historyState.dailySummary
        if (summary != null) {
            val filtered = if (transactionType != null) {
                summary.transactions.filter { it.transactionType == transactionType }
            } else {
                summary.transactions
            }
            historyState = historyState.copy(filteredTransactions = filtered)
        }
    }

    /**
     * Toggle date range view
     */
    fun toggleDateRangeView() {
        historyState = historyState.copy(showDateRange = !historyState.showDateRange)
    }

    /**
     * Set selected date
     */
    fun setSelectedDate(date: String) {
        historyState = historyState.copy(selectedDate = date)
        loadTransactionsForDate(date)
    }

    /**
     * Set date range
     */
    fun setDateRange(startDate: String, endDate: String) {
        historyState = historyState.copy(startDate = startDate, endDate = endDate)
        loadTransactionsForDateRange(startDate, endDate)
    }

    /**
     * Get today's date
     */
    fun getTodayDate(): String {
        return dateFormat.format(Date())
    }

    /**
     * Get yesterday's date
     */
    fun getYesterdayDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        return dateFormat.format(calendar.time)
    }

    /**
     * Get date N days ago
     */
    fun getDateNDaysAgo(days: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -days)
        return dateFormat.format(calendar.time)
    }

    /**
     * Get total amount for a specific transaction type
     */
    fun getTotalAmountForType(transactionType: HistoryTransactionType): Double {
        return historyState.filteredTransactions
            .filter { it.transactionType == transactionType }
            .sumOf { it.amount }
    }

    /**
     * Get transaction count for a specific transaction type
     */
    fun getTransactionCountForType(transactionType: HistoryTransactionType): Int {
        return historyState.filteredTransactions
            .filter { it.transactionType == transactionType }
            .size
    }

    /**
     * Clear error message
     */
    fun clearError() {
        historyState = historyState.copy(error = null)
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        historyState = historyState.copy(successMessage = null)
    }

    /**
     * Clear all messages
     */
    fun clearMessages() {
        historyState = historyState.copy(error = null, successMessage = null)
    }

    /**
     * Refresh current data
     */
    fun refresh() {
        if (historyState.showDateRange) {
            loadTransactionsForDateRange(historyState.startDate, historyState.endDate)
        } else {
            loadTransactionsForDate(historyState.selectedDate)
        }
    }
}
