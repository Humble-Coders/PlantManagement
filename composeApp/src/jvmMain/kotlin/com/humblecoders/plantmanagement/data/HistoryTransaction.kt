package com.humblecoders.plantmanagement.data

import com.google.cloud.Timestamp

/**
 * Represents a unified transaction record for the History module
 * This combines different types of transactions into a single view
 */
data class HistoryTransaction(
    val id: String = "",
    val transactionType: HistoryTransactionType = HistoryTransactionType.SALE,
    val customerId: String = "",
    val customerName: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val date: String = "",
    val createdAt: Timestamp? = null,
    val status: TransactionStatus = TransactionStatus.APPROVED,
    val notes: String = "",
    val additionalDetails: Map<String, Any> = emptyMap()
)

/**
 * Types of transactions that can appear in history
 */
enum class HistoryTransactionType {
    SALE,                    // Sale records
    PURCHASE,                // Purchase records
    CASH_IN_CUSTOMER,        // Cash in from customer transactions
    CASH_OUT_CUSTOMER,       // Cash out from customer transactions
    CASH_IN_SALES,           // Cash in from sales revenue
    CASH_OUT_PURCHASE,       // Cash out from purchases
    DIFFERENCE_HISTORY,      // Difference amount transactions
    EXPENSE                  // Expense records
}

/**
 * Summary data for a specific date
 */
data class DailyTransactionSummary(
    val date: String = "",
    val totalSales: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val totalCashIn: Double = 0.0,
    val totalCashOut: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netCashFlow: Double = 0.0,
    val transactionCount: Int = 0,
    val transactions: List<HistoryTransaction> = emptyList()
)
