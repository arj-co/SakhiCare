package com.sakhicare.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class RiskLevel {
    RED,
    AMBER,
    GREEN
}

data class DangerSigns(
    val bleeding: Boolean = false,
    val convulsions: Boolean = false,
    val severeHeadache: Boolean = false,
    val severeAbdominalPain: Boolean = false,
    val severeBreathlessness: Boolean = false,
    val fever: Boolean = false,
    val prematureLabourWaterBroke: Boolean = false,
    val reducedFetalMovement: Boolean = false
) {
    // Backward compatibility aliases
    val headache: Boolean get() = severeHeadache
    val convulsionsOrVisionLoss: Boolean get() = convulsions

    fun hasAny(): Boolean =
        bleeding || convulsions || severeHeadache || severeAbdominalPain ||
                severeBreathlessness || fever || prematureLabourWaterBroke || reducedFetalMovement

    fun count(): Int = listOf(
        bleeding, convulsions, severeHeadache, severeAbdominalPain,
        severeBreathlessness, fever, prematureLabourWaterBroke, reducedFetalMovement
    ).count { it }

    fun triggeredList(): List<String> {
        val list = mutableListOf<String>()
        if (bleeding) list.add("Vaginal Bleeding")
        if (convulsions) list.add("Convulsions / Unconsciousness")
        if (severeHeadache) list.add("Severe Headache / Vision Loss")
        if (severeAbdominalPain) list.add("Severe Abdominal Pain")
        if (severeBreathlessness) list.add("Severe Breathlessness")
        if (fever) list.add("High Fever")
        if (prematureLabourWaterBroke) list.add("Premature Labour / Water Broke")
        if (reducedFetalMovement) list.add("Reduced Fetal Movement")
        return list
    }
}

data class PatientCase(
    val id: String,
    val localId: String = id,
    val patientName: String,
    val village: String,
    val ageYears: Int? = null,
    val gestationalAgeWeeks: Int? = null,
    val gravida: Int? = null,
    val para: Int? = null,
    val travelConstraints: String? = null,
    val bloodPressure: String? = null,
    val haemoglobin: String? = null,
    val dangerSigns: DangerSigns = DangerSigns(),
    val riskLevel: RiskLevel,
    val riskScore: Int = 10,
    val clinicalRationale: String? = null,
    val recommendedProtocol: String? = null,
    val unmeasuredVitals: List<String> = emptyList(),
    val ashaSafeActions: List<String> = emptyList(),
    val clinicianDirectedActions: List<String> = emptyList(),
    val assessmentTimestamp: Long = System.currentTimeMillis(),
    val syncStatus: String = "SAVED_LOCALLY", // DRAFT, SAVED_LOCALLY, QUEUED, UPLOADING, ACKNOWLEDGED, FAILED, NEEDS_REVIEW
    val doctorAdvisory: String? = null,
    val ambulanceStatus: String? = null,
    val isDemo: Boolean = false,
    val rulePackVersion: String = "mohfw-hrp-v1.0",
    val audioUri: String? = null,
    val voiceTranscript: String? = null,
    val audioDurationSeconds: Int? = null,
    val audioUploadStatus: String? = null
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.ENGLISH)
            return sdf.format(Date(assessmentTimestamp))
        }

    val relativeTime: String
        get() {
            val now = System.currentTimeMillis()
            val diff = now - assessmentTimestamp
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            return when {
                minutes < 1 -> "Just now"
                minutes < 60 -> "${minutes} min ago"
                hours < 24 -> "${hours}h ago"
                days < 2 -> "Yesterday"
                days < 7 -> "${days} days ago"
                else -> {
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                    sdf.format(Date(assessmentTimestamp))
                }
            }
        }

    companion object {
        fun calculateRisk(
            dangerSigns: DangerSigns,
            bloodPressure: String?,
            haemoglobin: String? = null
        ): RiskLevel {
            return TriageEngine.evaluate(
                bloodPressure = bloodPressure,
                haemoglobinStr = haemoglobin,
                dangerSigns = dangerSigns
            ).riskLevel
        }
    }
}
