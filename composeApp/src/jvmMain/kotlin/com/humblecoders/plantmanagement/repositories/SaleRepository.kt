package com.humblecoders.plantmanagement.repositories

import com.google.cloud.firestore.Firestore
import com.humblecoders.plantmanagement.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SaleRepository(
    private val firestore: Firestore,
    private val userId: String,
    private val appId: String
) {
    private fun getSalesCollection() = firestore.collection("sales")
    private fun getEntitiesCollection() = firestore.collection("customers")
    private fun getInventoryCollection() = firestore.collection("inventory")

    suspend fun addSale(sale: Sale): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val saleData = mapOf(
                "userId" to userId,
                "customerId" to sale.customerId,
                "firmName" to sale.firmName,
                "saleDate" to sale.saleDate,
                "billNumber" to sale.billNumber,
                "portalBatchNumber" to sale.portalBatchNumber,
                "quantityKg" to sale.quantityKg,
                "numberOfBags" to sale.numberOfBags,
                "deductFromInventory" to sale.deductFromInventory,
                "originalRatePerKg" to sale.originalRatePerKg,
                "portalAmount" to sale.portalAmount,
                "gstAmount" to sale.gstAmount,
                "totalPortalAmount" to sale.totalPortalAmount,
                "discountType" to sale.discountType.name,
                "discountedRatePerKg" to sale.discountedRatePerKg,
                "extraQuantityKg" to sale.extraQuantityKg,
                "revenueAmount" to sale.revenueAmount,
                "totalRevenueAmount" to sale.totalRevenueAmount,
                "differenceAmount" to sale.differenceAmount,
                "portalAmountPaid" to sale.portalAmountPaid,
                "saleStatus" to sale.saleStatus.name,
                "differenceAmountPaid" to sale.differenceAmountPaid,
                "differenceStatus" to sale.differenceStatus.name,
                "billingStatus" to sale.billingStatus.name,
                "clearedInventory" to sale.clearedInventory,
                "truckNumber" to sale.truckNumber,
                "fareAmount" to sale.fareAmount,
                "farePaidBy" to sale.farePaidBy.name,
                "notes" to sale.notes,
                "imageUrls" to sale.imageUrls,
                "status" to sale.status.name,
                "createdAt" to com.google.cloud.Timestamp.now()
            )

            val docRef = getSalesCollection().document()

            firestore.runTransaction { transaction ->
                val entityRef = getEntitiesCollection().document(sale.customerId)
                val entityDocFuture = transaction.get(entityRef)

                val fortifiedRiceInventoryRef = getInventoryCollection()
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("name", "Fortified Rice")
                    .get()
                    .get(10, TimeUnit.SECONDS)

                val inventoryDocFuture = if (fortifiedRiceInventoryRef.documents.isNotEmpty()) {
                    val inventoryRef = fortifiedRiceInventoryRef.documents[0].reference
                    Pair(inventoryRef, transaction.get(inventoryRef))
                } else null

                val entityDoc = entityDocFuture.get()
                val inventoryData = inventoryDocFuture?.let { (ref, future) ->
                    Pair(ref, future.get())
                }

                transaction.set(docRef, saleData)

                val portalPending = sale.totalPortalAmount - sale.portalAmountPaid
                val differencePending = sale.differenceAmount - sale.differenceAmountPaid
                val totalPending = portalPending + differencePending

                if (entityDoc.exists()) {
                    val currentBalance = entityDoc.getDouble("balance") ?: 0.0
                    val newBalance = currentBalance + totalPending
                    transaction.update(entityRef, "balance", newBalance)
                }

                if (sale.deductFromInventory && inventoryData != null) {
                    val (inventoryRef, inventoryDoc) = inventoryData
                    if (inventoryDoc.exists()) {
                        val currentQuantity = inventoryDoc.getDouble("quantity") ?: 0.0
                        val totalQuantityToDeduct = sale.quantityKg + sale.extraQuantityKg
                        val newQuantity = currentQuantity - totalQuantityToDeduct
                        transaction.update(inventoryRef, "quantity", newQuantity)
                    }
                }

                null
            }.get(15, TimeUnit.SECONDS)

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSale(saleId: String, sale: Sale): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val saleData = mapOf(
                "customerId" to sale.customerId,
                "firmName" to sale.firmName,
                "saleDate" to sale.saleDate,
                "billNumber" to sale.billNumber,
                "portalBatchNumber" to sale.portalBatchNumber,
                "quantityKg" to sale.quantityKg,
                "numberOfBags" to sale.numberOfBags,
                "deductFromInventory" to sale.deductFromInventory,
                "originalRatePerKg" to sale.originalRatePerKg,
                "portalAmount" to sale.portalAmount,
                "gstAmount" to sale.gstAmount,
                "totalPortalAmount" to sale.totalPortalAmount,
                "discountType" to sale.discountType.name,
                "discountedRatePerKg" to sale.discountedRatePerKg,
                "extraQuantityKg" to sale.extraQuantityKg,
                "revenueAmount" to sale.revenueAmount,
                "totalRevenueAmount" to sale.totalRevenueAmount,
                "differenceAmount" to sale.differenceAmount,
                "portalAmountPaid" to sale.portalAmountPaid,
                "saleStatus" to sale.saleStatus.name,
                "differenceAmountPaid" to sale.differenceAmountPaid,
                "differenceStatus" to sale.differenceStatus.name,
                "billingStatus" to sale.billingStatus.name,
                "clearedInventory" to sale.clearedInventory,
                "truckNumber" to sale.truckNumber,
                "fareAmount" to sale.fareAmount,
                "farePaidBy" to sale.farePaidBy.name,
                "notes" to sale.notes,
                "imageUrls" to sale.imageUrls
            )

            firestore.runTransaction { transaction ->
                val saleRef = getSalesCollection().document(saleId)
                val oldSaleDoc = transaction.get(saleRef).get()

                if (oldSaleDoc.exists()) {
                    val oldTotalPortalAmount = oldSaleDoc.getDouble("totalPortalAmount") ?: 0.0
                    val oldPortalAmountPaid = oldSaleDoc.getDouble("portalAmountPaid") ?: 0.0
                    val oldDifferenceAmount = oldSaleDoc.getDouble("differenceAmount") ?: 0.0
                    val oldDifferenceAmountPaid = oldSaleDoc.getDouble("differenceAmountPaid") ?: 0.0
                    val oldPortalPending = oldTotalPortalAmount - oldPortalAmountPaid
                    val oldDifferencePending = oldDifferenceAmount - oldDifferenceAmountPaid
                    val oldTotalPending = oldPortalPending + oldDifferencePending

                    val oldCustomerId = oldSaleDoc.getString("customerId") ?: ""
                    val oldQuantityKg = oldSaleDoc.getDouble("quantityKg") ?: 0.0
                    val oldExtraQuantityKg = oldSaleDoc.getDouble("extraQuantityKg") ?: 0.0
                    val oldDeductFromInventory = oldSaleDoc.getBoolean("deductFromInventory") ?: true

                    val entityRef = getEntitiesCollection().document(sale.customerId)
                    val entityDocFuture = transaction.get(entityRef)

                    val fortifiedRiceInventoryRef = getInventoryCollection()
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("name", "Fortified Rice")
                        .get()
                        .get(10, TimeUnit.SECONDS)

                    val inventoryDocFuture = if (fortifiedRiceInventoryRef.documents.isNotEmpty()) {
                        val inventoryRef = fortifiedRiceInventoryRef.documents[0].reference
                        Pair(inventoryRef, transaction.get(inventoryRef))
                    } else null

                    val entityDoc = entityDocFuture.get()
                    val inventoryData = inventoryDocFuture?.let { (ref, future) ->
                        Pair(ref, future.get())
                    }

                    val newPortalPending = sale.totalPortalAmount - sale.portalAmountPaid
                    val newDifferencePending = sale.differenceAmount - sale.differenceAmountPaid
                    val newTotalPending = newPortalPending + newDifferencePending

                    if (oldCustomerId == sale.customerId && oldCustomerId.isNotBlank() && entityDoc.exists()) {
                        val currentBalance = entityDoc.getDouble("balance") ?: 0.0
                        val balanceChange = oldTotalPending - newTotalPending
                        val newBalance = currentBalance + balanceChange
                        transaction.update(entityRef, "balance", newBalance)
                    }

                    if (inventoryData != null) {
                        val (inventoryRef, inventoryDoc) = inventoryData
                        if (inventoryDoc.exists()) {
                            val currentQuantity = inventoryDoc.getDouble("quantity") ?: 0.0
                            var adjustedQuantity = currentQuantity

                            if (oldDeductFromInventory) {
                                val oldTotalQuantity = oldQuantityKg + oldExtraQuantityKg
                                adjustedQuantity += oldTotalQuantity
                            }

                            if (sale.deductFromInventory) {
                                val newTotalQuantity = sale.quantityKg + sale.extraQuantityKg
                                adjustedQuantity -= newTotalQuantity
                            }

                            transaction.update(inventoryRef, "quantity", adjustedQuantity)
                        }
                    }

                    transaction.update(saleRef, saleData)
                }

                null
            }.get(15, TimeUnit.SECONDS)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSale(saleId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            firestore.runTransaction { transaction ->
                val saleRef = getSalesCollection().document(saleId)
                val saleDoc = transaction.get(saleRef).get()

                if (saleDoc.exists()) {
                    val totalPortalAmount = saleDoc.getDouble("totalPortalAmount") ?: 0.0
                    val portalAmountPaid = saleDoc.getDouble("portalAmountPaid") ?: 0.0
                    val differenceAmount = saleDoc.getDouble("differenceAmount") ?: 0.0
                    val differenceAmountPaid = saleDoc.getDouble("differenceAmountPaid") ?: 0.0
                    val portalPending = totalPortalAmount - portalAmountPaid
                    val differencePending = differenceAmount - differenceAmountPaid
                    val totalPending = portalPending + differencePending

                    val customerId = saleDoc.getString("customerId") ?: ""
                    val quantityKg = saleDoc.getDouble("quantityKg") ?: 0.0
                    val extraQuantityKg = saleDoc.getDouble("extraQuantityKg") ?: 0.0
                    val deductFromInventory = saleDoc.getBoolean("deductFromInventory") ?: true

                    val entityRef = if (customerId.isNotBlank()) getEntitiesCollection().document(customerId) else null
                    val entityDocFuture = entityRef?.let { transaction.get(it) }

                    val fortifiedRiceInventoryRef = getInventoryCollection()
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("name", "Fortified Rice")
                        .get()
                        .get(10, TimeUnit.SECONDS)

                    val inventoryDocFuture = if (fortifiedRiceInventoryRef.documents.isNotEmpty()) {
                        val inventoryRef = fortifiedRiceInventoryRef.documents[0].reference
                        Pair(inventoryRef, transaction.get(inventoryRef))
                    } else null

                    val entityDoc = entityDocFuture?.get()
                    val inventoryData = inventoryDocFuture?.let { (ref, future) ->
                        Pair(ref, future.get())
                    }

                    if (entityRef != null && entityDoc?.exists() == true) {
                        val currentBalance = entityDoc.getDouble("balance") ?: 0.0
                        val newBalance = currentBalance - totalPending
                        transaction.update(entityRef, "balance", newBalance)
                    }

                    if (deductFromInventory && inventoryData != null) {
                        val (inventoryRef, inventoryDoc) = inventoryData
                        if (inventoryDoc.exists()) {
                            val currentQuantity = inventoryDoc.getDouble("quantity") ?: 0.0
                            val totalQuantity = quantityKg + extraQuantityKg
                            val newQuantity = currentQuantity + totalQuantity
                            transaction.update(inventoryRef, "quantity", newQuantity)
                        }
                    }

                    transaction.delete(saleRef)
                }

                null
            }.get(15, TimeUnit.SECONDS)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reverseSale(saleId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            firestore.runTransaction { transaction ->
                val saleRef = getSalesCollection().document(saleId)
                val saleDoc = transaction.get(saleRef).get()

                if (saleDoc.exists()) {
                    val totalPortalAmount = saleDoc.getDouble("totalPortalAmount") ?: 0.0
                    val portalAmountPaid = saleDoc.getDouble("portalAmountPaid") ?: 0.0
                    val differenceAmount = saleDoc.getDouble("differenceAmount") ?: 0.0
                    val differenceAmountPaid = saleDoc.getDouble("differenceAmountPaid") ?: 0.0
                    val portalPending = totalPortalAmount - portalAmountPaid
                    val differencePending = differenceAmount - differenceAmountPaid
                    val totalPending = portalPending + differencePending

                    val customerId = saleDoc.getString("customerId") ?: ""
                    val quantityKg = saleDoc.getDouble("quantityKg") ?: 0.0
                    val extraQuantityKg = saleDoc.getDouble("extraQuantityKg") ?: 0.0
                    val deductFromInventory = saleDoc.getBoolean("deductFromInventory") ?: true

                    val entityRef = if (customerId.isNotBlank()) getEntitiesCollection().document(customerId) else null
                    val entityDocFuture = entityRef?.let { transaction.get(it) }

                    val fortifiedRiceInventoryRef = getInventoryCollection()
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("name", "Fortified Rice")
                        .get()
                        .get(10, TimeUnit.SECONDS)

                    val inventoryDocFuture = if (fortifiedRiceInventoryRef.documents.isNotEmpty()) {
                        val inventoryRef = fortifiedRiceInventoryRef.documents[0].reference
                        Pair(inventoryRef, transaction.get(inventoryRef))
                    } else null

                    val entityDoc = entityDocFuture?.get()
                    val inventoryData = inventoryDocFuture?.let { (ref, future) ->
                        Pair(ref, future.get())
                    }

                    if (entityRef != null && entityDoc?.exists() == true) {
                        val currentBalance = entityDoc.getDouble("balance") ?: 0.0
                        val newBalance = currentBalance - totalPending
                        transaction.update(entityRef, "balance", newBalance)
                    }

                    if (deductFromInventory && inventoryData != null) {
                        val (inventoryRef, inventoryDoc) = inventoryData
                        if (inventoryDoc.exists()) {
                            val currentQuantity = inventoryDoc.getDouble("quantity") ?: 0.0
                            val totalQuantity = quantityKg + extraQuantityKg
                            val newQuantity = currentQuantity + totalQuantity
                            transaction.update(inventoryRef, "quantity", newQuantity)
                        }
                    }

                    val updateData = mapOf(
                        "status" to TransactionStatus.REVERSED.name,
                        "reversedAt" to com.google.cloud.Timestamp.now(),
                        "reversalReason" to reason
                    )
                    transaction.update(saleRef, updateData)
                }

                null
            }.get(15, TimeUnit.SECONDS)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToSales(onSalesChanged: (List<Sale>) -> Unit) {
        if (userId.isBlank() || appId.isBlank()) {
            onSalesChanged(emptyList())
            return
        }

        getSalesCollection()
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Error listening to sales: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val sales = snapshot.documents.mapNotNull { doc ->
                        try {
                            Sale(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                customerId = doc.getString("customerId") ?: "",
                                firmName = doc.getString("firmName") ?: "",
                                saleDate = doc.getString("saleDate") ?: "",
                                billNumber = doc.getString("billNumber") ?: "",
                                portalBatchNumber = doc.getString("portalBatchNumber") ?: "",
                                quantityKg = doc.getDouble("quantityKg") ?: 0.0,
                                numberOfBags = (doc.getLong("numberOfBags") ?: 0).toInt(),
                                deductFromInventory = doc.getBoolean("deductFromInventory") ?: true,
                                originalRatePerKg = doc.getDouble("originalRatePerKg") ?: 0.0,
                                portalAmount = doc.getDouble("portalAmount") ?: 0.0,
                                gstAmount = doc.getDouble("gstAmount") ?: 0.0,
                                totalPortalAmount = doc.getDouble("totalPortalAmount") ?: 0.0,
                                discountType = DiscountType.valueOf(
                                    doc.getString("discountType") ?: "NONE"
                                ),
                                discountedRatePerKg = doc.getDouble("discountedRatePerKg") ?: 0.0,
                                extraQuantityKg = doc.getDouble("extraQuantityKg") ?: 0.0,
                                revenueAmount = doc.getDouble("revenueAmount") ?: 0.0,
                                totalRevenueAmount = doc.getDouble("totalRevenueAmount") ?: 0.0,
                                differenceAmount = doc.getDouble("differenceAmount") ?: 0.0,
                                portalAmountPaid = doc.getDouble("portalAmountPaid") ?: 0.0,
                                saleStatus = SaleStatus.valueOf(
                                    doc.getString("saleStatus") ?: "PENDING"
                                ),
                                differenceAmountPaid = doc.getDouble("differenceAmountPaid") ?: 0.0,
                                differenceStatus = DifferenceStatus.valueOf(
                                    doc.getString("differenceStatus") ?: "PENDING"
                                ),
                                billingStatus = BillingStatus.valueOf(
                                    doc.getString("billingStatus") ?: "PENDING_BILLED"
                                ),
                                clearedInventory = doc.getDouble("clearedInventory") ?: 0.0,
                                truckNumber = doc.getString("truckNumber") ?: "",
                                fareAmount = doc.getDouble("fareAmount") ?: 0.0,
                                farePaidBy = FarePaidBy.valueOf(
                                    doc.getString("farePaidBy") ?: "COMPANY"
                                ),
                                notes = doc.getString("notes") ?: "",
                                imageUrls = (doc.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                status = TransactionStatus.valueOf(
                                    doc.getString("status") ?: "APPROVED"
                                ),
                                reversedAt = doc.getTimestamp("reversedAt"),
                                reversalReason = doc.getString("reversalReason") ?: "",
                                createdAt = doc.getTimestamp("createdAt")
                            )
                        } catch (e: Exception) {
                            println("Error parsing sale: ${e.message}")
                            null
                        }
                    }
                    onSalesChanged(sales)
                }
            }
    }

    /**
     * Clear bill by adding quantity to cleared inventory
     */
    suspend fun clearBill(saleId: String, quantityToClear: Double): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            firestore.runTransaction { transaction ->
                val saleRef = getSalesCollection().document(saleId)
                val saleDoc = transaction.get(saleRef).get()

                if (!saleDoc.exists()) {
                    throw Exception("Sale not found")
                }

                val currentClearedInventory = saleDoc.getDouble("clearedInventory") ?: 0.0
                val totalQuantity = saleDoc.getDouble("quantityKg") ?: 0.0
                val newClearedInventory = currentClearedInventory + quantityToClear

                // Check if we're trying to clear more than the total quantity
                if (newClearedInventory > totalQuantity) {
                    throw Exception("Cannot clear more than total quantity")
                }

                // Update cleared inventory
                transaction.update(saleRef, "clearedInventory", newClearedInventory)

                // If cleared inventory equals total quantity, mark as billed
                if (newClearedInventory >= totalQuantity) {
                    transaction.update(saleRef, "billingStatus", BillingStatus.BILLED.name)
                }

                null
            }.get(10, TimeUnit.SECONDS)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get sales by customer ID
     */
    suspend fun getSalesByCustomerId(customerId: String): Result<List<Sale>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = getSalesCollection()
                .whereEqualTo("userId", userId)
                .whereEqualTo("customerId", customerId)
                .get()
                .get(10, TimeUnit.SECONDS)

            val sales = snapshot.documents.mapNotNull { doc ->
                try {
                    Sale(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        customerId = doc.getString("customerId") ?: "",
                        firmName = doc.getString("firmName") ?: "",
                        saleDate = doc.getString("saleDate") ?: "",
                        billNumber = doc.getString("billNumber") ?: "",
                        portalBatchNumber = doc.getString("portalBatchNumber") ?: "",
                        quantityKg = doc.getDouble("quantityKg") ?: 0.0,
                        numberOfBags = (doc.getLong("numberOfBags") ?: 0).toInt(),
                        deductFromInventory = doc.getBoolean("deductFromInventory") ?: true,
                        originalRatePerKg = doc.getDouble("originalRatePerKg") ?: 0.0,
                        portalAmount = doc.getDouble("portalAmount") ?: 0.0,
                        gstAmount = doc.getDouble("gstAmount") ?: 0.0,
                        totalPortalAmount = doc.getDouble("totalPortalAmount") ?: 0.0,
                        discountType = DiscountType.valueOf(doc.getString("discountType") ?: "NONE"),
                        discountedRatePerKg = doc.getDouble("discountedRatePerKg") ?: 0.0,
                        extraQuantityKg = doc.getDouble("extraQuantityKg") ?: 0.0,
                        revenueAmount = doc.getDouble("revenueAmount") ?: 0.0,
                        totalRevenueAmount = doc.getDouble("totalRevenueAmount") ?: 0.0,
                        differenceAmount = doc.getDouble("differenceAmount") ?: 0.0,
                        portalAmountPaid = doc.getDouble("portalAmountPaid") ?: 0.0,
                        saleStatus = SaleStatus.valueOf(doc.getString("saleStatus") ?: "PENDING"),
                        differenceAmountPaid = doc.getDouble("differenceAmountPaid") ?: 0.0,
                        differenceStatus = DifferenceStatus.valueOf(doc.getString("differenceStatus") ?: "PENDING"),
                        billingStatus = BillingStatus.valueOf(doc.getString("billingStatus") ?: "PENDING_BILLED"),
                        clearedInventory = doc.getDouble("clearedInventory") ?: 0.0,
                        truckNumber = doc.getString("truckNumber") ?: "",
                        fareAmount = doc.getDouble("fareAmount") ?: 0.0,
                        farePaidBy = FarePaidBy.valueOf(doc.getString("farePaidBy") ?: "COMPANY"),
                        notes = doc.getString("notes") ?: "",
                        imageUrls = (doc.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        status = TransactionStatus.valueOf(doc.getString("status") ?: "APPROVED"),
                        reversedAt = doc.getTimestamp("reversedAt"),
                        reversalReason = doc.getString("reversalReason") ?: "",
                        createdAt = doc.getTimestamp("createdAt")
                    )
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(sales)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}