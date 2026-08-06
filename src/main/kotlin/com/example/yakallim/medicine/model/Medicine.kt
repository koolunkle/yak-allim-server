package com.example.yakallim.medicine.model

import com.example.yakallim.global.utils.HangulUtils
import jakarta.persistence.*

@Entity
@Table(
    name = "medicine",
    indexes = [Index(name = "idx_normalized_name", columnList = "normalized_name")]
)
class Medicine(
    @Id
    @Column(name = "name")
    var name: String = ""
) {
    @Column(name = "normalized_name", nullable = false)
    var normalizedName: String = HangulUtils.normalizeToJamo(name)

    @PrePersist
    @PreUpdate
    private fun updateNormalizedName() {
        this.normalizedName = HangulUtils.normalizeToJamo(this.name)
    }
}
