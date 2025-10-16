package com.humblecoders.plantmanagement.data

import com.google.cloud.Timestamp

/**
 * Represents a cash transaction between the company and a customer
 */
data class CashTransaction(
    val id: String = "",
    val userId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val amount: Double = 0.0,
    val transactionType: CashTransactionType = CashTransactionType.RECEIVE,
    val note: String = "",
    val previousBalance: Double = 0.0,
    val newBalance: Double = 0.0,
    val createdAt: Timestamp? = null
)

/**
 * Type of cash transaction
 */
enum class CashTransactionType {
    GIVE,    // Company gives money to customer
    RECEIVE  // Company receives money from customer
}
