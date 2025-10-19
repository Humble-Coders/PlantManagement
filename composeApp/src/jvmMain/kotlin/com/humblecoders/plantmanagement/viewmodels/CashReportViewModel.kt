package com.humblecoders.plantmanagement.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.cloud.Timestamp
import com.humblecoders.plantmanagement.data.CashReport
import com.humblecoders.plantmanagement.data.CashReportCategory
import com.humblecoders.plantmanagement.data.CashReportType
import com.humblecoders.plantmanagement.repositories.CashReportRepository
import com.humblecoders.plantmanagement.repositories.CashReportSummary
import com.humblecoders.plantmanagement.services.CashReportPdfService
import com.humblecoders.plantmanagement.services.FirebaseStorageService
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class CashReportSortField {
    DATE,
    CATEGORY,
    AMOUNT,
    TYPE
}

enum class CashReportTypeFilter {
    ALL,
    CASH_IN,
    CASH_OUT
}

data class CashReportState(
    val cashReports: List<CashReport> = emptyList(),
    val categories: List<CashReportCategory> = emptyList(),
    val summary: CashReportSummary = CashReportSummary(),
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val isRetrying: Boolean = false, // New field for retry state
    val isAddingCategory: Boolean = false,
    val isDeletingCategory: Boolean = false,
    val isDeletingCashReport: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = "",
    val sortBy: CashReportSortField = CashReportSortField.DATE,
    val sortDirection: SortDirection = SortDirection.DESCENDING,
    val filterDateFrom: String = "",
    val filterDateTo: String = "",
    val filterType: CashReportTypeFilter = CashReportTypeFilter.ALL
)

