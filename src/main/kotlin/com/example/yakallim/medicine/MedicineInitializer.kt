package com.example.yakallim.medicine

import com.example.yakallim.medicine.model.Medicine
import com.example.yakallim.medicine.repository.MedicineRepository
import com.example.yakallim.medicine.config.MedicineProperties
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
                log.info("Medicine data initialized successfully (total {} records, path: {})", entities.size, medicineProperties.dataPath)
            } else {
                log.info("Medicine data already exists (path: {})", medicineProperties.dataPath)
            }
        } catch (e: Exception) {
            log.error("Failed to initialize medicine data (path: {})", medicineProperties.dataPath, e)
        }
    }
}
