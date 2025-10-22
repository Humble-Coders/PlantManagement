package com.humblecoders.plantmanagement.repositories

import com.google.cloud.firestore.Firestore
import com.humblecoders.plantmanagement.data.PendingBill
import com.humblecoders.plantmanagement.data.PendingBillStatus
import com.humblecoders.plantmanagement.data.PendingBillTransaction
import com.humblecoders.plantmanagement.data.PendingBillTransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class PendingBillRepository(
    private val firestore: Firestore,
    private val userId: String,
    private val appId: String
) {

    private fun getPendingBillsCollection() =
        firestore.collection("pendingBills")

    private fun getPendingBillTransactionsCollection() =
        firestore.collection("pendingBillTransactions")

    private fun getEntitiesCollection() =
        firestore.collection("customers")

    private fun getInventoryCollection() =
        firestore.collection("inventory")

    /**
     * Add a new pending bill using transaction for atomicity
     */
    suspend fun addPendingBill(pendingBill: PendingBill): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            println("Starting to add pending bill to Firebase: ${pendingBill.billNumber}")
            val docRef = getPendingBillsCollection().document()
            
            // Use transaction to ensure atomic write
            firestore.runTransaction { transaction ->
                // First, do all reads before any writes
                var inventoryRef: com.google.cloud.firestore.DocumentReference? = null
                var currentQuantity = 0.0
                
                if (pendingBill.deductFromInventory) {
                    // Get inventory document reference first
                    val inventoryQuery = getInventoryCollection()
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("name", "Fortified Rice")
                    
                    val inventorySnapshot = inventoryQuery.get().get(10, TimeUnit.SECONDS)
                    if (inventorySnapshot.documents.isNotEmpty()) {
                        inventoryRef = inventorySnapshot.documents[0].reference
                        val inventoryDoc = transaction.get(inventoryRef).get()
                        if (inventoryDoc.exists()) {
                            currentQuantity = inventoryDoc.getDouble("quantity") ?: 0.0
                        }
                    }
                }
                
                // Now do all writes
                val pendingBillData = mapOf(
                    "userId" to userId,
                    "customerId" to pendingBill.customerId,
                    "firmName" to pendingBill.firmName,
                    "billDate" to pendingBill.billDate,
                    "billNumber" to pendingBill.billNumber,
                    "portalBatchNumber" to pendingBill.portalBatchNumber,
                    "quantityKg" to pendingBill.quantityKg,
                    "numberOfBags" to pendingBill.numberOfBags,
                    "deductFromInventory" to pendingBill.deductFromInventory,
                    "originalRatePerKg" to pendingBill.originalRatePerKg,
                    "portalAmount" to pendingBill.portalAmount,
                    "gstAmount" to pendingBill.gstAmount,
                    "totalPortalAmount" to pendingBill.totalPortalAmount,
                    "discountType" to pendingBill.discountType.name,
                    "discountedRatePerKg" to pendingBill.discountedRatePerKg,
                    "extraQuantityKg" to pendingBill.extraQuantityKg,
                    "revenueAmount" to pendingBill.revenueAmount,
                    "totalRevenueAmount" to pendingBill.totalRevenueAmount,
                    "differenceAmount" to pendingBill.differenceAmount,
                    "portalAmountPaid" to pendingBill.portalAmountPaid,
                    "differenceAmountPaid" to pendingBill.differenceAmountPaid,
                    "truckNumber" to pendingBill.truckNumber,
                    "fareAmount" to pendingBill.fareAmount,
                    "farePaidBy" to pendingBill.farePaidBy.name,
                    "notes" to pendingBill.notes,
                    "imageUrls" to pendingBill.imageUrls,
                    "status" to pendingBill.status.name,
                    "clearedQuantity" to pendingBill.clearedQuantity,
                    "createdAt" to com.google.cloud.Timestamp.now()
                )
                
                transaction.set(docRef, pendingBillData)

                // Update inventory if needed
                if (pendingBill.deductFromInventory && inventoryRef != null) {
                    val newQuantity = currentQuantity - pendingBill.quantityKg
                    
                    if (newQuantity >= 0) {
                        transaction.update(inventoryRef, "quantity", newQuantity)
                    } else {
                        throw Exception("Insufficient inventory. Available: ${currentQuantity} kg, Required: ${pendingBill.quantityKg} kg")
                    }
                }

                // Create transaction history entry
                val transactionRef = getPendingBillTransactionsCollection().document()
                val transactionData = mapOf(
                    "pendingBillId" to docRef.id,
                    "userId" to userId,
                    "transactionType" to PendingBillTransactionType.CREATED.name,
                    "quantityCleared" to 0.0,
                    "clearedBy" to userId,
                    "notes" to "Pending bill created",
                    "createdAt" to com.google.cloud.Timestamp.now()
                )
                transaction.set(transactionRef, transactionData)
                
                null
            }.get(10, TimeUnit.SECONDS)

            println("Pending bill successfully added to Firebase with ID: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            println("Error adding pending bill to Firebase: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Get all pending bills for the user
     */
    suspend fun getAllPendingBills(): Result<List<PendingBill>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = getPendingBillsCollection()
                .whereEqualTo("userId", userId)
                .get()
                .get(10, TimeUnit.SECONDS)

            val pendingBills = snapshot.documents.mapNotNull { doc ->
                try {
                    PendingBill(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        customerId = doc.getString("customerId") ?: "",
                        firmName = doc.getString("firmName") ?: "",
                        billDate = doc.getString("billDate") ?: "",
                        billNumber = doc.getString("billNumber") ?: "",
                        portalBatchNumber = doc.getString("portalBatchNumber") ?: "",
                        quantityKg = doc.getDouble("quantityKg") ?: 0.0,
                        numberOfBags = doc.getLong("numberOfBags")?.toInt() ?: 0,
                        deductFromInventory = doc.getBoolean("deductFromInventory") ?: true,
                        originalRatePerKg = doc.getDouble("originalRatePerKg") ?: 0.0,
                        portalAmount = doc.getDouble("portalAmount") ?: 0.0,
                        gstAmount = doc.getDouble("gstAmount") ?: 0.0,
                        totalPortalAmount = doc.getDouble("totalPortalAmount") ?: 0.0,
                        discountType = com.humblecoders.plantmanagement.data.DiscountType.valueOf(
                            doc.getString("discountType") ?: "NONE"
                        ),
                        discountedRatePerKg = doc.getDouble("discountedRatePerKg") ?: 0.0,
                        extraQuantityKg = doc.getDouble("extraQuantityKg") ?: 0.0,
                        revenueAmount = doc.getDouble("revenueAmount") ?: 0.0,
                        totalRevenueAmount = doc.getDouble("totalRevenueAmount") ?: 0.0,
                        differenceAmount = doc.getDouble("differenceAmount") ?: 0.0,
                        portalAmountPaid = doc.getDouble("portalAmountPaid") ?: 0.0,
                        differenceAmountPaid = doc.getDouble("differenceAmountPaid") ?: 0.0,
                        truckNumber = doc.getString("truckNumber") ?: "",
                        fareAmount = doc.getDouble("fareAmount") ?: 0.0,
                        farePaidBy = com.humblecoders.plantmanagement.data.FarePaidBy.valueOf(
                            doc.getString("farePaidBy") ?: "COMPANY"
                        ),
                        notes = doc.getString("notes") ?: "",
                        imageUrls = (doc.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        status = PendingBillStatus.valueOf(doc.getString("status") ?: "PENDING_BILLED"),
                        clearedQuantity = doc.getDouble("clearedQuantity") ?: 0.0,
                        createdAt = doc.getTimestamp("createdAt")
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.createdAt?.seconds ?: 0L }

            Result.success(pendingBills)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clear a pending bill (partial or full)
     */
    suspend fun clearBill(pendingBillId: String, clearedQuantity: Double): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            firestore.runTransaction { transaction ->
                val pendingBillRef = getPendingBillsCollection().document(pendingBillId)
                val pendingBillDoc = transaction.get(pendingBillRef).get()
                
                if (!pendingBillDoc.exists()) {
                    throw Exception("Pending bill not found")
                }
                
                val currentClearedQuantity = pendingBillDoc.getDouble("clearedQuantity") ?: 0.0
                val totalQuantity = pendingBillDoc.getDouble("quantityKg") ?: 0.0
                val newClearedQuantity = currentClearedQuantity + clearedQuantity
                
                // Check if we're trying to clear more than the total quantity
                if (newClearedQuantity > totalQuantity) {
                    throw Exception("Cannot clear more than total quantity")
                }
                
                // Update cleared quantity
                transaction.update(pendingBillRef, "clearedQuantity", newClearedQuantity)
                
                // If cleared quantity equals total quantity, mark as billed
                if (newClearedQuantity >= totalQuantity) {
                    transaction.update(pendingBillRef, "status", PendingBillStatus.BILLED.name)
                }
                
                // Create transaction history entry
                val transactionRef = getPendingBillTransactionsCollection().document()
                val transactionData = mapOf(
                    "pendingBillId" to pendingBillId,
                    "userId" to userId,
                    "transactionType" to if (newClearedQuantity >= totalQuantity) {
                        PendingBillTransactionType.FULLY_CLEARED.name
                    } else {
                        PendingBillTransactionType.PARTIALLY_CLEARED.name
                    },
                    "quantityCleared" to clearedQuantity,
                    "clearedBy" to userId,
                    "notes" to "Bill cleared: ${clearedQuantity} kg",
                    "createdAt" to com.google.cloud.Timestamp.now()
                )
                transaction.set(transactionRef, transactionData)
                
                null
            }.get(10, TimeUnit.SECONDS)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a pending bill
     */
    suspend fun deletePendingBill(pendingBillId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            firestore.runTransaction { transaction ->
                val pendingBillRef = getPendingBillsCollection().document(pendingBillId)
                val pendingBillDoc = transaction.get(pendingBillRef).get()
                
                if (!pendingBillDoc.exists()) {
                    throw Exception("Pending bill not found")
                }
                
                // Check if inventory was deducted and restore it
                val deductFromInventory = pendingBillDoc.getBoolean("deductFromInventory") ?: false
                if (deductFromInventory) {
                    val quantityKg = pendingBillDoc.getDouble("quantityKg") ?: 0.0
                    
                    val fortifiedRiceInventoryRef = getInventoryCollection()
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("name", "Fortified Rice")
                        .get()
                        .get(10, TimeUnit.SECONDS)

                    if (fortifiedRiceInventoryRef.documents.isNotEmpty()) {
                        val inventoryRef = fortifiedRiceInventoryRef.documents[0].reference
                        val inventoryDoc = transaction.get(inventoryRef).get()
                        
                        if (inventoryDoc.exists()) {
                            val currentQuantity = inventoryDoc.getDouble("quantity") ?: 0.0
                            val newQuantity = currentQuantity + quantityKg
                            transaction.update(inventoryRef, "quantity", newQuantity)
                        }
                    }
                }
                
                // Delete the pending bill
                transaction.delete(pendingBillRef)
                
                // Delete related transaction history
                val transactionsSnapshot = getPendingBillTransactionsCollection()
                    .whereEqualTo("pendingBillId", pendingBillId)
                    .get()
                    .get(10, TimeUnit.SECONDS)
                
                transactionsSnapshot.documents.forEach { doc ->
                    transaction.delete(doc.reference)
                }
                
                null
            }.get(10, TimeUnit.SECONDS)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get pending bill by ID
     */
    suspend fun getPendingBillById(pendingBillId: String): Result<PendingBill?> = withContext(Dispatchers.IO) {
        return@withContext try {
            val doc = getPendingBillsCollection()
                .document(pendingBillId)
                .get()
                .get(10, TimeUnit.SECONDS)

            if (!doc.exists()) return@withContext Result.success(null)

            val pendingBill = PendingBill(
                id = doc.id,
                userId = doc.getString("userId") ?: "",
                customerId = doc.getString("customerId") ?: "",
                firmName = doc.getString("firmName") ?: "",
                billDate = doc.getString("billDate") ?: "",
                billNumber = doc.getString("billNumber") ?: "",
                portalBatchNumber = doc.getString("portalBatchNumber") ?: "",
                quantityKg = doc.getDouble("quantityKg") ?: 0.0,
                numberOfBags = doc.getLong("numberOfBags")?.toInt() ?: 0,
                deductFromInventory = doc.getBoolean("deductFromInventory") ?: true,
                originalRatePerKg = doc.getDouble("originalRatePerKg") ?: 0.0,
                portalAmount = doc.getDouble("portalAmount") ?: 0.0,
                gstAmount = doc.getDouble("gstAmount") ?: 0.0,
                totalPortalAmount = doc.getDouble("totalPortalAmount") ?: 0.0,
                discountType = com.humblecoders.plantmanagement.data.DiscountType.valueOf(
                    doc.getString("discountType") ?: "NONE"
                ),
                discountedRatePerKg = doc.getDouble("discountedRatePerKg") ?: 0.0,
                extraQuantityKg = doc.getDouble("extraQuantityKg") ?: 0.0,
                revenueAmount = doc.getDouble("revenueAmount") ?: 0.0,
                totalRevenueAmount = doc.getDouble("totalRevenueAmount") ?: 0.0,
                differenceAmount = doc.getDouble("differenceAmount") ?: 0.0,
                portalAmountPaid = doc.getDouble("portalAmountPaid") ?: 0.0,
                differenceAmountPaid = doc.getDouble("differenceAmountPaid") ?: 0.0,
                truckNumber = doc.getString("truckNumber") ?: "",
                fareAmount = doc.getDouble("fareAmount") ?: 0.0,
                farePaidBy = com.humblecoders.plantmanagement.data.FarePaidBy.valueOf(
                    doc.getString("farePaidBy") ?: "COMPANY"
                ),
                notes = doc.getString("notes") ?: "",
                imageUrls = (doc.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                status = PendingBillStatus.valueOf(doc.getString("status") ?: "PENDING_BILLED"),
                clearedQuantity = doc.getDouble("clearedQuantity") ?: 0.0,
                createdAt = doc.getTimestamp("createdAt")
            )

            Result.success(pendingBill)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get transaction history for a pending bill
     */
    suspend fun getPendingBillTransactions(pendingBillId: String): Result<List<PendingBillTransaction>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = getPendingBillTransactionsCollection()
                .whereEqualTo("pendingBillId", pendingBillId)
                .orderBy("createdAt")
                .get()
                .get(10, TimeUnit.SECONDS)

            val transactions = snapshot.documents.mapNotNull { doc ->
                try {
                    PendingBillTransaction(
                        id = doc.id,
                        pendingBillId = doc.getString("pendingBillId") ?: "",
                        userId = doc.getString("userId") ?: "",
                        transactionType = PendingBillTransactionType.valueOf(
                            doc.getString("transactionType") ?: "CREATED"
                        ),
                        quantityCleared = doc.getDouble("quantityCleared") ?: 0.0,
                        clearedBy = doc.getString("clearedBy") ?: "",
                        notes = doc.getString("notes") ?: "",
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
