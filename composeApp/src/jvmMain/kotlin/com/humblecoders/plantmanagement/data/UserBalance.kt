package com.humblecoders.plantmanagement.data

import com.google.cloud.Timestamp

/**
 * Represents the shared balance for all users with USER role
 * This balance is separate from company cash and is managed by admin
 */
data class UserBalance(
    val id: String = "",
    val currentBalance: Double = 0.0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

/**
 * Represents a balance transfer from admin to shared user balance
 */
data class BalanceTransfer(
    val id: String = "",
    val fromAdminId: String = "",
    val fromAdminEmail: String = "",
    val amount: Double = 0.0,
    val transferType: BalanceTransferType = BalanceTransferType.ADD,
    val notes: String = "",
    val createdAt: Timestamp? = null
)

/**
 * Type of balance transfer
 */
enum class BalanceTransferType {
    ADD,        // Admin adds balance to user
    DEDUCT      // Admin deducts balance from user
}

/**
 * Represents a user's cash out transaction using the shared balance
 */
data class UserCashOutTransaction(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val amount: Double = 0.0,
    val notes: String = "",
    val previousBalance: Double = 0.0,
    val newBalance: Double = 0.0,
    val createdAt: Timestamp? = null
)
