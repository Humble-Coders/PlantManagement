// composeApp/src/jvmMain/kotlin/com/humblecoders/plantmanagement/data/Purchase.kt
package com.humblecoders.plantmanagement.data

import com.google.cloud.Timestamp

data class Purchase(
    val id: String = "",
    val userId: String = "",
    val customerId: String = "",
    val firmName: String = "",
    val purchaseDate: String = "",
    val items: List<PurchaseItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val gstRate: Double = 0.0,
    val gstAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val amountPaid: Double = 0.0,
    val notes: String = "",
    val imageUrl: String = "",
    val status: TransactionStatus = TransactionStatus.APPROVED,
    val reversedAt: Timestamp? = null,
    val reversalReason: String = "",
    val createdAt: Timestamp? = null
)

data class PurchaseItem(
    val inventoryItemId: String = "",
    val itemName: String = "",
    val quantity: Double = 0.0,
    val unit: String = "kg",
    val pricePerUnit: Double = 0.0,
    val totalPrice: Double = 0.0
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