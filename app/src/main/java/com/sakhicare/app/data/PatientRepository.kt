package com.sakhicare.app.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

object PatientRepository {

    private val _cases: SnapshotStateList<PatientCase> = mutableStateListOf(
        PatientCase(
            id = "SC-101",
            patientName = "Sunita Devi (सुनीता देवी)",
            village = "Rampur (रामपुर)",
            bloodPressure = "162/108",
            haemoglobin = "6.8 g/dL",
            dangerSigns = DangerSigns(bleeding = true, fever = false, headache = true, reducedFetalMovement = false),
            riskLevel = RiskLevel.RED,
            riskScore = 95,
            clinicalRationale = "Severe Hypertensive Crisis (162/108) + Severe Anemia (Hb 6.8 g/dL) + Antepartum Bleeding: Imminent risk of Eclampsia and Hypovolemic Shock.",
            recommendedProtocol = "1) Emergency 108 ambulance transport immediately. 2) Left lateral tilt position. 3) Alert PHC/CHC Medical Officer.",
            assessmentTimestamp = System.currentTimeMillis() - 15 * 60 * 1000, // 15 mins ago
            syncStatus = "Synced",
            doctorAdvisory = "High BP with bleeding: Keep patient flat with legs elevated. 108 ambulance notified.",
            ambulanceStatus = "108-AMB-Rampur-04 Dispatched (ETA: 10 mins)"
        ),
        PatientCase(
            id = "SC-102",
            patientName = "Meena Kumari (मीना कुमारी)",
            village = "Bhimpur (भीमपुर)",
            bloodPressure = "142/92",
            haemoglobin = "8.5 g/dL",
            dangerSigns = DangerSigns(bleeding = false, fever = true, headache = true, reducedFetalMovement = false),
            riskLevel = RiskLevel.AMBER,
            riskScore = 45,
            clinicalRationale = "Gestational Hypertension (142/92) + Moderate Anemia (Hb 8.5 g/dL) + Maternal Pyrexia.",
            recommendedProtocol = "1) Refer to PHC within 24h. 2) Paracetamol 500mg. 3) Daily BP monitoring.",
            assessmentTimestamp = System.currentTimeMillis() - 45 * 60 * 1000, // 45 mins ago
            syncStatus = "Pending",
            doctorAdvisory = "Administer paracetamol 500mg, check for malaria rapid test at Sub-centre.",
            ambulanceStatus = null
        ),
        PatientCase(
            id = "SC-103",
            patientName = "Radha Devi (राधा देवी)",
            village = "Gopalpur (गोपालपुर)",
            bloodPressure = "150/98",
            haemoglobin = "8.2 g/dL",
            dangerSigns = DangerSigns(bleeding = false, fever = false, headache = true, reducedFetalMovement = true),
            riskLevel = RiskLevel.RED,
            riskScore = 80,
            clinicalRationale = "Stage 2 Gestational HTN + Acute Fetal Distress (Reduced fetal movement in 3rd trimester).",
            recommendedProtocol = "1) Urgent transfer to CHC for Non-Stress Test (NST) and ultrasound. 2) Lateral positioning.",
            assessmentTimestamp = System.currentTimeMillis() - 2 * 3600 * 1000, // 2 hours ago
            syncStatus = "Synced",
            doctorAdvisory = "Shift to CHC for emergency NST and Doppler scan immediately.",
            ambulanceStatus = "108 Ambulance En Route"
        ),
        PatientCase(
            id = "SC-104",
            patientName = "Pooja Sharma (पूजा शर्मा)",
            village = "Kalyanpur (कल्याणपुर)",
            bloodPressure = "118/76",
            haemoglobin = "11.8 g/dL",
            dangerSigns = DangerSigns(bleeding = false, fever = false, headache = false, reducedFetalMovement = false),
            riskLevel = RiskLevel.GREEN,
            riskScore = 10,
            clinicalRationale = "All maternal vitals and fetal observations within normal gestational limits.",
            recommendedProtocol = "Continue routine ANC counseling, balanced diet, and daily IFA tablets.",
            assessmentTimestamp = System.currentTimeMillis() - 5 * 3600 * 1000, // 5 hours ago
            syncStatus = "Synced",
            doctorAdvisory = null,
            ambulanceStatus = null
        ),
        PatientCase(
            id = "SC-105",
            patientName = "Rekha Bai (रेखा बाई)",
            village = "Chandpur (चंदपुर)",
            bloodPressure = "124/82",
            haemoglobin = "10.4 g/dL",
            dangerSigns = DangerSigns(bleeding = false, fever = false, headache = false, reducedFetalMovement = false),
            riskLevel = RiskLevel.GREEN,
            riskScore = 10,
            clinicalRationale = "Second trimester ANC encounter normal. Mild physiological edema.",
            recommendedProtocol = "Advise leg elevation during rest, regular hydration, and TT booster check.",
            assessmentTimestamp = System.currentTimeMillis() - 24 * 3600 * 1000, // Yesterday
            syncStatus = "Synced",
            doctorAdvisory = null,
            ambulanceStatus = null
        )
    )

    val cases: List<PatientCase> get() = _cases

    fun getCaseById(id: String): PatientCase? = _cases.find { it.id == id }

    fun getTotalCount(): Int = _cases.size

    fun getPendingSyncCount(): Int = _cases.count { it.syncStatus == "Pending" }

    fun getRedRiskCount(): Int = _cases.count { it.riskLevel == RiskLevel.RED }

    fun getAmberRiskCount(): Int = _cases.count { it.riskLevel == RiskLevel.AMBER }

    fun getGreenRiskCount(): Int = _cases.count { it.riskLevel == RiskLevel.GREEN }

    fun getLastAssessmentTime(): Long? = _cases.maxByOrNull { it.assessmentTimestamp }?.assessmentTimestamp

    fun addCase(patientCase: PatientCase) {
        _cases.add(0, patientCase)
    }

    fun syncAllPending(): Int {
        var count = 0
        for (i in 0 until _cases.size) {
            if (_cases[i].syncStatus == "Pending") {
                _cases[i] = _cases[i].copy(syncStatus = "Synced")
                count++
            }
        }
        return count
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
