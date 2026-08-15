package com.sakhicare.app.data

import java.util.Calendar

object AncScheduleCalculator {
    fun calculateEdd(lmpTimestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = lmpTimestamp
            add(Calendar.DAY_OF_YEAR, 280)
        }
        return cal.timeInMillis
    }

    fun getRecommendedVisits(): List<String> = listOf(
        "ANC-1: Within first 12 weeks (Registration & baseline vitals)",
        "ANC-2: 14 to 26 weeks (TT1, Hb, ultrasound check)",
        "ANC-3: 28 to 34 weeks (TT2, Gestational HTN screening)",
        "ANC-4: 36 weeks to delivery (Birth preparedness & institutional delivery plan)"
    )
}
