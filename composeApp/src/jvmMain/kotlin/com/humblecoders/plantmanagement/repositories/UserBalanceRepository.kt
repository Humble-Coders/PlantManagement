package com.humblecoders.plantmanagement.repositories

import com.google.cloud.firestore.Firestore
import com.humblecoders.plantmanagement.data.UserBalance
import com.humblecoders.plantmanagement.data.BalanceTransfer
import com.humblecoders.plantmanagement.data.BalanceTransferType
import com.humblecoders.plantmanagement.data.UserCashOutTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class UserBalanceRepository(
    private val firestore: Firestore,
    private val userId: String,
    private val appId: String
) {
    
    private fun getUserBalancesCollection() = firestore.collection("user_balances")
    private fun getBalanceTransfersCollection() = firestore.collection("balance_transfers")
    private fun getUserCashOutTransactionsCollection() = firestore.collection("user_cash_out_transactions")
    private fun getUsersCollection() = firestore.collection("users")

    /**
     * Get the shared user balance
     */
    suspend fun getSharedUserBalance(): Result<UserBalance> = withContext(Dispatchers.IO) {
        return@withContext try {
            val doc = getUserBalancesCollection()
                .document("shared_balance")
                .get()
                .get(10, TimeUnit.SECONDS)
            
            if (!doc.exists()) {
                // Create initial shared balance if doesn't exist
                val initialBalance = UserBalance(
                    id = "shared_balance",
                    currentBalance = 0.0,
                    createdAt = com.google.cloud.Timestamp.now(),
                    updatedAt = com.google.cloud.Timestamp.now()
                )
                return@withContext Result.success(initialBalance)
            }
            
            val balance = UserBalance(
                id = doc.id,
                currentBalance = doc.getDouble("currentBalance") ?: 0.0,
                createdAt = doc.getTimestamp("createdAt"),
                updatedAt = doc.getTimestamp("updatedAt")
            )
            
            Result.success(balance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Transfer balance from admin to shared user balance (only admin can do this)
     */
    suspend fun transferBalanceToSharedUserBalance(
        amount: Double,
        transferType: BalanceTransferType,
        notes: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val transferDocRef = getBalanceTransfersCollection().document()
            val sharedBalanceRef = getUserBalancesCollection().document("shared_balance")
            
            firestore.runTransaction { transaction ->
                // Get shared balance first (all reads must be done before writes)
                val sharedBalanceDoc = transaction.get(sharedBalanceRef).get()
                
                val currentBalance = if (sharedBalanceDoc.exists()) {
                    sharedBalanceDoc.getDouble("currentBalance") ?: 0.0
                } else {
                    0.0
                }
                
                // Calculate new balance
                val newBalance = when (transferType) {
                    BalanceTransferType.ADD -> currentBalance + amount
                    BalanceTransferType.DEDUCT -> currentBalance - amount
                }
                
                // Now do all writes
                if (!sharedBalanceDoc.exists()) {
                    // Create initial shared balance
                    val initialBalance = mapOf<String, Any>(
                        "currentBalance" to newBalance,
                        "createdAt" to com.google.cloud.Timestamp.now(),
                        "updatedAt" to com.google.cloud.Timestamp.now()
                    )
                    transaction.set(sharedBalanceRef, initialBalance)
                } else {
                    // Update existing shared balance
                    transaction.update(sharedBalanceRef, mapOf(
                        "currentBalance" to newBalance,
                        "updatedAt" to com.google.cloud.Timestamp.now()
                    ))
                }
                
                // Create transfer record
                val transferData = mapOf<String, Any>(
                    "fromAdminId" to userId,
                    "fromAdminEmail" to "", // Will be filled from user data
                    "amount" to amount,
                    "transferType" to transferType.name,
                    "notes" to notes,
                    "createdAt" to com.google.cloud.Timestamp.now()
                )
                
                transaction.set(transferDocRef, transferData)
                null
            }.get(20, TimeUnit.SECONDS)
            
            Result.success(transferDocRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Process cash out transaction using shared user balance
     */
    suspend fun processUserCashOutTransaction(
        amount: Double,
        notes: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val transactionDocRef = getUserCashOutTransactionsCollection().document()
            val sharedBalanceRef = getUserBalancesCollection().document("shared_balance")
            
            firestore.runTransaction { transaction ->
                // Get shared balance
                val sharedBalanceDoc = transaction.get(sharedBalanceRef).get()
                
                if (!sharedBalanceDoc.exists()) {
                    throw Exception("Shared user balance not found")
                }
                
                val currentBalance = sharedBalanceDoc.getDouble("currentBalance") ?: 0.0
                
                // Allow negative balances - remove the insufficient balance check
                
                val newBalance = currentBalance - amount
                
                // Update shared balance
                transaction.update(sharedBalanceRef, mapOf(
                    "currentBalance" to newBalance,
                    "updatedAt" to com.google.cloud.Timestamp.now()
                ))
                
                // Create transaction record
                val transactionData = mapOf<String, Any>(
                    "userId" to userId,
                    "userEmail" to "", // Will be filled from user data
                    "userName" to "", // Will be filled from user data
                    "amount" to amount,
                    "notes" to notes,
                    "previousBalance" to currentBalance,
                    "newBalance" to newBalance,
                    "createdAt" to com.google.cloud.Timestamp.now()
                )
                
                transaction.set(transactionDocRef, transactionData)
                null
            }.get(20, TimeUnit.SECONDS)
            
            Result.success(transactionDocRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all balance transfers (admin only)
     */
    suspend fun getAllBalanceTransfers(): Result<List<BalanceTransfer>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = getBalanceTransfersCollection()
                .orderBy("createdAt", com.google.cloud.firestore.Query.Direction.DESCENDING)
                .get()
                .get(15, TimeUnit.SECONDS)
            
            val transfers = snapshot.documents.mapNotNull { doc ->
                try {
                    BalanceTransfer(
                        id = doc.id,
                        fromAdminId = doc.getString("fromAdminId") ?: "",
                        fromAdminEmail = doc.getString("fromAdminEmail") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        transferType = BalanceTransferType.valueOf(doc.getString("transferType") ?: "ADD"),
                        notes = doc.getString("notes") ?: "",
                        createdAt = doc.getTimestamp("createdAt")
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            Result.success(transfers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user's cash out transaction history
     */
    suspend fun getUserCashOutTransactions(targetUserId: String? = null): Result<List<UserCashOutTransaction>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val query = if (targetUserId != null) {
                getUserCashOutTransactionsCollection().whereEqualTo("userId", targetUserId)
            } else {
                getUserCashOutTransactionsCollection()
            }
            
            val snapshot = query
                .orderBy("createdAt", com.google.cloud.firestore.Query.Direction.DESCENDING)
                .get()
                .get(15, TimeUnit.SECONDS)
            
            val transactions = snapshot.documents.mapNotNull { doc ->
                try {
                    UserCashOutTransaction(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        userEmail = doc.getString("userEmail") ?: "",
                        userName = doc.getString("userName") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        notes = doc.getString("notes") ?: "",
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

    /**
     * Listen to shared user balance changes
     */
    fun listenToSharedUserBalance(onBalanceChanged: (UserBalance?) -> Unit) {
        getUserBalancesCollection()
            .document("shared_balance")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Error listening to shared user balance: ${error.message}")
                    onBalanceChanged(null)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val balance = UserBalance(
                        id = snapshot.id,
                        currentBalance = snapshot.getDouble("currentBalance") ?: 0.0,
                        createdAt = snapshot.getTimestamp("createdAt"),
                        updatedAt = snapshot.getTimestamp("updatedAt")
                    )
                    onBalanceChanged(balance)
                } else {
                    // Create initial shared balance if doesn't exist
                    val initialBalance = UserBalance(
                        id = "shared_balance",
                        currentBalance = 0.0,
                        createdAt = com.google.cloud.Timestamp.now(),
                        updatedAt = com.google.cloud.Timestamp.now()
                    )
                    onBalanceChanged(initialBalance)
                }
            }
    }

    /**
     * Listen to balance transfers
     */
    fun listenToBalanceTransfers(onTransfersChanged: (List<BalanceTransfer>) -> Unit) {
        getBalanceTransfersCollection()
            .orderBy("createdAt", com.google.cloud.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Error listening to balance transfers: ${error.message}")
                    onTransfersChanged(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val transfers = snapshot.documents.mapNotNull { doc ->
                        try {
                            BalanceTransfer(
                                id = doc.id,
                                fromAdminId = doc.getString("fromAdminId") ?: "",
                                fromAdminEmail = doc.getString("fromAdminEmail") ?: "",
                                amount = doc.getDouble("amount") ?: 0.0,
                                transferType = BalanceTransferType.valueOf(doc.getString("transferType") ?: "ADD"),
                                notes = doc.getString("notes") ?: "",
                                createdAt = doc.getTimestamp("createdAt")
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onTransfersChanged(transfers)
                }
            }
    }

    /**
     * Listen to user cash out transactions
     */
    fun listenToUserCashOutTransactions(targetUserId: String? = null, onTransactionsChanged: (List<UserCashOutTransaction>) -> Unit) {
        val query = if (targetUserId != null) {
            getUserCashOutTransactionsCollection().whereEqualTo("userId", targetUserId)
        } else {
            getUserCashOutTransactionsCollection()
        }

        query.orderBy("createdAt", com.google.cloud.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Error listening to user cash out transactions: ${error.message}")
                    onTransactionsChanged(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val transactions = snapshot.documents.mapNotNull { doc ->
                        try {
                            UserCashOutTransaction(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                userEmail = doc.getString("userEmail") ?: "",
                                userName = doc.getString("userName") ?: "",
                                amount = doc.getDouble("amount") ?: 0.0,
                                notes = doc.getString("notes") ?: "",
                                previousBalance = doc.getDouble("previousBalance") ?: 0.0,
                                newBalance = doc.getDouble("newBalance") ?: 0.0,
                                createdAt = doc.getTimestamp("createdAt")
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onTransactionsChanged(transactions)
                }
            }
    }
}
