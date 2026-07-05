package com.example.yakallim.medicine.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "medicine.standard")
data class MedicineProperties(
    val dataPath: String = "data/medicines.csv",
    val similarityThreshold: Int = 8
)
