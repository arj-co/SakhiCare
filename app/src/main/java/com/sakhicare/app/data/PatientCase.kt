package com.sakhicare.app.data

enum class RiskLevel {
    RED,
    AMBER,
    GREEN
}

data class DangerSigns(
    val bleeding: Boolean = false,
    val fever: Boolean = false,
    val headache: Boolean = false,
    val reducedFetalMovement: Boolean = false
)

data class PatientCase(
    val id: String,
    val patientName: String,
    val village: String,
    val bloodPressure: String,
    val haemoglobin: String,
    val dangerSigns: DangerSigns,
    val riskLevel: RiskLevel,
    val assessmentDate: String = "10 Aug 2026",
    val syncStatus: String = "Pending"
) {
    companion object {
        fun calculateRisk(dangerSigns: DangerSigns, bloodPressure: String): RiskLevel {
            val isHighBp = parseIsHighBp(bloodPressure)
            
            return when {
                dangerSigns.bleeding || isHighBp -> RiskLevel.RED
                dangerSigns.fever || dangerSigns.headache -> RiskLevel.AMBER
                else -> RiskLevel.GREEN
            }
        }

        private fun parseIsHighBp(bp: String): Boolean {
            val parts = bp.trim().split("/")
            if (parts.size == 2) {
                val sys = parts[0].trim().toIntOrNull() ?: 0
                val dia = parts[1].trim().toIntOrNull() ?: 0
                if (sys >= 140 || dia >= 90) return true
            }
            return false
        }

        val sampleCases = listOf(
            PatientCase(
                id = "SC-001",
                patientName = "Sunita Devi",
                village = "Rampur",
                bloodPressure = "145/95",
                haemoglobin = "10.2 g/dL",
                dangerSigns = DangerSigns(bleeding = false, fever = true, headache = true),
                riskLevel = RiskLevel.RED,
                assessmentDate = "09 Aug 2026",
                syncStatus = "Synced"
            ),
            PatientCase(
                id = "SC-002",
                patientName = "Meena Kumari",
                village = "Sitapur",
                bloodPressure = "118/78",
                haemoglobin = "11.8 g/dL",
                dangerSigns = DangerSigns(),
                riskLevel = RiskLevel.GREEN,
                assessmentDate = "08 Aug 2026",
                syncStatus = "Synced"
            )
        )
    }
}
