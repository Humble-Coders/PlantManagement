package com.humblecoders.plantmanagement.data

import com.google.cloud.Timestamp


data class Expense(
    val id: String = "",
    val userId: String = "",
    val categoryId: String = "",
    val amount: Double = 0.0,
    val date: Timestamp? = null,
    val notes: String = "",
    val createdAt: Timestamp? = null,
    val documentUrls: List<String> = emptyList()
)


data class ExpenseCategory(
    val id: String = "",
    val name: String = "",
    val createdAt: Timestamp? = null
)