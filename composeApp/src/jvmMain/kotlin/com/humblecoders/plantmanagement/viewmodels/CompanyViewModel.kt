package com.humblecoders.plantmanagement.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.humblecoders.plantmanagement.data.Company
import com.humblecoders.plantmanagement.repositories.CompanyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class CompanyState(
    val company: Company? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class CompanyViewModel(
    private val companyRepository: CompanyRepository
) {
    var companyState by mutableStateOf(CompanyState())
        private set

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    init {
        loadCompany()
    }

    fun loadCompany() {
        viewModelScope.launch {
            companyState = companyState.copy(isLoading = true, error = null)
            try {
                val result = companyRepository.getCompany()
                result.fold(
                    onSuccess = { company ->
                        companyState = companyState.copy(
                            company = company,
                            isLoading = false
                        )
                    },
                    onFailure = { error ->
                        companyState = companyState.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load company information"
                        )
                    }
                )
            } catch (e: Exception) {
                companyState = companyState.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load company information"
                )
            }
        }
    }

    fun saveCompany(company: Company) {
        viewModelScope.launch {
            companyState = companyState.copy(isSaving = true, error = null)
            try {
                val result = companyRepository.saveCompany(company)
                result.fold(
                    onSuccess = { companyId ->
                        companyState = companyState.copy(
                            isSaving = false,
                            successMessage = "Company information saved successfully"
                        )
                        loadCompany() // Refresh the data
                    },
                    onFailure = { error ->
                        companyState = companyState.copy(
                            isSaving = false,
                            error = error.message ?: "Failed to save company information"
                        )
                    }
                )
            } catch (e: Exception) {
                companyState = companyState.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save company information"
                )
            }
        }
    }

    fun deleteCompany() {
        viewModelScope.launch {
            companyState = companyState.copy(isDeleting = true, error = null)
            try {
                val result = companyRepository.deleteCompany()
                result.fold(
                    onSuccess = {
                        companyState = companyState.copy(
                            isDeleting = false,
                            successMessage = "Company information deleted successfully",
                            company = null
                        )
                    },
                    onFailure = { error ->
                        companyState = companyState.copy(
                            isDeleting = false,
                            error = error.message ?: "Failed to delete company information"
                        )
                    }
                )
            } catch (e: Exception) {
                companyState = companyState.copy(
                    isDeleting = false,
                    error = e.message ?: "Failed to delete company information"
                )
            }
        }
    }

    fun clearError() {
        companyState = companyState.copy(error = null)
    }

    fun clearSuccessMessage() {
        companyState = companyState.copy(successMessage = null)
    }
}
