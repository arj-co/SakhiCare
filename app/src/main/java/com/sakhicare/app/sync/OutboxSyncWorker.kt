package com.sakhicare.app.sync

import android.content.Context
import androidx.work.*
import com.google.gson.Gson
import com.sakhicare.app.data.db.AppDatabase
import com.sakhicare.app.data.db.entities.CaseEventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody

/**
 * WorkManager Outbox Worker
 *
 * Implements exponential backoff, network constraints, idempotency keys,
 * retry limits, and failure visibility. Never falsely marks a case as Synced
 * without explicit server acknowledgment.
 */
class OutboxSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val gson = Gson()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = AppDatabase.getInstance(applicationContext)
        val outboxDao = database.outboxDao()
        val caseDao = database.caseDao()
        val caseEventDao = database.caseEventDao()

        val pendingItems = outboxDao.getPendingItems()
        if (pendingItems.isEmpty()) {
            return@withContext Result.success()
        }

        var anyFailed = false

        for (item in pendingItems) {
            try {
                // Update status to UPLOADING
                outboxDao.updateStatus(item.id, "UPLOADING")

                if (item.entityType == "VOICE_AUDIO") {
                    val audioPayload = gson.fromJson(item.payloadJson, Map::class.java)
                    val filePath = audioPayload["file_path"] as? String
                    val sha256 = audioPayload["sha256"] as? String ?: ""
                    val artifactId = audioPayload["artifact_id"] as? String ?: ""
                    val duration = (audioPayload["duration_seconds"] as? Number)?.toInt() ?: 0
                    val language = audioPayload["language"] as? String ?: "hi-IN"

                    val file = if (filePath != null) java.io.File(filePath) else null
                    if (file != null && file.exists()) {
                        val requestFile = file.asRequestBody("audio/m4a".toMediaTypeOrNull())
                        val filePart = okhttp3.MultipartBody.Part.createFormData("file", file.name, requestFile)

                        val response = SakhiCareApiClient.service.uploadCaseAudio(
                            caseId = item.caseId,
                            idempotencyKey = item.id,
                            sha256 = sha256,
                            file = filePart,
                            durationSeconds = duration,
                            language = language
                        )

                        if (response.isSuccessful) {
                            outboxDao.recordAttemptResult(item.id, "ACKNOWLEDGED", null)
                            database.voiceArtifactDao().updateUploadStatus(artifactId, "UPLOADED")
                            caseEventDao.insertEvent(
                                CaseEventEntity(
                                    id = UUID.randomUUID().toString(),
                                    caseId = item.caseId,
                                    eventType = "AUDIO_SYNC_ACKNOWLEDGED",
                                    actorId = "SYSTEM_WORKER",
                                    actorRole = "SYSTEM",
                                    summary = "Voice note uploaded and acknowledged by Care Desk (SHA: ${sha256.take(8)}...)",
                                    occurredAt = System.currentTimeMillis()
                                )
                            )
                        } else {
                            val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                            val nextStatus = if (item.attempts + 1 >= item.maxAttempts) "FAILED" else "QUEUED"
                            outboxDao.recordAttemptResult(item.id, nextStatus, errorMsg)
                            anyFailed = true
                        }
                    } else {
                        outboxDao.recordAttemptResult(item.id, "FAILED", "Audio file not found on device storage")
                        database.voiceArtifactDao().updateUploadStatus(artifactId, "FAILED")
                        anyFailed = true
                    }
                } else {
                    caseDao.updateSyncStatus(item.caseId, "UPLOADING")
                    val request = gson.fromJson(item.payloadJson, SyncBatchRequest::class.java)
                    val response = SakhiCareApiClient.service.syncBatch(
                        idempotencyKey = item.id,
                        request = request
                    )

                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        // Server acknowledged the record
                        outboxDao.recordAttemptResult(item.id, "ACKNOWLEDGED", null)
                        caseDao.updateSyncStatus(item.caseId, "ACKNOWLEDGED")

                        caseEventDao.insertEvent(
                            CaseEventEntity(
                                id = UUID.randomUUID().toString(),
                                caseId = item.caseId,
                                eventType = "SYNC_ACKNOWLEDGED",
                                actorId = "SYSTEM_WORKER",
                                actorRole = "SYSTEM",
                                summary = "Server acknowledged sync receipt at ${body.serverTimestamp}",
                                occurredAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                        val nextStatus = if (item.attempts + 1 >= item.maxAttempts) "FAILED" else "QUEUED"
                        outboxDao.recordAttemptResult(item.id, nextStatus, errorMsg)
                        caseDao.updateSyncStatus(item.caseId, nextStatus)
                        anyFailed = true

                        caseEventDao.insertEvent(
                            CaseEventEntity(
                                id = UUID.randomUUID().toString(),
                                caseId = item.caseId,
                                eventType = "SYNC_ATTEMPT_FAILED",
                                actorId = "SYSTEM_WORKER",
                                actorRole = "SYSTEM",
                                summary = "Sync attempt failed: $errorMsg",
                                occurredAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "Network error: ${e.localizedMessage ?: "Unknown error"}"
                val nextStatus = if (item.attempts + 1 >= item.maxAttempts) "FAILED" else "QUEUED"
                outboxDao.recordAttemptResult(item.id, nextStatus, errorMsg)
                caseDao.updateSyncStatus(item.caseId, nextStatus)
                anyFailed = true

                caseEventDao.insertEvent(
                    CaseEventEntity(
                        id = UUID.randomUUID().toString(),
                        caseId = item.caseId,
                        eventType = "SYNC_ATTEMPT_FAILED",
                        actorId = "SYSTEM_WORKER",
                        actorRole = "SYSTEM",
                        summary = "Sync attempt error: $errorMsg",
                        occurredAt = System.currentTimeMillis()
                    )
                )
            }
        }

        if (anyFailed) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    companion object {
        const val WORK_NAME = "SakhiCareOutboxSyncWork"

        fun enqueueSync(context: Context, force: Boolean = false) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<OutboxSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.SECONDS
                )
                .addTag(WORK_NAME)
                .build()

            val workManager = WorkManager.getInstance(context)
            if (force) workManager.cancelUniqueWork(WORK_NAME)
            workManager.enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
