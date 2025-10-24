package com.humblecoders.plantmanagement.data

import com.google.cloud.Timestamp

/**
 * Represents a pending bill (draft sale) that can be converted to actual sales
 */
data class PendingBill(
    val id: String = "",
    val userId: String = "",
    val customerId: String = "",
    val firmName: String = "",
    val billDate: String = "",
    val billNumber: String = "",
    val portalBatchNumber: String = "",
    val quantityKg: Double = 0.0,
    val numberOfBags: Int = 0,
    val deductFromInventory: Boolean = true,
    val originalRatePerKg: Double = 0.0,
    val portalAmount: Double = 0.0,
    val gstAmount: Double = 0.0,
    val totalPortalAmount: Double = 0.0,
    val discountType: DiscountType = DiscountType.NONE,
    val discountedRatePerKg: Double = 0.0,
    val extraQuantityKg: Double = 0.0,
    val revenueAmount: Double = 0.0,
    val totalRevenueAmount: Double = 0.0,
    val differenceAmount: Double = 0.0,
    val portalAmountPaid: Double = 0.0,
    val differenceAmountPaid: Double = 0.0,
    val truckNumber: String = "",
    val fareAmount: Double = 0.0,
    val farePaidBy: FarePaidBy = FarePaidBy.COMPANY,
    val notes: String = "",
    val imageUrls: List<String> = emptyList(),
    val status: PendingBillStatus = PendingBillStatus.PENDING_BILLED,
    val clearedQuantity: Double = 0.0, // Total quantity cleared so far
    val createdAt: Timestamp? = null
)

enum class PendingBillStatus {
    PENDING_BILLED,
    BILLED
}

/**
 * Represents a transaction history entry for pending bills
 */
data class PendingBillTransaction(
    val id: String = "",
    val pendingBillId: String = "",
    val userId: String = "",
    val transactionType: PendingBillTransactionType = PendingBillTransactionType.CREATED,
    val quantityCleared: Double = 0.0,
    val clearedBy: String = "", // User who cleared the bill
    val notes: String = "",
    val createdAt: Timestamp? = null
)

enum class PendingBillTransactionType {
    CREATED,
    PARTIALLY_CLEARED,
    FULLY_CLEARED
}

/**
 * Represents customer details when clearing a pending bill
 */
data class PendingBillClearanceRecord(
    val id: String = "",
    val pendingBillId: String = "",
    val customerName: String = "",
    val quantityCleared: Double = 0.0,
    val clearanceDate: String = "",
    val userId: String = "",
    val createdAt: Timestamp? = null
)
