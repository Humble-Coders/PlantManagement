package com.humblecoders.plantmanagement.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.humblecoders.plantmanagement.data.PendingBill
import com.humblecoders.plantmanagement.data.PendingBillStatus
import com.humblecoders.plantmanagement.data.PendingBillClearanceRecord
import com.humblecoders.plantmanagement.repositories.PendingBillRepository
import com.humblecoders.plantmanagement.viewmodels.SortDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class PendingBillState(
    val pendingBills: List<PendingBill> = emptyList(),
    val clearanceRecords: List<PendingBillClearanceRecord> = emptyList(),
    val isLoading: Boolean = false,
    val isAdding: Boolean = false,
    val isUpdating: Boolean = false,
    val isDeleting: Boolean = false,
    val isLoadingClearanceRecords: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = "",
    val sortBy: PendingBillSortField = PendingBillSortField.DATE,
    val sortDirection: SortDirection = SortDirection.DESCENDING,
    val filterDateFrom: String = "",
    val filterDateTo: String = "",
    val filterStatus: PendingBillStatus? = null,
    val showAdvancedFilters: Boolean = false
)

enum class PendingBillSortField {
    DATE,
    ENTITY,
    BILL_NUMBER,
    STATUS,
    PORTAL_AMOUNT,
    REVENUE_AMOUNT,
    DIFFERENCE_AMOUNT,
    QUANTITY,
    CREATED_AT
}

