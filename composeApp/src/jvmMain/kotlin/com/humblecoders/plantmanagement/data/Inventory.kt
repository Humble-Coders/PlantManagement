package com.humblecoders.plantmanagement.data

import com.google.cloud.Timestamp

data class InventoryItem(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val quantity: Double = 0.0,
    val unit: String = "kg",
    val categoryType: CategoryType = CategoryType.RAW_MATERIAL,
    val createdAt: Timestamp? = null
)

enum class CategoryType {
    RAW_MATERIAL,
    OTHER
}