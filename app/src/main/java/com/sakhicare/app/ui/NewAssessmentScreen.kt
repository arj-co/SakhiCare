package com.sakhicare.app.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sakhicare.app.data.DangerSigns
import com.sakhicare.app.data.PatientCase
import com.sakhicare.app.data.RiskLevel
import com.sakhicare.app.i18n.AppLanguage
import com.sakhicare.app.i18n.Strings
import com.sakhicare.app.ui.theme.*
import com.sakhicare.app.voice.VoiceHelper
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAssessmentScreen(
    currentLanguage: AppLanguage,
    onAssessmentSubmitted: (PatientCase) -> Unit,
    onNavigateBack: () -> Unit
) {
    var patientName by remember { mutableStateOf("") }
    var village by remember { mutableStateOf("") }
    var bloodPressure by remember { mutableStateOf("") }
    var haemoglobin by remember { mutableStateOf("") }

    var bleeding by remember { mutableStateOf(false) }
    var fever by remember { mutableStateOf(false) }
    var headache by remember { mutableStateOf(false) }
    var reducedFetalMovement by remember { mutableStateOf(false) }

    var showSuccessOverlay by remember { mutableStateOf(false) }
    var submittedRisk by remember { mutableStateOf<RiskLevel?>(null) }
    var showVoiceModal by remember { mutableStateOf(false) }
    var voiceTranscript by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var sttStatusMessage by remember { mutableStateOf("Offline On-Device AI Speech Recognition Ready") }

    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )

    // Auto-navigate after success
    LaunchedEffect(showSuccessOverlay) {
        if (showSuccessOverlay) {
            delay(2500)
            onNavigateBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundSoft)
                .verticalScroll(scrollState)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header ──
            Text(
                text = Strings.get("new_assessment", currentLanguage),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Neutral900,
                    fontWeight = FontWeight.Bold
                )
            )

            // ── Voice STT Banner ──
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showVoiceModal = true }
            ) {
                Box(
                    modifier = Modifier
                        .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))))
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.25f),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(48.dp)
                                .scale(pulseScale)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    Strings.get("voice_assistant", currentLanguage),
                                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                Strings.get("voice_assistant_desc", currentLanguage),
                                style = MaterialTheme.typography.labelMedium.copy(color = Color.White.copy(alpha = 0.85f))
                            )
                        }
                    }
                }
            }

            // ── Section 1: Patient Info ──
            SectionCard(title = Strings.get("patient_details", currentLanguage)) {
                ModernTextField(value = patientName, onValueChange = { patientName = it }, label = Strings.get("patient_name", currentLanguage), placeholder = Strings.get("patient_name_placeholder", currentLanguage))
                Spacer(modifier = Modifier.height(12.dp))
                ModernTextField(value = village, onValueChange = { village = it }, label = Strings.get("village", currentLanguage), placeholder = Strings.get("village_placeholder", currentLanguage))
            }

            // ── Section 2: Vitals ──
            SectionCard(title = Strings.get("vital_measurements", currentLanguage)) {
                ModernTextField(value = bloodPressure, onValueChange = { bloodPressure = it }, label = Strings.get("bp", currentLanguage), placeholder = Strings.get("bp_placeholder", currentLanguage))
                Spacer(modifier = Modifier.height(12.dp))
                ModernTextField(value = haemoglobin, onValueChange = { haemoglobin = it }, label = Strings.get("haemoglobin", currentLanguage), placeholder = Strings.get("hb_placeholder", currentLanguage))
            }

            // ── Section 3: Danger Signs ──
            SectionCard(title = Strings.get("danger_signs", currentLanguage)) {
                ModernCheckRow(label = Strings.get("bleeding", currentLanguage), checked = bleeding, onCheckedChange = { bleeding = it })
                ModernCheckRow(label = Strings.get("fever", currentLanguage), checked = fever, onCheckedChange = { fever = it })
                ModernCheckRow(label = Strings.get("headache", currentLanguage), checked = headache, onCheckedChange = { headache = it })
                ModernCheckRow(label = Strings.get("reduced_fetal_movement", currentLanguage), checked = reducedFetalMovement, onCheckedChange = { reducedFetalMovement = it })
            }

            // ── Submit Button ──
            Button(
                onClick = {
                    if (patientName.isBlank() && bloodPressure.isBlank()) {
                        Toast.makeText(context, "Please enter patient name or vitals", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val ds = DangerSigns(bleeding, fever, headache, reducedFetalMovement)
                    val bp = bloodPressure.ifBlank { "120/80" }
                    val hb = if (haemoglobin.isBlank()) "11.0 g/dL" else if (haemoglobin.endsWith("g/dL")) haemoglobin else "$haemoglobin g/dL"
                    val evaluation = com.sakhicare.app.data.TriageEngine.evaluate(bp, hb, ds)
                    submittedRisk = evaluation.riskLevel

                    onAssessmentSubmitted(
                        PatientCase(
                            id = "SC-${System.currentTimeMillis() % 100000}",
                            patientName = patientName.ifBlank { "Unknown" },
                            village = village.ifBlank { "Unknown" },
                            bloodPressure = bp,
                            haemoglobin = hb,
                            dangerSigns = ds,
                            riskLevel = evaluation.riskLevel,
                            riskScore = evaluation.riskScore,
                            clinicalRationale = evaluation.clinicalRationale,
                            recommendedProtocol = evaluation.recommendedProtocol,
                            assessmentTimestamp = System.currentTimeMillis(),
                            syncStatus = "Pending"
                        )
                    )

                    // Reset form & show success
                    showSuccessOverlay = true
                    patientName = ""; village = ""; bloodPressure = ""; haemoglobin = ""
                    bleeding = false; fever = false; headache = false; reducedFetalMovement = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    Strings.get("submit_assessment", currentLanguage),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Success Overlay ──
        if (showSuccessOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val (successColor, successBg) = when (submittedRisk) {
                            RiskLevel.RED -> Pair(TriageRed, TriageRedBg)
                            RiskLevel.AMBER -> Pair(TriageAmber, TriageAmberBg)
                            RiskLevel.GREEN -> Pair(TriageGreen, TriageGreenBg)
                            null -> Pair(Primary, PrimaryLight)
                        }
                        Surface(color = successBg, shape = CircleShape, modifier = Modifier.size(68.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = successColor, modifier = Modifier.size(38.dp))
                            }
                        }
                        Text(
                            Strings.get("assessment_saved", currentLanguage),
                            style = MaterialTheme.typography.titleLarge.copy(color = Neutral900, fontWeight = FontWeight.Bold)
                        )
                        Surface(color = successBg, shape = RoundedCornerShape(12.dp)) {
                            Text(
                                "Triage: ${submittedRisk?.name ?: ""} RISK",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.titleMedium.copy(color = successColor, fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            Strings.get("returning_dashboard", currentLanguage),
                            style = MaterialTheme.typography.bodyMedium.copy(color = Neutral500)
                        )
                    }
                }
            }
        }
    }

    // ── Voice Dictation Modal ──
    if (showVoiceModal) {
        AlertDialog(
            onDismissRequest = { showVoiceModal = false; isListening = false },
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = AccentIndigo)
                    Text(
                        Strings.get("speech_modal_title", currentLanguage),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        color = AccentIndigoBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isListening) TriageRed else TriageGreen,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Text(
                                sttStatusMessage,
                                style = MaterialTheme.typography.labelSmall.copy(color = AccentIndigo, fontWeight = FontWeight.Medium)
                            )
                        }
                    }

                    Text(
                        Strings.get("speech_modal_desc", currentLanguage),
                        style = MaterialTheme.typography.bodySmall.copy(color = Neutral600)
                    )

                    // ── Speech Recognizer Trigger Button ──
                    Button(
                        onClick = {
                            val activity = context as? Activity ?: return@Button
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
                                return@Button
                            }

                            try {
                                val recognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
                                ) {
                                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                                } else {
                                    SpeechRecognizer.createSpeechRecognizer(context)
                                }

                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    val sttLocale = when (currentLanguage) {
                                        AppLanguage.HINDI -> "hi-IN"
                                        AppLanguage.MARATHI -> "mr-IN"
                                        AppLanguage.KANNADA -> "kn-IN"
                                        AppLanguage.BENGALI -> "bn-IN"
                                        else -> "en-IN"
                                    }
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, sttLocale)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, sttLocale)
                                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                                }

                                recognizer.setRecognitionListener(object : RecognitionListener {
                                    override fun onReadyForSpeech(params: Bundle?) {
                                        isListening = true
                                        sttStatusMessage = "Listening... Speak clearly in ${currentLanguage.displayName}"
                                    }
                                    override fun onBeginningOfSpeech() {
                                        isListening = true
                                    }
                                    override fun onRmsChanged(rmsdB: Float) {}
                                    override fun onBufferReceived(buffer: ByteArray?) {}
                                    override fun onEndOfSpeech() {
                                        isListening = false
                                        sttStatusMessage = "Processing speech..."
                                    }
                                    override fun onError(error: Int) {
                                        isListening = false
                                        sttStatusMessage = when (error) {
                                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking again."
                                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout. Tap to speak again."
                                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording issue. Check mic."
                                            else -> "Offline mode: type or select a quick template below"
                                        }
                                    }
                                    override fun onPartialResults(partialResults: Bundle?) {
                                        val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                                        if (partial != null) voiceTranscript = partial
                                    }
                                    override fun onResults(results: Bundle?) {
                                        val result = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                                        if (result != null) voiceTranscript = result
                                        isListening = false
                                        sttStatusMessage = "Speech captured! Ready to parse."
                                        recognizer.destroy()
                                    }
                                    override fun onEvent(eventType: Int, params: Bundle?) {}
                                })

                                isListening = true
                                recognizer.startListening(intent)
                            } catch (e: Exception) {
                                isListening = false
                                sttStatusMessage = "Speech recognizer unavailable on this device. Use text input."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListening) TriageRed else AccentIndigo
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isListening) Strings.get("listening", currentLanguage) else Strings.get("start_listening", currentLanguage),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // ── Text input fallback ──
                    OutlinedTextField(
                        value = voiceTranscript,
                        onValueChange = { voiceTranscript = it },
                        label = { Text(Strings.get("transcript_label", currentLanguage)) },
                        placeholder = { Text(Strings.get("transcript_placeholder", currentLanguage)) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                    )

                    // ── Quick Presets ──
                    Text(Strings.get("quick_presets", currentLanguage), style = MaterialTheme.typography.labelMedium.copy(color = Neutral700, fontWeight = FontWeight.Bold))
                    VoiceHelper.sampleMultilingualDictations.take(3).forEach { sample ->
                        Surface(
                            color = Neutral50,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Neutral200),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { voiceTranscript = sample }
                        ) {
                            Text(
                                "\"$sample\"",
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall.copy(color = Neutral700),
                                maxLines = 2
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = VoiceHelper.parseSpokenText(voiceTranscript)
                        if (p.patientName.isNotBlank()) patientName = p.patientName
                        if (p.village.isNotBlank()) village = p.village
                        if (p.bloodPressure.isNotBlank()) bloodPressure = p.bloodPressure
                        if (p.haemoglobin.isNotBlank()) haemoglobin = p.haemoglobin
                        bleeding = bleeding || p.dangerSigns.bleeding
                        fever = fever || p.dangerSigns.fever
                        headache = headache || p.dangerSigns.headache
                        reducedFetalMovement = reducedFetalMovement || p.dangerSigns.reducedFetalMovement
                        showVoiceModal = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Strings.get("parse_and_fill", currentLanguage), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoiceModal = false; isListening = false }) {
                    Text(Strings.get("cancel", currentLanguage))
                }
            }
        )
    }
}

// ── Reusable Components ──

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(color = Primary, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun ModernTextField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium.copy(color = Neutral400)) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ModernCheckRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        color = if (checked) PrimaryLight else Neutral50,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange, colors = CheckboxDefaults.colors(checkedColor = Primary))
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(color = Neutral900, fontWeight = FontWeight.Medium))
        }
    }
}
