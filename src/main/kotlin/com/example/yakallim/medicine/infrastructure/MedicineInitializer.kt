package com.example.yakallim.medicine.infrastructure

import com.example.yakallim.medicine.domain.model.Medicine
import com.example.yakallim.medicine.domain.repository.MedicineRepository
import com.example.yakallim.medicine.infrastructure.config.MedicineProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

@Component
@EnableConfigurationProperties(MedicineProperties::class)
class MedicineInitializer(
    private val medicineRepository: MedicineRepository,
    private val medicineProperties: MedicineProperties
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(MedicineInitializer::class.java)

    override fun run(vararg args: String?) {
        try {
            if (medicineRepository.count() == 0L) {
                val resource = ClassPathResource(medicineProperties.dataPath)
                val entities = resource.inputStream.use { inputStream ->
                    inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                        lines.filter { it.isNotBlank() }
                            .map { Medicine(it.trim()) }
                            .toList()
                    }
                }
                medicineRepository.saveAll(entities)
                log.info("약품 데이터 초기화 완료 (총 {}건, 파일 경로: {})", entities.size, medicineProperties.dataPath)
            } else {
                log.info("약품 데이터 조회 완료 (파일 경로: {})", medicineProperties.dataPath)
            }
        } catch (e: Exception) {
            log.error("약품 데이터 초기화 실패 (파일 경로: {})", medicineProperties.dataPath, e)
        }
    }
}
