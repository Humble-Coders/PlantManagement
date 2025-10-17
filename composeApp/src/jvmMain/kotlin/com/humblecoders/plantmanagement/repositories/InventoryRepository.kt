package com.humblecoders.plantmanagement.repositories

import com.google.cloud.firestore.Firestore
import com.humblecoders.plantmanagement.data.CategoryType
import com.humblecoders.plantmanagement.data.InventoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class InventoryRepository(
    private val firestore: Firestore,
    private val userId: String,
    private val appId: String
) {
    private fun getInventoryCollection() = firestore.collection("inventory")

    suspend fun addInventoryItem(item: InventoryItem): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val docRef = getInventoryCollection().document()
            
            // Use transaction to ensure atomic write
            firestore.runTransaction { transaction ->
                val itemData = mapOf(
                    "userId" to userId,
                    "name" to item.name,
                    "quantity" to item.quantity,
                    "unit" to item.unit,
                    "categoryType" to item.categoryType.name,
                    "averagePurchasePrice" to item.averagePurchasePrice,
                    "createdAt" to com.google.cloud.Timestamp.now()
                )
                
                transaction.set(docRef, itemData)
                null
            }.get(10, TimeUnit.SECONDS)

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateInventoryItem(itemId: String, item: InventoryItem): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Use transaction to ensure atomic update
            firestore.runTransaction { transaction ->
                val itemRef = getInventoryCollection().document(itemId)
                
                // Verify item exists before updating
                val doc = transaction.get(itemRef).get()
                if (!doc.exists()) {
                    throw Exception("Inventory item not found")
                }
                
                val itemData = mapOf(
                    "name" to item.name,
                    "quantity" to item.quantity,
                    "unit" to item.unit,
                    "categoryType" to item.categoryType.name,
                    "averagePurchasePrice" to item.averagePurchasePrice
                )
                
                transaction.update(itemRef, itemData)
                null
            }.get(10, TimeUnit.SECONDS)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteInventoryItem(itemId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Use transaction to ensure atomic delete
            firestore.runTransaction { transaction ->
                val itemRef = getInventoryCollection().document(itemId)
                
                // Verify item exists before deleting
                val doc = transaction.get(itemRef).get()
                if (!doc.exists()) {
                    throw Exception("Inventory item not found")
                }
                
                transaction.delete(itemRef)
                null
            }.get(10, TimeUnit.SECONDS)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToInventoryItems(onItemsChanged: (List<InventoryItem>) -> Unit) {
        if (userId.isBlank() || appId.isBlank()) {
            onItemsChanged(emptyList())
            return
        }

        getInventoryCollection().whereEqualTo("userId", userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                println("Error listening to inventory: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val items = snapshot.documents.mapNotNull { doc ->
                    try {
                        InventoryItem(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            name = doc.getString("name") ?: "",
                            quantity = doc.getDouble("quantity") ?: 0.0,
                            unit = doc.getString("unit") ?: "kg",
                            categoryType = CategoryType.valueOf(
                                doc.getString("categoryType") ?: "RAW_MATERIAL"
                            ),
                            averagePurchasePrice = doc.getDouble("averagePurchasePrice") ?: 0.0,
                            createdAt = doc.getTimestamp("createdAt")
                        )
                    } catch (e: Exception) {
                        println("Error parsing inventory item: ${e.message}")
                        null
                    }
                }
                onItemsChanged(items)
            }
        }
    }
}