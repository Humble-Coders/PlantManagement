package com.humblecoders.plantmanagement.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.humblecoders.plantmanagement.data.ProductionRecord
import com.humblecoders.plantmanagement.data.ProductionInput
import com.humblecoders.plantmanagement.data.WasteTracking
import com.humblecoders.plantmanagement.data.InventoryItem
import com.humblecoders.plantmanagement.repositories.ProductionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class ProductionState(
    val productionRecords: List<ProductionRecord> = emptyList(),
    val isLoading: Boolean = false,
    val isAdding: Boolean = false,
    val isUpdating: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = "",
    val sortBy: ProductionSortField = ProductionSortField.DATE,
    val sortDirection: SortDirection = SortDirection.DESCENDING,
    val filterDateFrom: String = "",
    val filterDateTo: String = "",
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val recordToEdit: ProductionRecord? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val recordToDelete: ProductionRecord? = null
)

enum class ProductionSortField {
    DATE,
    BATCH_NUMBER,
    QUANTITY
}

class ProductionViewModel(
    private val productionRepository: ProductionRepository,
    private val inventoryViewModel: com.humblecoders.plantmanagement.viewmodels.InventoryViewModel
) {
    var productionState by mutableStateOf(ProductionState())
        private set

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    init {
        // Start listening to production records
        viewModelScope.launch {
            productionRepository.listenToProductionRecords().collect { records ->
                productionState = productionState.copy(productionRecords = records)
            }
        }
    }

    /**
     * Add a new production record
     */
    fun updateSortBy(sortBy: ProductionSortField) {
        productionState = productionState.copy(sortBy = sortBy)
    }

    fun toggleSortDirection() {
        productionState = productionState.copy(
            sortDirection = if (productionState.sortDirection == SortDirection.ASCENDING) {
                SortDirection.DESCENDING
            } else {
                SortDirection.ASCENDING
            }
        )
    }

    fun updateDateFilter(from: String, to: String) {
        productionState = productionState.copy(
            filterDateFrom = from,
            filterDateTo = to
        )
    }

    fun getFilteredAndSortedRecords(): List<ProductionRecord> {
        val query = productionState.searchQuery.lowercase()

        val filtered = productionState.productionRecords.filter { record ->
            val textMatch = record.batchNumber.lowercase().contains(query) ||
                    record.supervisorName.lowercase().contains(query) ||
                    record.notes.lowercase().contains(query)

            val dateMatch = if (productionState.filterDateFrom.isNotBlank() || productionState.filterDateTo.isNotBlank()) {
                val productionDate = record.productionDate?.let {
                    java.time.LocalDate.ofInstant(
                        java.time.Instant.ofEpochMilli(it.seconds * 1000),
                        java.time.ZoneId.systemDefault()
                    )
                }

                val fromDate = if (productionState.filterDateFrom.isNotBlank()) {
                    try {
                        java.time.LocalDate.parse(productionState.filterDateFrom, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (e: Exception) {
                        java.time.LocalDate.MIN
                    }
                } else {
                    java.time.LocalDate.MIN
                }

                val toDate = if (productionState.filterDateTo.isNotBlank()) {
                    try {
                        java.time.LocalDate.parse(productionState.filterDateTo, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (e: Exception) {
                        java.time.LocalDate.MAX
                    }
                } else {
                    java.time.LocalDate.MAX
                }

                productionDate != null && !productionDate.isBefore(fromDate) && !productionDate.isAfter(toDate)
            } else {
                true
            }

            textMatch && dateMatch
        }

        return when (productionState.sortBy) {
            ProductionSortField.DATE -> {
                if (productionState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.productionDate?.seconds ?: 0L }
                } else {
                    filtered.sortedByDescending { it.productionDate?.seconds ?: 0L }
                }
            }
            ProductionSortField.BATCH_NUMBER -> {
                if (productionState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.batchNumber }
                } else {
                    filtered.sortedByDescending { it.batchNumber }
                }
            }
            ProductionSortField.QUANTITY -> {
                if (productionState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.quantityProduced }
                } else {
                    filtered.sortedByDescending { it.quantityProduced }
                }
            }
        }
    }

    fun addProductionRecord(productionInput: ProductionInput) {
        if (productionInput.batchNumber.isBlank()) {
            productionState = productionState.copy(error = "Batch number is required")
            return
        }

        if (productionInput.quantityProduced <= 0) {
            productionState = productionState.copy(error = "Quantity produced must be greater than 0")
            return
        }

        if (productionInput.supervisorName.isBlank()) {
            productionState = productionState.copy(error = "Supervisor name is required")
            return
        }

        if (productionInput.rawMaterialsUsed.isEmpty()) {
            productionState = productionState.copy(error = "At least one raw material must be selected")
            return
        }

        // Validate waste tracking if production difference exists (only when raw materials > quantity produced)
        val totalRawMaterialsUsed = productionInput.rawMaterialsUsed.values.sum()
        val productionDifference = totalRawMaterialsUsed - productionInput.quantityProduced
        
        if (productionDifference > 0) {
            if (productionInput.wasteTracking == null) {
                productionState = productionState.copy(error = "Waste tracking is required when there's production difference")
                return
            }
            
            val totalWaste = productionInput.wasteTracking.getTotalWaste()
            if (kotlin.math.abs(totalWaste - productionDifference) > 0.01) { // Allow small floating point differences
                productionState = productionState.copy(error = "Total waste (${String.format("%.2f", totalWaste)} kg) must exactly equal production difference (${String.format("%.2f", productionDifference)} kg)")
                return
            }
        }

        viewModelScope.launch {
            productionState = productionState.copy(isAdding = true, isLoading = true, error = null)

            try {
                // First, validate inventory quantities
                val inventoryItems = inventoryViewModel.getFilteredItems()
                val inventoryMap = inventoryItems.associate { it.id to it }
                
                // Check if all raw materials have sufficient quantity
                for ((itemId, quantityUsed) in productionInput.rawMaterialsUsed) {
                    val inventoryItem = inventoryMap[itemId]
                    if (inventoryItem == null) {
                        productionState = productionState.copy(
                            isAdding = false,
                            isLoading = false,
                            error = "Raw material not found in inventory"
                        )
                        return@launch
                    }
                    if (inventoryItem.quantity < quantityUsed) {
                        productionState = productionState.copy(
                            isAdding = false,
                            isLoading = false,
                            error = "Insufficient quantity of ${inventoryItem.name}. Available: ${inventoryItem.quantity} ${inventoryItem.unit}, Required: $quantityUsed ${inventoryItem.unit}"
                        )
                        return@launch
                    }
                }

                // Add production record first
                val result = productionRepository.addProductionRecord(productionInput)
                
                if (result.isSuccess) {
                    // Update inventory using transactions for atomicity
                    updateInventoryForProduction(productionInput.rawMaterialsUsed, productionInput.quantityProduced, isAddition = true)
                    
                    productionState = productionState.copy(
                        isAdding = false,
                        isLoading = false,
                        successMessage = "Production record added and inventory updated successfully!",
                        error = null,
                        showAddDialog = false
                    )
                } else {
                    productionState = productionState.copy(
                        isAdding = false,
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to add production record"
                    )
                }
            } catch (e: Exception) {
                productionState = productionState.copy(
                    isAdding = false,
                    isLoading = false,
                    error = "Error updating inventory: ${e.message}"
                )
            }
        }
    }

    /**
     * Update a production record
     */
    fun updateProductionRecord(recordId: String, productionInput: ProductionInput) {
        if (productionInput.batchNumber.isBlank()) {
            productionState = productionState.copy(error = "Batch number is required")
            return
        }

        if (productionInput.quantityProduced <= 0) {
            productionState = productionState.copy(error = "Quantity produced must be greater than 0")
            return
        }

        if (productionInput.supervisorName.isBlank()) {
            productionState = productionState.copy(error = "Supervisor name is required")
            return
        }

        if (productionInput.rawMaterialsUsed.isEmpty()) {
            productionState = productionState.copy(error = "At least one raw material must be selected")
            return
        }

        // Validate waste tracking if production difference exists (only when raw materials > quantity produced)
        val totalRawMaterialsUsed = productionInput.rawMaterialsUsed.values.sum()
        val productionDifference = totalRawMaterialsUsed - productionInput.quantityProduced
        
        if (productionDifference > 0) {
            if (productionInput.wasteTracking == null) {
                productionState = productionState.copy(error = "Waste tracking is required when there's production difference")
                return
            }
            
            val totalWaste = productionInput.wasteTracking.getTotalWaste()
            if (kotlin.math.abs(totalWaste - productionDifference) > 0.01) { // Allow small floating point differences
                productionState = productionState.copy(error = "Total waste (${String.format("%.2f", totalWaste)} kg) must exactly equal production difference (${String.format("%.2f", productionDifference)} kg)")
                return
            }
        }

        viewModelScope.launch {
            productionState = productionState.copy(isUpdating = true, isLoading = true, error = null)

            try {
                // Get the original record to calculate inventory differences
                val originalRecord = productionState.productionRecords.find { it.id == recordId }
                if (originalRecord == null) {
                    productionState = productionState.copy(
                        isUpdating = false,
                        isLoading = false,
                        error = "Original production record not found"
                    )
                    return@launch
                }

                // Validate inventory quantities for new raw materials
                val inventoryItems = inventoryViewModel.getFilteredItems()
                val inventoryMap = inventoryItems.associate { it.id to it }
                
                // Check if all raw materials have sufficient quantity
                for ((itemId, quantityUsed) in productionInput.rawMaterialsUsed) {
                    val inventoryItem = inventoryMap[itemId]
                    if (inventoryItem == null) {
                        productionState = productionState.copy(
                            isUpdating = false,
                            isLoading = false,
                            error = "Raw material not found in inventory"
                        )
                        return@launch
                    }
                    
                    // Calculate the difference from original usage
                    val originalUsed = originalRecord.rawMaterialsUsed[itemId] ?: 0.0
                    val quantityDifference = quantityUsed - originalUsed
                    
                    if (quantityDifference > 0 && inventoryItem.quantity < quantityDifference) {
                        productionState = productionState.copy(
                            isUpdating = false,
                            isLoading = false,
                            error = "Insufficient quantity of ${inventoryItem.name}. Available: ${inventoryItem.quantity} ${inventoryItem.unit}, Additional Required: $quantityDifference ${inventoryItem.unit}"
                        )
                        return@launch
                    }
                }

                // Update production record first
                val result = productionRepository.updateProductionRecord(recordId, productionInput)
                
                if (result.isSuccess) {
                    // Update inventory using transactions for atomicity
                    updateInventoryForProductionUpdate(originalRecord, productionInput)
                    
                    productionState = productionState.copy(
                        isUpdating = false,
                        isLoading = false,
                        successMessage = "Production record updated and inventory adjusted successfully!",
                        error = null,
                        showEditDialog = false,
                        recordToEdit = null
                    )
                } else {
                    productionState = productionState.copy(
                        isUpdating = false,
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to update production record"
                    )
                }
            } catch (e: Exception) {
                productionState = productionState.copy(
                    isUpdating = false,
                    isLoading = false,
                    error = "Error updating inventory: ${e.message}"
                )
            }
        }
    }

    /**
     * Delete a production record
     */
    fun deleteProductionRecord(recordId: String) {
        viewModelScope.launch {
            productionState = productionState.copy(isDeleting = true, isLoading = true, error = null)

            try {
                // Get the record to restore inventory
                val recordToDelete = productionState.productionRecords.find { it.id == recordId }
                if (recordToDelete == null) {
                    productionState = productionState.copy(
                        isDeleting = false,
                        isLoading = false,
                        error = "Production record not found"
                    )
                    return@launch
                }

                val result = productionRepository.deleteProductionRecord(recordId)

                if (result.isSuccess) {
                    // Restore inventory using transactions for atomicity
                    updateInventoryForProduction(recordToDelete.rawMaterialsUsed, recordToDelete.quantityProduced, isAddition = false)
                    
                    productionState = productionState.copy(
                        isDeleting = false,
                        isLoading = false,
                        successMessage = "Production record deleted and inventory restored successfully!",
                        error = null,
                        showDeleteConfirmDialog = false,
                        recordToDelete = null
                    )
                } else {
                    productionState = productionState.copy(
                        isDeleting = false,
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to delete production record"
                    )
                }
            } catch (e: Exception) {
                productionState = productionState.copy(
                    isDeleting = false,
                    isLoading = false,
                    error = "Error restoring inventory: ${e.message}"
                )
            }
        }
    }

    /**
     * Calculate production difference
     */
    fun calculateProductionDifference(rawMaterialsUsed: Map<String, Double>, quantityProduced: Double): Double {
        val totalRawMaterialsUsed = rawMaterialsUsed.values.sum()
        return totalRawMaterialsUsed - quantityProduced
    }

    /**
     * Get filtered production records based on search query
     */
    fun getFilteredRecords(): List<ProductionRecord> {
        val query = productionState.searchQuery.lowercase()
        return if (query.isBlank()) {
            productionState.productionRecords
        } else {
            productionState.productionRecords.filter { record ->
                record.batchNumber.lowercase().contains(query) ||
                record.supervisorName.lowercase().contains(query) ||
                record.notes.lowercase().contains(query)
            }
        }
    }

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        productionState = productionState.copy(searchQuery = query)
    }

    fun clearMessages() {
        productionState = productionState.copy(error = null, successMessage = null)
    }
    
    /**
     * Add or update Fortified Rice in inventory
     */
    private fun addOrUpdateFortifiedRice(quantityProduced: Double) {
        val inventoryItems = inventoryViewModel.getFilteredItems()
        val fortifiedRiceItem = inventoryItems.find { 
            it.name.equals("Fortified Rice", ignoreCase = true) || 
            it.name.equals("FRK", ignoreCase = true) ||
            it.name.equals("Fortified Rice Kernels", ignoreCase = true)
        }
        
        if (fortifiedRiceItem != null) {
            // Update existing Fortified Rice item
            val updatedItem = fortifiedRiceItem.copy(quantity = fortifiedRiceItem.quantity + quantityProduced)
            inventoryViewModel.updateInventoryItem(fortifiedRiceItem.id, updatedItem)
        } else {
            // Create new Fortified Rice item
            val newFortifiedRiceItem = com.humblecoders.plantmanagement.data.InventoryItem(
                name = "Fortified Rice",
                quantity = quantityProduced,
                unit = "kg",
                categoryType = com.humblecoders.plantmanagement.data.CategoryType.OTHER
            )
            inventoryViewModel.addInventoryItem(newFortifiedRiceItem)
        }
    }
    
    /**
     * Subtract Fortified Rice from inventory
     */
    private fun subtractFortifiedRice(quantityProduced: Double) {
        val inventoryItems = inventoryViewModel.getFilteredItems()
        val fortifiedRiceItem = inventoryItems.find { 
            it.name.equals("Fortified Rice", ignoreCase = true) || 
            it.name.equals("FRK", ignoreCase = true) ||
            it.name.equals("Fortified Rice Kernels", ignoreCase = true)
        }
        
        if (fortifiedRiceItem != null) {
            val newQuantity = fortifiedRiceItem.quantity - quantityProduced
            if (newQuantity > 0) {
                // Update existing Fortified Rice item
                val updatedItem = fortifiedRiceItem.copy(quantity = newQuantity)
                inventoryViewModel.updateInventoryItem(fortifiedRiceItem.id, updatedItem)
            } else {
                // Remove the item if quantity becomes zero or negative
                inventoryViewModel.deleteInventoryItem(fortifiedRiceItem.id)
            }
        }
    }
    
    /**
     * Update inventory for production operations using transactions for atomicity
     * @param rawMaterialsUsed Map of item IDs to quantities used
     * @param quantityProduced Quantity of fortified rice produced
     * @param isAddition True for adding production (subtract raw materials, add fortified rice), false for deletion (restore raw materials, subtract fortified rice)
     */
    private suspend fun updateInventoryForProduction(rawMaterialsUsed: Map<String, Double>, quantityProduced: Double, isAddition: Boolean) {
        val inventoryItems = inventoryViewModel.getFilteredItems()
        val inventoryMap = inventoryItems.associate { it.id to it }
        
        // Update raw materials inventory
        for ((itemId, quantityUsed) in rawMaterialsUsed) {
            val inventoryItem = inventoryMap[itemId]
            if (inventoryItem != null) {
                val quantityChange = if (isAddition) -quantityUsed else quantityUsed
                val updatedItem = inventoryItem.copy(quantity = inventoryItem.quantity + quantityChange)
                inventoryViewModel.updateInventoryItem(itemId, updatedItem)
            }
        }
        
        // Update Fortified Rice inventory
        if (isAddition) {
            addOrUpdateFortifiedRice(quantityProduced)
        } else {
            subtractFortifiedRice(quantityProduced)
        }
    }
    
    /**
     * Update inventory for production record updates using transactions for atomicity
     * @param originalRecord The original production record
     * @param newProductionInput The new production input
     */
    private suspend fun updateInventoryForProductionUpdate(originalRecord: ProductionRecord, newProductionInput: ProductionInput) {
        val inventoryItems = inventoryViewModel.getFilteredItems()
        val inventoryMap = inventoryItems.associate { it.id to it }
        
        // Handle raw materials changes
        for ((itemId, quantityUsed) in newProductionInput.rawMaterialsUsed) {
            val inventoryItem = inventoryMap[itemId]!!
            val originalUsed = originalRecord.rawMaterialsUsed[itemId] ?: 0.0
            val quantityDifference = quantityUsed - originalUsed
            
            if (quantityDifference != 0.0) {
                val updatedItem = inventoryItem.copy(quantity = inventoryItem.quantity - quantityDifference)
                inventoryViewModel.updateInventoryItem(itemId, updatedItem)
            }
        }
        
        // Handle raw materials that were removed
        for ((itemId, originalUsed) in originalRecord.rawMaterialsUsed) {
            if (!newProductionInput.rawMaterialsUsed.containsKey(itemId)) {
                val inventoryItem = inventoryMap[itemId]!!
                val updatedItem = inventoryItem.copy(quantity = inventoryItem.quantity + originalUsed)
                inventoryViewModel.updateInventoryItem(itemId, updatedItem)
            }
        }
        
        // Update Fortified Rice quantity difference
        val quantityDifference = newProductionInput.quantityProduced - originalRecord.quantityProduced
        if (quantityDifference != 0.0) {
            if (quantityDifference > 0) {
                addOrUpdateFortifiedRice(quantityDifference)
            } else {
                subtractFortifiedRice(-quantityDifference)
            }
        }
    }
}
