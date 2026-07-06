package com.example.yakallim.ocr.application

import com.example.yakallim.ocr.domain.model.PipelineStep
import com.example.yakallim.ocr.domain.repository.OcrJobRepository
import com.example.yakallim.ocr.presentation.dto.OcrJobResponse
import com.example.yakallim.ocr.presentation.dto.OcrProgressResponse
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.*

@Component
class OcrProgressManager(
    private val ocrJobRepository: OcrJobRepository
) {
    private val log = LoggerFactory.getLogger(OcrProgressManager::class.java)

    private val emittersMap = ConcurrentHashMap<String, MutableList<SseEmitter>>()
    private val progressCache = ConcurrentHashMap<String, OcrProgressResponse>()

    private val keepAliveScheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "sse-keep-alive-scheduler").apply { isDaemon = true }
    }

    @PostConstruct
    fun startKeepAliveScheduler() {
        keepAliveScheduler.scheduleAtFixedRate({
            sendKeepAliveToAll()
        }, 15, 15, TimeUnit.SECONDS)
        log.info("SSE Keep-Alive Heartbeat 스케줄러 구동 완료 (주기: 15초)")
    }

    @PreDestroy
    fun stopKeepAliveScheduler() {
        keepAliveScheduler.shutdown()
        try {
            if (!keepAliveScheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                keepAliveScheduler.shutdownNow()
            }
        } catch (_: InterruptedException) {
            keepAliveScheduler.shutdownNow()
        }
        log.info("SSE Keep-Alive Heartbeat 스케줄러 정상 종료")
    }

    fun registerEmitter(jobId: String, timeoutMs: Long = 180000L): SseEmitter {
        val emitter = SseEmitter(timeoutMs)

        val job = ocrJobRepository.getJob(jobId)
        if (job != null && (job.status == OcrJobResponse.JobStatus.COMPLETED ||
                    job.status == OcrJobResponse.JobStatus.FAILED ||
                    job.status == OcrJobResponse.JobStatus.CANCELLED)
        ) {
            val step = when (job.status) {
                OcrJobResponse.JobStatus.COMPLETED -> PipelineStep.COMPLETED
                OcrJobResponse.JobStatus.FAILED -> PipelineStep.FAILED
                OcrJobResponse.JobStatus.CANCELLED -> PipelineStep.FAILED
                else -> PipelineStep.FAILED
            }
            val message = when (job.status) {
                OcrJobResponse.JobStatus.COMPLETED -> job.result?.message ?: step.defaultMessage
                OcrJobResponse.JobStatus.FAILED -> job.error ?: step.defaultMessage
                OcrJobResponse.JobStatus.CANCELLED -> "작업이 취소되었습니다."
                else -> step.defaultMessage
            }
            val payload = OcrProgressResponse(
                step = step,
                message = message,
                progress = 100,
                isFinished = true
            )
            try {
                emitter.send(
                    SseEmitter.event()
                        .name("connect")
                        .reconnectTime(3000L)
                        .data("Connected to progress stream for job: $jobId")
                )
                sendToEmitter(emitter, payload)
                emitter.complete()
            } catch (_: IOException) {
                emitter.complete()
            }
            return emitter
        }

        emittersMap.computeIfAbsent(jobId) { CopyOnWriteArrayList() }.add(emitter)

        emitter.onCompletion {
            removeEmitter(jobId, emitter)
        }
        emitter.onTimeout {
            emitter.complete()
            removeEmitter(jobId, emitter)
        }
        emitter.onError {
            emitter.complete()
            removeEmitter(jobId, emitter)
        }

        try {
            emitter.send(
                SseEmitter.event()
                    .name("connect")
                    .reconnectTime(3000L)
                    .data("Connected to progress stream for job: $jobId")
            )
        } catch (_: IOException) {
            emitter.complete()
            removeEmitter(jobId, emitter)
            return emitter
        }

        val cached = progressCache[jobId]
        if (cached != null) {
            try {
                sendToEmitter(emitter, cached)
                if (cached.isFinished) {
                    emitter.complete()
                    removeEmitter(jobId, emitter)
                }
            } catch (_: Exception) {
                try {
                    emitter.complete()
                } catch (_: Exception) {}
                removeEmitter(jobId, emitter)
            }
        }

        return emitter
    }

    fun publishProgress(jobId: String, step: PipelineStep, message: String? = null, progress: Int? = null) {
        val finalMessage = message ?: step.defaultMessage
        val finalProgress = progress ?: step.defaultProgress
        val isFinished = step == PipelineStep.COMPLETED || step == PipelineStep.FAILED

        val payload = OcrProgressResponse(
            step = step,
            message = finalMessage,
            progress = finalProgress,
            isFinished = isFinished
        )

        if (isFinished) {
            progressCache.remove(jobId)
        } else {
            progressCache[jobId] = payload
        }

        log.info("Job [$jobId] Progress: step=$step, progress=$finalProgress%, message='$finalMessage'")

        val emitters = emittersMap[jobId] ?: return
        val failedEmitters = mutableListOf<SseEmitter>()

        for (emitter in emitters) {
            try {
                sendToEmitter(emitter, payload)
                if (isFinished) {
                    emitter.complete()
                    failedEmitters.add(emitter)
                }
            } catch (_: Exception) {
                try {
                    emitter.complete()
                } catch (_: Exception) {}
                failedEmitters.add(emitter)
            }
        }

        if (failedEmitters.isNotEmpty()) {
            emitters.removeAll(failedEmitters)
            if (emitters.isEmpty()) {
                emittersMap.remove(jobId)
            }
        }

        if (isFinished) {
            emittersMap.remove(jobId)
        }
    }

    private fun sendKeepAliveToAll() {
        val activeJobs = emittersMap.keys.toList()
        for (jobId in activeJobs) {
            val emitters = emittersMap[jobId] ?: continue
            val failedEmitters = mutableListOf<SseEmitter>()

            for (emitter in emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("keep-alive"))
                } catch (_: Exception) {
                    emitter.complete()
                    failedEmitters.add(emitter)
                }
            }

            if (failedEmitters.isNotEmpty()) {
                emitters.removeAll(failedEmitters)
                if (emitters.isEmpty()) {
                    emittersMap.remove(jobId)
                }
            }
        }
    }

    private fun sendToEmitter(emitter: SseEmitter, payload: OcrProgressResponse) {
        emitter.send(
            SseEmitter.event()
                .name("progress")
                .data(payload)
        )
    }

    private fun removeEmitter(jobId: String, emitter: SseEmitter) {
        val emitters = emittersMap[jobId] ?: return
        emitters.remove(emitter)
        if (emitters.isEmpty()) {
            emittersMap.remove(jobId)
        }
    }
}
