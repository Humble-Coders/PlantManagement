// composeApp/src/jvmMain/kotlin/com/humblecoders/plantmanagement/viewmodels/ExpenseViewModel.kt
package com.humblecoders.plantmanagement.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.cloud.Timestamp
import com.humblecoders.plantmanagement.data.Expense
import com.humblecoders.plantmanagement.data.ExpenseCategory
import com.humblecoders.plantmanagement.repositories.ExpenseRepository
import com.humblecoders.plantmanagement.repositories.ExpenseSummary
import com.humblecoders.plantmanagement.services.ExpensePdfService
import com.humblecoders.plantmanagement.services.FirebaseStorageService
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class ExpenseSortField {
    DATE,
    CATEGORY,
    AMOUNT
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

data class ExpenseState(
    val expenses: List<Expense> = emptyList(),
    val categories: List<ExpenseCategory> = emptyList(),
    val summary: ExpenseSummary = ExpenseSummary(),
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val isRetrying: Boolean = false,
    val isAddingCategory: Boolean = false,
    val isDeletingCategory: Boolean = false,
    val isDeletingExpense: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = "",
    val sortBy: ExpenseSortField = ExpenseSortField.DATE,
    val sortDirection: SortDirection = SortDirection.DESCENDING,
    val filterDateFrom: String = "",
    val filterDateTo: String = ""
)

class ExpenseViewModel(
    private val expenseRepository: ExpenseRepository,
    private val storageService: FirebaseStorageService // ADD THIS LINE

) : ViewModel() {

    var expenseState by mutableStateOf(ExpenseState())
        private set
    private val pdfService = ExpensePdfService()  // Initialize internally


    fun addExpense(
        categoryId: String,
        amount: Double,
        date: LocalDate,
        notes: String,
        documentFiles: List<File> = emptyList()
    ) {
        viewModelScope.launch {
            expenseState = expenseState.copy(isProcessing = true, error = null)

            // Upload documents if provided
            val documentUrls = mutableListOf<String>()
            if (documentFiles.isNotEmpty()) {
                for (file in documentFiles) {
                    val uploadResult = storageService.uploadDocument(file, "expenses")
                    uploadResult.fold(
                        onSuccess = { url -> documentUrls.add(url) },
                        onFailure = { error ->
                            expenseState = expenseState.copy(
                                isProcessing = false,
                                error = "Failed to upload document: ${error.message}"
                            )
                            return@launch
                        }
                    )
                }
            }

            val timestamp = Timestamp.of(java.util.Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()))

            val expense = Expense(
                categoryId = categoryId,
                amount = amount,
                date = timestamp,
                notes = notes,
                documentUrls = documentUrls
            )

            val result = expenseRepository.addExpense(expense)

            result.fold(
                onSuccess = { expenseId ->
                    expenseState = expenseState.copy(
                        isProcessing = false,
                        successMessage = "Expense added successfully"
                    )
                    loadExpenses()
                    loadSummary()
                },
                onFailure = { error ->
                    expenseState = expenseState.copy(
                        isProcessing = false,
                        error = error.message ?: "Failed to add expense"
                    )
                }
            )
        }
    }

    fun addCategory(categoryName: String) {
        viewModelScope.launch {
            // Prevent multiple simultaneous add operations
            if (expenseState.isAddingCategory) return@launch
            
            expenseState = expenseState.copy(
                isAddingCategory = true,
                error = null,
                successMessage = null
            )
            
            val category = ExpenseCategory(name = categoryName)
            val result = expenseRepository.addCategory(category)

            result.fold(
                onSuccess = { categoryId ->
                    expenseState = expenseState.copy(
                        isAddingCategory = false,
                        successMessage = "Category added successfully"
                    )
                    loadCategories()
                },
                onFailure = { error ->
                    expenseState = expenseState.copy(
                        isAddingCategory = false,
                        error = error.message ?: "Failed to add category"
                    )
                }
            )
        }
    }

    fun updateCategory(categoryId: String, newName: String) {
        viewModelScope.launch {
            val result = expenseRepository.updateCategory(categoryId, newName)

            result.fold(
                onSuccess = {
                    expenseState = expenseState.copy(
                        successMessage = "Category updated successfully"
                    )
                    loadCategories()
                },
                onFailure = { error ->
                    expenseState = expenseState.copy(
                        error = error.message ?: "Failed to update category"
                    )
                }
            )
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            // Prevent multiple simultaneous delete operations
            if (expenseState.isDeletingCategory) return@launch
            
            expenseState = expenseState.copy(
                isDeletingCategory = true,
                error = null,
                successMessage = null
            )
            
            val result = expenseRepository.deleteCategory(categoryId)

            result.fold(
                onSuccess = {
                    expenseState = expenseState.copy(
                        isDeletingCategory = false,
                        successMessage = "Category deleted successfully"
                    )
                    loadCategories()
                },
                onFailure = { error ->
                    expenseState = expenseState.copy(
                        isDeletingCategory = false,
                        error = error.message ?: "Failed to delete category"
                    )
                }
            )
        }
    }

    fun loadExpenses(retryCount: Int = 0) {
        viewModelScope.launch {
            if (retryCount == 0) {
                expenseState = expenseState.copy(isLoading = true, error = null, isRetrying = false)
            } else {
                expenseState = expenseState.copy(isRetrying = true, error = null)
            }

            val result = expenseRepository.getExpenses()
            result.fold(
                onSuccess = { expenses ->
                    expenseState = expenseState.copy(
                        isLoading = false,
                        isRetrying = false,
                        expenses = expenses
                    )
                },
                onFailure = { error ->
                    if (retryCount < 3) {
                        kotlinx.coroutines.delay(1000 * (retryCount + 1).toLong())
                        loadExpenses(retryCount + 1)
                    } else {
                        expenseState = expenseState.copy(
                            isLoading = false,
                            isRetrying = false,
                            error = "Failed to load expenses after 3 attempts: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun loadCategories(retryCount: Int = 0) {
        viewModelScope.launch {
            if (retryCount > 0) {
                expenseState = expenseState.copy(isRetrying = true, error = null)
            }

            val result = expenseRepository.getCategories()
            result.fold(
                onSuccess = { categories ->
                    expenseState = expenseState.copy(
                        isRetrying = false,
                        categories = categories
                    )
                },
                onFailure = { error ->
                    if (retryCount < 3) {
                        kotlinx.coroutines.delay(1000 * (retryCount + 1).toLong())
                        loadCategories(retryCount + 1)
                    } else {
                        expenseState = expenseState.copy(
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
            val result = expenseRepository.searchCategories(query)
            result.fold(
                onSuccess = { categories ->
                    expenseState = expenseState.copy(categories = categories)
                },
                onFailure = { error ->
                    expenseState = expenseState.copy(
                        error = error.message ?: "Failed to search categories"
                    )
                }
            )
        }
    }

    fun loadSummary(retryCount: Int = 0) {
        viewModelScope.launch {
            if (retryCount > 0) {
                expenseState = expenseState.copy(isRetrying = true, error = null)
            }

            val result = expenseRepository.getExpenseSummary()
            result.fold(
                onSuccess = { summary ->
                    expenseState = expenseState.copy(
                        isRetrying = false,
                        summary = summary
                    )
                },
                onFailure = { error ->
                    if (retryCount < 3) {
                        kotlinx.coroutines.delay(1000 * (retryCount + 1).toLong())
                        loadSummary(retryCount + 1)
                    } else {
                        expenseState = expenseState.copy(
                            isRetrying = false,
                            error = "Failed to load summary after 3 attempts: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun listenToExpenses() {
        expenseRepository.listenToExpenses { expenses ->
            expenseState = expenseState.copy(expenses = expenses)
            viewModelScope.launch {
                loadSummary()
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            // Prevent multiple simultaneous delete operations
            if (expenseState.isDeletingExpense) return@launch
            
            expenseState = expenseState.copy(
                isDeletingExpense = true,
                error = null,
                successMessage = null
            )

            // Find the expense to get its document URLs
            val expense = expenseState.expenses.find { it.id == expenseId }
            val documentUrls = expense?.documentUrls ?: emptyList()

            val result = if (documentUrls.isNotEmpty()) {
                expenseRepository.deleteExpenseWithDocuments(expenseId, documentUrls)
            } else {
                expenseRepository.deleteExpense(expenseId)
            }

            result.fold(
                onSuccess = {
                    expenseState = expenseState.copy(
                        isDeletingExpense = false,
                        successMessage = "Expense deleted successfully"
                    )
                    loadSummary()
                },
                onFailure = { error ->
                    expenseState = expenseState.copy(
                        isDeletingExpense = false,
                        error = error.message ?: "Failed to delete expense"
                    )
                }
            )
        }
    }

    fun clearMessages() {
        expenseState = expenseState.copy(
            error = null,
            successMessage = null
        )
    }

    fun showPdfSuccessMessage() {
        expenseState = expenseState.copy(
            successMessage = "PDF generated and opened successfully"
        )
    }

    fun getFilteredSummary(): ExpenseSummary {
        val filteredExpenses = getFilteredAndSortedExpenses()
        val totalExpenses = filteredExpenses.sumOf { it.amount }

        return ExpenseSummary(
            totalExpenses = totalExpenses
        )
    }

    fun updateSearchQuery(query: String) {
        expenseState = expenseState.copy(searchQuery = query)
    }

    fun updateSortBy(sortBy: ExpenseSortField) {
        expenseState = expenseState.copy(sortBy = sortBy)
    }

    fun toggleSortDirection() {
        expenseState = expenseState.copy(
            sortDirection = if (expenseState.sortDirection == SortDirection.ASCENDING) {
                SortDirection.DESCENDING
            } else {
                SortDirection.ASCENDING
            }
        )
    }

    fun updateDateFilter(from: String, to: String) {
        expenseState = expenseState.copy(
            filterDateFrom = from,
            filterDateTo = to
        )
    }

    fun getFilteredAndSortedExpenses(): List<Expense> {
        val query = expenseState.searchQuery.lowercase()

        val filtered = expenseState.expenses.filter { expense ->
            val categoryName = expenseState.categories.find { it.id == expense.categoryId }?.name ?: ""
            val textMatch = categoryName.lowercase().contains(query) ||
                    expense.notes.lowercase().contains(query)

            val dateMatch = if (expenseState.filterDateFrom.isNotBlank() || expenseState.filterDateTo.isNotBlank()) {
                val expenseDate = expense.date?.let {
                    java.time.LocalDate.ofInstant(
                        java.time.Instant.ofEpochMilli(it.seconds * 1000),
                        java.time.ZoneId.systemDefault()
                    )
                }

                val fromDate = if (expenseState.filterDateFrom.isNotBlank()) {
                    try {
                        java.time.LocalDate.parse(expenseState.filterDateFrom, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (e: Exception) {
                        java.time.LocalDate.MIN
                    }
                } else {
                    java.time.LocalDate.MIN
                }

                val toDate = if (expenseState.filterDateTo.isNotBlank()) {
                    try {
                        java.time.LocalDate.parse(expenseState.filterDateTo, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (e: Exception) {
                        java.time.LocalDate.MAX
                    }
                } else {
                    java.time.LocalDate.MAX
                }

                expenseDate != null && !expenseDate.isBefore(fromDate) && !expenseDate.isAfter(toDate)
            } else {
                true
            }

            textMatch && dateMatch
        }

        return when (expenseState.sortBy) {
            ExpenseSortField.DATE -> {
                if (expenseState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.date?.seconds ?: 0L }
                } else {
                    filtered.sortedByDescending { it.date?.seconds ?: 0L }
                }
            }
            ExpenseSortField.CATEGORY -> {
                if (expenseState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy {
                        expenseState.categories.find { cat -> cat.id == it.categoryId }?.name ?: ""
                    }
                } else {
                    filtered.sortedByDescending {
                        expenseState.categories.find { cat -> cat.id == it.categoryId }?.name ?: ""
                    }
                }
            }
            ExpenseSortField.AMOUNT -> {
                if (expenseState.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.amount }
                } else {
                    filtered.sortedByDescending { it.amount }
                }
            }
        }
    }

    suspend fun generatePdf(): Result<ByteArray> {
        return withContext(Dispatchers.IO) {
            try {
                val filteredExpenses = getFilteredAndSortedExpenses()
                val filteredSummary = getFilteredSummary()

                val filterInfo = buildString {
                    val filters = mutableListOf<String>()

                    if (expenseState.searchQuery.isNotBlank()) {
                        filters.add("Search: \"${expenseState.searchQuery}\"")
                    }

                    if (expenseState.filterDateFrom.isNotBlank() || expenseState.filterDateTo.isNotBlank()) {
                        val dateRange = when {
                            expenseState.filterDateFrom.isNotBlank() && expenseState.filterDateTo.isNotBlank() ->
                                "Date: ${expenseState.filterDateFrom} to ${expenseState.filterDateTo}"
                            expenseState.filterDateFrom.isNotBlank() ->
                                "Date: From ${expenseState.filterDateFrom}"
                            else ->
                                "Date: Until ${expenseState.filterDateTo}"
                        }
                        filters.add(dateRange)
                    }

                    if (expenseState.sortBy != ExpenseSortField.DATE || expenseState.sortDirection != SortDirection.DESCENDING) {
                        filters.add("Sort: ${expenseState.sortBy.name.lowercase().replaceFirstChar { it.uppercase() }} (${expenseState.sortDirection.name.lowercase()})")
                    }

                    if (filters.isEmpty()) {
                        append("All expenses")
                    } else {
                        append(filters.joinToString(", "))
                    }
                }

                val pdfBytes = pdfService.generateExpensePdf(
                    expenses = filteredExpenses,
                    categories = expenseState.categories,
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