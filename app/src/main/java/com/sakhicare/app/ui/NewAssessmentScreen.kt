package com.sakhicare.app.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakhicare.app.data.DangerSigns
import com.sakhicare.app.data.PatientCase
import com.sakhicare.app.data.RiskLevel
import com.sakhicare.app.data.TriageEngine
import com.sakhicare.app.i18n.AppLanguage
import com.sakhicare.app.i18n.Strings
import com.sakhicare.app.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAssessmentScreen(
    currentLanguage: AppLanguage,
    onAssessmentSubmitted: (PatientCase, java.io.File?, String?, Int?) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var currentStep by remember { mutableIntStateOf(1) }

    // Phase 2: Voice Note to Form state
    var showVoiceDialog by remember { mutableStateOf(false) }
    var attachedAudioFile by remember { mutableStateOf<java.io.File?>(null) }
    var attachedVoiceTranscript by remember { mutableStateOf<String?>(null) }
    var attachedAudioDurationSeconds by remember { mutableStateOf<Int?>(null) }
    val currentCaseId = remember { "SC-${UUID.randomUUID().toString().take(8).uppercase()}" }

    // Step 1: Identification & Gestational Details
    var patientName by remember { mutableStateOf("") }
    var village by remember { mutableStateOf("") }
    var ageStr by remember { mutableStateOf("") }
    var gestationalWeeksStr by remember { mutableStateOf("") }
    var gravidaStr by remember { mutableStateOf("") }
    var paraStr by remember { mutableStateOf("") }

    // Step 2: 8 Danger Signs
    var bleeding by remember { mutableStateOf(false) }
    var convulsions by remember { mutableStateOf(false) }
    var severeHeadache by remember { mutableStateOf(false) }
    var severeAbdominalPain by remember { mutableStateOf(false) }
    var severeBreathlessness by remember { mutableStateOf(false) }
    var fever by remember { mutableStateOf(false) }
    var prematureLabourWaterBroke by remember { mutableStateOf(false) }
    var reducedFetalMovement by remember { mutableStateOf(false) }

    // Step 3: Vitals & Measurements
    var bpNotMeasured by remember { mutableStateOf(false) }
    var systolicBp by remember { mutableStateOf("") }
    var diastolicBp by remember { mutableStateOf("") }

    var hbNotMeasured by remember { mutableStateOf(false) }
    var haemoglobinVal by remember { mutableStateOf("") }

    // Step 4: Travel & Transport Constraints
    var distanceKm by remember { mutableStateOf("") }
    var travelRoadBlocked by remember { mutableStateOf(false) }
    var travelNightTime by remember { mutableStateOf(false) }
    var travelNoVehicle by remember { mutableStateOf(false) }

    // Step 5: Save confirmation
    var showSuccessModal by remember { mutableStateOf(false) }
    var createdCaseId by remember { mutableStateOf("") }
    var createdRisk by remember { mutableStateOf(RiskLevel.GREEN) }

    val dangerSigns = remember(bleeding, convulsions, severeHeadache, severeAbdominalPain, severeBreathlessness, fever, prematureLabourWaterBroke, reducedFetalMovement) {
        DangerSigns(
            bleeding = bleeding,
            convulsions = convulsions,
            severeHeadache = severeHeadache,
            severeAbdominalPain = severeAbdominalPain,
            severeBreathlessness = severeBreathlessness,
            fever = fever,
            prematureLabourWaterBroke = prematureLabourWaterBroke,
            reducedFetalMovement = reducedFetalMovement
        )
    }

    val bloodPressureString = remember(bpNotMeasured, systolicBp, diastolicBp) {
        if (bpNotMeasured || (systolicBp.isBlank() && diastolicBp.isBlank())) null
        else "${systolicBp.trim()}/${diastolicBp.trim()}"
    }

    val haemoglobinString = remember(hbNotMeasured, haemoglobinVal) {
        if (hbNotMeasured || haemoglobinVal.isBlank()) null
        else "${haemoglobinVal.trim()} g/dL"
    }

    val clinicalEvaluation = remember(dangerSigns, bloodPressureString, haemoglobinString) {
        TriageEngine.evaluate(
            bloodPressure = bloodPressureString,
            haemoglobinStr = haemoglobinString,
            dangerSigns = dangerSigns
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoft)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header Bar
            Surface(color = SurfaceWhite, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Neutral900)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Maternal Check (गर्भावस्था जांच)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Neutral900)
                        )
                        Text(
                            text = "Step $currentStep of 5 • Progressive Encounter",
                            style = MaterialTheme.typography.labelSmall.copy(color = Primary)
                        )
                    }
                }
            }

            // Step Body
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (currentStep) {
                    1 -> {
                        // ── STEP 1: Case Identification & Pregnancy History ──
                        Text(
                            text = "1. मरीज एवं गर्भावस्था जानकारी\nPatient & Pregnancy Details",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Neutral900)
                        )
                        Text(
                            text = "Record minimal necessary demographics. No unnecessary personal data is collected.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Neutral500)
                        )

                        // Voice Note to Form Intake Shortcut
                        Button(
                            onClick = { showVoiceDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Voice Note to Form (आवाज़ से भरें)", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        if (attachedAudioFile != null) {
                            Surface(
                                color = CoralSecondary.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "🎙️ Voice note attached (${attachedAudioDurationSeconds ?: 0}s)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CoralPrimary
                                    )
                                    TextButton(onClick = { showVoiceDialog = true }) {
                                        Text("Review", fontSize = 12.sp, color = CoralPrimary)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = patientName,
                            onValueChange = { patientName = it },
                            label = { Text("Mother's Name or Local ID (नाम / पहचान)") },
                            placeholder = { Text("e.g. Meera Devi") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = village,
                            onValueChange = { village = it },
                            label = { Text("Village / Hamlet (गांव / टोला)") },
                            placeholder = { Text("e.g. Rampur Tola") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = ageStr,
                                onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) ageStr = it },
                                label = { Text("Age (Years)") },
                                placeholder = { Text("e.g. 24") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = gestationalWeeksStr,
                                onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) gestationalWeeksStr = it },
                                label = { Text("Gestational (Weeks)") },
                                placeholder = { Text("e.g. 32") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = gravidaStr,
                                onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) gravidaStr = it },
                                label = { Text("Gravida (Total Pregnancies)") },
                                placeholder = { Text("G") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = paraStr,
                                onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) paraStr = it },
                                label = { Text("Para (Deliveries)") },
                                placeholder = { Text("P") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (patientName.isBlank()) {
                                    Toast.makeText(context, "Please enter patient name or case identifier", Toast.LENGTH_SHORT).show()
                                } else {
                                    currentStep = 2
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text("Next: Immediate Danger Signs", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                        }
                    }

                    2 -> {
                        // ── STEP 2: Immediate Danger Signs ──
                        Text(
                            text = "2. खतरे के गंभीर लक्षण\nImmediate Danger Signs",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Neutral900)
                        )
                        Text(
                            text = "Ask emergency questions directly. Any positive sign requires urgent escalation.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Neutral500)
                        )

                        DangerSignCheckboxCard("योनि से अत्यधिक रक्तस्राव (Heavy Vaginal Bleeding)", bleeding) { bleeding = it }
                        DangerSignCheckboxCard("दौरे, बेहोशी या आंखों के आगे अंधेरा (Convulsions, Fits or Fainting)", convulsions) { convulsions = it }
                        DangerSignCheckboxCard("अत्यधिक सिरदर्द या धुंधला दिखना (Severe Headache or Blurred Vision)", severeHeadache) { severeHeadache = it }
                        DangerSignCheckboxCard("पेट में असहनीय दर्द (Severe Abdominal Pain)", severeAbdominalPain) { severeAbdominalPain = it }
                        DangerSignCheckboxCard("सांस लेने में अत्यधिक कठिनाई या सीने में दर्द (Severe Breathlessness)", severeBreathlessness) { severeBreathlessness = it }
                        DangerSignCheckboxCard("तेज बुखार एवं अत्यधिक कमजोरी (High Fever with Severe Illness)", fever) { fever = it }
                        DangerSignCheckboxCard("समय से पहले पानी छूटना या प्रसव पीड़ा (Water Broke Early / Premature Labour)", prematureLabourWaterBroke) { prematureLabourWaterBroke = it }
                        DangerSignCheckboxCard("शिशु की हलचल बंद या कम होना (Absent or Reduced Baby Movement)", reducedFetalMovement) { reducedFetalMovement = it }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { currentStep = 1 },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = { currentStep = 3 },
                                modifier = Modifier.weight(1.5f).height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Text("Next: Measurements", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    3 -> {
                        // ── STEP 3: Vitals & Measurements (NO SILENT DEFAULTS!) ──
                        Text(
                            text = "3. जांच एवं माप\nVitals & Measurements",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Neutral900)
                        )
                        Text(
                            text = "Explicit 'Not measured' state is preserved. Missing values are NEVER defaulted to normal.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Neutral500)
                        )

                        // Blood Pressure Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("रक्तचाप (Blood Pressure)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = bpNotMeasured,
                                            onCheckedChange = {
                                                bpNotMeasured = it
                                                if (it) {
                                                    systolicBp = ""
                                                    diastolicBp = ""
                                                }
                                            }
                                        )
                                        Text("Not measured", style = MaterialTheme.typography.bodySmall.copy(color = Neutral500))
                                    }
                                }

                                if (!bpNotMeasured) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        OutlinedTextField(
                                            value = systolicBp,
                                            onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) systolicBp = it },
                                            label = { Text("Systolic (ऊपर)") },
                                            placeholder = { Text("e.g. 140") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        OutlinedTextField(
                                            value = diastolicBp,
                                            onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) diastolicBp = it },
                                            label = { Text("Diastolic (नीचे)") },
                                            placeholder = { Text("e.g. 90") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                    Text(
                                        "Tip: Ensure mother rested for 5 minutes before reading. Repeat measurement if >= 140/90.",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Neutral500)
                                    )
                                } else {
                                    Surface(color = Neutral100, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            "BP will be marked as 'Not measured' and accounted for in clinical decision support.",
                                            modifier = Modifier.padding(10.dp),
                                            style = MaterialTheme.typography.bodySmall.copy(color = Neutral700)
                                        )
                                    }
                                }
                            }
                        }

                        // Haemoglobin Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("हीमोग्लोबिन (Haemoglobin)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = hbNotMeasured,
                                            onCheckedChange = {
                                                hbNotMeasured = it
                                                if (it) haemoglobinVal = ""
                                            }
                                        )
                                        Text("Not measured", style = MaterialTheme.typography.bodySmall.copy(color = Neutral500))
                                    }
                                }

                                if (!hbNotMeasured) {
                                    OutlinedTextField(
                                        value = haemoglobinVal,
                                        onValueChange = { haemoglobinVal = it },
                                        label = { Text("Haemoglobin Level (g/dL)") },
                                        placeholder = { Text("e.g. 8.5") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                } else {
                                    Surface(color = Neutral100, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            "Hb strip unavailable: will be marked as 'Not measured'.",
                                            modifier = Modifier.padding(10.dp),
                                            style = MaterialTheme.typography.bodySmall.copy(color = Neutral700)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { currentStep = 2 },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = { currentStep = 4 },
                                modifier = Modifier.weight(1.5f).height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Text("Next: Travel Constraints", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    4 -> {
                        // ── STEP 4: Travel & Transport Constraints ──
                        Text(
                            text = "4. दूरी एवं परिवहन बाधाएं\nTravel & Transport Constraints",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Neutral900)
                        )
                        Text(
                            text = "Record practical access constraints to help the Medical Officer and Dispatcher.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Neutral500)
                        )

                        OutlinedTextField(
                            value = distanceKm,
                            onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) distanceKm = it },
                            label = { Text("Distance to PHC/CHC (km)") },
                            placeholder = { Text("e.g. 18") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        DangerSignCheckboxCard("सड़क खराब / पानी भरा / पुल टूटा (Road Blocked / Flood / Rough Terrain)", travelRoadBlocked) { travelRoadBlocked = it }
                        DangerSignCheckboxCard("रात्रि समय / अंधेरा (Night Time / Zero Street Lighting)", travelNightTime) { travelNightTime = it }
                        DangerSignCheckboxCard("कोई निजी या सार्वजनिक वाहन उपलब्ध नहीं (No Vehicle / Auto Available)", travelNoVehicle) { travelNoVehicle = it }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { currentStep = 3 },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = { currentStep = 5 },
                                modifier = Modifier.weight(1.5f).height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Text("Review & Triage", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    5 -> {
                        // ── STEP 5: Review & Confirmation ──
                        Text(
                            text = "5. समीक्षा एवं क्लिनिकल निर्णय\nReview & Clinical Triage",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Neutral900)
                        )

                        // Triage Result Banner
                        val (riskColor, riskBg) = when (clinicalEvaluation.riskLevel) {
                            RiskLevel.RED -> Pair(TriageRed, TriageRedBg)
                            RiskLevel.AMBER -> Pair(TriageAmber, TriageAmberBg)
                            RiskLevel.GREEN -> Pair(TriageGreen, TriageGreenBg)
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = riskBg),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(riskColor))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${clinicalEvaluation.riskLevel.name} TRIAGE RESULT",
                                        style = MaterialTheme.typography.titleMedium.copy(color = riskColor, fontWeight = FontWeight.ExtraBold)
                                    )
                                    Surface(shape = RoundedCornerShape(6.dp), color = Color.White) {
                                        Text(
                                            clinicalEvaluation.rulePackVersion,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(color = Neutral700)
                                        )
                                    }
                                }
                                Text(
                                    clinicalEvaluation.clinicalRationale,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Neutral900, fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }

                        // What was found
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("What We Found (जांच में क्या मिला):", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text("• Patient: ${patientName.ifBlank { "Unknown" }} ($village)", style = MaterialTheme.typography.bodySmall)
                                Text("• BP: ${bloodPressureString ?: "Not measured"}", style = MaterialTheme.typography.bodySmall)
                                Text("• Hb: ${haemoglobinString ?: "Not measured"}", style = MaterialTheme.typography.bodySmall)
                                if (clinicalEvaluation.primaryFactors.isNotEmpty()) {
                                    Text("• Triggers: ${clinicalEvaluation.primaryFactors.joinToString("; ")}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (clinicalEvaluation.unmeasuredVitals.isNotEmpty()) {
                                    Text("• Unmeasured: ${clinicalEvaluation.unmeasuredVitals.joinToString("; ")}", style = MaterialTheme.typography.bodySmall.copy(color = Neutral500))
                                }
                            }
                        }

                        // ASHA Safe First Response Actions
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("What To Do Now (ASHA-Safe Actions):", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = PrimaryDark))
                                clinicalEvaluation.ashaSafeActions.forEach { action ->
                                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                                        Text(action, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        // Clinician Directed Procedures
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Neutral100)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Requires Medical Officer Direction:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Neutral700))
                                clinicalEvaluation.clinicianDirectedActions.forEach { action ->
                                    Text("• $action", style = MaterialTheme.typography.bodySmall.copy(color = Neutral700))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { currentStep = 4 },
                                modifier = Modifier.weight(1f).height(54.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = {
                                    val newCaseId = "SC-${UUID.randomUUID().toString().take(6).uppercase()}"
                                    val newCase = PatientCase(
                                        id = newCaseId,
                                        localId = newCaseId,
                                        patientName = patientName.trim().ifBlank { "Unknown" },
                                        village = village.trim().ifBlank { "Unknown" },
                                        ageYears = ageStr.toIntOrNull(),
                                        gestationalAgeWeeks = gestationalWeeksStr.toIntOrNull(),
                                        gravida = gravidaStr.toIntOrNull(),
                                        para = paraStr.toIntOrNull(),
                                        travelConstraints = listOfNotNull(
                                            if (travelRoadBlocked) "Road blocked" else null,
                                            if (travelNightTime) "Night" else null,
                                            if (travelNoVehicle) "No vehicle" else null
                                        ).joinToString(", ").ifBlank { null },
                                        bloodPressure = bloodPressureString,
                                        haemoglobin = haemoglobinString,
                                        dangerSigns = dangerSigns,
                                        riskLevel = clinicalEvaluation.riskLevel,
                                        riskScore = clinicalEvaluation.riskScore,
                                        clinicalRationale = clinicalEvaluation.clinicalRationale,
                                        recommendedProtocol = clinicalEvaluation.recommendedProtocol,
                                        unmeasuredVitals = clinicalEvaluation.unmeasuredVitals,
                                        ashaSafeActions = clinicalEvaluation.ashaSafeActions,
                                        clinicianDirectedActions = clinicalEvaluation.clinicianDirectedActions,
                                        assessmentTimestamp = System.currentTimeMillis(),
                                        syncStatus = "QUEUED",
                                        isDemo = false,
                                        rulePackVersion = "mohfw-hrp-v1.0"
                                    )

                                    onAssessmentSubmitted(
                                        newCase,
                                        attachedAudioFile,
                                        attachedVoiceTranscript,
                                        attachedAudioDurationSeconds
                                    )
                                    createdCaseId = newCaseId
                                    createdRisk = clinicalEvaluation.riskLevel
                                    showSuccessModal = true
                                },
                                modifier = Modifier.weight(1.8f).height(54.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Case Locally", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }

        // Success Confirmation Modal
        if (showSuccessModal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    modifier = Modifier.padding(24.dp).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            color = PrimaryLight,
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(36.dp))
                            }
                        }

                        Text(
                            text = "सुरक्षित सहेजा गया\nSaved on This Phone",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        )

                        Text(
                            text = "Case $createdCaseId stored in encrypted local database. Added to outbox queue for server sync.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Neutral500, textAlign = TextAlign.Center)
                        )

                        Button(
                            onClick = {
                                showSuccessModal = false
                                onNavigateBack()
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text("Return to Dashboard", style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        // Voice Note to Form Dialog
        if (showVoiceDialog) {
            VoiceNoteToFormDialog(
                caseId = currentCaseId,
                onDismiss = { showVoiceDialog = false },
                onConfirmFields = { fields, audioFile, transcript, duration ->
                    attachedAudioFile = audioFile
                    attachedVoiceTranscript = transcript.ifBlank { null }
                    attachedAudioDurationSeconds = duration

                    fields.patientName?.let { patientName = it }
                    fields.village?.let { village = it }
                    fields.gestationalAgeWeeks?.let { gestationalWeeksStr = it.toString() }

                    fields.bloodPressure?.let { bp ->
                        val parts = bp.split("/")
                        if (parts.size == 2) {
                            systolicBp = parts[0]
                            diastolicBp = parts[1]
                            bpNotMeasured = false
                        }
                    }

                    fields.haemoglobin?.let { hb ->
                        haemoglobinVal = hb.toString()
                        hbNotMeasured = false
                    }

                    if (fields.dangerSigns.bleeding) bleeding = true
                    if (fields.dangerSigns.convulsions) convulsions = true
                    if (fields.dangerSigns.severeHeadache) severeHeadache = true
                    if (fields.dangerSigns.severeAbdominalPain) severeAbdominalPain = true
                    if (fields.dangerSigns.severeBreathlessness) severeBreathlessness = true
                    if (fields.dangerSigns.fever) fever = true
                    if (fields.dangerSigns.prematureLabourWaterBroke) prematureLabourWaterBroke = true
                    if (fields.dangerSigns.reducedFetalMovement) reducedFetalMovement = true

                    showVoiceDialog = false
                    Toast.makeText(context, "Voice fields confirmed and applied to draft", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun DangerSignCheckboxCard(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) TriageRedBg else SurfaceWhite
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (checked) TriageRed else Neutral300)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = TriageRed)
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
                    color = if (checked) TriageRed else Neutral900
                )
            )
        }
    }
}
