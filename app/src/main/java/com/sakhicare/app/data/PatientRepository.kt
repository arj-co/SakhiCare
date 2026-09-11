package com.sakhicare.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.sakhicare.app.data.db.AppDatabase
import com.sakhicare.app.data.repository.OfflineCaseRepository
import com.sakhicare.app.sync.OutboxSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object PatientRepository {

    private val _cases: SnapshotStateList<PatientCase> = mutableStateListOf()
    val cases: List<PatientCase> get() = _cases

    private var offlineRepository: OfflineCaseRepository? = null
    private var isInitialized = false

    fun initialize(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) {
        if (isInitialized) return
        isInitialized = true

        val db = AppDatabase.getInstance(context.applicationContext)
        val repo = OfflineCaseRepository(db)
        offlineRepository = repo

        scope.launch {
            repo.casesFlow.collectLatest { freshCases ->
                _cases.clear()
                _cases.addAll(freshCases)
            }
        }
    }

    fun getCaseById(id: String): PatientCase? = _cases.find { it.id == id }

    fun getTotalCount(): Int = _cases.size

    fun getPendingSyncCount(): Int = _cases.count {
        it.syncStatus in listOf("SAVED_LOCALLY", "QUEUED", "UPLOADING", "FAILED")
    }

    fun getRedRiskCount(): Int = _cases.count { it.riskLevel == RiskLevel.RED }

    fun getAmberRiskCount(): Int = _cases.count { it.riskLevel == RiskLevel.AMBER }

    fun getGreenRiskCount(): Int = _cases.count { it.riskLevel == RiskLevel.GREEN }

    fun getLastAssessmentTime(): Long? = _cases.maxByOrNull { it.assessmentTimestamp }?.assessmentTimestamp

    fun addCase(
        patientCase: PatientCase,
        context: Context? = null,
        audioFile: java.io.File? = null,
        voiceTranscript: String? = null,
        audioDurationSeconds: Int? = null
    ) {
        _cases.add(0, patientCase)
        context?.let { ctx ->
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getInstance(ctx.applicationContext)
                val repo = offlineRepository ?: OfflineCaseRepository(db)
                val prefs = com.sakhicare.app.data.preferences.OnboardingPreferences(ctx)
                val existingWorker = db.workerDao().getCurrentWorker()
                val workerId = existingWorker?.id ?: if (prefs.workerPhone.isNotBlank()) "WKR-${prefs.workerPhone}" else "WKR-LOCAL"
                val facilityId = existingWorker?.facilityId ?: prefs.facilityCode.ifBlank { "FAC-01" }

                if (existingWorker == null) {
                    db.workerDao().insertWorker(
                        com.sakhicare.app.data.db.entities.WorkerEntity(
                            id = workerId,
                            name = prefs.workerName.ifBlank { "ASHA Worker" },
                            role = prefs.workerRole.ifBlank { "ASHA" },
                            phone = prefs.workerPhone.ifBlank { "9999999999" },
                            facilityId = facilityId,
                            facilityName = prefs.facilityName.ifBlank { "Primary Health Catchment" },
                            locale = "hi-IN",
                            status = "ACTIVE"
                        )
                    )
                }

                repo.saveNewAssessment(
                    patientCase = patientCase,
                    workerId = workerId,
                    facilityId = facilityId,
                    context = ctx,
                    audioFile = audioFile,
                    voiceTranscript = voiceTranscript,
                    audioDurationSeconds = audioDurationSeconds
                )
            }
        }
    }

    suspend fun getVoiceArtifact(caseId: String, context: Context): com.sakhicare.app.data.db.entities.VoiceArtifactEntity? {
        val db = AppDatabase.getInstance(context.applicationContext)
        val repo = offlineRepository ?: OfflineCaseRepository(db)
        return repo.getVoiceArtifactForCase(caseId)
    }

    fun getVoiceArtifactFlow(caseId: String, context: Context): kotlinx.coroutines.flow.Flow<com.sakhicare.app.data.db.entities.VoiceArtifactEntity?> {
        val db = AppDatabase.getInstance(context.applicationContext)
        val repo = offlineRepository ?: OfflineCaseRepository(db)
        return repo.getVoiceArtifactForCaseFlow(caseId)
    }

    fun syncAllPending(context: Context? = null, force: Boolean = false) {
        context?.let {
            OutboxSyncWorker.enqueueSync(it, force = force)
        }
    }

    fun filterCases(query: String, filterRisk: RiskLevel?): List<PatientCase> {
        return _cases.filter { case ->
            val matchesQuery = query.isBlank() ||
                    case.patientName.contains(query, ignoreCase = true) ||
                    case.village.contains(query, ignoreCase = true) ||
                    case.id.contains(query, ignoreCase = true)

            val matchesRisk = filterRisk == null || case.riskLevel == filterRisk

            matchesQuery && matchesRisk
        }
    }
}
