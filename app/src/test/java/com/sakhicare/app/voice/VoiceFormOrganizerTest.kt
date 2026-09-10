package com.sakhicare.app.voice

import org.junit.Assert.*
import org.junit.Test

class VoiceFormOrganizerTest {

    @Test
    fun testIndicNumberNormalization() {
        val raw = "बीपी एक सौ पचास बटा सौ हीमोग्लोबिन सात दशमलव दो"
        val normalized = VoiceFormOrganizer.normalizeIndicSpeech(raw)
        assertTrue("Expected 150/100 in normalized string: $normalized", normalized.contains("150/100"))
        assertTrue("Expected 7.2 in normalized string: $normalized", normalized.contains("7.2"))
    }

    @Test
    fun testExtractHindiCaseWithSevereDangerSigns() {
        val transcript = "मरीज ललिता देवी, गांव रामपुर, बीपी एक सौ साठ बटा एक सौ दस, हीमोग्लोबिन छह दशमलव आठ, खून बहना और तेज सिरदर्द"
        val draft = VoiceFormOrganizer.organizeTranscript(transcript)

        assertTrue(draft.isRecognized)
        assertEquals("ललिता देवी", draft.candidateFields.patientName)
        assertEquals("रामपुर", draft.candidateFields.village)
        assertEquals("160/110", draft.candidateFields.bloodPressure)
        assertEquals(6.8, draft.candidateFields.haemoglobin ?: 0.0, 0.01)
        assertTrue(draft.candidateFields.dangerSigns.bleeding)
        assertTrue(draft.candidateFields.dangerSigns.severeHeadache)

        assertTrue(draft.extractedFieldKeys.contains("patientName"))
        assertTrue(draft.extractedFieldKeys.contains("village"))
        assertTrue(draft.extractedFieldKeys.contains("bloodPressure"))
        assertTrue(draft.extractedFieldKeys.contains("haemoglobin"))
        assertTrue(draft.extractedFieldKeys.contains("dangerSigns.bleeding"))
        assertTrue(draft.extractedFieldKeys.contains("dangerSigns.severeHeadache"))
    }

    @Test
    fun testExtractEnglishCase() {
        val transcript = "Patient Sunita Devi village Rampur BP 140 over 90 haemoglobin 9.5 fever and severe abdominal pain"
        val draft = VoiceFormOrganizer.organizeTranscript(transcript)

        assertTrue(draft.isRecognized)
        assertEquals("Sunita Devi", draft.candidateFields.patientName)
        assertEquals("Rampur", draft.candidateFields.village)
        assertEquals("140/90", draft.candidateFields.bloodPressure)
        assertEquals(9.5, draft.candidateFields.haemoglobin ?: 0.0, 0.01)
        assertTrue(draft.candidateFields.dangerSigns.fever)
        assertTrue(draft.candidateFields.dangerSigns.severeAbdominalPain)
    }

    @Test
    fun testNonHallucinationOnEmptyOrUnrelatedSpeech() {
        val emptyDraft = VoiceFormOrganizer.organizeTranscript("   ")
        assertFalse(emptyDraft.isRecognized)
        assertNull(emptyDraft.candidateFields.patientName)
        assertNull(emptyDraft.candidateFields.bloodPressure)
        assertNull(emptyDraft.candidateFields.haemoglobin)
        assertFalse(emptyDraft.candidateFields.dangerSigns.hasAny())
        assertTrue(emptyDraft.extractedFieldKeys.isEmpty())

        val unrelatedDraft = VoiceFormOrganizer.organizeTranscript("नमस्ते आज मौसम बहुत अच्छा है")
        assertNull(unrelatedDraft.candidateFields.bloodPressure)
        assertNull(unrelatedDraft.candidateFields.haemoglobin)
        assertFalse(unrelatedDraft.candidateFields.dangerSigns.hasAny())
    }

    @Test
    fun testTravelConstraintsExtraction() {
        val transcript = "गांव रामपुर बाढ़ के कारण रास्ता बंद है no vehicle available"
        val draft = VoiceFormOrganizer.organizeTranscript(transcript)

        assertNotNull(draft.candidateFields.travelConstraints)
        assertTrue(draft.extractedFieldKeys.contains("travelConstraints"))
    }
}
