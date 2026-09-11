package com.sakhicare.app.data

/**
 * SakhiCare Native On-Device Deterministic Clinical Triage Engine
 * Protocol Pack: mohfw-hrp-v1.0
 * Adheres to Indian MoHFW, WHO, and ACOG High-Risk Pregnancy (HRP) Guidelines.
 */
data class NativeClinicalEvaluation(
    val riskLevel: RiskLevel,
    val riskScore: Int,
    val primaryFactors: List<String>,
    val unmeasuredVitals: List<String>,
    val clinicalRationale: String,
    val recommendedProtocol: String,
    val ashaSafeActions: List<String>,
    val clinicianDirectedActions: List<String>,
    val requiresAmbulance: Boolean,
    val requiresBloodTransfusion: Boolean,
    val rulePackVersion: String = "mohfw-hrp-v1.0"
)

object TriageEngine {

    const val PROTOCOL_VERSION = "mohfw-hrp-v1.0"
    const val RULE_PACK_VERSION = PROTOCOL_VERSION

    fun evaluate(
        bloodPressure: String?,
        haemoglobinStr: String?,
        dangerSigns: DangerSigns
    ): NativeClinicalEvaluation {
        val (sbp, dbp) = parseBp(bloodPressure)
        val hb = parseHb(haemoglobinStr)
        val factors = mutableListOf<String>()
        val unmeasured = mutableListOf<String>()
        var score = 10

        if (sbp == null || dbp == null) {
            unmeasured.add("Blood Pressure: Not measured")
        }
        if (hb == null) {
            unmeasured.add("Haemoglobin: Not measured")
        }

        var isEmergencyRed = false
        var isUrgentAmber = false
        var needsAmbulance = false
        var needsBlood = false

        // 1. Blood Pressure Stratification
        if (sbp != null && dbp != null) {
            if (sbp >= 160 || dbp >= 110) {
                isEmergencyRed = true
                needsAmbulance = true
                score += 45
                factors.add("Severe Hypertensive Crisis / Pre-eclampsia (BP: $sbp/$dbp mmHg)")
            } else if (sbp < 90 || dbp < 50) {
                isEmergencyRed = true
                needsAmbulance = true
                score += 50
                factors.add("Obstetric Shock / Severe Hypotension (BP: $sbp/$dbp mmHg)")
            } else if ((sbp in 140..159) || (dbp in 90..109)) {
                isUrgentAmber = true
                score += 25
                factors.add("Gestational Hypertension (BP: $sbp/$dbp mmHg)")
            } else if ((sbp in 130..139) || (dbp in 85..89)) {
                score += 10
                factors.add("High-Normal Blood Pressure (BP: $sbp/$dbp mmHg)")
            }
        }

        // 2. Haemoglobin (Anemia) Stratification
        if (hb != null) {
            if (hb < 7.0) {
                isEmergencyRed = true
                needsAmbulance = true
                needsBlood = true
                score += 40
                factors.add("Severe Anemia (Hb: $hb g/dL) - Transfusion Risk")
            } else if (hb in 7.0..9.9) {
                isUrgentAmber = true
                score += 20
                factors.add("Moderate Anemia (Hb: $hb g/dL)")
            } else if (hb in 10.0..10.9) {
                score += 5
                factors.add("Mild Anemia (Hb: $hb g/dL)")
            }
        }

        // 3. Danger Signs Stratification
        if (dangerSigns.bleeding) {
            isEmergencyRed = true
            needsAmbulance = true
            score += 45
            factors.add("Antepartum / Postpartum Vaginal Hemorrhage (Severe Danger Sign)")
        }

        if (dangerSigns.convulsions) {
            isEmergencyRed = true
            needsAmbulance = true
            score += 50
            factors.add("Convulsions / Eclamptic Fits / Unconsciousness")
        }

        if (dangerSigns.severeAbdominalPain) {
            isEmergencyRed = true
            needsAmbulance = true
            score += 40
            factors.add("Severe Abdominal Pain (Possible Abruption or Ectopic/Rupture)")
        }

        if (dangerSigns.severeBreathlessness) {
            isEmergencyRed = true
            needsAmbulance = true
            score += 40
            factors.add("Severe Breathlessness / Chest Pain (Possible Pulmonary Edema)")
        }

        if (dangerSigns.prematureLabourWaterBroke) {
            isEmergencyRed = true
            needsAmbulance = true
            score += 40
            factors.add("Premature Labour Pains / Water Breaking Early (Preterm ROM)")
        }

        if (dangerSigns.reducedFetalMovement) {
            isEmergencyRed = true
            needsAmbulance = true
            score += 35
            factors.add("Acute Fetal Distress (Reduced / Absent Fetal Movement in 3rd Trimester)")
        }

        if (dangerSigns.severeHeadache) {
            if (sbp != null && dbp != null && (sbp >= 140 || dbp >= 90)) {
                isEmergencyRed = true
                needsAmbulance = true
                score += 35
                factors.add("Severe Headache with Hypertension (Impending Eclampsia Alert)")
            } else {
                isUrgentAmber = true
                score += 15
                factors.add("Persistent Severe Headache / Visual Blurring")
            }
        }

        if (dangerSigns.fever) {
            if (isEmergencyRed || (sbp != null && sbp < 90)) {
                factors.add("High Fever with Septic Warning Signs")
            } else {
                isUrgentAmber = true
                score += 15
                factors.add("Maternal Pyrexia / Fever (Possible Systemic or Intrauterine Infection)")
            }
        }

        // 4. Compound Interaction Evaluation
        if (hb != null && hb in 7.0..9.9) {
            if (sbp != null && dbp != null && (sbp >= 140 || dbp >= 90)) {
                if (dangerSigns.fever || dangerSigns.severeHeadache) {
                    isEmergencyRed = true
                    needsAmbulance = true
                    factors.add("Compound High Risk (Moderate Anemia + Gestational HTN + Symptoms)")
                }
            }
        }

        val finalScore = score.coerceIn(0, 100)

        val ashaSafeActions = mutableListOf<String>()
        val clinicianDirectedActions = mutableListOf<String>()

        val (riskLevel, rationale, protocol) = when {
            isEmergencyRed || finalScore >= 60 -> {
                ashaSafeActions.addAll(
                    listOf(
                        "Call 108 ambulance immediately — do not delay referral",
                        "Position mother in left lateral tilt position",
                        "Keep airways clear; do not place objects in mouth if fitting",
                        "Notify family members and village coordinator",
                        "Contact Primary Health Centre (PHC) Medical Officer"
                    )
                )
                clinicianDirectedActions.addAll(
                    listOf(
                        "IV access line placement (Clinician order required)",
                        "Antihypertensive or Magnesium Sulphate administration (Clinician only)",
                        "Blood grouping, cross-matching & transfusion authorization (Clinician only)"
                    )
                )
                Triple(
                    RiskLevel.RED,
                    "Critical Emergency: ${factors.joinToString("; ")}. High risk of maternal-fetal mortality.",
                    "1) Call 108 ambulance immediately. 2) Position patient in left lateral tilt. 3) Alert Medical Officer. 4) Do not give oral solids/fluids if fitting/unconscious."
                )
            }
            isUrgentAmber || finalScore >= 30 -> {
                ashaSafeActions.addAll(
                    listOf(
                        "Advise family on referral to PHC within 24 hours",
                        "Schedule daily follow-up visit for BP and symptom check",
                        "Identify emergency transport contact in case signs worsen"
                    )
                )
                clinicianDirectedActions.addAll(
                    listOf(
                        "Clinical diagnosis and lab order (Malaria, Urine Protein, Complete Blood Count)",
                        "Medication prescription (Antipyretics, Antihypertensives, Therapeutic Iron)"
                    )
                )
                Triple(
                    RiskLevel.AMBER,
                    "High Priority Observation: ${factors.joinToString("; ")}. Requires clinical consultation within 24 hours.",
                    "1) Refer to Primary Health Centre within 24 hours. 2) Daily BP & temperature monitoring. 3) Plan transport."
                )
            }
            else -> {
                ashaSafeActions.addAll(
                    listOf(
                        "Continue routine Antenatal Care (ANC) counseling",
                        "Advise nutritious iron-rich diet and daily IFA tablets",
                        "Counsel family on recognizing maternal danger signs"
                    )
                )
                clinicianDirectedActions.addAll(
                    listOf(
                        "Schedule routine 2nd/3rd trimester medical officer review"
                    )
                )
                val greenRationale = if (unmeasured.isNotEmpty()) {
                    "No danger sign detected from information entered. Unmeasured: ${unmeasured.joinToString(", ")}."
                } else {
                    "All maternal vitals and clinical observations within normal gestational parameters."
                }
                Triple(
                    RiskLevel.GREEN,
                    greenRationale,
                    "Continue routine Antenatal Care (ANC) counseling, nutrition guidance, and daily IFA supplementation."
                )
            }
        }

        return NativeClinicalEvaluation(
            riskLevel = riskLevel,
            riskScore = finalScore,
            primaryFactors = factors.ifEmpty { listOf("Normal checkup") },
            unmeasuredVitals = unmeasured,
            clinicalRationale = rationale,
            recommendedProtocol = protocol,
            ashaSafeActions = ashaSafeActions,
            clinicianDirectedActions = clinicianDirectedActions,
            requiresAmbulance = needsAmbulance,
            requiresBloodTransfusion = needsBlood,
            rulePackVersion = PROTOCOL_VERSION
        )
    }

    private fun parseBp(bp: String?): Pair<Int?, Int?> {
        if (bp.isNullOrBlank()) return Pair(null, null)
        val clean = bp.trim().lowercase()
        if (clean in listOf("not measured", "unknown", "none", "")) return Pair(null, null)
        val normalized = clean.replace("over", "/").replace("बटा", "/")
        val parts = normalized.split("/")
        if (parts.size == 2) {
            val s = parts[0].trim().toIntOrNull()
            val d = parts[1].trim().toIntOrNull()
            return Pair(s, d)
        }
        return Pair(null, null)
    }

    private fun parseHb(hbStr: String?): Double? {
        if (hbStr.isNullOrBlank()) return null
        val clean = hbStr.trim().lowercase()
        if (clean in listOf("not measured", "unknown", "none", "")) return null
        val numStr = clean.replace("g/dl", "").replace("gm/dl", "").trim()
        return numStr.toDoubleOrNull()
    }
}
