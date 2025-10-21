package com.humblecoders.plantmanagement.data

data class Company(
    val id: String = "",
    val companyName: String = "",
    val address: String = "",
    val stateFssaiLicenseNo: String = "",
    val centreFssaiLicenseNo: String = "",
    val gstinUin: String = "",
    val state: String = "",
    val email: String = "",
    val createdAt: com.google.cloud.Timestamp? = null,
    val updatedAt: com.google.cloud.Timestamp? = null
)
