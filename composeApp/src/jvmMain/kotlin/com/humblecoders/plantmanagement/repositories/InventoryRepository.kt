package com.humblecoders.plantmanagement.repositories

import com.google.cloud.firestore.Firestore
import com.humblecoders.plantmanagement.data.CategoryType
import com.humblecoders.plantmanagement.data.InventoryItem
import java.util.concurrent.TimeUnit

class InventoryRepository(
    private val firestore: Firestore,
    private val userId: String,
    private val appId: String
) {
    private fun getInventoryCollection() = firestore.collection("inventory")

    suspend fun addInventoryItem(item: InventoryItem): Result<String> {
        return try {
            val itemData = mapOf(
                "userId" to userId,
                "name" to item.name,
                "quantity" to item.quantity,
                "unit" to item.unit,
                "categoryType" to item.categoryType.name,
                "createdAt" to com.google.cloud.Timestamp.now()
            )

            val docRef = getInventoryCollection().document()
            docRef.set(itemData).get(10, TimeUnit.SECONDS)

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateInventoryItem(itemId: String, item: InventoryItem): Result<Unit> {
        return try {
            val itemData = mapOf(
                "name" to item.name,
                "quantity" to item.quantity,
                "unit" to item.unit,
                "categoryType" to item.categoryType.name
            )

            getInventoryCollection()
                .document(itemId)
                .update(itemData)
                .get(10, TimeUnit.SECONDS)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteInventoryItem(itemId: String): Result<Unit> {
        return try {
            getInventoryCollection()
                .document(itemId)
                .delete()
                .get(10, TimeUnit.SECONDS)

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