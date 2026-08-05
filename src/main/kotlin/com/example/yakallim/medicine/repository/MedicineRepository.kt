package com.example.yakallim.medicine.repository

import com.example.yakallim.medicine.model.Medicine
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MedicineRepository : JpaRepository<Medicine, String> {
    fun findByNormalizedNameStartingWith(prefix: String): List<Medicine>
}
