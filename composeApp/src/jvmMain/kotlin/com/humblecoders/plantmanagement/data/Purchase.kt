package com.humblecoders.plantmanagement.data

import com.google.cloud.Timestamp

data class Purchase(
    val id: String = "",
    val userId: String = "",
    val customerId: String = "",
    val firmName: String = "",
    val purchaseDate: String = "",
    val itemName: String = "RICE",
    val quantity: Double = 0.0,
    val unit: String = "kg",
    val pricePerUnit: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val amountPaid: Double = 0.0,
    val notes: String = "",
    val status: TransactionStatus = TransactionStatus.APPROVED,
    val reversedAt: Timestamp? = null,
    val reversalReason: String = "",
    val createdAt: Timestamp? = null
)

enum class PaymentStatus {
    PENDING,
    PAID,
    PARTIALLY_PAID
}

enum class TransactionStatus {
    APPROVED,
    REVERSED
}

enum class ItemName {
    RICE,
    RICE1,
    PREMIX,
    PACKAGING,
    OTHERS,
    FORTIFIED_RICE
}