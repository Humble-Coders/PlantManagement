package com.humblecoders.plantmanagement.data

import java.time.LocalDate

data class Note(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val isCompleted: Boolean = false,
    val createdAt: com.google.cloud.Timestamp? = null
)
