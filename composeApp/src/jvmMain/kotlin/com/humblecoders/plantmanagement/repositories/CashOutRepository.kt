package com.humblecoders.plantmanagement.repositories

import com.google.cloud.firestore.Firestore
import com.humblecoders.plantmanagement.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class CashOutRepository(
    private val firestore: Firestore,
    private val userId: String,
    private val appId: String
) {

    private fun getCashOutCollection() = firestore.collection("cash_out")
    private fun getPurchasesCollection() = firestore.collection("purchases")
    private fun getEntitiesCollection() = firestore.collection("customers")
    private fun getUserBalancesCollection() = firestore.collection("user_balances")
    private fun getUserCashOutTransactionsCollection() = firestore.collection("user_cash_out_transactions")

    /**
     * Process cash out transaction with Firebase transaction for atomicity
     * This ensures all purchases and entity balances are updated atomically
     */
    suspend fun processCashOut(
        cashOutAmount: Double,
        purchaseAllocations: List<PurchaseAllocation>,
        notes: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val cashOutDocRef = getCashOutCollection().document()
            
            // Use Firestore transaction to ensure atomicity
            firestore.runTransaction { transaction ->
                // IMPORTANT: All reads must come before any writes in Firestore transactions
                
                // Read all purchases first
                val purchaseFutures = purchaseAllocations.map { allocation ->
                    val purchaseRef = getPurchasesCollection().document(allocation.purchaseId)
                    Pair(allocation, purchaseRef to transaction.get(purchaseRef))
                }
                
                // Group by customer ID and prepare entity reads
                val customerIds = purchaseAllocations.map { it.customerId }.distinct()
                val entityFutures = customerIds.map { customerId ->
                    val entityRef = getEntitiesCollection().document(customerId)
                    customerId to Pair(entityRef, transaction.get(entityRef))
                }
                
                // Resolve all purchase futures
                val purchaseData = purchaseFutures.map { (allocation, refAndFuture) ->
                    val (ref, future) = refAndFuture
                    Triple(allocation, ref, future.get())
                }
                
                // Resolve all entity futures
                val entityData = entityFutures.map { (customerId, refAndFuture) ->
                    val (ref, future) = refAndFuture
                    Triple(customerId, ref, future.get())
                }
                
                // Now perform all writes
                
                // Update purchases
                purchaseData.forEach { (allocation, purchaseRef, purchaseDoc) ->
                    if (purchaseDoc.exists()) {
                        val updateData = mutableMapOf<String, Any>(
                            "amountPaid" to allocation.newAmountPaid,
                            "paymentStatus" to allocation.newPaymentStatus.name
                        )
                        transaction.update(purchaseRef, updateData)
                    }
                }
                
                // Update entity balances (group allocations by customer)
                val customerAllocations = purchaseAllocations.groupBy { it.customerId }
                entityData.forEach { (customerId, entityRef, entityDoc) ->
                    if (entityDoc.exists()) {
                        val allocations = customerAllocations[customerId] ?: emptyList()
                        val totalAllocatedToCustomer = allocations.sumOf { it.allocatedAmount }
                        
                        // Customer balance increases by the allocated amount
                        val currentBalance = entityDoc.getDouble("balance") ?: 0.0
                        val newBalance = currentBalance + totalAllocatedToCustomer
                        transaction.update(entityRef, "balance", newBalance)
                    }
                }
                
                // Save cash out record
                val allocationsData = purchaseAllocations.map { allocation ->
                    mapOf(
                        "purchaseId" to allocation.purchaseId,
                        "firmName" to allocation.firmName,
                        "purchaseDate" to allocation.purchaseDate,
                        "customerId" to allocation.customerId,
                        "grandTotal" to allocation.grandTotal,
                        "allocatedAmount" to allocation.allocatedAmount,
                        "previousAmountPaid" to allocation.previousAmountPaid,
                        "newAmountPaid" to allocation.newAmountPaid,
                        "previousPaymentStatus" to allocation.previousPaymentStatus.name,
                        "newPaymentStatus" to allocation.newPaymentStatus.name
                    )
                }
                
                val cashOutData = mapOf(
                    "userId" to userId,
                    "totalAmount" to cashOutAmount,
                    "purchaseAllocations" to allocationsData,
                    "notes" to notes,
                    "createdAt" to com.google.cloud.Timestamp.now()
                )
                
                transaction.set(cashOutDocRef, cashOutData)
                
                null
            }.get(20, TimeUnit.SECONDS)

            Result.success(cashOutDocRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all pending/partially paid purchases in chronological order for a specific customer
     */
    suspend fun getPendingPurchases(customerId: String? = null): List<Purchase> = withContext(Dispatchers.IO) {
        return@withContext try {
            val query = if (!customerId.isNullOrBlank()) {
                getPurchasesCollection().whereEqualTo("customerId", customerId)
            } else {
                getPurchasesCollection()
            }
            
            val snapshot = query.get().get(15, TimeUnit.SECONDS)
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    val paymentStatus = PaymentStatus.valueOf(
                        doc.getString("paymentStatus") ?: "PENDING"
                    )
                    val status = TransactionStatus.valueOf(
                        doc.getString("status") ?: "APPROVED"
                    )
                    
                    // Only include pending or partially paid purchases that are approved
                    if ((paymentStatus == PaymentStatus.PENDING || 
                         paymentStatus == PaymentStatus.PARTIALLY_PAID) &&
                        status == TransactionStatus.APPROVED) {
                        
                        val itemsList = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                        val purchaseItems = itemsList.map { itemMap ->
                            PurchaseItem(
                                inventoryItemId = itemMap["inventoryItemId"] as? String ?: "",
                                itemName = itemMap["itemName"] as? String ?: "",
                                quantity = (itemMap["quantity"] as? Number)?.toDouble() ?: 0.0,
                                unit = itemMap["unit"] as? String ?: "kg",
                                pricePerUnit = (itemMap["pricePerUnit"] as? Number)?.toDouble() ?: 0.0,
                                totalPrice = (itemMap["totalPrice"] as? Number)?.toDouble() ?: 0.0
                            )
                        }

                        Purchase(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            customerId = doc.getString("customerId") ?: "",
                            firmName = doc.getString("firmName") ?: "",
                            purchaseDate = doc.getString("purchaseDate") ?: "",
                            items = purchaseItems,
                            totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                            gstRate = doc.getDouble("gstRate") ?: 0.0,
                            gstAmount = doc.getDouble("gstAmount") ?: 0.0,
                            grandTotal = doc.getDouble("grandTotal") ?: doc.getDouble("totalAmount") ?: 0.0,
                            paymentStatus = paymentStatus,
                            amountPaid = doc.getDouble("amountPaid") ?: 0.0,
                            notes = doc.getString("notes") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            status = status,
                            reversedAt = doc.getTimestamp("reversedAt"),
                            reversalReason = doc.getString("reversalReason") ?: "",
                            createdAt = doc.getTimestamp("createdAt")
                        )
                    } else null
                } catch (e: Exception) {
                    println("Error parsing purchase: ${e.message}")
                    null
                }
            }.sortedBy { it.createdAt?.seconds ?: 0L } // Sort by creation time (chronological)
        } catch (e: Exception) {
            println("Error getting pending purchases: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Process cash out transaction using shared user balance
     * This is separate from company cash out transactions
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
     * Get shared user balance
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
     * Listen to cash out history in real-time
     */
    fun listenToCashOutHistory(onCashOutsChanged: (List<CashOut>) -> Unit) {
        if (userId.isBlank() || appId.isBlank()) {
            onCashOutsChanged(emptyList())
            return
        }

        getCashOutCollection()
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Error listening to cash outs: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val cashOuts = snapshot.documents.mapNotNull { doc ->
                        try {
                            val allocationsList = doc.get("purchaseAllocations") as? List<Map<String, Any>> ?: emptyList()
                            val allocations = allocationsList.map { allocationMap ->
                                PurchaseAllocation(
                                    purchaseId = allocationMap["purchaseId"] as? String ?: "",
                                    firmName = allocationMap["firmName"] as? String ?: "",
                                    purchaseDate = allocationMap["purchaseDate"] as? String ?: "",
                                    customerId = allocationMap["customerId"] as? String ?: "",
                                    grandTotal = (allocationMap["grandTotal"] as? Number)?.toDouble() ?: 0.0,
                                    allocatedAmount = (allocationMap["allocatedAmount"] as? Number)?.toDouble() ?: 0.0,
                                    previousAmountPaid = (allocationMap["previousAmountPaid"] as? Number)?.toDouble() ?: 0.0,
                                    newAmountPaid = (allocationMap["newAmountPaid"] as? Number)?.toDouble() ?: 0.0,
                                    previousPaymentStatus = PaymentStatus.valueOf(
                                        allocationMap["previousPaymentStatus"] as? String ?: "PENDING"
                                    ),
                                    newPaymentStatus = PaymentStatus.valueOf(
                                        allocationMap["newPaymentStatus"] as? String ?: "PENDING"
                                    )
                                )
                            }

                            CashOut(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                                purchaseAllocations = allocations,
                                notes = doc.getString("notes") ?: "",
                                createdAt = doc.getTimestamp("createdAt")
                            )
                        } catch (e: Exception) {
                            println("Error parsing cash out: ${e.message}")
                            null
                        }
                    }
                    onCashOutsChanged(cashOuts)
                }
            }
    }

    /**
     * Listen to user cash out transactions
     */
    fun listenToUserCashOutTransactions(onTransactionsChanged: (List<UserCashOutTransaction>) -> Unit) {
        if (userId.isBlank()) {
            onTransactionsChanged(emptyList())
            return
        }

        getUserCashOutTransactionsCollection()
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", com.google.cloud.firestore.Query.Direction.DESCENDING)
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

    /**
     * Listen to user balance changes
     */
    fun listenToUserBalance(onBalanceChanged: (UserBalance?) -> Unit) {
        if (userId.isBlank()) {
            onBalanceChanged(null)
            return
        }

        getUserBalancesCollection()
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Error listening to user balance: ${error.message}")
                    onBalanceChanged(null)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val doc = snapshot.documents.first()
                    val balance = UserBalance(
                        id = doc.id,
                        currentBalance = doc.getDouble("currentBalance") ?: 0.0,
                        createdAt = doc.getTimestamp("createdAt"),
                        updatedAt = doc.getTimestamp("updatedAt")
                    )
                    onBalanceChanged(balance)
                } else {
                    // Create initial balance if doesn't exist
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
}

