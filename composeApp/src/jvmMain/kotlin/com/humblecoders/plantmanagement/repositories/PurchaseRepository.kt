package com.humblecoders.plantmanagement.repositories

import com.google.cloud.firestore.Firestore
import com.humblecoders.plantmanagement.data.Purchase
import com.humblecoders.plantmanagement.data.PaymentStatus
import com.humblecoders.plantmanagement.data.TransactionStatus
import java.util.concurrent.TimeUnit

class PurchaseRepository(
    private val firestore: Firestore,
    private val userId: String,
    private val appId: String
) {

    private fun getPurchasesCollection() = firestore.collection("purchases")

    suspend fun addPurchase(purchase: Purchase): Result<String> {
        return try {
            val purchaseData = mapOf(
                "userId" to userId,
                "customerId" to purchase.customerId,
                "firmName" to purchase.firmName,
                "purchaseDate" to purchase.purchaseDate,
                "itemName" to purchase.itemName,
                "quantity" to purchase.quantity,
                "unit" to purchase.unit,
                "pricePerUnit" to purchase.pricePerUnit,
                "totalAmount" to purchase.totalAmount,
                "paymentStatus" to purchase.paymentStatus.name,
                "amountPaid" to purchase.amountPaid,
                "notes" to purchase.notes,
                "status" to purchase.status.name,
                "createdAt" to com.google.cloud.Timestamp.now()
            )

            val docRef = getPurchasesCollection().document()
            docRef.set(purchaseData).get(10, TimeUnit.SECONDS)

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePurchase(purchaseId: String, purchase: Purchase): Result<Unit> {
        return try {
            val purchaseData = mapOf(
                "customerId" to purchase.customerId,
                "firmName" to purchase.firmName,
                "purchaseDate" to purchase.purchaseDate,
                "itemName" to purchase.itemName,
                "quantity" to purchase.quantity,
                "unit" to purchase.unit,
                "pricePerUnit" to purchase.pricePerUnit,
                "totalAmount" to purchase.totalAmount,
                "paymentStatus" to purchase.paymentStatus.name,
                "amountPaid" to purchase.amountPaid,
                "notes" to purchase.notes
            )

            getPurchasesCollection()
                .document(purchaseId)
                .update(purchaseData)
                .get(10, TimeUnit.SECONDS)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePurchase(purchaseId: String): Result<Unit> {
        return try {
            getPurchasesCollection()
                .document(purchaseId)
                .delete()
                .get(10, TimeUnit.SECONDS)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reversePurchase(purchaseId: String, reason: String): Result<Unit> {
        return try {
            val updateData = mapOf(
                "status" to TransactionStatus.REVERSED.name,
                "reversedAt" to com.google.cloud.Timestamp.now(),
                "reversalReason" to reason
            )

            getPurchasesCollection()
                .document(purchaseId)
                .update(updateData)
                .get(10, TimeUnit.SECONDS)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToPurchases(onPurchasesChanged: (List<Purchase>) -> Unit) {
        if (userId.isBlank() || appId.isBlank()) {
            onPurchasesChanged(emptyList())
            return
        }

        getPurchasesCollection()
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Error listening to purchases: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val purchases = snapshot.documents.mapNotNull { doc ->
                        try {
                            Purchase(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                customerId = doc.getString("customerId") ?: "",
                                firmName = doc.getString("firmName") ?: "",
                                purchaseDate = doc.getString("purchaseDate") ?: "",
                                itemName = doc.getString("itemName") ?: "RICE",
                                quantity = doc.getDouble("quantity") ?: 0.0,
                                unit = doc.getString("unit") ?: "kg",
                                pricePerUnit = doc.getDouble("pricePerUnit") ?: 0.0,
                                totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                                paymentStatus = PaymentStatus.valueOf(
                                    doc.getString("paymentStatus") ?: "PENDING"
                                ),
                                amountPaid = doc.getDouble("amountPaid") ?: 0.0,
                                notes = doc.getString("notes") ?: "",
                                status = TransactionStatus.valueOf(
                                    doc.getString("status") ?: "APPROVED"
                                ),
                                reversedAt = doc.getTimestamp("reversedAt"),
                                reversalReason = doc.getString("reversalReason") ?: "",
                                createdAt = doc.getTimestamp("createdAt")
                            )
                        } catch (e: Exception) {
                            println("Error parsing purchase: ${e.message}")
                            null
                        }
                    }
                    onPurchasesChanged(purchases)
                }
            }
    }
}