package com.example.yakallim.ocr.presentation.dto

data class Prescription(
    val medicineName: String,
    val dosagePerTake: String?,
    val dailyFrequency: Int?,
    val durationDays: Int?,
    val bounds: List<Polygon> = emptyList()
) {
    data class Polygon(val points: List<Coordinate>)
    data class Coordinate(val x: Int, val y: Int)
}
