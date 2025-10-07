package com.humblecoders.plantmanagement.data

data class User(
    val uid: String = "",
    val email: String = "",
    val role: UserRole = UserRole.USER
)