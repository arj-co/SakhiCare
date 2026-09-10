package com.sakhicare.app.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "workers")
data class WorkerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val role: String = "ASHA",
    val phone: String,
    val facilityId: String,
    val facilityName: String,
    val locale: String = "hi-IN",
    val status: String = "ACTIVE",
    val lastSeenAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "pregnancy_cases")
data class PregnancyCaseEntity(
    @PrimaryKey val id: String,
    val localId: String,
    val patientName: String,
    val village: String,
    val ageYears: Int?,
    val gestationalAgeWeeks: Int?,
    val gravida: Int?,
    val para: Int?,
    val travelConstraints: String?,
    val workerId: String,
    val facilityId: String,
    val syncStatus: String = "SAVED_LOCALLY",
    val doctorAdvisory: String? = null,
    val ambulanceStatus: String? = null,
    val isDemo: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "assessments",
    foreignKeys = [
        ForeignKey(
            entity = PregnancyCaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["caseId"])]
)
data class AssessmentEntity(
    @PrimaryKey val id: String,
    val caseId: String,
    val rulePackVersion: String = "mohfw-hrp-v1.0",
    val riskLevel: String, // RED, AMBER, GREEN
    val riskScore: Int,
    val primaryFactorsJson: String,
    val unmeasuredVitalsJson: String,
    val clinicalRationale: String,
    val recommendedProtocol: String,
    val ashaSafeActionsJson: String,
    val clinicianDirectedActionsJson: String,
    val requiresImmediateAmbulance: Boolean,
    val requiresBloodTransfusionAlert: Boolean,
    val source: String = "MANUAL",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "observations",
    foreignKeys = [
        ForeignKey(
            entity = AssessmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["assessmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["assessmentId"]), Index(value = ["caseId"])]
)
data class ObservationEntity(
    @PrimaryKey val id: String,
    val assessmentId: String,
    val caseId: String,
    val type: String, // BP_SYSTOLIC, BP_DIASTOLIC, HAEMOGLOBIN, DANGER_SIGN, etc.
    val value: String?,
    val unit: String?,
    val isMeasured: Boolean,
    val source: String = "MANUAL",
    val confidence: Float = 1.0f,
    val confirmedByWorker: Boolean = true,
    val measuredAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "case_events",
    foreignKeys = [
        ForeignKey(
            entity = PregnancyCaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["caseId"])]
)
data class CaseEventEntity(
    @PrimaryKey val id: String,
    val caseId: String,
    val eventType: String, // CASE_CREATED, ASSESSMENT_RECORDED, SYNC_QUEUED, SYNC_ACKNOWLEDGED, SYNC_FAILED
    val actorId: String,
    val actorRole: String,
    val summary: String,
    val detailsJson: String? = null,
    val occurredAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "outbox_items",
    indices = [Index(value = ["status"]), Index(value = ["nextAttemptAt"])]
)
data class OutboxItemEntity(
    @PrimaryKey val id: String, // Idempotency key UUID
    val caseId: String,
    val entityType: String = "CASE_ASSESSMENT",
    val payloadJson: String,
    val attempts: Int = 0,
    val maxAttempts: Int = 5,
    val nextAttemptAt: Long = System.currentTimeMillis(),
    val status: String = "QUEUED", // QUEUED, UPLOADING, ACKNOWLEDGED, FAILED, NEEDS_REVIEW
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "voice_artifacts",
    foreignKeys = [
        ForeignKey(
            entity = PregnancyCaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["caseId"]), Index(value = ["uploadStatus"])]
)
data class VoiceArtifactEntity(
    @PrimaryKey val id: String,
    val caseId: String,
    val localAudioPath: String,
    val mimeType: String = "audio/m4a",
    val fileSizeBytes: Long,
    val sha256Checksum: String,
    val language: String = "hi-IN",
    val durationSeconds: Int,
    val transcript: String? = null,
    val processingStatus: String = "UNPROCESSED", // UNPROCESSED, PROCESSING, EXTRACTED, CONFIRMED, FAILED
    val uploadStatus: String = "LOCAL_ONLY", // LOCAL_ONLY, QUEUED, UPLOADING, UPLOADED, FAILED
    val retentionDeadlineTimestamp: Long = System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000L), // 90 days retention
    val confirmedAtTimestamp: Long? = null,
    val uploadedAtTimestamp: Long? = null,
    val createdAtTimestamp: Long = System.currentTimeMillis()
)

