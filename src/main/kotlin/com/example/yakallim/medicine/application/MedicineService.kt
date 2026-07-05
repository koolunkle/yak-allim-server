package com.example.yakallim.medicine.application

import com.example.yakallim.global.utils.HangulUtils
import com.example.yakallim.medicine.domain.repository.MedicineRepository
import com.example.yakallim.medicine.infrastructure.config.MedicineProperties
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MedicineService(
    private val medicineRepository: MedicineRepository,
    private val medicineProperties: MedicineProperties
) {

    companion object {
        private val nameCleanupRegex = Regex("[-_\\s]+$")
    }

    @Transactional(readOnly = true)
    fun findStandardName(rawName: String): String {
        val cleanedName = rawName.trim().replace(nameCleanupRegex, "").replace(" ", "")
        if (cleanedName.isEmpty()) return ""

        val normalizedName = HangulUtils.normalizeToJamo(cleanedName)
        val prefix = if (normalizedName.length >= 2) normalizedName.take(2) else normalizedName

        val candidates = if (prefix.isNotEmpty()) {
            val results = medicineRepository.findByNormalizedNameStartingWith(prefix)
            results.ifEmpty { medicineRepository.findAll() }
        } else {
            emptyList()
        }

        if (candidates.isEmpty()) return rawName

        var bestMatch: String? = null
        var minimumDistance = Int.MAX_VALUE

        for (candidate in candidates) {
            val distance = HangulUtils.levenshteinDistanceTo(normalizedName, candidate.normalizedName)
            if (distance < minimumDistance) {
                minimumDistance = distance
                bestMatch = candidate.name
            }
        }

        return if (minimumDistance <= medicineProperties.similarityThreshold && bestMatch != null) bestMatch else rawName
    }
}
