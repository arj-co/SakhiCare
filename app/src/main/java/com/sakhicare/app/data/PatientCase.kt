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
    val fever: Boolean = false,
    val headache: Boolean = false,
    val reducedFetalMovement: Boolean = false
) {
    fun hasAny(): Boolean = bleeding || fever || headache || reducedFetalMovement
    fun count(): Int = listOf(bleeding, fever, headache, reducedFetalMovement).count { it }
}

data class PatientCase(
    val id: String,
    val patientName: String,
    val village: String,
    val bloodPressure: String,
    val haemoglobin: String,
    val dangerSigns: DangerSigns,
    val riskLevel: RiskLevel,
    val riskScore: Int = 10,
    val clinicalRationale: String? = null,
    val recommendedProtocol: String? = null,
    val assessmentTimestamp: Long = System.currentTimeMillis(),
    val syncStatus: String = "Pending",
    val doctorAdvisory: String? = null,
    val ambulanceStatus: String? = null
) {
    /** Formatted date string like "18 Aug 2026, 1:40 PM" */
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.ENGLISH)
            return sdf.format(Date(assessmentTimestamp))
        }

    /** Relative time like "2 min ago", "1 hour ago", "Today at 1:40 PM" */
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
            bloodPressure: String,
            haemoglobin: String = "11.0"
        ): RiskLevel {
            return TriageEngine.evaluate(
                bloodPressure = bloodPressure,
                haemoglobinStr = haemoglobin,
                dangerSigns = dangerSigns
            ).riskLevel
        }
    }
}
