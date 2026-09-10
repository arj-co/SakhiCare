package com.sakhicare.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.sakhicare.app.data.*
import com.sakhicare.app.data.db.AppDatabase
import com.sakhicare.app.data.db.daos.CaseWithAssessment
import com.sakhicare.app.data.db.entities.*
import com.sakhicare.app.sync.OutboxSyncWorker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class OfflineCaseRepository(
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val gson = Gson()

    val casesFlow: Flow<List<PatientCase>> = database.caseDao().getAllCasesWithAssessmentFlow()
        .map { dbCases ->
            val mapped = dbCases.map { caseWithAssessmentToDomain(it) }
            if (DemoConfig.isDemoEnabled) {
                DemoFixtures.getDemoCases() + mapped
            } else {
                mapped
            }
        }
        .flowOn(ioDispatcher)

    val pendingSyncCountFlow: Flow<Int> = database.outboxDao().getPendingOutboxCountFlow()
        .flowOn(ioDispatcher)

    val lastSuccessfulSyncTimeFlow: Flow<Long?> = database.caseDao().getLastSuccessfulSyncTimeFlow()
        .flowOn(ioDispatcher)

    suspend fun getCaseById(id: String): PatientCase? = withContext(ioDispatcher) {
        if (DemoConfig.isDemoEnabled) {
            val demo = DemoFixtures.getDemoCases().find { it.id == id }
            if (demo != null) return@withContext demo
        }
        val dbCase = database.caseDao().getCaseWithAssessment(id)
        dbCase?.let { caseWithAssessmentToDomain(it) }
    }

    suspend fun saveNewAssessment(
        patientCase: PatientCase,
        workerId: String = "ASHA-DEFAULT",
        facilityId: String = "FACILITY-DEFAULT",
        context: Context? = null,
        audioFile: java.io.File? = null,
        voiceTranscript: String? = null,
        audioDurationSeconds: Int? = null
    ): String = withContext(ioDispatcher) {
        val caseId = patientCase.id.ifBlank { "SC-${UUID.randomUUID().toString().take(8).uppercase()}" }
        val assessmentId = "ASM-${UUID.randomUUID().toString().take(8).uppercase()}"
        val idempotencyKey = UUID.randomUUID().toString()

        // 1. Insert Case Entity
        val caseEntity = PregnancyCaseEntity(
            id = caseId,
            localId = caseId,
            patientName = patientCase.patientName,
            village = patientCase.village,
            ageYears = patientCase.ageYears,
            gestationalAgeWeeks = patientCase.gestationalAgeWeeks,
            gravida = patientCase.gravida,
            para = patientCase.para,
            travelConstraints = patientCase.travelConstraints,
            workerId = workerId,
            facilityId = facilityId,
            syncStatus = "QUEUED",
            doctorAdvisory = null,
            ambulanceStatus = null,
            isDemo = patientCase.isDemo,
            createdAt = patientCase.assessmentTimestamp,
            updatedAt = System.currentTimeMillis()
        )
        database.caseDao().insertCase(caseEntity)

        // 2. Insert Assessment Entity
        val assessmentEntity = AssessmentEntity(
            id = assessmentId,
            caseId = caseId,
            rulePackVersion = patientCase.rulePackVersion,
            riskLevel = patientCase.riskLevel.name,
            riskScore = patientCase.riskScore,
            primaryFactorsJson = gson.toJson(patientCase.dangerSigns.triggeredList()),
            unmeasuredVitalsJson = gson.toJson(patientCase.unmeasuredVitals),
            clinicalRationale = patientCase.clinicalRationale ?: "",
            recommendedProtocol = patientCase.recommendedProtocol ?: "",
            ashaSafeActionsJson = gson.toJson(patientCase.ashaSafeActions),
            clinicianDirectedActionsJson = gson.toJson(patientCase.clinicianDirectedActions),
            requiresImmediateAmbulance = patientCase.riskLevel == RiskLevel.RED,
            requiresBloodTransfusionAlert = patientCase.haemoglobin?.replace("g/dL", "")?.trim()?.toDoubleOrNull()?.let { it < 7.0 } ?: false,
            source = "MANUAL",
            createdAt = System.currentTimeMillis()
        )
        database.assessmentDao().insertAssessment(assessmentEntity)

        // 3. Insert Observations
        val observations = mutableListOf<ObservationEntity>()
        patientCase.bloodPressure?.let { bp ->
            val parts = bp.split("/")
            if (parts.size == 2) {
                observations.add(ObservationEntity(
                    id = UUID.randomUUID().toString(),
                    assessmentId = assessmentId,
                    caseId = caseId,
                    type = "BP_SYSTOLIC",
                    value = parts[0].trim(),
                    unit = "mmHg",
                    isMeasured = true
                ))
                observations.add(ObservationEntity(
                    id = UUID.randomUUID().toString(),
                    assessmentId = assessmentId,
                    caseId = caseId,
                    type = "BP_DIASTOLIC",
                    value = parts[1].trim(),
                    unit = "mmHg",
                    isMeasured = true
                ))
            }
        } ?: run {
            observations.add(ObservationEntity(
                id = UUID.randomUUID().toString(),
                assessmentId = assessmentId,
                caseId = caseId,
                type = "BLOOD_PRESSURE",
                value = null,
                unit = "mmHg",
                isMeasured = false
            ))
        }

        patientCase.haemoglobin?.let { hb ->
            observations.add(ObservationEntity(
                id = UUID.randomUUID().toString(),
                assessmentId = assessmentId,
                caseId = caseId,
                type = "HAEMOGLOBIN",
                value = hb.replace("g/dL", "").trim(),
                unit = "g/dL",
                isMeasured = true
            ))
        } ?: run {
            observations.add(ObservationEntity(
                id = UUID.randomUUID().toString(),
                assessmentId = assessmentId,
                caseId = caseId,
                type = "HAEMOGLOBIN",
                value = null,
                unit = "g/dL",
                isMeasured = false
            ))
        }

        database.observationDao().insertAll(observations)

        // 4. Record Local Case Event (Audit Timeline)
        database.caseEventDao().insertEvent(
            CaseEventEntity(
                id = UUID.randomUUID().toString(),
                caseId = caseId,
                eventType = "CASE_CREATED",
                actorId = workerId,
                actorRole = "ASHA",
                summary = "Encounter recorded offline: ${patientCase.riskLevel.name} triage",
                detailsJson = gson.toJson(mapOf("riskLevel" to patientCase.riskLevel.name, "score" to patientCase.riskScore)),
                occurredAt = System.currentTimeMillis()
            )
        )

        // 5. Enqueue Outbox Item for Sync
        val outboxPayload = mapOf(
            "idempotency_key" to idempotencyKey,
            "case_id" to caseId,
            "patient_name" to patientCase.patientName,
            "village" to patientCase.village,
            "blood_pressure" to patientCase.bloodPressure,
            "haemoglobin" to patientCase.haemoglobin,
            "danger_signs" to mapOf(
                "bleeding" to patientCase.dangerSigns.bleeding,
                "convulsions" to patientCase.dangerSigns.convulsions,
                "severe_headache" to patientCase.dangerSigns.severeHeadache,
                "severe_abdominal_pain" to patientCase.dangerSigns.severeAbdominalPain,
                "severe_breathlessness" to patientCase.dangerSigns.severeBreathlessness,
                "fever" to patientCase.dangerSigns.fever,
                "premature_labour_water_broke" to patientCase.dangerSigns.prematureLabourWaterBroke,
                "reduced_fetal_movement" to patientCase.dangerSigns.reducedFetalMovement
            ),
            "risk_level" to patientCase.riskLevel.name,
            "risk_score" to patientCase.riskScore,
            "rule_pack_version" to patientCase.rulePackVersion,
            "created_at" to patientCase.assessmentTimestamp
        )

        database.outboxDao().insertItem(
            OutboxItemEntity(
                id = idempotencyKey,
                caseId = caseId,
                entityType = "CASE_ASSESSMENT",
                payloadJson = gson.toJson(outboxPayload),
                status = "QUEUED",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        // 6. If audio attachment exists, persist VoiceArtifactEntity and queue linked audio upload
        if (audioFile != null && audioFile.exists()) {
            val audioArtifactId = UUID.randomUUID().toString()
            val sha256 = com.sakhicare.app.voice.AudioRecordManager.computeSha256(audioFile)
            val duration = audioDurationSeconds ?: 0

            val voiceArtifact = VoiceArtifactEntity(
                id = audioArtifactId,
                caseId = caseId,
                localAudioPath = audioFile.absolutePath,
                mimeType = "audio/m4a",
                fileSizeBytes = audioFile.length(),
                sha256Checksum = sha256,
                language = "hi-IN",
                durationSeconds = duration,
                transcript = voiceTranscript,
                processingStatus = if (!voiceTranscript.isNullOrBlank()) "CONFIRMED" else "UNPROCESSED",
                uploadStatus = "LOCAL_ONLY",
                retentionDeadlineTimestamp = System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000L),
                confirmedAtTimestamp = if (!voiceTranscript.isNullOrBlank()) System.currentTimeMillis() else null,
                createdAtTimestamp = System.currentTimeMillis()
            )
            database.voiceArtifactDao().insert(voiceArtifact)

            val audioOutboxId = UUID.randomUUID().toString()
            val audioPayload = mapOf(
                "idempotency_key" to audioOutboxId,
                "case_id" to caseId,
                "artifact_id" to audioArtifactId,
                "file_path" to audioFile.absolutePath,
                "sha256" to sha256,
                "language" to "hi-IN",
                "duration_seconds" to duration,
                "transcript" to voiceTranscript
            )

            database.outboxDao().insertItem(
                OutboxItemEntity(
                    id = audioOutboxId,
                    caseId = caseId,
                    entityType = "VOICE_AUDIO",
                    payloadJson = gson.toJson(audioPayload),
                    status = "QUEUED",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )

            database.caseEventDao().insertEvent(
                CaseEventEntity(
                    id = UUID.randomUUID().toString(),
                    caseId = caseId,
                    eventType = "VOICE_NOTE_ATTACHED",
                    actorId = workerId,
                    actorRole = "ASHA",
                    summary = "Voice note attached ($duration s, SHA-256: ${sha256.take(8)}...)",
                    detailsJson = gson.toJson(mapOf("sha256" to sha256, "duration" to duration)),
                    occurredAt = System.currentTimeMillis()
                )
            )
        }

        // 7. Schedule WorkManager sync job if context available
        context?.let {
            OutboxSyncWorker.enqueueSync(it)
        }

        caseId
    }

    suspend fun getVoiceArtifactForCase(caseId: String): VoiceArtifactEntity? = withContext(ioDispatcher) {
        database.voiceArtifactDao().getArtifactForCase(caseId)
    }

    fun getVoiceArtifactForCaseFlow(caseId: String): Flow<VoiceArtifactEntity?> {
        return database.voiceArtifactDao().getArtifactForCaseFlow(caseId).flowOn(ioDispatcher)
    }

    suspend fun getTimelineForCase(caseId: String): List<CaseEventEntity> = withContext(ioDispatcher) {
        database.caseEventDao().getEventsForCase(caseId)
    }

    private fun caseWithAssessmentToDomain(item: CaseWithAssessment): PatientCase {
        val c = item.pregnancyCase
        val a = item.assessment
        val risk = when (a?.riskLevel?.uppercase()) {
            "RED" -> RiskLevel.RED
            "AMBER" -> RiskLevel.AMBER
            else -> RiskLevel.GREEN
        }

        val unmeasuredList = try {
            if (a?.unmeasuredVitalsJson.isNullOrBlank()) emptyList()
            else gson.fromJson(a?.unmeasuredVitalsJson, Array<String>::class.java)?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList<String>()
        }

        val ashaActions = try {
            if (a?.ashaSafeActionsJson.isNullOrBlank()) emptyList()
            else gson.fromJson(a?.ashaSafeActionsJson, Array<String>::class.java)?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList<String>()
        }

        val clinicianActions = try {
            if (a?.clinicianDirectedActionsJson.isNullOrBlank()) emptyList()
            else gson.fromJson(a?.clinicianDirectedActionsJson, Array<String>::class.java)?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList<String>()
        }

        return PatientCase(
            id = c.id,
            localId = c.localId,
            patientName = c.patientName,
            village = c.village,
            ageYears = c.ageYears,
            gestationalAgeWeeks = c.gestationalAgeWeeks,
            gravida = c.gravida,
            para = c.para,
            travelConstraints = c.travelConstraints,
            bloodPressure = null, // Vitals are in observations
            haemoglobin = null,
            dangerSigns = DangerSigns(),
            riskLevel = risk,
            riskScore = a?.riskScore ?: 10,
            clinicalRationale = a?.clinicalRationale,
            recommendedProtocol = a?.recommendedProtocol,
            unmeasuredVitals = unmeasuredList,
            ashaSafeActions = ashaActions,
            clinicianDirectedActions = clinicianActions,
            assessmentTimestamp = c.createdAt,
            syncStatus = c.syncStatus,
            doctorAdvisory = c.doctorAdvisory,
            ambulanceStatus = c.ambulanceStatus,
            isDemo = c.isDemo,
            rulePackVersion = a?.rulePackVersion ?: "mohfw-hrp-v1.0"
        )
    }
}
