package com.sakhicare.app.data.db.daos

import androidx.room.*
import com.sakhicare.app.data.db.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: WorkerEntity)

    @Query("SELECT * FROM workers LIMIT 1")
    suspend fun getCurrentWorker(): WorkerEntity?

    @Query("SELECT * FROM workers LIMIT 1")
    fun getCurrentWorkerFlow(): Flow<WorkerEntity?>
}

data class CaseWithAssessment(
    @Embedded val pregnancyCase: PregnancyCaseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "caseId"
    )
    val assessment: AssessmentEntity?
)

@Dao
interface CaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCase(pregnancyCase: PregnancyCaseEntity)

    @Update
    suspend fun updateCase(pregnancyCase: PregnancyCaseEntity)

    @Query("UPDATE pregnancy_cases SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :caseId")
    suspend fun updateSyncStatus(caseId: String, syncStatus: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM pregnancy_cases WHERE id = :caseId LIMIT 1")
    suspend fun getCaseById(caseId: String): PregnancyCaseEntity?

    @Transaction
    @Query("SELECT * FROM pregnancy_cases WHERE id = :caseId LIMIT 1")
    suspend fun getCaseWithAssessment(caseId: String): CaseWithAssessment?

    @Transaction
    @Query("SELECT * FROM pregnancy_cases ORDER BY createdAt DESC")
    fun getAllCasesWithAssessmentFlow(): Flow<List<CaseWithAssessment>>

    @Query("SELECT COUNT(*) FROM pregnancy_cases WHERE syncStatus IN ('SAVED_LOCALLY', 'QUEUED', 'FAILED')")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT MAX(updatedAt) FROM pregnancy_cases WHERE syncStatus = 'ACKNOWLEDGED'")
    fun getLastSuccessfulSyncTimeFlow(): Flow<Long?>

    @Query("DELETE FROM pregnancy_cases WHERE id = :caseId")
    suspend fun deleteCaseById(caseId: String)

    @androidx.room.Delete
    suspend fun deleteCase(pregnancyCase: PregnancyCaseEntity)
}

@Dao
interface AssessmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(assessment: AssessmentEntity)

    @Query("SELECT * FROM assessments WHERE caseId = :caseId LIMIT 1")
    suspend fun getAssessmentForCase(caseId: String): AssessmentEntity?
}

@Dao
interface ObservationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(observations: List<ObservationEntity>)

    @Query("SELECT * FROM observations WHERE assessmentId = :assessmentId")
    suspend fun getObservationsForAssessment(assessmentId: String): List<ObservationEntity>
}

@Dao
interface CaseEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CaseEventEntity)

    @Query("SELECT * FROM case_events WHERE caseId = :caseId ORDER BY occurredAt ASC")
    fun getEventsForCaseFlow(caseId: String): Flow<List<CaseEventEntity>>

    @Query("SELECT * FROM case_events WHERE caseId = :caseId ORDER BY occurredAt ASC")
    suspend fun getEventsForCase(caseId: String): List<CaseEventEntity>
}

@Dao
interface OutboxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: OutboxItemEntity)

    @Query("SELECT * FROM outbox_items WHERE status IN ('QUEUED', 'FAILED') AND attempts < maxAttempts ORDER BY createdAt ASC")
    suspend fun getPendingItems(): List<OutboxItemEntity>

    @Query("UPDATE outbox_items SET status = :status, attempts = attempts + 1, lastError = :lastError, updatedAt = :updatedAt WHERE id = :id")
    suspend fun recordAttemptResult(id: String, status: String, lastError: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE outbox_items SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM outbox_items WHERE status IN ('QUEUED', 'FAILED')")
    fun getPendingOutboxCountFlow(): Flow<Int>
}

@Dao
interface VoiceArtifactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(artifact: VoiceArtifactEntity)

    @Query("SELECT * FROM voice_artifacts WHERE caseId = :caseId ORDER BY createdAtTimestamp DESC LIMIT 1")
    fun getArtifactForCaseFlow(caseId: String): Flow<VoiceArtifactEntity?>

    @Query("SELECT * FROM voice_artifacts WHERE caseId = :caseId ORDER BY createdAtTimestamp DESC LIMIT 1")
    suspend fun getArtifactForCase(caseId: String): VoiceArtifactEntity?

    @Query("SELECT * FROM voice_artifacts WHERE id = :id")
    suspend fun getById(id: String): VoiceArtifactEntity?

    @Query("UPDATE voice_artifacts SET uploadStatus = :status, uploadedAtTimestamp = :uploadedAt WHERE id = :id")
    suspend fun updateUploadStatus(id: String, status: String, uploadedAt: Long? = System.currentTimeMillis())

    @Query("UPDATE voice_artifacts SET transcript = :transcript, processingStatus = :processingStatus, confirmedAtTimestamp = :confirmedAt WHERE id = :id")
    suspend fun updateConfirmation(id: String, transcript: String, processingStatus: String, confirmedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM voice_artifacts WHERE id = :id")
    suspend fun deleteById(id: String)
}

