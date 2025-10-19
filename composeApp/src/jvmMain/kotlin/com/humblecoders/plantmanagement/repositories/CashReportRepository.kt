package com.humblecoders.plantmanagement.repositories

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import com.google.cloud.firestore.Transaction
import com.google.cloud.firestore.WriteBatch
import com.google.cloud.Timestamp
import com.humblecoders.plantmanagement.data.CashReport
import com.humblecoders.plantmanagement.data.CashReportCategory
import com.humblecoders.plantmanagement.data.CashReportType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class CashReportRepository(
    private val firestore: Firestore
) {
    private val cashReportsCollection = firestore.collection("cashReports")
    private val cashReportCategoriesCollection = firestore.collection("cashReportCategories")

    /**
     * Add a new cash report transaction using Firebase transaction
     */
    suspend fun addCashReport(cashReport: CashReport): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val result = firestore.runTransaction { transaction ->
                    val docRef = cashReportsCollection.document()
                    val cashReportWithTimestamp = cashReport.copy(
                        id = docRef.id,
                        createdAt = Timestamp.now()
                    )
                    
                    transaction.set(docRef, cashReportWithTimestamp)
                    docRef.id
                }.get()
                
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(
                    Exception("Failed to add cash report: ${e.message}", e)
                )
            }
        }
    }

    /**
     * Get all cash report transactions with proper error handling
     */
    suspend fun getCashReports(): Result<List<CashReport>> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = cashReportsCollection
                    .orderBy("date", Query.Direction.DESCENDING)
                    .get()
                    .get()
                
                val cashReports = snapshot.documents.mapNotNull { document ->
                    try {
                        document.toObject(CashReport::class.java)?.copy(id = document.id)
                    } catch (e: Exception) {
                        println("Error parsing document ${document.id}: ${e.message}")
                        null
                    }
                }
                
                Result.success(cashReports)
            } catch (e: Exception) {
                Result.failure(
                    Exception("Failed to fetch cash reports: ${e.message}", e)
                )
            }
        }
    }

    /**
     * Listen to cash report transactions in real-time
     */
    fun listenToCashReports(onUpdate: (List<CashReport>) -> Unit) {
        cashReportsCollection
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Error listening to cash reports: ${error.message}")
                    return@addSnapshotListener
                }

                val cashReports = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(CashReport::class.java)?.copy(id = document.id)
                } ?: emptyList()

                onUpdate(cashReports)
            }
    }

    /**
     * Add a new category with proper error handling
     */
    suspend fun addCategory(category: CashReportCategory): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if category already exists (by name only)
                val existingCategoriesResult = getCategories()
                existingCategoriesResult.fold(
                    onSuccess = { categories ->
                        val existingCategory = categories.find { 
                            it.name.equals(category.name, ignoreCase = true) && it.type == category.type
                        }
                        
                        if (existingCategory != null) {
                            return@withContext Result.success(existingCategory.id)
                        }
                        
                        val docRef = cashReportCategoriesCollection.add(category.copy(
                            createdAt = Timestamp.now()
                        )).get()
                        Result.success(docRef.id)
                    },
                    onFailure = { error ->
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                Result.failure(
                    Exception("Failed to add category: ${e.message}", e)
                )
            }
        }
    }

    /**
     * Get all categories with proper error handling
     */
    suspend fun getCategories(): Result<List<CashReportCategory>> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = cashReportCategoriesCollection
                    .orderBy("name", Query.Direction.ASCENDING)
                    .get()
                    .get()
                
                val categories = snapshot.documents.mapNotNull { document ->
                    try {
                        document.toObject(CashReportCategory::class.java)?.copy(id = document.id)
                    } catch (e: Exception) {
                        println("Error parsing category document ${document.id}: ${e.message}")
                        null
                    }
                }
                
                Result.success(categories)
            } catch (e: Exception) {
                Result.failure(
                    Exception("Failed to fetch categories: ${e.message}", e)
                )
            }
        }
    }

    /**
     * Search categories by name with proper error handling
     */
    suspend fun searchCategories(query: String): Result<List<CashReportCategory>> {
        return withContext(Dispatchers.IO) {
            try {
                val categoriesResult = getCategories()
                categoriesResult.fold(
                    onSuccess = { categories ->
                        val filteredCategories = categories.filter { 
                            it.name.contains(query, ignoreCase = true) 
                        }
                        Result.success(filteredCategories)
                    },
                    onFailure = { error ->
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                Result.failure(
                    Exception("Failed to search categories: ${e.message}", e)
                )
            }
        }
    }

    /**
     * Get cash report summary with proper error handling
     */
    suspend fun getCashReportSummary(): Result<CashReportSummary> {
        return withContext(Dispatchers.IO) {
            try {
                val cashReportsResult = getCashReports()
                cashReportsResult.fold(
                    onSuccess = { cashReports ->
                        val totalCashIn = cashReports
                            .filter { it.transactionType == CashReportType.CASH_IN }
                            .sumOf { it.amount }
                        
                        val totalCashOut = cashReports
                            .filter { it.transactionType == CashReportType.CASH_OUT }
                            .sumOf { it.amount }
                        
                        val netChange = totalCashIn - totalCashOut
                        
                        val summary = CashReportSummary(
                            totalCashIn = totalCashIn,
                            totalCashOut = totalCashOut,
                            netChange = netChange
                        )
                        
                        Result.success(summary)
                    },
                    onFailure = { error ->
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                Result.failure(
                    Exception("Failed to calculate cash report summary: ${e.message}", e)
                )
            }
        }
    }

    /**
     * Update a category with proper error handling
     */
    suspend fun updateCategory(categoryId: String, newName: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if category exists
                val categoryDoc = cashReportCategoriesCollection.document(categoryId).get().get()
                if (!categoryDoc.exists()) {
                    return@withContext Result.failure(
                        Exception("Category with ID $categoryId not found")
                    )
                }
                
                // Check if another category with the same name exists
                val existingCategoriesResult = getCategories()
                existingCategoriesResult.fold(
                    onSuccess = { categories ->
                        // Get the existing category to check its type
                        val existingCategory = categories.find { it.id == categoryId }
                        if (existingCategory == null) {
                            return@withContext Result.failure(
                                Exception("Category with ID $categoryId not found")
                            )
                        }
                        
                        val conflictingCategory = categories.find { 
                            it.name.equals(newName, ignoreCase = true) && 
                            it.type == existingCategory.type && 
                            it.id != categoryId 
                        }
                        
                        if (conflictingCategory != null) {
                            return@withContext Result.failure(
                                Exception("A ${existingCategory.type.name.lowercase().replace("_", " ")} category with name '$newName' already exists")
                            )
                        }
                        
                        cashReportCategoriesCollection.document(categoryId)
                            .update("name", newName)
                            .get()
                        
                        Result.success(Unit)
                    },
                    onFailure = { error ->
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                Result.failure(
                    Exception("Failed to update category: ${e.message}", e)
                )
            }
        }
    }

    /**
     * Delete a category with proper error handling
     */
    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if category exists
                val categoryDoc = cashReportCategoriesCollection.document(categoryId).get().get()
                if (!categoryDoc.exists()) {
                    return@withContext Result.failure(
                        Exception("Category with ID $categoryId not found")
                    )
                }
                
                // Check if category is being used by any cash reports
                val cashReportsUsingCategory = cashReportsCollection
                    .whereEqualTo("categoryId", categoryId)
                    .get()
                    .get()
                
                if (!cashReportsUsingCategory.isEmpty) {
                    return@withContext Result.failure(
                        Exception("Cannot delete category '$categoryId' as it is being used by ${cashReportsUsingCategory.size()} cash report(s)")
                    )
                }
                
                cashReportCategoriesCollection.document(categoryId).delete().get()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(
                    Exception("Failed to delete category: ${e.message}", e)
                )
            }
        }
    }

    /**
     * Delete a cash report transaction with proper error handling
     */
    suspend fun deleteCashReport(cashReportId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if cash report exists
                val cashReportDoc = cashReportsCollection.document(cashReportId).get().get()
                if (!cashReportDoc.exists()) {
                    return@withContext Result.failure(
                        Exception("Cash report with ID $cashReportId not found")
                    )
                }
                
                cashReportsCollection.document(cashReportId).delete().get()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(
                    Exception("Failed to delete cash report: ${e.message}", e)
                )
            }
        }
    }
}

/**
 * Data class for cash report summary
 */
data class CashReportSummary(
    val totalCashIn: Double = 0.0,
    val totalCashOut: Double = 0.0,
    val netChange: Double = 0.0
)
