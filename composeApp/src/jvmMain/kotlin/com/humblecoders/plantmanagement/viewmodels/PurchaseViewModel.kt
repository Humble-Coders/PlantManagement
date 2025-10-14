package com.humblecoders.plantmanagement.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.humblecoders.plantmanagement.data.Purchase
import com.humblecoders.plantmanagement.repositories.PurchaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class PurchaseState(
    val purchases: List<Purchase> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = "",
    val sortBy: PurchaseSortField = PurchaseSortField.DATE,
    val sortDirection: SortDirection = SortDirection.DESCENDING,
    val filterDateFrom: String = "",
    val filterDateTo: String = ""
)

enum class PurchaseSortField {
    DATE,
    ENTITY,
    STATUS
}

class PurchaseViewModel(
    private val purchaseRepository: PurchaseRepository
) {
    var purchaseState by mutableStateOf(PurchaseState())
        private set

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    init {
        purchaseRepository.listenToPurchases { purchases ->
            purchaseState = purchaseState.copy(purchases = purchases)
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
            purchaseState = purchaseState.copy(isLoading = true, error = null)

            val result = purchaseRepository.addPurchase(purchase)

            purchaseState = if (result.isSuccess) {
                purchaseState.copy(
                    isLoading = false,
                    successMessage = "Purchase logged successfully!",
                    error = null
                )
            } else {
                purchaseState.copy(
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
            purchaseState = purchaseState.copy(isLoading = true, error = null)

            val result = purchaseRepository.updatePurchase(purchaseId, purchase)

            purchaseState = if (result.isSuccess) {
                purchaseState.copy(
                    isLoading = false,
                    successMessage = "Purchase updated successfully!",
                    error = null
                )
            } else {
                purchaseState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to update purchase"
                )
            }
        }
    }

    fun deletePurchase(purchaseId: String) {
        viewModelScope.launch {
            purchaseState = purchaseState.copy(isLoading = true, error = null)

            val result = purchaseRepository.deletePurchase(purchaseId)

            purchaseState = if (result.isSuccess) {
                purchaseState.copy(
                    isLoading = false,
                    successMessage = "Purchase deleted successfully!",
                    error = null
                )
            } else {
                purchaseState.copy(
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

    fun clearMessages() {
        purchaseState = purchaseState.copy(error = null, successMessage = null)
    }
}