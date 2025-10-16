package com.humblecoders.plantmanagement.data

import com.google.cloud.Timestamp

/**
 * Represents a single cash out transaction
 */
data class CashOut(
    val id: String = "",
    val userId: String = "",
    val totalAmount: Double = 0.0,
    val purchaseAllocations: List<PurchaseAllocation> = emptyList(),
    val notes: String = "",
    val createdAt: Timestamp? = null
)

/**
 * Represents how much of the cash out amount was allocated to a specific purchase
 */
data class PurchaseAllocation(
    val purchaseId: String = "",
    val firmName: String = "",
    val purchaseDate: String = "",
    val customerId: String = "",
    val grandTotal: Double = 0.0,
    val allocatedAmount: Double = 0.0,
    val previousAmountPaid: Double = 0.0,
    val newAmountPaid: Double = 0.0,
    val previousPaymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val newPaymentStatus: PaymentStatus = PaymentStatus.PENDING
)

