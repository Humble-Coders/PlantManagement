package com.humblecoders.plantmanagement.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.humblecoders.plantmanagement.data.Purchase
import com.humblecoders.plantmanagement.data.CashOut
import com.humblecoders.plantmanagement.data.PurchaseAllocation
import com.humblecoders.plantmanagement.repositories.PurchaseRepository
import com.humblecoders.plantmanagement.repositories.CashOutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class PurchaseState(
    val purchases: List<Purchase> = emptyList(),
    val isLoading: Boolean = false,
    val isAdding: Boolean = false,
    val isUpdating: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = "",
    val sortBy: PurchaseSortField = PurchaseSortField.DATE,
    val sortDirection: SortDirection = SortDirection.DESCENDING,
    val filterDateFrom: String = "",
    val filterDateTo: String = "",
    val cashOuts: List<CashOut> = emptyList(),
    val isCashingOut: Boolean = false
)

enum class PurchaseSortField {
    DATE,
    ENTITY,
    STATUS
}

class PurchaseViewModel(
    private val purchaseRepository: PurchaseRepository,
    private val cashOutRepository: CashOutRepository
) {
    var purchaseState by mutableStateOf(PurchaseState())
        private set

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    init {
        purchaseRepository.listenToPurchases { purchases ->
            purchaseState = purchaseState.copy(purchases = purchases)
        }
        
        cashOutRepository.listenToCashOutHistory { cashOuts ->
            purchaseState = purchaseState.copy(cashOuts = cashOuts)
        }
    }

    fun addPurchase(purchase: Purchase) {
        if (purchase.customerId.isBlank()) {
            purchaseState = purchaseState.copy(error = "Please select an entity")
            return
        }

        if (purchase.items.isEmpty()) {
            purchaseState = purchaseState.copy(error = "Please add at least one item")
            return
        }

        if (purchase.items.any { it.quantity <= 0 }) {
            purchaseState = purchaseState.copy(error = "All item quantities must be greater than 0")
            return
        }

        if (purchase.items.any { it.pricePerUnit <= 0 }) {
            purchaseState = purchaseState.copy(error = "All item prices must be greater than 0")
            return
        }

        viewModelScope.launch {
            purchaseState = purchaseState.copy(isAdding = true, isLoading = true, error = null)

            val result = purchaseRepository.addPurchase(purchase)

            purchaseState = if (result.isSuccess) {
                purchaseState.copy(
                    isAdding = false,
                    isLoading = false,
                    successMessage = "Purchase logged successfully!",
                    error = null
                )
            } else {
                purchaseState.copy(
                    isAdding = false,
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to log purchase"
                )
            }
        }
    }

    fun updatePurchase(purchaseId: String, purchase: Purchase) {
        if (purchase.items.isEmpty()) {
            purchaseState = purchaseState.copy(error = "Please add at least one item")
            return
        }

        if (purchase.items.any { it.quantity <= 0 }) {
            purchaseState = purchaseState.copy(error = "All item quantities must be greater than 0")
            return
        }

        if (purchase.items.any { it.pricePerUnit <= 0 }) {
            purchaseState = purchaseState.copy(error = "All item prices must be greater than 0")
            return
        }

        viewModelScope.launch {
            purchaseState = purchaseState.copy(isUpdating = true, isLoading = true, error = null)

            val result = purchaseRepository.updatePurchase(purchaseId, purchase)

            purchaseState = if (result.isSuccess) {
                purchaseState.copy(
                    isUpdating = false,
                    isLoading = false,
                    successMessage = "Purchase updated successfully!",
                    error = null
                )
            } else {
                purchaseState.copy(
                    isUpdating = false,
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to update purchase"
                )
            }
        }
    }

    fun deletePurchase(purchaseId: String) {
        viewModelScope.launch {
            purchaseState = purchaseState.copy(isDeleting = true, isLoading = true, error = null)

            val result = purchaseRepository.deletePurchase(purchaseId)

            purchaseState = if (result.isSuccess) {
                purchaseState.copy(
                    isDeleting = false,
                    isLoading = false,
                    successMessage = "Purchase deleted successfully!",
                    error = null
                )
            } else {
                purchaseState.copy(
                    isDeleting = false,
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to delete purchase"
                )
            }
        }
    }

    fun reversePurchase(purchaseId: String, reason: String) {
        viewModelScope.launch {
            purchaseState = purchaseState.copy(isLoading = true, error = null)

            val result = purchaseRepository.reversePurchase(purchaseId, reason)

            purchaseState = if (result.isSuccess) {
                purchaseState.copy(
                    isLoading = false,
                    successMessage = "Purchase reversed successfully!",
                    error = null
                )
            } else {
                purchaseState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to reverse purchase"
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        purchaseState = purchaseState.copy(searchQuery = query)
    }

    fun updateSortBy(sortBy: PurchaseSortField) {
        purchaseState = purchaseState.copy(sortBy = sortBy)
    }

    fun toggleSortDirection() {
        purchaseState = purchaseState.copy(
            sortDirection = if (purchaseState.sortDirection == SortDirection.ASCENDING) {
                SortDirection.DESCENDING
            } else {
                SortDirection.ASCENDING
            }
        )
    }

    fun updateDateFilter(from: String, to: String) {
        purchaseState = purchaseState.copy(
            filterDateFrom = from,
            filterDateTo = to
        )
    }

    fun getFilteredAndSortedPurchases(): List<Purchase> {
        val filtered = purchaseState.purchases.filter { purchase ->
            val query = purchaseState.searchQuery.lowercase()
            val textMatch = purchase.firmName.lowercase().contains(query) ||
                    purchase.purchaseDate.lowercase().contains(query) ||
                    purchase.items.any { it.itemName.lowercase().contains(query) }

            val dateMatch = if (purchaseState.filterDateFrom.isNotBlank() || purchaseState.filterDateTo.isNotBlank()) {
                val purchaseDate = try {
                    LocalDate.parse(purchase.purchaseDate, DateTimeFormatter.ISO_LOCAL_DATE)
                } catch (e: Exception) {
                    null
                }

                val fromDate = if (purchaseState.filterDateFrom.isNotBlank()) {
                    try {
                        LocalDate.parse(purchaseState.filterDateFrom, DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (e: Exception) {
                        LocalDate.MIN
                    }
                } else {
                    LocalDate.MIN
                }

                val toDate = if (purchaseState.filterDateTo.isNotBlank()) {
                    try {
                        LocalDate.parse(purchaseState.filterDateTo, DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (e: Exception) {
                        LocalDate.MAX
                    }
                } else {
                    LocalDate.MAX
                }

                purchaseDate != null && !purchaseDate.isBefore(fromDate) && !purchaseDate.isAfter(toDate)
            } else {
                true
            }

            textMatch && dateMatch
        }

        val sorted = when (purchaseState.sortBy) {
            PurchaseSortField.DATE -> {
                if (purchaseState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.purchaseDate }
                } else {
                    filtered.sortedByDescending { it.purchaseDate }
                }
            }
            PurchaseSortField.ENTITY -> {
                if (purchaseState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.firmName }
                } else {
                    filtered.sortedByDescending { it.firmName }
                }
            }
            PurchaseSortField.STATUS -> {
                if (purchaseState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.paymentStatus.name }
                } else {
                    filtered.sortedByDescending { it.paymentStatus.name }
                }
            }
        }

        return sorted
    }

    /**
     * Manually refresh purchases data
     */
    fun refreshPurchases() {
        viewModelScope.launch {
            purchaseState = purchaseState.copy(isLoading = true, error = null)
            
            val result = purchaseRepository.refreshPurchases()
            
            purchaseState = if (result.isSuccess) {
                purchaseState.copy(
                    isLoading = false,
                    purchases = result.getOrNull() ?: emptyList(),
                    error = null
                )
            } else {
                purchaseState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to refresh purchases"
                )
            }
        }
    }

    /**
     * Get purchases by customer ID
     */
    fun getPurchasesByCustomerId(customerId: String) {
        viewModelScope.launch {
            purchaseState = purchaseState.copy(isLoading = true, error = null)
            
            val result = purchaseRepository.getPurchasesByCustomerId(customerId)
            
            purchaseState = if (result.isSuccess) {
                purchaseState.copy(
                    isLoading = false,
                    purchases = result.getOrNull() ?: emptyList(),
                    error = null
                )
            } else {
                purchaseState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to load purchases"
                )
            }
        }
    }

    fun clearMessages() {
        purchaseState = purchaseState.copy(error = null, successMessage = null)
    }
    
    /**
     * Calculate automatic cash out allocations for a specific customer
     */
    suspend fun calculateCashOutAllocations(cashOutAmount: Double, customerId: String): List<PurchaseAllocation> {
        val pendingPurchases = cashOutRepository.getPendingPurchases(customerId)
        val allocations = mutableListOf<PurchaseAllocation>()
        var remainingAmount = cashOutAmount
        
        for (purchase in pendingPurchases) {
            if (remainingAmount <= 0) break
            
            val pendingAmount = purchase.grandTotal - purchase.amountPaid
            val allocationAmount = minOf(remainingAmount, pendingAmount)
            
            val newAmountPaid = purchase.amountPaid + allocationAmount
            val newPaymentStatus = when {
                newAmountPaid >= purchase.grandTotal -> com.humblecoders.plantmanagement.data.PaymentStatus.PAID
                newAmountPaid > 0 -> com.humblecoders.plantmanagement.data.PaymentStatus.PARTIALLY_PAID
                else -> com.humblecoders.plantmanagement.data.PaymentStatus.PENDING
            }
            
            allocations.add(
                PurchaseAllocation(
                    purchaseId = purchase.id,
                    firmName = purchase.firmName,
                    purchaseDate = purchase.purchaseDate,
                    customerId = purchase.customerId,
                    grandTotal = purchase.grandTotal,
                    allocatedAmount = allocationAmount,
                    previousAmountPaid = purchase.amountPaid,
                    newAmountPaid = newAmountPaid,
                    previousPaymentStatus = purchase.paymentStatus,
                    newPaymentStatus = newPaymentStatus
                )
            )
            
            remainingAmount -= allocationAmount
        }
        
        return allocations
    }
    
    /**
     * Process cash out transaction
     */
    fun processCashOut(
        totalAmount: Double,
        allocations: List<PurchaseAllocation>,
        notes: String = ""
    ) {
        if (totalAmount <= 0) {
            purchaseState = purchaseState.copy(error = "Cash out amount must be greater than 0")
            return
        }
        
        if (allocations.isEmpty()) {
            purchaseState = purchaseState.copy(error = "No purchases to allocate cash out")
            return
        }
        
        viewModelScope.launch {
            purchaseState = purchaseState.copy(isCashingOut = true, isLoading = true, error = null)

            val result = cashOutRepository.processCashOut(totalAmount, allocations, notes)

            purchaseState = if (result.isSuccess) {
                purchaseState.copy(
                    isCashingOut = false,
                    isLoading = false,
                    successMessage = "Cash out processed successfully!",
                    error = null
                )
            } else {
                purchaseState.copy(
                    isCashingOut = false,
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to process cash out"
                )
            }
        }
    }
}