class PendingBillViewModel(
    private val pendingBillRepository: PendingBillRepository
) {
    var pendingBillState by mutableStateOf(PendingBillState())
        private set

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    init {
        loadPendingBills()
        loadClearanceRecords()
    }

    /**
     * Load all pending bills
     */
    fun loadPendingBills() {
        viewModelScope.launch {
            pendingBillState = pendingBillState.copy(isLoading = true, error = null)
            
            val result = pendingBillRepository.getAllPendingBills()
            
            pendingBillState = if (result.isSuccess) {
                pendingBillState.copy(
                    isLoading = false,
                    pendingBills = result.getOrNull() ?: emptyList()
                )
            } else {
                pendingBillState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to load pending bills"
                )
            }
        }
    }

    /**
     * Load all clearance records
     */
    fun loadClearanceRecords() {
        viewModelScope.launch {
            pendingBillState = pendingBillState.copy(isLoadingClearanceRecords = true, error = null)
            
            val result = pendingBillRepository.getAllClearanceRecords()
            
            pendingBillState = if (result.isSuccess) {
                pendingBillState.copy(
                    isLoadingClearanceRecords = false,
                    clearanceRecords = result.getOrNull() ?: emptyList()
                )
            } else {
                pendingBillState.copy(
                    isLoadingClearanceRecords = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to load clearance records"
                )
            }
        }
    }

    /**
     * Add a new pending bill
     */
    fun addPendingBill(pendingBill: PendingBill) {
        viewModelScope.launch {
            pendingBillState = pendingBillState.copy(isAdding = true, error = null)
            
            println("Adding pending bill: ${pendingBill.billNumber}")
            val result = pendingBillRepository.addPendingBill(pendingBill)
            
            pendingBillState = if (result.isSuccess) {
                println("Pending bill added successfully: ${result.getOrNull()}")
                loadPendingBills() // Reload to get updated list
                pendingBillState.copy(
                    isAdding = false,
                    successMessage = "Pending bill added successfully"
                )
            } else {
                println("Failed to add pending bill: ${result.exceptionOrNull()?.message}")
                pendingBillState.copy(
                    isAdding = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to add pending bill"
                )
            }
        }
    }

    /**
     * Clear a pending bill (partial or full)
     */
    fun clearBill(pendingBillId: String, clearedQuantity: Double, customerName: String) {
        viewModelScope.launch {
            pendingBillState = pendingBillState.copy(isUpdating = true, error = null)
            
            val result = pendingBillRepository.clearBill(pendingBillId, clearedQuantity, customerName)
            
            pendingBillState = if (result.isSuccess) {
                loadPendingBills() // Reload to get updated list
                loadClearanceRecords() // Load clearance records
                pendingBillState.copy(
                    isUpdating = false,
                    successMessage = "Bill cleared successfully"
                )
            } else {
                pendingBillState.copy(
                    isUpdating = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to clear bill"
                )
            }
        }
    }

    /**
     * Delete a pending bill
     */
    fun deletePendingBill(pendingBillId: String) {
        viewModelScope.launch {
            pendingBillState = pendingBillState.copy(isDeleting = true, error = null)
            
            val result = pendingBillRepository.deletePendingBill(pendingBillId)
            
            pendingBillState = if (result.isSuccess) {
                loadPendingBills() // Reload to get updated list
                pendingBillState.copy(
                    isDeleting = false,
                    successMessage = "Pending bill deleted successfully"
                )
            } else {
                pendingBillState.copy(
                    isDeleting = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to delete pending bill"
                )
            }
        }
    }

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        pendingBillState = pendingBillState.copy(searchQuery = query)
    }

    /**
     * Update sort field
     */
    fun updateSortField(field: PendingBillSortField) {
        pendingBillState = pendingBillState.copy(sortBy = field)
    }

    /**
     * Update sort direction
     */
    fun updateSortDirection(direction: SortDirection) {
        pendingBillState = pendingBillState.copy(sortDirection = direction)
    }

    /**
     * Update date filter
     */
    fun updateDateFilter(from: String, to: String) {
        pendingBillState = pendingBillState.copy(
            filterDateFrom = from,
            filterDateTo = to
        )
    }

    /**
     * Update status filter
     */
    fun updateStatusFilter(status: PendingBillStatus?) {
        pendingBillState = pendingBillState.copy(filterStatus = status)
    }

    /**
     * Toggle advanced filters visibility
     */
    fun toggleAdvancedFilters() {
        pendingBillState = pendingBillState.copy(
            showAdvancedFilters = !pendingBillState.showAdvancedFilters
        )
    }

    /**
     * Clear all filters
     */
    fun clearAllFilters() {
        pendingBillState = pendingBillState.copy(
            searchQuery = "",
            filterDateFrom = "",
            filterDateTo = "",
            filterStatus = null,
            sortBy = PendingBillSortField.DATE,
            sortDirection = SortDirection.DESCENDING
        )
    }

    /**
     * Get filtered and sorted pending bills
     */
    fun getFilteredAndSortedPendingBills(): List<PendingBill> {
        val filtered = pendingBillState.pendingBills.filter { pendingBill ->
            val query = pendingBillState.searchQuery.lowercase()
            val textMatch = pendingBill.firmName.lowercase().contains(query) ||
                    pendingBill.billNumber.lowercase().contains(query) ||
                    pendingBill.portalBatchNumber.lowercase().contains(query) ||
                    pendingBill.truckNumber.lowercase().contains(query) ||
                    pendingBill.notes.lowercase().contains(query)

            val dateMatch = if (pendingBillState.filterDateFrom.isNotBlank() && pendingBillState.filterDateTo.isNotBlank()) {
                try {
                    val billDate = java.time.LocalDate.parse(pendingBill.billDate)
                    val fromDate = java.time.LocalDate.parse(pendingBillState.filterDateFrom)
                    val toDate = java.time.LocalDate.parse(pendingBillState.filterDateTo)
                    billDate != null && !billDate.isBefore(fromDate) && !billDate.isAfter(toDate)
                } catch (e: Exception) {
                    true
                }
            } else {
                true
            }

            val statusMatch = pendingBillState.filterStatus?.let { filter ->
                pendingBill.status == filter
            } ?: true

            textMatch && dateMatch && statusMatch
        }

        val sorted = when (pendingBillState.sortBy) {
            PendingBillSortField.DATE -> {
                if (pendingBillState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.billDate }
                } else {
                    filtered.sortedByDescending { it.billDate }
                }
            }
            PendingBillSortField.ENTITY -> {
                if (pendingBillState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.firmName }
                } else {
                    filtered.sortedByDescending { it.firmName }
                }
            }
            PendingBillSortField.BILL_NUMBER -> {
                if (pendingBillState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.billNumber }
                } else {
                    filtered.sortedByDescending { it.billNumber }
                }
            }
            PendingBillSortField.STATUS -> {
                if (pendingBillState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.status.name }
                } else {
                    filtered.sortedByDescending { it.status.name }
                }
            }
            PendingBillSortField.PORTAL_AMOUNT -> {
                if (pendingBillState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.totalPortalAmount }
                } else {
                    filtered.sortedByDescending { it.totalPortalAmount }
                }
            }
            PendingBillSortField.REVENUE_AMOUNT -> {
                if (pendingBillState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.totalRevenueAmount }
                } else {
                    filtered.sortedByDescending { it.totalRevenueAmount }
                }
            }
            PendingBillSortField.DIFFERENCE_AMOUNT -> {
                if (pendingBillState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.differenceAmount }
                } else {
                    filtered.sortedByDescending { it.differenceAmount }
                }
            }
            PendingBillSortField.QUANTITY -> {
                if (pendingBillState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.quantityKg }
                } else {
                    filtered.sortedByDescending { it.quantityKg }
                }
            }
            PendingBillSortField.CREATED_AT -> {
                if (pendingBillState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.createdAt }
                } else {
                    filtered.sortedByDescending { it.createdAt }
                }
            }
        }

        return sorted
    }

    /**
     * Clear error message
     */
    fun clearError() {
        pendingBillState = pendingBillState.copy(error = null)
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        pendingBillState = pendingBillState.copy(successMessage = null)
    }
}
