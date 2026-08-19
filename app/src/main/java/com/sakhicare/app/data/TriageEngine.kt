package com.sakhicare.app.data

/**
 * SakhiCare Native On-Device Clinical Triage Engine
 * Adheres to Indian MoHFW, WHO, and ACOG High-Risk Pregnancy guidelines.
 */
data class NativeClinicalEvaluation(
    val riskLevel: RiskLevel,
    val riskScore: Int,
    val primaryFactors: List<String>,
    val clinicalRationale: String,
    val recommendedProtocol: String,
    val requiresAmbulance: Boolean,
    val requiresBloodTransfusion: Boolean
)

object TriageEngine {

    fun evaluate(
        bloodPressure: String,
        haemoglobinStr: String,
        dangerSigns: DangerSigns
    ): NativeClinicalEvaluation {
        val (sbp, dbp) = parseBp(bloodPressure)
        val hb = haemoglobinStr.replace("g/dL", "").trim().toDoubleOrNull() ?: 11.0
        val factors = mutableListOf<String>()
        var score = 10

        var isEmergencyRed = false
        var isUrgentAmber = false
        var needsAmbulance = false
        var needsBlood = false

        // 1. Blood Pressure Stratification
        if (sbp >= 160 || dbp >= 110) {
            isEmergencyRed = true
            needsAmbulance = true
            score += 45
            factors.add("Severe Hypertensive Crisis / Pre-eclampsia (BP: $sbp/$dbp)")
        } else if (sbp < 90 || dbp < 50) {
            isEmergencyRed = true
            needsAmbulance = true
            score += 50
            factors.add("Obstetric Shock / Severe Hypotension (BP: $sbp/$dbp)")
        } else if ((sbp in 140..159) || (dbp in 90..109)) {
            isUrgentAmber = true
            score += 25
            factors.add("Gestational Hypertension (BP: $sbp/$dbp)")
        } else if ((sbp in 130..139) || (dbp in 85..89)) {
            score += 10
            factors.add("High-Normal Blood Pressure (BP: $sbp/$dbp)")
        }

        // 2. Haemoglobin (Anemia) Stratification
        if (hb < 7.0) {
            isEmergencyRed = true
            needsBlood = true
            score += 40
            factors.add("Severe Anemia (Hb: $hb g/dL) - Transfusion Risk")
        } else if (hb in 7.0..9.9) {
            isUrgentAmber = true
            score += 20
            factors.add("Moderate Anemia (Hb: $hb g/dL)")
        }

        // 3. Danger Signs
        if (dangerSigns.bleeding) {
            isEmergencyRed = true
            needsAmbulance = true
            score += 45
            factors.add("Vaginal Hemorrhage (Severe Danger Sign)")
        }

        if (dangerSigns.reducedFetalMovement) {
            isEmergencyRed = true
            needsAmbulance = true
            score += 35
            factors.add("Acute Fetal Distress (Reduced Fetal Movement)")
        }

        if (dangerSigns.headache) {
            if (sbp >= 140 || dbp >= 90) {
                isEmergencyRed = true
                needsAmbulance = true
                score += 35
                factors.add("Severe Headache with HTN (Impending Eclampsia)")
            } else {
                isUrgentAmber = true
                score += 15
                factors.add("Persistent Severe Headache")
            }
        }

        if (dangerSigns.fever) {
            isUrgentAmber = true
            score += 15
            factors.add("Maternal Pyrexia / Fever")
        }

        // 4. Compound Correlation
        if (hb in 7.0..9.9 && (sbp >= 140 || dbp >= 90) && (dangerSigns.fever || dangerSigns.headache)) {
            isEmergencyRed = true
            needsAmbulance = true
            factors.add("Compound High Risk (Moderate Anemia + HTN + Symptoms)")
        }

        val finalScore = score.coerceIn(0, 100)

        val (riskLevel, rationale, protocol) = when {
            isEmergencyRed || finalScore >= 60 -> Triple(
                RiskLevel.RED,
                "Critical Emergency: ${factors.joinToString("; ")}.",
                "1) Call 108 ambulance immediately. 2) Position patient in left lateral tilt. 3) Keep IV line ready. 4) Alert PHC/CHC Medical Officer."
            )
            isUrgentAmber || finalScore >= 30 -> Triple(
                RiskLevel.AMBER,
                "High Priority: ${factors.joinToString("; ")}.",
                "1) Refer to PHC within 24 hours. 2) Daily BP & temperature monitoring. 3) Prescribe oral IFA / antipyretics as per protocol."
            )
            else -> Triple(
                RiskLevel.GREEN,
                "Normal maternal parameters.",
                "Continue routine Antenatal Care (ANC) counseling, nutrition guidance, and daily IFA supplementation."
            )
        }

        return NativeClinicalEvaluation(
            riskLevel = riskLevel,
            riskScore = finalScore,
            primaryFactors = factors.ifEmpty { listOf("Normal checkup") },
            clinicalRationale = rationale,
            recommendedProtocol = protocol,
            requiresAmbulance = needsAmbulance,
            requiresBloodTransfusion = needsBlood
        )
    }

    private fun parseBp(bp: String): Pair<Int, Int> {
        val clean = bp.trim().replace("over", "/").replace("बटा", "/")
        val parts = clean.split("/")
        if (parts.size == 2) {
            val s = parts[0].trim().toIntOrNull() ?: 120
            val d = parts[1].trim().toIntOrNull() ?: 80
            return Pair(s, d)
        }
        return Pair(120, 80)
    }
}
