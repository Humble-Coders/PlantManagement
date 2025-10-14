package com.humblecoders.plantmanagement.repositories

import com.google.cloud.firestore.Firestore
import com.humblecoders.plantmanagement.data.Purchase
import com.humblecoders.plantmanagement.data.PurchaseItem
import com.humblecoders.plantmanagement.data.PaymentStatus
import com.humblecoders.plantmanagement.data.TransactionStatus
import java.util.concurrent.TimeUnit

class PurchaseRepository(
    private val firestore: Firestore,
    private val userId: String,
    private val appId: String
) {

    private fun getPurchasesCollection() = firestore.collection("purchases")
    private fun getEntitiesCollection() = firestore.collection("customers")
    private fun getInventoryCollection() = firestore.collection("inventory")

    suspend fun addPurchase(purchase: Purchase): Result<String> {
        return try {
            val itemsData = purchase.items.map { item ->
                mapOf(
                    "inventoryItemId" to item.inventoryItemId,
                    "itemName" to item.itemName,
                    "quantity" to item.quantity,
                    "unit" to item.unit,
                    "pricePerUnit" to item.pricePerUnit,
                    "totalPrice" to item.totalPrice
                )
            }

            val purchaseData = mapOf(
                "userId" to userId,
                "customerId" to purchase.customerId,
                "firmName" to purchase.firmName,
                "purchaseDate" to purchase.purchaseDate,
                "items" to itemsData,
                "totalAmount" to purchase.totalAmount,
                "gstRate" to purchase.gstRate,
                "gstAmount" to purchase.gstAmount,
                "grandTotal" to purchase.grandTotal,
                "paymentStatus" to purchase.paymentStatus.name,
                "amountPaid" to purchase.amountPaid,
                "notes" to purchase.notes,
                "status" to purchase.status.name,
                "createdAt" to com.google.cloud.Timestamp.now()
            )

            val docRef = getPurchasesCollection().document()
            
            // Use Firestore transaction to update entity balance and inventory atomically
            firestore.runTransaction { transaction ->
                // IMPORTANT: All reads must come before any writes in Firestore transactions
                
                // Read entity balance first
                val entityRef = getEntitiesCollection().document(purchase.customerId)
                val entityDoc = transaction.get(entityRef).get()
                
                // Read all inventory items first
                val inventoryRefsAndDocs = purchase.items.map { item ->
                    val inventoryRef = getInventoryCollection().document(item.inventoryItemId)
                    val inventoryDoc = transaction.get(inventoryRef).get()
                    Pair(inventoryRef, inventoryDoc)
                }
                
                // Now perform all writes
                
                // Add purchase
                transaction.set(docRef, purchaseData)
                
                // Calculate pending amount using grand total
                val pendingAmount = purchase.grandTotal - purchase.amountPaid
                
                // Update entity balance: balance = balance + (-pendingAmount)
                if (entityDoc.exists()) {
                    val currentBalance = entityDoc.getDouble("balance") ?: 0.0
                    val newBalance = currentBalance + (-pendingAmount)
                    transaction.update(entityRef, "balance", newBalance)
                }
                
                // Update inventory quantities (increase them)
                purchase.items.forEachIndexed { index, item ->
                    val (inventoryRef, inventoryDoc) = inventoryRefsAndDocs[index]
                    if (inventoryDoc.exists()) {
                        val currentQuantity = inventoryDoc.getDouble("quantity") ?: 0.0
                        val newQuantity = currentQuantity + item.quantity
                        transaction.update(inventoryRef, "quantity", newQuantity)
                    }
                }
                
                null
            }.get(10, TimeUnit.SECONDS)

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePurchase(purchaseId: String, purchase: Purchase): Result<Unit> {
        return try {
            val itemsData = purchase.items.map { item ->
                mapOf(
                    "inventoryItemId" to item.inventoryItemId,
                    "itemName" to item.itemName,
                    "quantity" to item.quantity,
                    "unit" to item.unit,
                    "pricePerUnit" to item.pricePerUnit,
                    "totalPrice" to item.totalPrice
                )
            }

            val purchaseData = mapOf(
                "customerId" to purchase.customerId,
                "firmName" to purchase.firmName,
                "purchaseDate" to purchase.purchaseDate,
                "items" to itemsData,
                "totalAmount" to purchase.totalAmount,
                "gstRate" to purchase.gstRate,
                "gstAmount" to purchase.gstAmount,
                "grandTotal" to purchase.grandTotal,
                "paymentStatus" to purchase.paymentStatus.name,
                "amountPaid" to purchase.amountPaid,
                "notes" to purchase.notes
            )

            // Use transaction to update purchase and adjust entity balance and inventory
            firestore.runTransaction { transaction ->
                // IMPORTANT: All reads must come before any writes in Firestore transactions
                
                // Read old purchase first
                val purchaseRef = getPurchasesCollection().document(purchaseId)
                val oldPurchaseDoc = transaction.get(purchaseRef).get()
                
                if (oldPurchaseDoc.exists()) {
                    // Get old purchase data
                    val oldGrandTotal = oldPurchaseDoc.getDouble("grandTotal") ?: oldPurchaseDoc.getDouble("totalAmount") ?: 0.0
                    val oldAmountPaid = oldPurchaseDoc.getDouble("amountPaid") ?: 0.0
                    val oldPendingAmount = oldGrandTotal - oldAmountPaid
                    val oldCustomerId = oldPurchaseDoc.getString("customerId") ?: ""
                    
                    // Get old items
                    val oldItemsList = oldPurchaseDoc.get("items") as? List<Map<String, Any>> ?: emptyList()
                    
                    // Read entity balance
                    val entityRef = getEntitiesCollection().document(purchase.customerId)
                    val entityDoc = transaction.get(entityRef).get()
                    
                    // Read all old inventory items
                    val oldInventoryRefsAndDocs = oldItemsList.mapNotNull { itemMap ->
                        val inventoryItemId = itemMap["inventoryItemId"] as? String ?: ""
                        if (inventoryItemId.isNotBlank()) {
                            val inventoryRef = getInventoryCollection().document(inventoryItemId)
                            val inventoryDoc = transaction.get(inventoryRef).get()
                            val oldQuantity = (itemMap["quantity"] as? Number)?.toDouble() ?: 0.0
                            Triple(inventoryRef, inventoryDoc, oldQuantity)
                        } else null
                    }
                    
                    // Read all new inventory items
                    val newInventoryRefsAndDocs = purchase.items.map { item ->
                        val inventoryRef = getInventoryCollection().document(item.inventoryItemId)
                        val inventoryDoc = transaction.get(inventoryRef).get()
                        Pair(inventoryRef, inventoryDoc)
                    }
                    
                    // Now perform all writes
                    
                    // Calculate new pending amount using grand total
                    val newPendingAmount = purchase.grandTotal - purchase.amountPaid
                    
                    // Update entity balance
                    if (oldCustomerId == purchase.customerId && oldCustomerId.isNotBlank() && entityDoc.exists()) {
                        val currentBalance = entityDoc.getDouble("balance") ?: 0.0
                        val balanceChange = oldPendingAmount - newPendingAmount
                        val newBalance = currentBalance + balanceChange
                        transaction.update(entityRef, "balance", newBalance)
                    }
                    
                    // Update inventory quantities - reverse old quantities (subtract)
                    oldInventoryRefsAndDocs.forEach { (inventoryRef, inventoryDoc, oldQuantity) ->
                        if (inventoryDoc.exists()) {
                            val currentQuantity = inventoryDoc.getDouble("quantity") ?: 0.0
                            val adjustedQuantity = currentQuantity - oldQuantity
                            transaction.update(inventoryRef, "quantity", adjustedQuantity)
                        }
                    }
                    
                    // Update inventory quantities - apply new quantities (add)
                    purchase.items.forEachIndexed { index, item ->
                        val (inventoryRef, inventoryDoc) = newInventoryRefsAndDocs[index]
                        if (inventoryDoc.exists()) {
                            val currentQuantity = inventoryDoc.getDouble("quantity") ?: 0.0
                            val newQuantity = currentQuantity + item.quantity
                            transaction.update(inventoryRef, "quantity", newQuantity)
                        }
                    }
                    
                    // Update purchase
                    transaction.update(purchaseRef, purchaseData)
                }
                
                null
            }.get(10, TimeUnit.SECONDS)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePurchase(purchaseId: String): Result<Unit> {
        return try {
            // Use transaction to delete purchase and reverse entity balance and inventory changes
            firestore.runTransaction { transaction ->
                // IMPORTANT: All reads must come before any writes in Firestore transactions
                
                // Read purchase first
                val purchaseRef = getPurchasesCollection().document(purchaseId)
                val purchaseDoc = transaction.get(purchaseRef).get()
                
                if (purchaseDoc.exists()) {
                    // Get purchase data
                    val grandTotal = purchaseDoc.getDouble("grandTotal") ?: purchaseDoc.getDouble("totalAmount") ?: 0.0
                    val amountPaid = purchaseDoc.getDouble("amountPaid") ?: 0.0
                    val pendingAmount = grandTotal - amountPaid
                    val customerId = purchaseDoc.getString("customerId") ?: ""
                    
                    // Get items
                    val itemsList = purchaseDoc.get("items") as? List<Map<String, Any>> ?: emptyList()
                    
                    // Read entity balance
                    val entityRef = if (customerId.isNotBlank()) getEntitiesCollection().document(customerId) else null
                    val entityDoc = entityRef?.let { transaction.get(it).get() }
                    
                    // Read all inventory items
                    val inventoryRefsAndDocs = itemsList.mapNotNull { itemMap ->
                        val inventoryItemId = itemMap["inventoryItemId"] as? String ?: ""
                        val quantity = (itemMap["quantity"] as? Number)?.toDouble() ?: 0.0
                        
                        if (inventoryItemId.isNotBlank()) {
                            val inventoryRef = getInventoryCollection().document(inventoryItemId)
                            val inventoryDoc = transaction.get(inventoryRef).get()
                            Triple(inventoryRef, inventoryDoc, quantity)
                        } else null
                    }
                    
                    // Now perform all writes
                    
                    // Reverse entity balance: balance = balance - (-pendingAmount) = balance + pendingAmount
                    if (entityRef != null && entityDoc?.exists() == true) {
                        val currentBalance = entityDoc.getDouble("balance") ?: 0.0
                        val newBalance = currentBalance + pendingAmount
                        transaction.update(entityRef, "balance", newBalance)
                    }
                    
                    // Reverse inventory quantities (subtract what was added)
                    inventoryRefsAndDocs.forEach { (inventoryRef, inventoryDoc, quantity) ->
                        if (inventoryDoc.exists()) {
                            val currentQuantity = inventoryDoc.getDouble("quantity") ?: 0.0
                            val newQuantity = currentQuantity - quantity
                            transaction.update(inventoryRef, "quantity", newQuantity)
                        }
                    }
                    
                    // Delete purchase
                    transaction.delete(purchaseRef)
                }
                
                null
            }.get(10, TimeUnit.SECONDS)

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