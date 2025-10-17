package com.humblecoders.plantmanagement.data

data class ProductionRecord(
    val id: String = "",
    val batchNumber: String = "",
    val quantityProduced: Double = 0.0, // FRK quantity produced
    val productionDate: com.google.cloud.Timestamp? = null,
    val supervisorName: String = "",
    val notes: String = "",
    val rawMaterialsUsed: Map<String, Double> = emptyMap(), // itemId -> quantity used
    val wasteTracking: WasteTracking? = null,
    val createdAt: com.google.cloud.Timestamp? = null
)

data class WasteTracking(
    val wastage: Double = 0.0,
    val burn: Double = 0.0,
    val regrind: Double = 0.0,
    val others: Double = 0.0
) {
    fun getTotalWaste(): Double = wastage + burn + regrind + others
}

data class ProductionInput(
    val batchNumber: String = "",
    val quantityProduced: Double = 0.0,
    val productionDate: Long = System.currentTimeMillis(),
    val supervisorName: String = "",
    val notes: String = "",
    val rawMaterialsUsed: Map<String, Double> = emptyMap(),
    val wasteTracking: WasteTracking? = null
)
