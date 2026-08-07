package com.example.yakallim.ocr.model

data class PrescribedMedicine(
    val medicineName: String,
    val dosagePerTake: String?,
    val dailyFrequency: Int?,
    val durationDays: Int?,
    val bounds: List<Polygon> = emptyList()
)

