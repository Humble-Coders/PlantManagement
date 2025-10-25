package com.humblecoders.plantmanagement.data

import com.google.cloud.Timestamp

/**
 * Represents a unified transaction history item for a specific date
 */
data class HistoryTransaction(
    val id: String = "",
    val transactionType: HistoryTransactionType = HistoryTransactionType.SALE,
    val date: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val status: String = "",
    val createdAt: Timestamp? = null,
    val originalData: Any? = null // Store original Sale, Purchase, CashTransaction, etc.
)

/**
 * Types of transactions that can appear in history
 */
enum class HistoryTransactionType {
    SALE,
    PURCHASE,
    CASH_IN_CUSTOMER,
    CASH_OUT_CUSTOMER,
    CASH_IN_SALES,
    CASH_OUT_PURCHASES,
    DIFFERENCE_CASH_IN,
    DIFFERENCE_CASH_OUT,
    CASH_OUT_GENERAL,
    PRODUCTION,
    EXPENSE,
    CASH_REPORT_IN,
    CASH_REPORT_OUT
}

/**
 * Represents a day's transaction summary
 */
data class DayHistory(
    val date: String = "",
    val transactions: List<HistoryTransaction> = emptyList(),
    val summary: DaySummary = DaySummary()
)

/**
 * Summary of transactions for a specific day
 */
data class DaySummary(
    val totalSales: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val totalCashIn: Double = 0.0,
    val totalCashOut: Double = 0.0,
    val totalDifferenceIn: Double = 0.0,
    val totalDifferenceOut: Double = 0.0,
    val totalProduction: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalCashReportIn: Double = 0.0,
    val totalCashReportOut: Double = 0.0,
    val netAmount: Double = 0.0,
    val transactionCount: Int = 0
)
