package com.sakhicare.app.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

object PatientRepository {

    private val _cases: SnapshotStateList<PatientCase> = mutableStateListOf()

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
