package com.humblecoders.plantmanagement.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.humblecoders.plantmanagement.data.CategoryType
import com.humblecoders.plantmanagement.data.InventoryItem
import com.humblecoders.plantmanagement.repositories.InventoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class InventoryState(
    val items: List<InventoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = "",
    val filterCategory: CategoryType? = null
)

class InventoryViewModel(
    private val inventoryRepository: InventoryRepository
) {
    var inventoryState by mutableStateOf(InventoryState())
        private set

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    init {
        inventoryRepository.listenToInventoryItems { items ->
            inventoryState = inventoryState.copy(items = items)
        }
    }

    fun addInventoryItem(item: InventoryItem) {
        if (item.name.isBlank()) {
            inventoryState = inventoryState.copy(error = "Item name is required")
            return
        }

        if (item.quantity < 0) {
            inventoryState = inventoryState.copy(error = "Quantity cannot be negative")
            return
        }

        if (item.unit.isBlank()) {
            inventoryState = inventoryState.copy(error = "Unit is required")
            return
        }

        viewModelScope.launch {
            inventoryState = inventoryState.copy(isLoading = true, error = null)

            val result = inventoryRepository.addInventoryItem(item)

            inventoryState = if (result.isSuccess) {
                inventoryState.copy(
                    isLoading = false,
                    successMessage = "Inventory item added successfully!",
                    error = null
                )
            } else {
                inventoryState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to add inventory item"
                )
            }
        }
    }

    fun updateInventoryItem(itemId: String, item: InventoryItem) {
        if (item.name.isBlank()) {
            inventoryState = inventoryState.copy(error = "Item name is required")
            return
        }

        if (item.quantity < 0) {
            inventoryState = inventoryState.copy(error = "Quantity cannot be negative")
            return
        }

        if (item.unit.isBlank()) {
            inventoryState = inventoryState.copy(error = "Unit is required")
            return
        }

        viewModelScope.launch {
            inventoryState = inventoryState.copy(isLoading = true, error = null)

            val result = inventoryRepository.updateInventoryItem(itemId, item)

            inventoryState = if (result.isSuccess) {
                inventoryState.copy(
                    isLoading = false,
                    successMessage = "Inventory item updated successfully!",
                    error = null
                )
            } else {
                inventoryState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to update inventory item"
                )
            }
        }
    }

    fun deleteInventoryItem(itemId: String) {
        viewModelScope.launch {
            inventoryState = inventoryState.copy(isLoading = true, error = null)

            val result = inventoryRepository.deleteInventoryItem(itemId)

            inventoryState = if (result.isSuccess) {
                inventoryState.copy(
                    isLoading = false,
                    successMessage = "Inventory item deleted successfully!",
                    error = null
                )
            } else {
                inventoryState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to delete inventory item"
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        inventoryState = inventoryState.copy(searchQuery = query)
    }

    fun updateFilterCategory(category: CategoryType?) {
        inventoryState = inventoryState.copy(filterCategory = category)
    }

    fun getFilteredItems(): List<InventoryItem> {
        return inventoryState.items.filter { item ->
            val query = inventoryState.searchQuery.lowercase()
            val textMatch = item.name.lowercase().contains(query) ||
                    item.unit.lowercase().contains(query)

            val categoryMatch = inventoryState.filterCategory?.let { filter ->
                item.categoryType == filter
            } ?: true

            textMatch && categoryMatch
        }
    }

    fun clearMessages() {
        inventoryState = inventoryState.copy(error = null, successMessage = null)
    }
}