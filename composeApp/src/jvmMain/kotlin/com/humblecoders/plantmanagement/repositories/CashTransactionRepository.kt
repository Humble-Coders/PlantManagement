package com.humblecoders.plantmanagement.repositories

import com.google.cloud.firestore.Firestore
import com.humblecoders.plantmanagement.data.CashTransaction
import com.humblecoders.plantmanagement.data.CashTransactionType
import com.humblecoders.plantmanagement.data.Entity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class CashTransactionRepository(
    private val firestore: Firestore,
    private val userId: String,
    private val appId: String
) {

    private fun getCashTransactionsCollection() = firestore.collection("cash_transactions")
    private fun getEntitiesCollection() = firestore.collection("customers")

    /**
     * Process a cash transaction with Firebase transaction for atomicity
     * This ensures both the cash transaction record and customer balance are updated atomically
     */
    suspend fun processCashTransaction(
        customerId: String,
        amount: Double,
        transactionType: CashTransactionType,
        note: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val cashTransactionDocRef = getCashTransactionsCollection().document()
            val entityRef = getEntitiesCollection().document(customerId)
            
            // Use Firestore transaction to ensure atomicity
            firestore.runTransaction { transaction ->
                // Read customer data first
                val entityDoc = transaction.get(entityRef).get()
                
                if (!entityDoc.exists()) {
                    throw Exception("Customer not found")
                }
                
                val currentBalance = entityDoc.getDouble("balance") ?: 0.0
                val customerName = entityDoc.getString("firmName") ?: ""
                
                // Calculate new balance based on transaction type
                val newBalance = when (transactionType) {
                    CashTransactionType.RECEIVE -> currentBalance - amount  // Customer pays us (we owe them less, balance decreases)
                    CashTransactionType.GIVE -> currentBalance + amount      // We pay customer (they owe us more, balance increases)
                }
                
                // Update customer balance
                transaction.update(entityRef, "balance", newBalance)
                
                // Save cash transaction record
                val cashTransactionData = mapOf(
                    "userId" to userId,
                    "customerId" to customerId,
                    "customerName" to customerName,
                    "amount" to amount,
                    "transactionType" to transactionType.name,
                    "note" to note,
                    "previousBalance" to currentBalance,
                    "newBalance" to newBalance,
                    "createdAt" to com.google.cloud.Timestamp.now()
                )
                
                transaction.set(cashTransactionDocRef, cashTransactionData)
                
                null
            }.get(20, TimeUnit.SECONDS)

            Result.success(cashTransactionDocRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all cash transactions with optional filters
     * Simplified to avoid Firebase index requirements
     */
    suspend fun getCashTransactions(
        customerId: String? = null,
        transactionType: CashTransactionType? = null,
        startDate: com.google.cloud.Timestamp? = null,
        endDate: com.google.cloud.Timestamp? = null
    ): List<CashTransaction> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Simple query without complex filters to avoid index requirements
            val snapshot = getCashTransactionsCollection()
                .whereEqualTo("userId", userId)
                .get().get(15, TimeUnit.SECONDS)
            
            val allTransactions = snapshot.documents.mapNotNull { doc ->
                try {
                    CashTransaction(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        customerId = doc.getString("customerId") ?: "",
                        customerName = doc.getString("customerName") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        transactionType = CashTransactionType.valueOf(
                            doc.getString("transactionType") ?: "RECEIVE"
                        ),
                        note = doc.getString("note") ?: "",
                        previousBalance = doc.getDouble("previousBalance") ?: 0.0,
                        newBalance = doc.getDouble("newBalance") ?: 0.0,
                        createdAt = doc.getTimestamp("createdAt")
                    )
                } catch (e: Exception) {
                    println("Error parsing cash transaction: ${e.message}")
                    null
                }
            }
            
            // Apply filters in memory to avoid Firebase index requirements
            allTransactions
                .filter { transaction ->
                    // Filter by customer if specified
                    if (!customerId.isNullOrBlank()) {
                        transaction.customerId == customerId
                    } else true
                }
                .filter { transaction ->
                    // Filter by transaction type if specified
                    if (transactionType != null) {
                        transaction.transactionType == transactionType
                    } else true
                }
                .filter { transaction ->
                    // Filter by date range if specified
                    val createdAt = transaction.createdAt
                    when {
                        startDate != null && endDate != null -> 
                            createdAt != null && createdAt >= startDate && createdAt <= endDate
                        startDate != null -> 
                            createdAt != null && createdAt >= startDate
                        endDate != null -> 
                            createdAt != null && createdAt <= endDate
                        else -> true
                    }
                }
                .sortedByDescending { it.createdAt?.seconds ?: 0L } // Sort by creation time descending
        } catch (e: Exception) {
            println("Error getting cash transactions: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Listen to cash transaction history in real-time
     * Simplified to avoid Firebase index requirements
     */
    fun listenToCashTransactions(
        customerId: String? = null,
        transactionType: CashTransactionType? = null,
        onTransactionsChanged: (List<CashTransaction>) -> Unit
    ) {
        if (userId.isBlank() || appId.isBlank()) {
            onTransactionsChanged(emptyList())
            return
        }

        // Simple query without complex filters to avoid index requirements
        getCashTransactionsCollection()
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Error listening to cash transactions: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val allTransactions = snapshot.documents.mapNotNull { doc ->
                        try {
                            CashTransaction(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                customerId = doc.getString("customerId") ?: "",
                                customerName = doc.getString("customerName") ?: "",
                                amount = doc.getDouble("amount") ?: 0.0,
                                transactionType = CashTransactionType.valueOf(
                                    doc.getString("transactionType") ?: "RECEIVE"
                                ),
                                note = doc.getString("note") ?: "",
                                previousBalance = doc.getDouble("previousBalance") ?: 0.0,
                                newBalance = doc.getDouble("newBalance") ?: 0.0,
                                createdAt = doc.getTimestamp("createdAt")
                            )
                        } catch (e: Exception) {
                            println("Error parsing cash transaction: ${e.message}")
                            null
                        }
                    }
                    
                    // Apply filters in memory to avoid Firebase index requirements
                    val filteredTransactions = allTransactions
                        .filter { transaction ->
                            // Filter by customer if specified
                            if (!customerId.isNullOrBlank()) {
                                transaction.customerId == customerId
                            } else true
                        }
                        .filter { transaction ->
                            // Filter by transaction type if specified
                            if (transactionType != null) {
                                transaction.transactionType == transactionType
                            } else true
                        }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L } // Sort by creation time descending
                    
                    onTransactionsChanged(filteredTransactions)
                }
            }
    }
    
    /**
     * Get customer details for cash transaction preview
     */
    suspend fun getCustomerDetails(customerId: String): Result<Entity> = withContext(Dispatchers.IO) {
        return@withContext try {
            val doc = getEntitiesCollection().document(customerId).get().get(10, TimeUnit.SECONDS)
            
            if (!doc.exists()) {
                return@withContext Result.failure(Exception("Customer not found"))
            }
            
            val entity = Entity(
                id = doc.id,
                firmName = doc.getString("firmName") ?: "",
                contactPerson = doc.getString("contactPerson") ?: "",
                contactNo = doc.getString("contactNo") ?: "",
                city = doc.getString("city") ?: "",
                state = doc.getString("state") ?: "",
                gstin = doc.getString("gstin") ?: "",
                balance = doc.getDouble("balance") ?: 0.0,
                createdAt = doc.getTimestamp("createdAt")
            )
            
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get cash transactions by customer ID
     */
    suspend fun getCashTransactionsByCustomerId(customerId: String): Result<List<CashTransaction>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = getCashTransactionsCollection()
                .whereEqualTo("userId", userId)
                .whereEqualTo("customerId", customerId)
                .orderBy("createdAt", com.google.cloud.firestore.Query.Direction.DESCENDING)
                .get()
                .get(10, TimeUnit.SECONDS)

            val transactions = snapshot.documents.mapNotNull { doc ->
                try {
                    CashTransaction(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        customerId = doc.getString("customerId") ?: "",
                        customerName = doc.getString("customerName") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        transactionType = CashTransactionType.valueOf(doc.getString("transactionType") ?: "RECEIVE"),
                        note = doc.getString("note") ?: "",
                        previousBalance = doc.getDouble("previousBalance") ?: 0.0,
                        newBalance = doc.getDouble("newBalance") ?: 0.0,
                        createdAt = doc.getTimestamp("createdAt")
                    )
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
