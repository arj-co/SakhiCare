package com.sakhicare.app.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sakhicare.app.data.db.entities.PregnancyCaseEntity
import com.sakhicare.app.data.db.entities.VoiceArtifactEntity
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
class VoiceArtifactDatabaseTest {

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
    fun testVoiceArtifactInsertAndQuery() = runBlocking {
        val caseId = "CASE-VOICE-01"
        val caseEntity = PregnancyCaseEntity(
            id = caseId,
            localId = caseId,
            patientName = "Lalita Devi",
            village = "Rampur",
            ageYears = 24,
            gestationalAgeWeeks = 32,
            gravida = 2,
            para = 1,
            travelConstraints = null,
            workerId = "WKR-01",
            facilityId = "FAC-01"
        )
        database.caseDao().insertCase(caseEntity)

        val artifactId = UUID.randomUUID().toString()
        val artifact = VoiceArtifactEntity(
            id = artifactId,
            caseId = caseId,
            localAudioPath = "/data/user/0/com.sakhicare.app/files/voice_notes/test.m4a",
            mimeType = "audio/m4a",
            fileSizeBytes = 45120L,
            sha256Checksum = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            language = "hi-IN",
            durationSeconds = 18,
            transcript = "मरीज ललिता देवी बीपी 150/100",
            processingStatus = "CONFIRMED",
            uploadStatus = "LOCAL_ONLY"
        )
        database.voiceArtifactDao().insert(artifact)

        val retrieved = database.voiceArtifactDao().getArtifactForCase(caseId)
        assertNotNull("Artifact should be retrievable by case ID", retrieved)
        assertEquals(artifactId, retrieved?.id)
        assertEquals("audio/m4a", retrieved?.mimeType)
        assertEquals(45120L, retrieved?.fileSizeBytes)
        assertEquals(18, retrieved?.durationSeconds)
        assertEquals("CONFIRMED", retrieved?.processingStatus)
        assertEquals("LOCAL_ONLY", retrieved?.uploadStatus)

        // Test upload status transition
        database.voiceArtifactDao().updateUploadStatus(artifactId, "UPLOADED")
        val updated = database.voiceArtifactDao().getById(artifactId)
        assertEquals("UPLOADED", updated?.uploadStatus)
        assertNotNull("Uploaded timestamp should be recorded", updated?.uploadedAtTimestamp)

        // Test reactive Flow query
        val flowArtifact = database.voiceArtifactDao().getArtifactForCaseFlow(caseId).first()
        assertEquals("UPLOADED", flowArtifact?.uploadStatus)
    }

    @Test
    fun testVoiceArtifactCascadeDeleteWithCase() = runBlocking {
        val caseId = "CASE-VOICE-02"
        val caseEntity = PregnancyCaseEntity(
            id = caseId,
            localId = caseId,
            patientName = "Sunita Devi",
            village = "Sitapur",
            ageYears = 26,
            gestationalAgeWeeks = 36,
            gravida = 1,
            para = 0,
            travelConstraints = null,
            workerId = "WKR-01",
            facilityId = "FAC-01"
        )
        database.caseDao().insertCase(caseEntity)

        val artifactId = UUID.randomUUID().toString()
        val artifact = VoiceArtifactEntity(
            id = artifactId,
            caseId = caseId,
            localAudioPath = "/path/audio.m4a",
            fileSizeBytes = 12000L,
            sha256Checksum = "abc123sha",
            durationSeconds = 10
        )
        database.voiceArtifactDao().insert(artifact)
        assertNotNull(database.voiceArtifactDao().getById(artifactId))

        // Deleting the case should cascade delete the voice artifact
        database.caseDao().deleteCaseById(caseId)
        assertNull("Voice artifact should cascade delete when parent case is deleted", database.voiceArtifactDao().getById(artifactId))
    }
}
