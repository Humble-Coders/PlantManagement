package com.humblecoders.plantmanagement.data

data class Entity(
    val id: String = "",
    val firmName: String = "",
    val contactPerson: String = "",
    val contactNo: String = "",
    val city: String = "",
    val state: String = "",
    val gstin: String = "",
    val balance: Double = 0.0,
    val createdAt: com.google.cloud.Timestamp? = null
)