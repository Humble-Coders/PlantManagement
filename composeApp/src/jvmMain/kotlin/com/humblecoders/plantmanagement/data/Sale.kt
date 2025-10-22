package com.humblecoders.plantmanagement.data

import com.google.cloud.Timestamp

/**
 * Represents a single sale transaction
 */
data class Sale(
    val id: String = "",
    val userId: String = "",
    val customerId: String = "",
    val firmName: String = "",
    val saleDate: String = "",
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
    val saleStatus: SaleStatus = SaleStatus.PENDING,
    val differenceAmountPaid: Double = 0.0,
    val differenceStatus: DifferenceStatus = DifferenceStatus.PENDING,
    val clearedInventory: Double = 0.0,
    val truckNumber: String = "",
    val fareAmount: Double = 0.0,
    val farePaidBy: FarePaidBy = FarePaidBy.COMPANY,
    val notes: String = "",
    val imageUrls: List<String> = emptyList(),
    val status: TransactionStatus = TransactionStatus.APPROVED,
    val reversedAt: Timestamp? = null,
    val reversalReason: String = "",
    val createdAt: Timestamp? = null
)

enum class DiscountType {
    NONE,
    DISCOUNT_PREMIUM,
    INDIRECT_DISCOUNT
}

enum class SaleStatus {
    PENDING,
    PARTIALLY_PAID,
    PAID
}

enum class DifferenceStatus {
    PENDING,
    PARTIALLY_PAID,
    PAID
}

enum class FarePaidBy {
    COMPANY,
    CUSTOMER
}

// Add these data classes to Sale.kt

/**
 * Represents a cash in transaction for revenue
 */
data class CashInRevenue(
    val id: String = "",
    val userId: String = "",
    val totalAmount: Double = 0.0,
    val saleAllocations: List<SaleAllocation> = emptyList(),
    val notes: String = "",
    val createdAt: Timestamp? = null
)

/**
 * Represents a cash in/out transaction for difference amount
 */
data class CashInOutDifference(
    val id: String = "",
    val userId: String = "",
    val totalAmount: Double = 0.0,
    val transactionType: DifferenceTransactionType = DifferenceTransactionType.CASH_IN,
    val saleAllocations: List<SaleAllocation> = emptyList(),
    val notes: String = "",
    val createdAt: Timestamp? = null
)

/**
 * Represents how much of the cash amount was allocated to a specific sale
 */
data class SaleAllocation(
    val saleId: String = "",
    val firmName: String = "",
    val saleDate: String = "",
    val customerId: String = "",
    val billNumber: String = "",
    val totalRevenueAmount: Double = 0.0,
    val totalPortalAmount: Double = 0.0,
    val differenceAmount: Double = 0.0,
    val allocatedAmount: Double = 0.0,
    val previousAmountPaid: Double = 0.0,
    val newAmountPaid: Double = 0.0,
    val previousPaymentStatus: SaleStatus = SaleStatus.PENDING,
    val newPaymentStatus: SaleStatus = SaleStatus.PENDING,
    val allocationType: AllocationType = AllocationType.REVENUE
)

enum class DifferenceTransactionType {
    CASH_IN,
    CASH_OUT
}

enum class AllocationType {
    REVENUE,
    DIFFERENCE
}