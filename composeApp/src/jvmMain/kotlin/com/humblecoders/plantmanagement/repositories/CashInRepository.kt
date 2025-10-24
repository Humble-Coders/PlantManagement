package com.humblecoders.plantmanagement.repositories

import com.google.cloud.firestore.Firestore
import com.humblecoders.plantmanagement.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class CashInRepository(
    private val firestore: Firestore,
    private val userId: String,
    private val appId: String
) {
    private fun getCashInRevenueCollection() = firestore.collection("cash_in_revenue")
    private fun getCashInOutDifferenceCollection() = firestore.collection("cash_in_out_difference")
    private fun getSalesCollection() = firestore.collection("sales")
    private fun getEntitiesCollection() = firestore.collection("customers")

    /**
     * Process cash in for revenue amount
     */
    suspend fun processCashInRevenue(
        cashInAmount: Double,
        saleAllocations: List<SaleAllocation>,
        notes: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val cashInDocRef = getCashInRevenueCollection().document()

            firestore.runTransaction { transaction ->
                // Read all sales first
                val saleFutures = saleAllocations.map { allocation ->
                    val saleRef = getSalesCollection().document(allocation.saleId)
                    Pair(allocation, saleRef to transaction.get(saleRef))
                }

                // Group by customer and prepare entity reads
                val customerIds = saleAllocations.map { it.customerId }.distinct()
                val entityFutures = customerIds.map { customerId ->
                    val entityRef = getEntitiesCollection().document(customerId)
                    customerId to Pair(entityRef, transaction.get(entityRef))
                }

                // Resolve futures
                val saleData = saleFutures.map { (allocation, refAndFuture) ->
                    val (ref, future) = refAndFuture
                    Triple(allocation, ref, future.get())
                }

                val entityData = entityFutures.map { (customerId, refAndFuture) ->
                    val (ref, future) = refAndFuture
                    Triple(customerId, ref, future.get())
                }

                // Update sales
                saleData.forEach { (allocation, saleRef, saleDoc) ->
                    if (saleDoc.exists()) {
                        val updateData = mutableMapOf<String, Any>(
                            "portalAmountPaid" to allocation.newAmountPaid,
                            "saleStatus" to allocation.newPaymentStatus.name
                        )
                        transaction.update(saleRef, updateData)
                    }
                }

                // Update entity balances (decrease balance as customer pays us)
                val customerAllocations = saleAllocations.groupBy { it.customerId }
                entityData.forEach { (customerId, entityRef, entityDoc) ->
                    if (entityDoc.exists()) {
                        val allocations = customerAllocations[customerId] ?: emptyList()
                        val totalAllocatedToCustomer = allocations.sumOf { it.allocatedAmount }

                        // Customer balance decreases when they pay us
                        val currentBalance = entityDoc.getDouble("balance") ?: 0.0
                        val newBalance = currentBalance - totalAllocatedToCustomer
                        transaction.update(entityRef, "balance", newBalance)
                    }
                }

                // Save cash in record
                val allocationsData = saleAllocations.map { allocation ->
                    mapOf(
                        "saleId" to allocation.saleId,
                        "firmName" to allocation.firmName,
                        "saleDate" to allocation.saleDate,
                        "customerId" to allocation.customerId,
                        "billNumber" to allocation.billNumber,
                        "totalRevenueAmount" to allocation.totalRevenueAmount,
                        "totalPortalAmount" to allocation.totalPortalAmount,
                        "allocatedAmount" to allocation.allocatedAmount,
                        "previousAmountPaid" to allocation.previousAmountPaid,
                        "newAmountPaid" to allocation.newAmountPaid,
                        "previousPaymentStatus" to allocation.previousPaymentStatus.name,
                        "newPaymentStatus" to allocation.newPaymentStatus.name,
                        "allocationType" to allocation.allocationType.name
                    )
                }

                val cashInData = mapOf(
                    "userId" to userId,
                    "totalAmount" to cashInAmount,
                    "saleAllocations" to allocationsData,
                    "notes" to notes,
                    "createdAt" to com.google.cloud.Timestamp.now()
                )

                transaction.set(cashInDocRef, cashInData)
                null
            }.get(20, TimeUnit.SECONDS)

            Result.success(cashInDocRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Process cash in/out for difference amount
     */
    suspend fun processCashInOutDifference(
        amount: Double,
        transactionType: DifferenceTransactionType,
        saleAllocations: List<SaleAllocation>,
        notes: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val docRef = getCashInOutDifferenceCollection().document()

            firestore.runTransaction { transaction ->
                // Read all sales first
                val saleFutures = saleAllocations.map { allocation ->
                    val saleRef = getSalesCollection().document(allocation.saleId)
                    Pair(allocation, saleRef to transaction.get(saleRef))
                }

                // Group by customer and prepare entity reads
                val customerIds = saleAllocations.map { it.customerId }.distinct()
                val entityFutures = customerIds.map { customerId ->
                    val entityRef = getEntitiesCollection().document(customerId)
                    customerId to Pair(entityRef, transaction.get(entityRef))
                }

                // Resolve futures
                val saleData = saleFutures.map { (allocation, refAndFuture) ->
                    val (ref, future) = refAndFuture
                    Triple(allocation, ref, future.get())
                }

                val entityData = entityFutures.map { (customerId, refAndFuture) ->
                    val (ref, future) = refAndFuture
                    Triple(customerId, ref, future.get())
                }

                // Update sales
                saleData.forEach { (allocation, saleRef, saleDoc) ->
                    if (saleDoc.exists()) {
                        val updateData = mutableMapOf<String, Any>(
                            "differenceAmountPaid" to allocation.newAmountPaid,
                            "differenceStatus" to allocation.newPaymentStatus.name
                        )
                        transaction.update(saleRef, updateData)
                    }
                }

                // Update entity balances
                val customerAllocations = saleAllocations.groupBy { it.customerId }
                entityData.forEach { (customerId, entityRef, entityDoc) ->
                    if (entityDoc.exists()) {
                        val allocations = customerAllocations[customerId] ?: emptyList()
                        val totalAllocatedToCustomer = allocations.sumOf { it.allocatedAmount }

                        val currentBalance = entityDoc.getDouble("balance") ?: 0.0
                        val newBalance = if (transactionType == DifferenceTransactionType.CASH_IN) {
                            // Customer pays us positive difference - reduces what they owe us
                            currentBalance - totalAllocatedToCustomer
                        } else {
                            // We pay customer negative difference - reduces what we owe them (balance becomes less negative)
                            currentBalance + totalAllocatedToCustomer
                        }
                        transaction.update(entityRef, "balance", newBalance)
                    }
                }

                // Save cash in/out record
                val allocationsData = saleAllocations.map { allocation ->
                    mapOf(
                        "saleId" to allocation.saleId,
                        "firmName" to allocation.firmName,
                        "saleDate" to allocation.saleDate,
                        "customerId" to allocation.customerId,
                        "billNumber" to allocation.billNumber,
                        "differenceAmount" to allocation.differenceAmount,
                        "allocatedAmount" to allocation.allocatedAmount,
                        "previousAmountPaid" to allocation.previousAmountPaid,
                        "newAmountPaid" to allocation.newAmountPaid,
                        "previousPaymentStatus" to allocation.previousPaymentStatus.name,
                        "newPaymentStatus" to allocation.newPaymentStatus.name,
                        "allocationType" to allocation.allocationType.name
                    )
                }

                val cashInOutData = mapOf(
                    "userId" to userId,
                    "totalAmount" to amount,
                    "transactionType" to transactionType.name,
                    "saleAllocations" to allocationsData,
                    "notes" to notes,
                    "createdAt" to com.google.cloud.Timestamp.now()
                )

                transaction.set(docRef, cashInOutData)
                null
            }.get(20, TimeUnit.SECONDS)

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all pending/partially paid sales for revenue (chronological order)
     */
    suspend fun getPendingSalesForRevenue(customerId: String? = null): List<Sale> = withContext(Dispatchers.IO) {
        return@withContext try {
            val query = if (!customerId.isNullOrBlank()) {
                getSalesCollection().whereEqualTo("customerId", customerId)
            } else {
                getSalesCollection()
            }

            val snapshot = query.get().get(15, TimeUnit.SECONDS)

            snapshot.documents.mapNotNull { doc ->
                try {
                    val saleStatus = SaleStatus.valueOf(doc.getString("saleStatus") ?: "PENDING")
                    val status = TransactionStatus.valueOf(doc.getString("status") ?: "APPROVED")

                    if ((saleStatus == SaleStatus.PENDING || saleStatus == SaleStatus.PARTIALLY_PAID) &&
                        status == TransactionStatus.APPROVED) {

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
                            saleStatus = saleStatus,
                            differenceAmountPaid = doc.getDouble("differenceAmountPaid") ?: 0.0,
                            differenceStatus = DifferenceStatus.valueOf(doc.getString("differenceStatus") ?: "PENDING"),
                            truckNumber = doc.getString("truckNumber") ?: "",
                            fareAmount = doc.getDouble("fareAmount") ?: 0.0,
                            farePaidBy = FarePaidBy.valueOf(doc.getString("farePaidBy") ?: "COMPANY"),
                            notes = doc.getString("notes") ?: "",
                            status = status,
                            reversedAt = doc.getTimestamp("reversedAt"),
                            reversalReason = doc.getString("reversalReason") ?: "",
                            createdAt = doc.getTimestamp("createdAt")
                        )
                    } else null
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.createdAt?.seconds ?: 0L }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get sales with pending difference amount (positive or negative)
     */
    suspend fun getSalesWithPendingDifference(
        customerId: String,
        transactionType: DifferenceTransactionType
    ): List<Sale> = withContext(Dispatchers.IO) {
        return@withContext try {
            val query = getSalesCollection()
                .whereEqualTo("customerId", customerId)

            val snapshot = query.get().get(15, TimeUnit.SECONDS)

            snapshot.documents.mapNotNull { doc ->
                try {
                    val differenceStatus = DifferenceStatus.valueOf(doc.getString("differenceStatus") ?: "PENDING")
                    val status = TransactionStatus.valueOf(doc.getString("status") ?: "APPROVED")
                    val differenceAmount = doc.getDouble("differenceAmount") ?: 0.0
                    val differenceAmountPaid = doc.getDouble("differenceAmountPaid") ?: 0.0
                    val pendingDifference = differenceAmount - differenceAmountPaid

                    val shouldInclude = when (transactionType) {
                        DifferenceTransactionType.CASH_IN -> pendingDifference > 0 // Positive difference
                        DifferenceTransactionType.CASH_OUT -> pendingDifference < 0 // Negative difference
                    } && kotlin.math.abs(differenceAmount) > 0.0 // Exclude sales with 0 difference amount (they are already PAID)

                    if (shouldInclude &&
                        (differenceStatus == DifferenceStatus.PENDING || differenceStatus == DifferenceStatus.PARTIALLY_PAID) &&
                        status == TransactionStatus.APPROVED) {

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
                            differenceAmount = differenceAmount,
                            portalAmountPaid = doc.getDouble("portalAmountPaid") ?: 0.0,
                            saleStatus = SaleStatus.valueOf(doc.getString("saleStatus") ?: "PENDING"),
                            differenceAmountPaid = differenceAmountPaid,
                            differenceStatus = differenceStatus,
                            truckNumber = doc.getString("truckNumber") ?: "",
                            fareAmount = doc.getDouble("fareAmount") ?: 0.0,
                            farePaidBy = FarePaidBy.valueOf(doc.getString("farePaidBy") ?: "COMPANY"),
                            notes = doc.getString("notes") ?: "",
                            status = status,
                            reversedAt = doc.getTimestamp("reversedAt"),
                            reversalReason = doc.getString("reversalReason") ?: "",
                            createdAt = doc.getTimestamp("createdAt")
                        )
                    } else null
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.createdAt?.seconds ?: 0L }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Listen to cash in revenue history
     */
    fun listenToCashInRevenueHistory(onChanged: (List<CashInRevenue>) -> Unit) {
        if (userId.isBlank() || appId.isBlank()) {
            onChanged(emptyList())
            return
        }

        getCashInRevenueCollection()
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Error listening to cash in revenue: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val cashIns = snapshot.documents.mapNotNull { doc ->
                        try {
                            val allocationsList = doc.get("saleAllocations") as? List<Map<String, Any>> ?: emptyList()
                            val allocations = allocationsList.map { allocationMap ->
                                SaleAllocation(
                                    saleId = allocationMap["saleId"] as? String ?: "",
                                    firmName = allocationMap["firmName"] as? String ?: "",
                                    saleDate = allocationMap["saleDate"] as? String ?: "",
                                    customerId = allocationMap["customerId"] as? String ?: "",
                                    billNumber = allocationMap["billNumber"] as? String ?: "",
                                    totalRevenueAmount = (allocationMap["totalRevenueAmount"] as? Number)?.toDouble() ?: 0.0,
                                    totalPortalAmount = (allocationMap["totalPortalAmount"] as? Number)?.toDouble() ?: 0.0,
                                    allocatedAmount = (allocationMap["allocatedAmount"] as? Number)?.toDouble() ?: 0.0,
                                    previousAmountPaid = (allocationMap["previousAmountPaid"] as? Number)?.toDouble() ?: 0.0,
                                    newAmountPaid = (allocationMap["newAmountPaid"] as? Number)?.toDouble() ?: 0.0,
                                    previousPaymentStatus = SaleStatus.valueOf(allocationMap["previousPaymentStatus"] as? String ?: "PENDING"),
                                    newPaymentStatus = SaleStatus.valueOf(allocationMap["newPaymentStatus"] as? String ?: "PENDING"),
                                    allocationType = AllocationType.valueOf(allocationMap["allocationType"] as? String ?: "REVENUE")
                                )
                            }

                            CashInRevenue(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                                saleAllocations = allocations,
                                notes = doc.getString("notes") ?: "",
                                createdAt = doc.getTimestamp("createdAt")
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onChanged(cashIns)
                }
            }
    }

    /**
     * Listen to cash in/out difference history
     */
    fun listenToCashInOutDifferenceHistory(onChanged: (List<CashInOutDifference>) -> Unit) {
        if (userId.isBlank() || appId.isBlank()) {
            onChanged(emptyList())
            return
        }

        getCashInOutDifferenceCollection()
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Error listening to cash in/out difference: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val transactions = snapshot.documents.mapNotNull { doc ->
                        try {
                            val allocationsList = doc.get("saleAllocations") as? List<Map<String, Any>> ?: emptyList()
                            val allocations = allocationsList.map { allocationMap ->
                                SaleAllocation(
                                    saleId = allocationMap["saleId"] as? String ?: "",
                                    firmName = allocationMap["firmName"] as? String ?: "",
                                    saleDate = allocationMap["saleDate"] as? String ?: "",
                                    customerId = allocationMap["customerId"] as? String ?: "",
                                    billNumber = allocationMap["billNumber"] as? String ?: "",
                                    totalRevenueAmount = (allocationMap["totalRevenueAmount"] as? Number)?.toDouble() ?: 0.0,
                                    totalPortalAmount = (allocationMap["totalPortalAmount"] as? Number)?.toDouble() ?: 0.0,
                                    differenceAmount = (allocationMap["differenceAmount"] as? Number)?.toDouble() ?: 0.0,
                                    allocatedAmount = (allocationMap["allocatedAmount"] as? Number)?.toDouble() ?: 0.0,
                                    previousAmountPaid = (allocationMap["previousAmountPaid"] as? Number)?.toDouble() ?: 0.0,
                                    newAmountPaid = (allocationMap["newAmountPaid"] as? Number)?.toDouble() ?: 0.0,
                                    previousPaymentStatus = SaleStatus.valueOf(allocationMap["previousPaymentStatus"] as? String ?: "PENDING"),
                                    newPaymentStatus = SaleStatus.valueOf(allocationMap["newPaymentStatus"] as? String ?: "PENDING"),
                                    allocationType = AllocationType.valueOf(allocationMap["allocationType"] as? String ?: "DIFFERENCE")
                                )
                            }

                            CashInOutDifference(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                                transactionType = DifferenceTransactionType.valueOf(doc.getString("transactionType") ?: "CASH_IN"),
                                saleAllocations = allocations,
                                notes = doc.getString("notes") ?: "",
                                createdAt = doc.getTimestamp("createdAt")
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onChanged(transactions)
                }
            }
    }
}