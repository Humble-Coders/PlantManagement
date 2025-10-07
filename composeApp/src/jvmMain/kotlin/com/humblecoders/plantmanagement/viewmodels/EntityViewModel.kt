package com.humblecoders.plantmanagement.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.humblecoders.plantmanagement.data.Entity
import com.humblecoders.plantmanagement.repositories.EntityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class EntityState(
    val entities: List<Entity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = "",
    val sortBy: SortField = SortField.FIRM_NAME,
    val sortDirection: SortDirection = SortDirection.ASCENDING
)

enum class SortField {
    FIRM_NAME,
    CITY,
    GSTIN
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

class EntityViewModel(
    private val entityRepository: EntityRepository
) {
    var entityState by mutableStateOf(EntityState())
        private set

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    init {
        // Start listening to entities
        entityRepository.listenToEntities { entities ->
            entityState = entityState.copy(entities = entities)
        }
    }

    /**
     * Add a new entity
     */
    fun addEntity(entity: Entity) {
        if (entity.firmName.isBlank()) {
            entityState = entityState.copy(error = "Firm name is required")
            return
        }

        if (entity.contactPerson.isBlank()) {
            entityState = entityState.copy(error = "Contact person is required")
            return
        }

        if (entity.contactNo.isBlank()) {
            entityState = entityState.copy(error = "Contact number is required")
            return
        }

        if (entity.city.isBlank()) {
            entityState = entityState.copy(error = "City is required")
            return
        }

        if (entity.state.isBlank()) {
            entityState = entityState.copy(error = "State is required")
            return
        }

        if (entity.gstin.isBlank()) {
            entityState = entityState.copy(error = "GSTIN is required")
            return
        }

        viewModelScope.launch {
            entityState = entityState.copy(isLoading = true, error = null)

            val result = entityRepository.addEntity(entity)

            entityState = if (result.isSuccess) {
                entityState.copy(
                    isLoading = false,
                    successMessage = "Entity added successfully!",
                    error = null
                )
            } else {
                entityState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to add entity"
                )
            }
        }
    }

    /**
     * Update an existing entity
     */
    fun updateEntity(entityId: String, entity: Entity) {
        if (entity.firmName.isBlank()) {
            entityState = entityState.copy(error = "Firm name is required")
            return
        }

        if (entity.contactPerson.isBlank()) {
            entityState = entityState.copy(error = "Contact person is required")
            return
        }

        if (entity.contactNo.isBlank()) {
            entityState = entityState.copy(error = "Contact number is required")
            return
        }

        if (entity.city.isBlank()) {
            entityState = entityState.copy(error = "City is required")
            return
        }

        if (entity.state.isBlank()) {
            entityState = entityState.copy(error = "State is required")
            return
        }

        if (entity.gstin.isBlank()) {
            entityState = entityState.copy(error = "GSTIN is required")
            return
        }

        viewModelScope.launch {
            entityState = entityState.copy(isLoading = true, error = null)

            val result = entityRepository.updateEntity(entityId, entity)

            entityState = if (result.isSuccess) {
                entityState.copy(
                    isLoading = false,
                    successMessage = "Entity updated successfully!",
                    error = null
                )
            } else {
                entityState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to update entity"
                )
            }
        }
    }

    /**
     * Delete an entity
     */
    fun deleteEntity(entityId: String) {
        viewModelScope.launch {
            entityState = entityState.copy(isLoading = true, error = null)

            val result = entityRepository.deleteEntity(entityId)

            entityState = if (result.isSuccess) {
                entityState.copy(
                    isLoading = false,
                    successMessage = "Entity deleted successfully!",
                    error = null
                )
            } else {
                entityState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to delete entity"
                )
            }
        }
    }

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        entityState = entityState.copy(searchQuery = query)
    }

    /**
     * Update sort field
     */
    fun updateSortBy(sortBy: SortField) {
        entityState = entityState.copy(sortBy = sortBy)
    }

    /**
     * Toggle sort direction
     */
    fun toggleSortDirection() {
        entityState = entityState.copy(
            sortDirection = if (entityState.sortDirection == SortDirection.ASCENDING) {
                SortDirection.DESCENDING
            } else {
                SortDirection.ASCENDING
            }
        )
    }

    /**
     * Get filtered and sorted entities
     */
    fun getFilteredAndSortedEntities(): List<Entity> {
        val filtered = entityState.entities.filter { entity ->
            val query = entityState.searchQuery.lowercase()
            entity.firmName.lowercase().contains(query) ||
                    entity.contactPerson.lowercase().contains(query) ||
                    entity.city.lowercase().contains(query) ||
                    entity.state.lowercase().contains(query) ||
                    entity.gstin.lowercase().contains(query)
        }

        val sorted = when (entityState.sortBy) {
            SortField.FIRM_NAME -> {
                if (entityState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.firmName }
                } else {
                    filtered.sortedByDescending { it.firmName }
                }
            }
            SortField.CITY -> {
                if (entityState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.city }
                } else {
                    filtered.sortedByDescending { it.city }
                }
            }
            SortField.GSTIN -> {
                if (entityState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.gstin }
                } else {
                    filtered.sortedByDescending { it.gstin }
                }
            }
        }

        return sorted
    }

    /**
     * Clear messages
     */
    fun clearMessages() {
        entityState = entityState.copy(error = null, successMessage = null)
    }
}