package com.humblecoders.plantmanagement.data

import com.google.cloud.Timestamp

/**
 * Represents a standalone cash report transaction for company cash tracking
 * This is separate from customer-related cash transactions
 */
data class CashReport(
    val id: String = "",
    val userId: String = "",
    val transactionType: CashReportType = CashReportType.CASH_IN,
    val categoryId: String = "",
    val amount: Double = 0.0,
    val date: Timestamp? = null,
    val notes: String = "",
    val createdAt: Timestamp? = null
)

/**
 * Type of cash report transaction
 */
enum class CashReportType {
    CASH_IN,    // Money coming into the company
    CASH_OUT    // Money going out of the company
}

/**
 * Represents a category for cash report transactions
 */
data class CashReportCategory(
    val id: String = "",
    val name: String = "",
    val createdAt: Timestamp? = null
)