class CashReportViewModel(
    private val cashReportRepository: CashReportRepository,
    private val storageService: FirebaseStorageService,
    private val pdfService: CashReportPdfService = CashReportPdfService()
) : ViewModel() {

    var cashReportState by mutableStateOf(CashReportState())
        private set

    fun addCashReport(
        transactionType: CashReportType,
        categoryId: String,
        amount: Double,
        date: LocalDate,
        notes: String,
        imageFile: File? = null
    ) {
        viewModelScope.launch {
            cashReportState = cashReportState.copy(isProcessing = true, error = null)
            
            // Upload image if provided
            var imageUrl = ""
            if (imageFile != null) {
                val uploadResult = storageService.uploadImage(imageFile, "cash_reports")
                uploadResult.fold(
                    onSuccess = { url -> imageUrl = url },
                    onFailure = { error ->
                        cashReportState = cashReportState.copy(
                            isProcessing = false,
                            error = "Failed to upload image: ${error.message}"
                        )
                        return@launch
                    }
                )
            }
            
            val timestamp = Timestamp.of(java.util.Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()))
            
            val cashReport = CashReport(
                transactionType = transactionType,
                categoryId = categoryId,
                amount = amount,
                date = timestamp,
                notes = notes,
                imageUrl = imageUrl
            )
            
            val result = cashReportRepository.addCashReport(cashReport)
            
            result.fold(
                onSuccess = { _ ->
                    cashReportState = cashReportState.copy(
                        isProcessing = false,
                        successMessage = "Cash transaction added successfully"
                    )
                    // Refresh data
                    loadCashReports()
                    loadSummary()
                },
                onFailure = { error ->
                    cashReportState = cashReportState.copy(
                        isProcessing = false,
                        error = error.message ?: "Failed to add cash transaction"
                    )
                }
            )
        }
    }

    fun addCategory(categoryName: String, categoryType: CashReportType) {
        viewModelScope.launch {
            // Prevent multiple simultaneous add operations
            if (cashReportState.isAddingCategory) return@launch
            
            cashReportState = cashReportState.copy(
                isAddingCategory = true,
                error = null,
                successMessage = null
            )
            
            val category = CashReportCategory(name = categoryName, type = categoryType)
            val result = cashReportRepository.addCategory(category)
            
            result.fold(
                onSuccess = { _ ->
                    cashReportState = cashReportState.copy(
                        isAddingCategory = false,
                        successMessage = "Category added successfully"
                    )
                    loadCategories()
                },
                onFailure = { error ->
                    cashReportState = cashReportState.copy(
                        isAddingCategory = false,
                        error = error.message ?: "Failed to add category"
                    )
                }
            )
        }
    }

    fun updateCategory(categoryId: String, newName: String) {
        viewModelScope.launch {
            val result = cashReportRepository.updateCategory(categoryId, newName)
            
            result.fold(
                onSuccess = {
                    cashReportState = cashReportState.copy(
                        successMessage = "Category updated successfully"
                    )
                    loadCategories()
                },
                onFailure = { error ->
                    cashReportState = cashReportState.copy(
                        error = error.message ?: "Failed to update category"
                    )
                }
            )
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            // Prevent multiple simultaneous delete operations
            if (cashReportState.isDeletingCategory) return@launch
            
            cashReportState = cashReportState.copy(
                isDeletingCategory = true,
                error = null,
                successMessage = null
            )
            
            val result = cashReportRepository.deleteCategory(categoryId)
            
            result.fold(
                onSuccess = {
                    cashReportState = cashReportState.copy(
                        isDeletingCategory = false,
                        successMessage = "Category deleted successfully"
                    )
                    loadCategories()
                },
                onFailure = { error ->
                    cashReportState = cashReportState.copy(
                        isDeletingCategory = false,
                        error = error.message ?: "Failed to delete category"
                    )
                }
            )
        }
    }

    fun loadCashReports(retryCount: Int = 0) {
        viewModelScope.launch {
            if (retryCount == 0) {
                cashReportState = cashReportState.copy(isLoading = true, error = null, isRetrying = false)
            } else {
                cashReportState = cashReportState.copy(isRetrying = true, error = null)
            }
            
            val result = cashReportRepository.getCashReports()
            result.fold(
                onSuccess = { cashReports ->
                    cashReportState = cashReportState.copy(
                        isLoading = false,
                        isRetrying = false,
                        cashReports = cashReports
                    )
                },
                onFailure = { error ->
                    if (retryCount < 3) {
                        // Retry after a delay without showing error
                        kotlinx.coroutines.delay(1000 * (retryCount + 1).toLong())
                        loadCashReports(retryCount + 1)
                    } else {
                        cashReportState = cashReportState.copy(
                            isLoading = false,
                            isRetrying = false,
                            error = "Failed to load cash reports after 3 attempts: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun loadCategories(retryCount: Int = 0) {
        viewModelScope.launch {
            if (retryCount > 0) {
                cashReportState = cashReportState.copy(isRetrying = true, error = null)
            }
            
            val result = cashReportRepository.getCategories()
            result.fold(
                onSuccess = { categories ->
                    cashReportState = cashReportState.copy(
                        isRetrying = false,
                        categories = categories
                    )
                },
                onFailure = { error ->
                    if (retryCount < 3) {
                        // Retry after a delay without showing error
                        kotlinx.coroutines.delay(1000 * (retryCount + 1).toLong())
                        loadCategories(retryCount + 1)
                    } else {
                        cashReportState = cashReportState.copy(
                            isRetrying = false,
                            error = "Failed to load categories after 3 attempts: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun searchCategories(query: String) {
        viewModelScope.launch {
            val result = cashReportRepository.searchCategories(query)
            result.fold(
                onSuccess = { categories ->
                    cashReportState = cashReportState.copy(categories = categories)
                },
                onFailure = { error ->
                    cashReportState = cashReportState.copy(
                        error = error.message ?: "Failed to search categories"
                    )
                }
            )
        }
    }

    fun loadSummary(retryCount: Int = 0) {
        viewModelScope.launch {
            if (retryCount > 0) {
                cashReportState = cashReportState.copy(isRetrying = true, error = null)
            }
            
            val result = cashReportRepository.getCashReportSummary()
            result.fold(
                onSuccess = { summary ->
                    cashReportState = cashReportState.copy(
                        isRetrying = false,
                        summary = summary
                    )
                },
                onFailure = { error ->
                    if (retryCount < 3) {
                        // Retry after a delay without showing error
                        kotlinx.coroutines.delay(1000 * (retryCount + 1).toLong())
                        loadSummary(retryCount + 1)
                    } else {
                        cashReportState = cashReportState.copy(
                            isRetrying = false,
                            error = "Failed to load summary after 3 attempts: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun listenToCashReports() {
        cashReportRepository.listenToCashReports { cashReports ->
            cashReportState = cashReportState.copy(cashReports = cashReports)
            // Also update summary when transactions change
            viewModelScope.launch {
                loadSummary()
            }
        }
    }

    fun deleteCashReport(cashReportId: String) {
        viewModelScope.launch {
            // Prevent multiple simultaneous delete operations
            if (cashReportState.isDeletingCashReport) return@launch
            
            cashReportState = cashReportState.copy(
                isDeletingCashReport = true,
                error = null,
                successMessage = null
            )
            
            val result = cashReportRepository.deleteCashReport(cashReportId)
            
            result.fold(
                onSuccess = {
                    cashReportState = cashReportState.copy(
                        isDeletingCashReport = false,
                        successMessage = "Transaction deleted successfully"
                    )
                    loadSummary()
                },
                onFailure = { error ->
                    cashReportState = cashReportState.copy(
                        isDeletingCashReport = false,
                        error = error.message ?: "Failed to delete transaction"
                    )
                }
            )
        }
    }

    fun clearMessages() {
        cashReportState = cashReportState.copy(
            error = null,
            successMessage = null
        )
    }

    fun showPdfSuccessMessage() {
        cashReportState = cashReportState.copy(
            successMessage = "PDF generated and opened successfully"
        )
    }

    /**
     * Get filtered summary based on current filters
     */
    fun getFilteredSummary(): CashReportSummary {
        val filteredReports = getFilteredAndSortedReports()
        
        val totalCashIn = filteredReports
            .filter { it.transactionType == CashReportType.CASH_IN }
            .sumOf { it.amount }
        
        val totalCashOut = filteredReports
            .filter { it.transactionType == CashReportType.CASH_OUT }
            .sumOf { it.amount }
        
        val netChange = totalCashIn - totalCashOut
        
        return CashReportSummary(
            totalCashIn = totalCashIn,
            totalCashOut = totalCashOut,
            netChange = netChange
        )
    }

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        cashReportState = cashReportState.copy(searchQuery = query)
    }

    /**
     * Update sort field
     */
    fun updateSortBy(sortBy: CashReportSortField) {
        cashReportState = cashReportState.copy(sortBy = sortBy)
    }

    /**
     * Toggle sort direction
     */
    fun toggleSortDirection() {
        cashReportState = cashReportState.copy(
            sortDirection = if (cashReportState.sortDirection == SortDirection.ASCENDING) {
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
        cashReportState = cashReportState.copy(
            filterDateFrom = from,
            filterDateTo = to
        )
    }

    /**
     * Update type filter
     */
    fun updateTypeFilter(filter: CashReportTypeFilter) {
        cashReportState = cashReportState.copy(filterType = filter)
    }

    /**
     * Get filtered and sorted cash reports
     */
    fun getFilteredAndSortedReports(): List<CashReport> {
        val query = cashReportState.searchQuery.lowercase()

        val filtered = cashReportState.cashReports.filter { report ->
            // Search filter
            val categoryName = cashReportState.categories.find { it.id == report.categoryId }?.name ?: ""
            val textMatch = categoryName.lowercase().contains(query) ||
                    report.notes.lowercase().contains(query)

            // Type filter
            val typeMatch = when (cashReportState.filterType) {
                CashReportTypeFilter.ALL -> true
                CashReportTypeFilter.CASH_IN -> report.transactionType == CashReportType.CASH_IN
                CashReportTypeFilter.CASH_OUT -> report.transactionType == CashReportType.CASH_OUT
            }

            // Date filter
            val dateMatch = if (cashReportState.filterDateFrom.isNotBlank() || cashReportState.filterDateTo.isNotBlank()) {
                val reportDate = report.date?.let {
                    java.time.LocalDate.ofInstant(
                        java.time.Instant.ofEpochMilli(it.seconds * 1000),
                        java.time.ZoneId.systemDefault()
                    )
                }

                val fromDate = if (cashReportState.filterDateFrom.isNotBlank()) {
                    try {
                        java.time.LocalDate.parse(cashReportState.filterDateFrom, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (e: Exception) {
                        java.time.LocalDate.MIN
                    }
                } else {
                    java.time.LocalDate.MIN
                }

                val toDate = if (cashReportState.filterDateTo.isNotBlank()) {
                    try {
                        java.time.LocalDate.parse(cashReportState.filterDateTo, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (e: Exception) {
                        java.time.LocalDate.MAX
                    }
                } else {
                    java.time.LocalDate.MAX
                }

                reportDate != null && !reportDate.isBefore(fromDate) && !reportDate.isAfter(toDate)
            } else {
                true
            }

            textMatch && typeMatch && dateMatch
        }

        return when (cashReportState.sortBy) {
            CashReportSortField.DATE -> {
                if (cashReportState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.date?.seconds ?: 0L }
                } else {
                    filtered.sortedByDescending { it.date?.seconds ?: 0L }
                }
            }
            CashReportSortField.CATEGORY -> {
                if (cashReportState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { 
                        cashReportState.categories.find { cat -> cat.id == it.categoryId }?.name ?: ""
                    }
                } else {
                    filtered.sortedByDescending { 
                        cashReportState.categories.find { cat -> cat.id == it.categoryId }?.name ?: ""
                    }
                }
            }
            CashReportSortField.AMOUNT -> {
                if (cashReportState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.amount }
                } else {
                    filtered.sortedByDescending { it.amount }
                }
            }
            CashReportSortField.TYPE -> {
                if (cashReportState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.transactionType.name }
                } else {
                    filtered.sortedByDescending { it.transactionType.name }
                }
            }
        }
    }

    /**
     * Generate PDF for filtered cash reports
     */
    suspend fun generatePdf(): Result<ByteArray> {
        return withContext(Dispatchers.IO) {
            try {
                val filteredReports = getFilteredAndSortedReports()
                val filteredSummary = getFilteredSummary()
                
                // Create filter description
                val filterInfo = buildString {
                    val filters = mutableListOf<String>()
                    
                    if (cashReportState.searchQuery.isNotBlank()) {
                        filters.add("Search: \"${cashReportState.searchQuery}\"")
                    }
                    
                    if (cashReportState.filterType != CashReportTypeFilter.ALL) {
                        filters.add("Type: ${cashReportState.filterType.name.replace("_", " ")}")
                    }
                    
                    if (cashReportState.filterDateFrom.isNotBlank() || cashReportState.filterDateTo.isNotBlank()) {
                        val dateRange = when {
                            cashReportState.filterDateFrom.isNotBlank() && cashReportState.filterDateTo.isNotBlank() -> 
                                "Date: ${cashReportState.filterDateFrom} to ${cashReportState.filterDateTo}"
                            cashReportState.filterDateFrom.isNotBlank() -> 
                                "Date: From ${cashReportState.filterDateFrom}"
                            else -> 
                                "Date: Until ${cashReportState.filterDateTo}"
                        }
                        filters.add(dateRange)
                    }
                    
                    if (cashReportState.sortBy != CashReportSortField.DATE || cashReportState.sortDirection != SortDirection.DESCENDING) {
                        filters.add("Sort: ${cashReportState.sortBy.name.lowercase().replaceFirstChar { it.uppercase() }} (${cashReportState.sortDirection.name.lowercase()})")
                    }
                    
                    if (filters.isEmpty()) {
                        append("All transactions")
                    } else {
                        append(filters.joinToString(", "))
                    }
                }
                
                val pdfBytes = pdfService.generateCashReportPdf(
                    transactions = filteredReports,
                    categories = cashReportState.categories,
                    summary = filteredSummary,
                    filterInfo = filterInfo
                )
                
                Result.success(pdfBytes)
            } catch (e: Exception) {
                Result.failure(Exception("Failed to generate PDF: ${e.message}", e))
            }
        }
    }
}
