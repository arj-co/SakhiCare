package com.sakhicare.app.data

object DemoFixtures {
    fun getDemoCases(): List<PatientCase> {
        return listOf(
            PatientCase(
                id = "DEMO-SC-101",
                patientName = "Sunita Devi (सुनीता देवी)",
                village = "Rampur (रामपुर)",
                bloodPressure = "162/108",
                haemoglobin = "6.8 g/dL",
                dangerSigns = DangerSigns(bleeding = true, severeHeadache = true),
                riskLevel = RiskLevel.RED,
                riskScore = 95,
                clinicalRationale = "Severe Hypertensive Crisis (162/108) + Severe Anemia (Hb 6.8 g/dL) + Antepartum Bleeding: Imminent risk of Eclampsia and Hypovolemic Shock.",
                recommendedProtocol = "1) Emergency 108 ambulance transport immediately. 2) Left lateral tilt position. 3) Alert PHC/CHC Medical Officer.",
                assessmentTimestamp = System.currentTimeMillis() - 15 * 60 * 1000,
                syncStatus = "ACKNOWLEDGED",
                doctorAdvisory = "High BP with bleeding: Keep patient flat with legs elevated. 108 ambulance notified.",
                ambulanceStatus = "108-AMB-Rampur-04 Dispatched (ETA: 10 mins)",
                isDemo = true
            ),
            PatientCase(
                id = "DEMO-SC-102",
                patientName = "Meena Kumari (मीना कुमारी)",
                village = "Bhimpur (भीमपुर)",
                bloodPressure = "142/92",
                haemoglobin = "8.5 g/dL",
                dangerSigns = DangerSigns(fever = true, severeHeadache = true),
                riskLevel = RiskLevel.AMBER,
                riskScore = 45,
                clinicalRationale = "Gestational Hypertension (142/92) + Moderate Anemia (Hb 8.5 g/dL) + Maternal Pyrexia.",
                recommendedProtocol = "1) Refer to PHC within 24h. 2) Daily BP monitoring.",
                assessmentTimestamp = System.currentTimeMillis() - 45 * 60 * 1000,
                syncStatus = "QUEUED",
                doctorAdvisory = "Refer to PHC for malaria rapid test and lab workup.",
                ambulanceStatus = null,
                isDemo = true
            ),
            PatientCase(
                id = "DEMO-SC-103",
                patientName = "Radha Devi (राधा देवी)",
                village = "Gopalpur (गोपालपुर)",
                bloodPressure = "150/98",
                haemoglobin = "8.2 g/dL",
                dangerSigns = DangerSigns(severeHeadache = true, reducedFetalMovement = true),
                riskLevel = RiskLevel.RED,
                riskScore = 80,
                clinicalRationale = "Stage 2 Gestational HTN + Acute Fetal Distress (Reduced fetal movement in 3rd trimester).",
                recommendedProtocol = "1) Urgent transfer to CHC for Non-Stress Test (NST) and ultrasound. 2) Lateral positioning.",
                assessmentTimestamp = System.currentTimeMillis() - 2 * 3600 * 1000,
                syncStatus = "ACKNOWLEDGED",
                doctorAdvisory = "Shift to CHC for emergency NST and Doppler scan immediately.",
                ambulanceStatus = "108 Ambulance En Route",
                isDemo = true
            ),
            PatientCase(
                id = "DEMO-SC-104",
                patientName = "Pooja Sharma (पूजा शर्मा)",
                village = "Kalyanpur (कल्याणपुर)",
                bloodPressure = "118/76",
                haemoglobin = "11.8 g/dL",
                dangerSigns = DangerSigns(),
                riskLevel = RiskLevel.GREEN,
                riskScore = 10,
                clinicalRationale = "All maternal vitals and fetal observations within normal gestational limits.",
                recommendedProtocol = "Continue routine ANC counseling, balanced diet, and daily IFA tablets.",
                assessmentTimestamp = System.currentTimeMillis() - 5 * 3600 * 1000,
                syncStatus = "ACKNOWLEDGED",
                doctorAdvisory = null,
                ambulanceStatus = null,
                isDemo = true
            )
        )
    }
}
