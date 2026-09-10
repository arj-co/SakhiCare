package com.sakhicare.app.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sakhicare.app.data.db.entities.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class AppDatabaseTest {

    private lateinit var database: AppDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.createInMemoryTestDatabase(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testWorkerInsertionAndQuery() = runBlocking {
        val worker = WorkerEntity(
            id = "WKR-101",
            name = "Shanti Devi",
            role = "ASHA",
            phone = "9876543210",
            facilityId = "FAC-01",
            facilityName = "Rampur Sub-Centre"
        )
        database.workerDao().insertWorker(worker)

        val retrieved = database.workerDao().getCurrentWorker()
        assertNotNull(retrieved)
        assertEquals("Shanti Devi", retrieved?.name)
        assertEquals("Rampur Sub-Centre", retrieved?.facilityName)
    }

    @Test
    fun testCaseAssessmentAndEventPersistence() = runBlocking {
        val caseId = "SC-TEST-001"
        val assessmentId = "ASM-TEST-001"

        val case = PregnancyCaseEntity(
            id = caseId,
            localId = caseId,
            patientName = "Sita Devi",
            village = "Kalyanpur",
            ageYears = 26,
            gestationalAgeWeeks = 34,
            gravida = 2,
            para = 1,
            travelConstraints = "None",
            workerId = "WKR-101",
            facilityId = "FAC-01",
            syncStatus = "QUEUED"
        )
        database.caseDao().insertCase(case)

        val assessment = AssessmentEntity(
            id = assessmentId,
            caseId = caseId,
            rulePackVersion = "mohfw-hrp-v1.0",
            riskLevel = "RED",
            riskScore = 85,
            primaryFactorsJson = "[\"Vaginal Hemorrhage\"]",
            unmeasuredVitalsJson = "[]",
            clinicalRationale = "Critical Emergency: Vaginal Hemorrhage",
            recommendedProtocol = "Call 108 ambulance immediately",
            ashaSafeActionsJson = "[\"Position in left lateral tilt\"]",
            clinicianDirectedActionsJson = "[\"IV Line\"]",
            requiresImmediateAmbulance = true,
            requiresBloodTransfusionAlert = false
        )
        database.assessmentDao().insertAssessment(assessment)

        val event = CaseEventEntity(
            id = UUID.randomUUID().toString(),
            caseId = caseId,
            eventType = "CASE_CREATED",
            actorId = "WKR-101",
            actorRole = "ASHA",
            summary = "Encounter recorded offline"
        )
        database.caseEventDao().insertEvent(event)

        val outboxItem = OutboxItemEntity(
            id = "IDEMP-001",
            caseId = caseId,
            entityType = "CASE_ASSESSMENT",
            payloadJson = "{}",
            status = "QUEUED"
        )
        database.outboxDao().insertItem(outboxItem)

        // Verify retrieval
        val caseWithAssessment = database.caseDao().getCaseWithAssessment(caseId)
        assertNotNull(caseWithAssessment)
        assertEquals("Sita Devi", caseWithAssessment?.pregnancyCase?.patientName)
        assertEquals("RED", caseWithAssessment?.assessment?.riskLevel)
        assertEquals(true, caseWithAssessment?.assessment?.requiresImmediateAmbulance)

        // Verify case events timeline
        val timeline = database.caseEventDao().getEventsForCase(caseId)
        assertEquals(1, timeline.size)
        assertEquals("CASE_CREATED", timeline[0].eventType)

        // Verify outbox pending count
        val pendingCount = database.outboxDao().getPendingOutboxCountFlow().first()
        assertEquals(1, pendingCount)
    }

    @Test
    fun testOutboxStatusTransitions() = runBlocking {
        val outboxItem = OutboxItemEntity(
            id = "IDEMP-RETRY-01",
            caseId = "SC-RETRY-01",
            entityType = "CASE_ASSESSMENT",
            payloadJson = "{}",
            attempts = 0,
            maxAttempts = 5,
            status = "QUEUED"
        )
        database.outboxDao().insertItem(outboxItem)

        // Attempt 1 fails
        database.outboxDao().recordAttemptResult("IDEMP-RETRY-01", "FAILED", "Network Timeout")
        var items = database.outboxDao().getPendingItems()
        assertEquals(1, items.size)
        assertEquals("FAILED", items[0].status)
        assertEquals("Network Timeout", items[0].lastError)
        assertEquals(1, items[0].attempts)

        // Attempt 2 succeeds
        database.outboxDao().recordAttemptResult("IDEMP-RETRY-01", "ACKNOWLEDGED", null)
        items = database.outboxDao().getPendingItems()
        assertEquals(0, items.size) // No longer pending!
    }
}
