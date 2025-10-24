package com.humblecoders.plantmanagement.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.humblecoders.plantmanagement.data.*
import com.humblecoders.plantmanagement.repositories.CashInRepository
import com.humblecoders.plantmanagement.repositories.SaleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class SaleState(
    val sales: List<Sale> = emptyList(),
    val isLoading: Boolean = false,
    val isAdding: Boolean = false,
    val isUpdating: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = "",
    val sortBy: SaleSortField = SaleSortField.DATE,
    val sortDirection: SortDirection = SortDirection.DESCENDING,
    val filterDateFrom: String = "",
    val filterDateTo: String = "",
    val filterSaleStatus: SaleStatus? = null,
    val cashInRevenueHistory: List<CashInRevenue> = emptyList(),
    val cashInOutDifferenceHistory: List<CashInOutDifference> = emptyList(),
    val isCashingIn: Boolean = false
)

enum class SaleSortField {
    DATE,
    ENTITY,
    BILL_NUMBER,
    STATUS
}

//enum class SortDirection {
//    ASCENDING,
//    DESCENDING
//}

class SaleViewModel(
    private val saleRepository: SaleRepository,
    private val cashInRepository: CashInRepository,
    private val storageService: com.humblecoders.plantmanagement.services.FirebaseStorageService
) {
    var saleState by mutableStateOf(SaleState())
        private set

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    init {
        saleRepository.listenToSales { sales ->
            saleState = saleState.copy(sales = sales)
        }

        // Add listeners for cash in history
        cashInRepository.listenToCashInRevenueHistory { history ->
            saleState = saleState.copy(cashInRevenueHistory = history)
        }

        cashInRepository.listenToCashInOutDifferenceHistory { history ->
            saleState = saleState.copy(cashInOutDifferenceHistory = history)
        }
    }

    /**
     * Calculate all sale amounts based on inputs
     */
    fun calculateSaleAmounts(
        quantityKg: Double,
        originalRatePerKg: Double,
        discountType: DiscountType,
        discountedRatePerKg: Double,
        extraQuantityKg: Double
    ): SaleCalculation {
        // Portal Amount = Quantity * Original Rate
        val portalAmount = quantityKg * originalRatePerKg

        // GST is always 5% on Portal Amount
        val gstAmount = portalAmount * 0.05

        // Total Portal Amount = Portal Amount + GST
        val totalPortalAmount = portalAmount + gstAmount

        // Revenue Amount calculation based on discount type
        val revenueAmount = when (discountType) {
            DiscountType.DISCOUNT_PREMIUM -> quantityKg * discountedRatePerKg
            DiscountType.INDIRECT_DISCOUNT -> portalAmount
            DiscountType.NONE -> portalAmount
        }

        // Total Revenue Amount = Revenue Amount + GST (GST stays same)
        val totalRevenueAmount = revenueAmount + gstAmount

        // Difference Amount = Revenue Amount - Portal Amount
        // Positive = Customer pays us, Negative = We pay customer
        val differenceAmount = revenueAmount - portalAmount

        return SaleCalculation(
            portalAmount = portalAmount,
            gstAmount = gstAmount,
            totalPortalAmount = totalPortalAmount,
            revenueAmount = revenueAmount,
            totalRevenueAmount = totalRevenueAmount,
            differenceAmount = differenceAmount
        )
    }

    /**
     * Add a new sale
     */
    fun addSale(sale: Sale, skipInventoryDeduction: Boolean = false) {
        // Validations
        if (sale.customerId.isBlank()) {
            saleState = saleState.copy(error = "Please select a customer")
            return
        }

        if (sale.billNumber.isBlank()) {
            saleState = saleState.copy(error = "Bill number is required")
            return
        }

        if (sale.portalBatchNumber.isBlank()) {
            saleState = saleState.copy(error = "Portal batch number is required")
            return
        }

        if (sale.quantityKg <= 0) {
            saleState = saleState.copy(error = "Quantity must be greater than 0")
            return
        }

        if (sale.originalRatePerKg <= 0) {
            saleState = saleState.copy(error = "Original rate must be greater than 0")
            return
        }

        if (sale.discountType == DiscountType.DISCOUNT_PREMIUM && sale.discountedRatePerKg <= 0) {
            saleState = saleState.copy(error = "Offered rate must be greater than 0")
            return
        }

        if (sale.discountType == DiscountType.INDIRECT_DISCOUNT && sale.extraQuantityKg <= 0) {
            saleState = saleState.copy(error = "Extra quantity must be greater than 0")
            return
        }

        viewModelScope.launch {
            saleState = saleState.copy(isAdding = true, isLoading = true, error = null)

            val result = saleRepository.addSale(sale, skipInventoryDeduction)

            saleState = if (result.isSuccess) {
                saleState.copy(
                    isAdding = false,
                    isLoading = false,
                    successMessage = "Sale added successfully!",
                    error = null
                )
            } else {
                saleState.copy(
                    isAdding = false,
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to add sale"
                )
            }
        }
    }

    /**
     * Update an existing sale
     */
    fun updateSale(saleId: String, sale: Sale) {
        if (sale.billNumber.isBlank()) {
            saleState = saleState.copy(error = "Bill number is required")
            return
        }

        if (sale.portalBatchNumber.isBlank()) {
            saleState = saleState.copy(error = "Portal batch number is required")
            return
        }

        if (sale.quantityKg <= 0) {
            saleState = saleState.copy(error = "Quantity must be greater than 0")
            return
        }

        if (sale.originalRatePerKg <= 0) {
            saleState = saleState.copy(error = "Original rate must be greater than 0")
            return
        }

        viewModelScope.launch {
            saleState = saleState.copy(isUpdating = true, isLoading = true, error = null)

            val result = saleRepository.updateSale(saleId, sale)

            saleState = if (result.isSuccess) {
                saleState.copy(
                    isUpdating = false,
                    isLoading = false,
                    successMessage = "Sale updated successfully!",
                    error = null
                )
            } else {
                saleState.copy(
                    isUpdating = false,
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to update sale"
                )
            }
        }
    }

    /**
     * Delete a sale
     */
    fun deleteSale(saleId: String) {
        viewModelScope.launch {
            saleState = saleState.copy(isDeleting = true, isLoading = true, error = null)

            val result = saleRepository.deleteSale(saleId)

            saleState = if (result.isSuccess) {
                saleState.copy(
                    isDeleting = false,
                    isLoading = false,
                    successMessage = "Sale deleted successfully!",
                    error = null
                )
            } else {
                saleState.copy(
                    isDeleting = false,
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to delete sale"
                )
            }
        }
    }

    /**
     * Reverse a sale
     */
    fun reverseSale(saleId: String, reason: String) {
        viewModelScope.launch {
            saleState = saleState.copy(isLoading = true, error = null)

            val result = saleRepository.reverseSale(saleId, reason)

            saleState = if (result.isSuccess) {
                saleState.copy(
                    isLoading = false,
                    successMessage = "Sale reversed successfully!",
                    error = null
                )
            } else {
                saleState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to reverse sale"
                )
            }
        }
    }

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        saleState = saleState.copy(searchQuery = query)
    }

    /**
     * Update sort field
     */
    fun updateSortBy(sortBy: SaleSortField) {
        saleState = saleState.copy(sortBy = sortBy)
    }

    /**
     * Toggle sort direction
     */
    fun toggleSortDirection() {
        saleState = saleState.copy(
            sortDirection = if (saleState.sortDirection == SortDirection.ASCENDING) {
                SortDirection.DESCENDING
            } else {
                SortDirection.ASCENDING
            }
        )
    }

    /**
     * Update date filter
     */
    fun updateDateFilter(from: String, to: String) {
        saleState = saleState.copy(
            filterDateFrom = from,
            filterDateTo = to
        )
    }

    /**
     * Update sale status filter
     */
    fun updateSaleStatusFilter(status: SaleStatus?) {
        saleState = saleState.copy(filterSaleStatus = status)
    }

    /**
     * Get filtered and sorted sales
     */
    fun getFilteredAndSortedSales(): List<Sale> {
        val filtered = saleState.sales.filter { sale ->
            val query = saleState.searchQuery.lowercase()
            val textMatch = sale.firmName.lowercase().contains(query) ||
                    sale.billNumber.lowercase().contains(query) ||
                    sale.portalBatchNumber.lowercase().contains(query) ||
                    sale.saleDate.lowercase().contains(query) ||
                    sale.customerCity.lowercase().contains(query)

            val dateMatch = if (saleState.filterDateFrom.isNotBlank() || saleState.filterDateTo.isNotBlank()) {
                val saleDate = try {
                    LocalDate.parse(sale.saleDate, DateTimeFormatter.ISO_LOCAL_DATE)
                } catch (e: Exception) {
                    null
                }

                val fromDate = if (saleState.filterDateFrom.isNotBlank()) {
                    try {
                        LocalDate.parse(saleState.filterDateFrom, DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (e: Exception) {
                        LocalDate.MIN
                    }
                } else {
                    LocalDate.MIN
                }

                val toDate = if (saleState.filterDateTo.isNotBlank()) {
                    try {
                        LocalDate.parse(saleState.filterDateTo, DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (e: Exception) {
                        LocalDate.MAX
                    }
                } else {
                    LocalDate.MAX
                }

                saleDate != null && !saleDate.isBefore(fromDate) && !saleDate.isAfter(toDate)
            } else {
                true
            }

            val saleStatusMatch = saleState.filterSaleStatus?.let { filter ->
                sale.saleStatus == filter
            } ?: true

            textMatch && dateMatch && saleStatusMatch
        }

        val sorted = when (saleState.sortBy) {
            SaleSortField.DATE -> {
                if (saleState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.saleDate }
                } else {
                    filtered.sortedByDescending { it.saleDate }
                }
            }
            SaleSortField.ENTITY -> {
                if (saleState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.firmName }
                } else {
                    filtered.sortedByDescending { it.firmName }
                }
            }
            SaleSortField.BILL_NUMBER -> {
                if (saleState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.billNumber }
                } else {
                    filtered.sortedByDescending { it.billNumber }
                }
            }
            SaleSortField.STATUS -> {
                if (saleState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.saleStatus.name }
                } else {
                    filtered.sortedByDescending { it.saleStatus.name }
                }
            }
        }

        return sorted
    }

    /**
     * Manually refresh sales data
     */
    fun refreshSales() {
        viewModelScope.launch {
            saleState = saleState.copy(isLoading = true, error = null)
            
            val result = saleRepository.refreshSales()
            
            saleState = if (result.isSuccess) {
                saleState.copy(
                    isLoading = false,
                    sales = result.getOrNull() ?: emptyList(),
                    error = null
                )
            } else {
                saleState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to refresh sales"
                )
            }
        }
    }

    /**
     * Get sales by customer ID
     */
    fun getSalesByCustomerId(customerId: String) {
        viewModelScope.launch {
            saleState = saleState.copy(isLoading = true, error = null)
            
            val result = saleRepository.getSalesByCustomerId(customerId)
            
            saleState = if (result.isSuccess) {
                saleState.copy(
                    isLoading = false,
                    sales = result.getOrNull() ?: emptyList(),
                    error = null
                )
            } else {
                saleState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to load sales"
                )
            }
        }
    }

    /**
     * Clear messages
     */
    fun clearMessages() {
        saleState = saleState.copy(error = null, successMessage = null)
    }


    /**
     * Calculate cash in allocations for revenue
     */
    suspend fun calculateCashInRevenueAllocations(cashInAmount: Double, customerId: String): List<SaleAllocation> {
        val pendingSales = cashInRepository.getPendingSalesForRevenue(customerId)
        val allocations = mutableListOf<SaleAllocation>()
        var remainingAmount = cashInAmount

        for (sale in pendingSales) {
            if (remainingAmount <= 0) break

            val pendingAmount = sale.totalPortalAmount - sale.portalAmountPaid
            val allocationAmount = minOf(remainingAmount, pendingAmount)

            val newAmountPaid = sale.portalAmountPaid + allocationAmount
            val newPaymentStatus = when {
                newAmountPaid >= sale.totalPortalAmount -> SaleStatus.PAID
                newAmountPaid > 0 -> SaleStatus.PARTIALLY_PAID
                else -> SaleStatus.PENDING
            }

            allocations.add(
                SaleAllocation(
                    saleId = sale.id,
                    firmName = sale.firmName,
                    saleDate = sale.saleDate,
                    customerId = sale.customerId,
                    billNumber = sale.billNumber,
                    totalRevenueAmount = sale.totalRevenueAmount,
                    totalPortalAmount = sale.totalPortalAmount,
                    allocatedAmount = allocationAmount,
                    previousAmountPaid = sale.portalAmountPaid,
                    newAmountPaid = newAmountPaid,
                    previousPaymentStatus = sale.saleStatus,
                    newPaymentStatus = newPaymentStatus,
                    allocationType = AllocationType.REVENUE
                )
            )

            remainingAmount -= allocationAmount
        }

        return allocations
    }

    /**
     * Calculate cash in/out allocations for difference amount
     */
    suspend fun calculateCashInOutDifferenceAllocations(
        amount: Double,
        customerId: String,
        transactionType: DifferenceTransactionType
    ): List<SaleAllocation> {
        val sales = cashInRepository.getSalesWithPendingDifference(customerId, transactionType)
        val allocations = mutableListOf<SaleAllocation>()
        var remainingAmount = amount

        for (sale in sales) {
            if (remainingAmount <= 0) break

            val pendingDifference = kotlin.math.abs(sale.differenceAmount - sale.differenceAmountPaid)
            val allocationAmount = minOf(remainingAmount, pendingDifference)

            val newAmountPaid = sale.differenceAmountPaid + allocationAmount
            val newPaymentStatus = when {
                // If difference amount is 0, status should be PAID (nothing to pay)
                kotlin.math.abs(sale.differenceAmount) == 0.0 -> DifferenceStatus.PAID
                // For positive difference (customer pays us)
                sale.differenceAmount > 0 -> {
                    when {
                        newAmountPaid >= sale.differenceAmount -> DifferenceStatus.PAID
                        newAmountPaid > 0 -> DifferenceStatus.PARTIALLY_PAID
                        else -> DifferenceStatus.PENDING
                    }
                }
                // For negative difference (we pay customer)
                sale.differenceAmount < 0 -> {
                    when {
                        newAmountPaid >= kotlin.math.abs(sale.differenceAmount) -> DifferenceStatus.PAID
                        newAmountPaid > 0 -> DifferenceStatus.PARTIALLY_PAID
                        else -> DifferenceStatus.PENDING
                    }
                }
                else -> DifferenceStatus.PENDING
            }

            // CORRECT
            allocations.add(
                SaleAllocation(
                    saleId = sale.id,
                    firmName = sale.firmName,
                    saleDate = sale.saleDate,
                    customerId = sale.customerId,
                    billNumber = sale.billNumber,
                    totalRevenueAmount = sale.totalRevenueAmount,
                    totalPortalAmount = sale.totalPortalAmount,
                    differenceAmount = sale.differenceAmount,
                    allocatedAmount = allocationAmount,
                    previousAmountPaid = sale.differenceAmountPaid,
                    newAmountPaid = newAmountPaid,
                    previousPaymentStatus = SaleStatus.valueOf(sale.differenceStatus.name),  // Convert DifferenceStatus to SaleStatus
                    newPaymentStatus = SaleStatus.valueOf(newPaymentStatus.name),  // Convert DifferenceStatus to SaleStatus
                    allocationType = AllocationType.DIFFERENCE
                )
            )

            remainingAmount -= allocationAmount
        }

        return allocations
    }

    /**
     * Process cash in for revenue
     */
    fun processCashInRevenue(
        totalAmount: Double,
        allocations: List<SaleAllocation>,
        notes: String = ""
    ) {
        viewModelScope.launch {
            saleState = saleState.copy(isCashingIn = true, isLoading = true, error = null)

            val result = cashInRepository.processCashInRevenue(totalAmount, allocations, notes)

            saleState = if (result.isSuccess) {
                saleState.copy(
                    isCashingIn = false,
                    isLoading = false,
                    successMessage = "Cash in processed successfully!",
                    error = null
                )
            } else {
                saleState.copy(
                    isCashingIn = false,
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to process cash in"
                )
            }
        }
    }

    /**
     * Process cash in/out for difference
     */
    fun processCashInOutDifference(
        amount: Double,
        transactionType: DifferenceTransactionType,
        allocations: List<SaleAllocation>,
        notes: String = ""
    ) {
        viewModelScope.launch {
            saleState = saleState.copy(isCashingIn = true, isLoading = true, error = null)

            val result = cashInRepository.processCashInOutDifference(amount, transactionType, allocations, notes)

            saleState = if (result.isSuccess) {
                saleState.copy(
                    isCashingIn = false,
                    isLoading = false,
                    successMessage= "Cash ${transactionType.name.lowercase().replace("_", " ")} processed successfully!",
                    error = null
                )
            } else {
                saleState.copy(
                    isCashingIn = false,
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to process transaction"
                )
            }
        }
    }

    /**
     * Clear bill by adding quantity to cleared inventory
     */
    fun clearBill(saleId: String, quantityToClear: Double) {
        viewModelScope.launch {
            saleState = saleState.copy(isUpdating = true, isLoading = true, error = null)

            val result = saleRepository.clearBill(saleId, quantityToClear)

            saleState = if (result.isSuccess) {
                saleState.copy(
                    isUpdating = false,
                    isLoading = false,
                    successMessage = "Bill cleared successfully!",
                    error = null
                )
            } else {
                saleState.copy(
                    isUpdating = false,
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to clear bill"
                )
            }
        }
    }


}

/**
 * Data class to hold calculated sale amounts
 */
data class SaleCalculation(
    val portalAmount: Double,
    val gstAmount: Double,
    val totalPortalAmount: Double,
    val revenueAmount: Double,
    val totalRevenueAmount: Double,
    val differenceAmount: Double
)