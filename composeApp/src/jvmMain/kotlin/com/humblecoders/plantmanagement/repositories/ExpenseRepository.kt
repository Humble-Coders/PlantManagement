// composeApp/src/jvmMain/kotlin/com/humblecoders/plantmanagement/repositories/ExpenseRepository.kt
package com.humblecoders.plantmanagement.repositories

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import com.google.cloud.Timestamp
import com.humblecoders.plantmanagement.data.Expense
import com.humblecoders.plantmanagement.data.ExpenseCategory
import com.humblecoders.plantmanagement.services.FirebaseStorageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExpenseRepository(
    private val firestore: Firestore,
    private val storageService: FirebaseStorageService
) {
    private val expensesCollection = firestore.collection("expenses")
    private val expenseCategoriesCollection = firestore.collection("expenseCategories")

    suspend fun addExpense(expense: Expense): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val result = firestore.runTransaction { transaction ->
                    val docRef = expensesCollection.document()
                    val expenseWithTimestamp = expense.copy(
                        id = docRef.id,
                        createdAt = Timestamp.now()
                    )

                    transaction.set(docRef, expenseWithTimestamp)
                    docRef.id
                }.get()

                Result.success(result)
            } catch (e: Exception) {
                Result.failure(
                    Exception("Failed to add expense: ${e.message}", e)
                )
            }
        }
    }

    suspend fun getExpenses(): Result<List<Expense>> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = expensesCollection
                    .orderBy("date", Query.Direction.DESCENDING)
                    .get()
                    .get()

                val expenses = snapshot.documents.mapNotNull { document ->
                    try {
                        document.toObject(Expense::class.java)?.copy(id = document.id)
                    } catch (e: Exception) {
                        println("Error parsing document ${document.id}: ${e.message}")
                        null
                    }
                }

                Result.success(expenses)
            } catch (e: Exception) {
                Result.failure(
                    Exception("Failed to fetch expenses: ${e.message}", e)
                )
            }
        }
    }

    suspend fun refreshExpenses(): Result<List<Expense>> {
        return getExpenses()
    }

    fun listenToExpenses(onUpdate: (List<Expense>) -> Unit) {
        expensesCollection
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Error listening to expenses: ${error.message}")
                    return@addSnapshotListener
                }

                val expenses = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(Expense::class.java)?.copy(id = document.id)
                } ?: emptyList()

                onUpdate(expenses)
            }
    }

    suspend fun addCategory(category: ExpenseCategory): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val existingCategoriesResult = getCategories()
                existingCategoriesResult.fold(
                    onSuccess = { categories ->
                        val existingCategory = categories.find {
                            it.name.equals(category.name, ignoreCase = true)
                        }

                        if (existingCategory != null) {
                            return@withContext Result.success(existingCategory.id)
                        }

                        val docRef = expenseCategoriesCollection.add(category.copy(
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

    suspend fun getCategories(): Result<List<ExpenseCategory>> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = expenseCategoriesCollection
                    .orderBy("name", Query.Direction.ASCENDING)
                    .get()
                    .get()

                val categories = snapshot.documents.mapNotNull { document ->
                    try {
                        document.toObject(ExpenseCategory::class.java)?.copy(id = document.id)
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

    suspend fun searchCategories(query: String): Result<List<ExpenseCategory>> {
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

    suspend fun getExpenseSummary(): Result<ExpenseSummary> {
        return withContext(Dispatchers.IO) {
            try {
                val expensesResult = getExpenses()
                expensesResult.fold(
                    onSuccess = { expenses ->
                        val totalExpenses = expenses.sumOf { it.amount }

                        val summary = ExpenseSummary(
                            totalExpenses = totalExpenses
                        )

                        Result.success(summary)
                    },
                    onFailure = { error ->
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                Result.failure(
                    Exception("Failed to calculate expense summary: ${e.message}", e)
                )
            }
        }
    }

    suspend fun updateCategory(categoryId: String, newName: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val categoryDoc = expenseCategoriesCollection.document(categoryId).get().get()
                if (!categoryDoc.exists()) {
                    return@withContext Result.failure(
                        Exception("Category with ID $categoryId not found")
                    )
                }

                val existingCategoriesResult = getCategories()
                existingCategoriesResult.fold(
                    onSuccess = { categories ->
                        val conflictingCategory = categories.find {
                            it.name.equals(newName, ignoreCase = true) && it.id != categoryId
                        }

                        if (conflictingCategory != null) {
                            return@withContext Result.failure(
                                Exception("A category with name '$newName' already exists")
                            )
                        }

                        expenseCategoriesCollection.document(categoryId)
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

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val categoryDoc = expenseCategoriesCollection.document(categoryId).get().get()
                if (!categoryDoc.exists()) {
                    return@withContext Result.failure(
                        Exception("Category with ID $categoryId not found")
                    )
                }

                val expensesUsingCategory = expensesCollection
                    .whereEqualTo("categoryId", categoryId)
                    .get()
                    .get()

                if (!expensesUsingCategory.isEmpty) {
                    return@withContext Result.failure(
                        Exception("Cannot delete category '$categoryId' as it is being used by ${expensesUsingCategory.size()} expense(s)")
                    )
                }

                expenseCategoriesCollection.document(categoryId).delete().get()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(
                    Exception("Failed to delete category: ${e.message}", e)
                )
            }
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val expenseDoc = expensesCollection.document(expenseId).get().get()
                if (!expenseDoc.exists()) {
                    return@withContext Result.failure(
                        Exception("Expense with ID $expenseId not found")
                    )
                }

                expensesCollection.document(expenseId).delete().get()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(
                    Exception("Failed to delete expense: ${e.message}", e)
                )
            }
        }
    }
    suspend fun deleteExpenseWithImage(expenseId: String, imageUrl: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val expenseDoc = expensesCollection.document(expenseId).get().get()
                if (!expenseDoc.exists()) {
                    return@withContext Result.failure(
                        Exception("Expense with ID $expenseId not found")
                    )
                }

                // Delete image from storage if exists
                if (imageUrl.isNotBlank()) {
                    storageService.deleteImage(imageUrl)
                }

                // Delete expense document
                expensesCollection.document(expenseId).delete().get()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(
                    Exception("Failed to delete expense: ${e.message}", e)
                )
            }
        }
    }
    
    suspend fun deleteExpenseWithDocuments(expenseId: String, documentUrls: List<String>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val expenseDoc = expensesCollection.document(expenseId).get().get()
                if (!expenseDoc.exists()) {
                    return@withContext Result.failure(
                        Exception("Expense with ID $expenseId not found")
                    )
                }

                // Delete documents from storage if they exist
                documentUrls.forEach { url ->
                    if (url.isNotBlank()) {
                        storageService.deleteImage(url) // This method handles both images and documents
                    }
                }

                // Delete expense document
                expensesCollection.document(expenseId).delete().get()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(
                    Exception("Failed to delete expense: ${e.message}", e)
                )
            }
        }
    }
}

data class ExpenseSummary(
    val totalExpenses: Double = 0.0
)