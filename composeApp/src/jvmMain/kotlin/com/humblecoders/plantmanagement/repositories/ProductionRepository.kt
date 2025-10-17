package com.humblecoders.plantmanagement.repositories

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import com.humblecoders.plantmanagement.data.ProductionRecord
import com.humblecoders.plantmanagement.data.ProductionInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.TimeUnit

class ProductionRepository(
    private val firestore: Firestore,
    private val userId: String,
    private val appId: String
) {

    private fun getProductionCollection() =
        firestore.collection("production_records")

    /**
     * Add a new production record using transaction for atomicity
     */
    suspend fun addProductionRecord(productionInput: ProductionInput): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val docRef = getProductionCollection().document()
            
            // Use transaction to ensure atomic write
            firestore.runTransaction { transaction ->
                val productionData = mapOf(
                    "userId" to userId,
                    "batchNumber" to productionInput.batchNumber,
                    "quantityProduced" to productionInput.quantityProduced,
                    "productionDate" to com.google.cloud.Timestamp.of(java.util.Date(productionInput.productionDate)),
                    "supervisorName" to productionInput.supervisorName,
                    "notes" to productionInput.notes,
                    "rawMaterialsUsed" to productionInput.rawMaterialsUsed,
                    "wasteTracking" to productionInput.wasteTracking?.let { waste ->
                        mapOf(
                            "wastage" to waste.wastage,
                            "burn" to waste.burn,
                            "regrind" to waste.regrind,
                            "others" to waste.others
                        )
                    },
                    "createdAt" to com.google.cloud.Timestamp.now()
                )
                
                transaction.set(docRef, productionData)
                null
            }.get(10, TimeUnit.SECONDS)

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all production records for the current user
     */
    suspend fun getProductionRecords(): Result<List<ProductionRecord>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = getProductionCollection()
                .whereEqualTo("userId", userId)
                .get()
                .get(10, TimeUnit.SECONDS)

            val records = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data
                    ProductionRecord(
                        id = doc.id,
                        batchNumber = data["batchNumber"] as? String ?: "",
                        quantityProduced = (data["quantityProduced"] as? Number)?.toDouble() ?: 0.0,
                        productionDate = data["productionDate"] as? com.google.cloud.Timestamp,
                        supervisorName = data["supervisorName"] as? String ?: "",
                        notes = data["notes"] as? String ?: "",
                        rawMaterialsUsed = (data["rawMaterialsUsed"] as? Map<String, Any>)?.mapValues { 
                            (it.value as? Number)?.toDouble() ?: 0.0 
                        } ?: emptyMap(),
                        wasteTracking = (data["wasteTracking"] as? Map<String, Any>)?.let { wasteData ->
                            com.humblecoders.plantmanagement.data.WasteTracking(
                                wastage = (wasteData["wastage"] as? Number)?.toDouble() ?: 0.0,
                                burn = (wasteData["burn"] as? Number)?.toDouble() ?: 0.0,
                                regrind = (wasteData["regrind"] as? Number)?.toDouble() ?: 0.0,
                                others = (wasteData["others"] as? Number)?.toDouble() ?: 0.0
                            )
                        },
                        createdAt = data["createdAt"] as? com.google.cloud.Timestamp
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedByDescending { it.productionDate }

            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Listen to production records changes in real-time
     */
    fun listenToProductionRecords(): Flow<List<ProductionRecord>> = callbackFlow {
        val listener = getProductionCollection()
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val records = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data
                            ProductionRecord(
                                id = doc.id,
                                batchNumber = data["batchNumber"] as? String ?: "",
                                quantityProduced = (data["quantityProduced"] as? Number)?.toDouble() ?: 0.0,
                                productionDate = data["productionDate"] as? com.google.cloud.Timestamp,
                                supervisorName = data["supervisorName"] as? String ?: "",
                                notes = data["notes"] as? String ?: "",
                                rawMaterialsUsed = (data["rawMaterialsUsed"] as? Map<String, Any>)?.mapValues { 
                                    (it.value as? Number)?.toDouble() ?: 0.0 
                                } ?: emptyMap(),
                                wasteTracking = (data["wasteTracking"] as? Map<String, Any>)?.let { wasteData ->
                                    com.humblecoders.plantmanagement.data.WasteTracking(
                                        wastage = (wasteData["wastage"] as? Number)?.toDouble() ?: 0.0,
                                        burn = (wasteData["burn"] as? Number)?.toDouble() ?: 0.0,
                                        regrind = (wasteData["regrind"] as? Number)?.toDouble() ?: 0.0,
                                        others = (wasteData["others"] as? Number)?.toDouble() ?: 0.0
                                    )
                                },
                                createdAt = data["createdAt"] as? com.google.cloud.Timestamp
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedByDescending { it.productionDate }

                    trySend(records)
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Update a production record using transaction for atomicity
     */
    suspend fun updateProductionRecord(recordId: String, productionInput: ProductionInput): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val docRef = getProductionCollection().document(recordId)
            
            // Use transaction to ensure atomic update
            firestore.runTransaction { transaction ->
                // Verify document exists before updating
                val doc = transaction.get(docRef).get()
                if (!doc.exists()) {
                    throw Exception("Production record not found")
                }
                
                val productionData = mapOf(
                    "batchNumber" to productionInput.batchNumber,
                    "quantityProduced" to productionInput.quantityProduced,
                    "productionDate" to com.google.cloud.Timestamp.of(java.util.Date(productionInput.productionDate)),
                    "supervisorName" to productionInput.supervisorName,
                    "notes" to productionInput.notes,
                    "rawMaterialsUsed" to productionInput.rawMaterialsUsed,
                    "wasteTracking" to productionInput.wasteTracking?.let { waste ->
                        mapOf(
                            "wastage" to waste.wastage,
                            "burn" to waste.burn,
                            "regrind" to waste.regrind,
                            "others" to waste.others
                        )
                    }
                )
                
                transaction.update(docRef, productionData)
                null
            }.get(10, TimeUnit.SECONDS)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a production record using transaction for atomicity
     */
    suspend fun deleteProductionRecord(recordId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val docRef = getProductionCollection().document(recordId)
            
            // Use transaction to ensure atomic delete
            firestore.runTransaction { transaction ->
                // Verify document exists before deleting
                val doc = transaction.get(docRef).get()
                if (!doc.exists()) {
                    throw Exception("Production record not found")
                }
                
                transaction.delete(docRef)
                null
            }.get(10, TimeUnit.SECONDS)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
