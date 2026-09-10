package com.sakhicare.app.data

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class TriageEngineTest {

    @Test
    fun testProtocolVersionIsCorrect() {
        assertEquals("mohfw-hrp-v1.0", TriageEngine.PROTOCOL_VERSION)
    }

    @Test
    fun testAllSharedFixturesParity() {
        val fixtureFileCandidates = listOf(
            File("../../shared/clinical_triage_fixtures.json"),
            File("../shared/clinical_triage_fixtures.json"),
            File("shared/clinical_triage_fixtures.json")
        )
        val fixtureFile = fixtureFileCandidates.firstOrNull { it.exists() }
        assertNotNull("Fixture file shared/clinical_triage_fixtures.json must exist", fixtureFile)

        val jsonString = fixtureFile!!.readText(Charsets.UTF_8)
        val root = JsonParser.parseString(jsonString).asJsonObject
        assertEquals("mohfw-hrp-v1.0", root.get("protocol_version").asString)

        val fixtures = root.getAsJsonArray("fixtures")
        assertTrue("Must have at least 10 fixtures", fixtures.size() >= 10)

        for (i in 0 until fixtures.size()) {
            val item = fixtures[i].asJsonObject
            val fid = item.get("id").asString
            val input = item.getAsJsonObject("input")
            val expected = item.getAsJsonObject("expected")

            val bpStr = if (input.get("blood_pressure").isJsonNull) null else input.get("blood_pressure").asString
            val hbStr = if (input.get("haemoglobin").isJsonNull) null else "${input.get("haemoglobin").asDouble} g/dL"

            val dsObj = input.getAsJsonObject("danger_signs")
            val dangerSigns = DangerSigns(
                bleeding = dsObj.get("bleeding")?.asBoolean ?: false,
                convulsions = dsObj.get("convulsions_or_vision_loss")?.asBoolean ?: false,
                severeHeadache = dsObj.get("severe_headache")?.asBoolean ?: false,
                severeAbdominalPain = dsObj.get("severe_abdominal_pain")?.asBoolean ?: false,
                severeBreathlessness = dsObj.get("severe_breathlessness")?.asBoolean ?: false,
                fever = dsObj.get("fever")?.asBoolean ?: false,
                prematureLabourWaterBroke = dsObj.get("premature_labour_water_broke")?.asBoolean ?: false,
                reducedFetalMovement = dsObj.get("reduced_fetal_movement")?.asBoolean ?: false
            )

            val evaluation = TriageEngine.evaluate(bpStr, hbStr, dangerSigns)

            assertEquals("Fixture $fid risk mismatch", expected.get("risk_level").asString, evaluation.riskLevel.name)
            assertEquals("Fixture $fid ambulance mismatch", expected.get("requires_ambulance").asBoolean, evaluation.requiresAmbulance)
            assertEquals("Fixture $fid blood alert mismatch", expected.get("requires_blood_alert").asBoolean, evaluation.requiresBloodTransfusion)
            assertEquals("mohfw-hrp-v1.0", evaluation.rulePackVersion)
        }
    }

    @Test
    fun testNoSilentDefaultsForMissingVitals() {
        val result = TriageEngine.evaluate(
            bloodPressure = null,
            haemoglobinStr = null,
            dangerSigns = DangerSigns()
        )

        assertEquals(RiskLevel.GREEN, result.riskLevel)
        assertTrue("Should list unmeasured BP", result.unmeasuredVitals.any { it.contains("Blood Pressure: Not measured") })
        assertTrue("Should list unmeasured Hb", result.unmeasuredVitals.any { it.contains("Haemoglobin: Not measured") })
        assertTrue("Rationale should explicitly state unmeasured vitals", result.clinicalRationale.contains("Unmeasured:"))
        assertFalse("Blank vitals should never trigger emergency", result.requiresAmbulance)
    }

    @Test
    fun testSevereAnemiaEmergency() {
        val result = TriageEngine.evaluate(
            bloodPressure = "118/75",
            haemoglobinStr = "6.5 g/dL",
            dangerSigns = DangerSigns()
        )
        assertEquals(RiskLevel.RED, result.riskLevel)
        assertTrue(result.requiresAmbulance)
        assertTrue(result.requiresBloodTransfusion)
    }

    @Test
    fun testSevereHypertensionEmergency() {
        val result = TriageEngine.evaluate(
            bloodPressure = "168/112",
            haemoglobinStr = null,
            dangerSigns = DangerSigns()
        )
        assertEquals(RiskLevel.RED, result.riskLevel)
        assertTrue(result.requiresAmbulance)
        assertFalse(result.requiresBloodTransfusion)
    }

    @Test
    fun testVaginalBleedingOverridesNormalVitals() {
        val result = TriageEngine.evaluate(
            bloodPressure = "120/80",
            haemoglobinStr = "12.0 g/dL",
            dangerSigns = DangerSigns(bleeding = true)
        )
        assertEquals(RiskLevel.RED, result.riskLevel)
        assertTrue(result.requiresAmbulance)
    }
}
