package com.humblecoders.plantmanagement.repositories

import com.google.cloud.firestore.Firestore
import com.humblecoders.plantmanagement.data.Entity
import java.util.concurrent.TimeUnit

class EntityRepository(
    private val firestore: Firestore,
    private val userId: String,
    private val appId: String
) {

    private fun getEntitiesCollection() =
        firestore.collection("customers")

    /**
     * Add a new entity
     */
    suspend fun addEntity(entity: Entity): Result<String> {
        return try {
            val entityData = mapOf(
                "userId" to userId,
                "firmName" to entity.firmName,
                "contactPerson" to entity.contactPerson,
                "contactNo" to entity.contactNo,
                "city" to entity.city,
                "state" to entity.state,
                "gstin" to entity.gstin,
                "balance" to entity.balance,
                "createdAt" to com.google.cloud.Timestamp.now()
            )

            val docRef = getEntitiesCollection().document()
            docRef.set(entityData).get(10, TimeUnit.SECONDS)

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update an existing entity
     */
    suspend fun updateEntity(entityId: String, entity: Entity): Result<Unit> {
        return try {
            val entityData = mapOf(
                "firmName" to entity.firmName,
                "contactPerson" to entity.contactPerson,
                "contactNo" to entity.contactNo,
                "city" to entity.city,
                "state" to entity.state,
                "gstin" to entity.gstin,
                "balance" to entity.balance
            )

            getEntitiesCollection()
                .document(entityId)
                .update(entityData)
                .get(10, TimeUnit.SECONDS)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete an entity
     */
    suspend fun deleteEntity(entityId: String): Result<Unit> {
        return try {
            getEntitiesCollection()
                .document(entityId)
                .delete()
                .get(10, TimeUnit.SECONDS)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get entity by ID
     */
    suspend fun getEntityById(entityId: String): Entity? {
        return try {
            val doc = getEntitiesCollection()
                .document(entityId)
                .get()
                .get(10, TimeUnit.SECONDS)

            if (!doc.exists()) return null

            Entity(
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
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Listen to entities in real-time
     */
    fun listenToEntities(onEntitiesChanged: (List<Entity>) -> Unit) {
        if (userId.isBlank() || appId.isBlank()) {
            // No authenticated user or invalid appId; do not start listener
            onEntitiesChanged(emptyList())
            return
        }
        getEntitiesCollection().whereEqualTo("userId", userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                println("Error listening to entities: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val entities = snapshot.documents.mapNotNull { doc ->
                    try {
                        Entity(
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
                    } catch (e: Exception) {
                        null
                    }
                }
                onEntitiesChanged(entities)
            }
        }
    }
}