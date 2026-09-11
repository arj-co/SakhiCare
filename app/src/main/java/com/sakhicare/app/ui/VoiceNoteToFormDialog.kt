package com.sakhicare.app.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sakhicare.app.ui.theme.*
import com.sakhicare.app.voice.AudioRecordManager
import com.sakhicare.app.voice.AudioRecordState
import com.sakhicare.app.voice.CandidateAssessmentFields
import com.sakhicare.app.voice.VoiceFormOrganizer
import java.io.File

/**
 * Voice Note to Form Dialog (Phase 2)
 *
 * Provides microphone consent, audio recording controls, live playback,
 * rule-based candidate field extraction, and an explicit confirmation workflow
 * before any field is applied to the clinical assessment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceNoteToFormDialog(
    caseId: String,
    onDismiss: () -> Unit,
    onConfirmFields: (
        fields: CandidateAssessmentFields,
        audioFile: File?,
        transcript: String,
        durationSeconds: Int
    ) -> Unit
) {
    val context = LocalContext.current
    val recordManager = remember { AudioRecordManager(context) }

    val recordState by recordManager.recordState.collectAsState()
    val durationSeconds by recordManager.durationSeconds.collectAsState()
    val currentAudioFile by recordManager.currentAudioFile.collectAsState()

    var transcriptText by remember { mutableStateOf("") }
    var consentAgreed by remember { mutableStateOf(true) }

    // Extracted candidate fields
    var candidateDraft by remember {
        mutableStateOf(VoiceFormOrganizer.organizeTranscript(""))
    }

    // Editable draft field states
    var editableName by remember { mutableStateOf("") }
    var editableVillage by remember { mutableStateOf("") }
    var editableGa by remember { mutableStateOf("") }
    var editableBp by remember { mutableStateOf("") }
    var editableHb by remember { mutableStateOf("") }
    var editableTravel by remember { mutableStateOf("") }

    // Re-extract whenever transcript text changes
    LaunchedEffect(transcriptText) {
        val organized = VoiceFormOrganizer.organizeTranscript(transcriptText)
        candidateDraft = organized
        if (organized.extractedFieldKeys.contains("patientName")) {
            editableName = organized.candidateFields.patientName ?: ""
        }
        if (organized.extractedFieldKeys.contains("village")) {
            editableVillage = organized.candidateFields.village ?: ""
        }
        if (organized.extractedFieldKeys.contains("gestationalAgeWeeks")) {
            editableGa = organized.candidateFields.gestationalAgeWeeks?.toString() ?: ""
        }
        if (organized.extractedFieldKeys.contains("bloodPressure")) {
            editableBp = organized.candidateFields.bloodPressure ?: ""
        }
        if (organized.extractedFieldKeys.contains("haemoglobin")) {
            editableHb = organized.candidateFields.haemoglobin?.toString() ?: ""
        }
        if (organized.extractedFieldKeys.contains("travelConstraints")) {
            editableTravel = organized.candidateFields.travelConstraints ?: ""
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recordManager.cleanUp()
        }
    }

    Dialog(
        onDismissRequest = {
            recordManager.cleanUp()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice note",
                            tint = CoralPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Voice Note to Form (आवाज़ से भरें)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    IconButton(onClick = {
                        recordManager.cleanUp()
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // 1. Consent Banner
                Surface(
                    color = SageGreenLight,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = "Consent",
                            tint = ForestGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Consent: Your voice note will be attached to this case and sent to the Care Desk when possible.",
                            fontSize = 12.sp,
                            color = ForestGreen,
                            lineHeight = 16.sp
                        )
                    }
                }

                // 2. Audio Recording Deck
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Timer & Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (recordState) {
                                    AudioRecordState.RECORDING -> "🔴 Recording..."
                                    AudioRecordState.PAUSED -> "⏸️ Paused"
                                    AudioRecordState.STOPPED -> "⏹️ Audio Captured"
                                    AudioRecordState.PLAYING -> "▶️ Playing back"
                                    AudioRecordState.IDLE -> "⚪ Tap Record to begin"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            val mins = durationSeconds / 60
                            val secs = durationSeconds % 60
                            Text(
                                text = "%02d:%02d / 02:00".format(mins, secs),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (recordState == AudioRecordState.RECORDING) CoralPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Recorder Control Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (recordState) {
                                AudioRecordState.IDLE -> {
                                    Button(
                                        onClick = {
                                            val started = recordManager.startRecording(caseId)
                                            if (!started) {
                                                Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                                        shape = CircleShape,
                                        modifier = Modifier.size(56.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = "Start Recording", tint = Color.White, modifier = Modifier.size(28.dp))
                                    }
                                }
                                AudioRecordState.RECORDING -> {
                                    IconButton(
                                        onClick = { recordManager.pauseRecording() },
                                        modifier = Modifier.background(Color.Gray.copy(alpha = 0.2f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Pause, contentDescription = "Pause")
                                    }
                                    Button(
                                        onClick = { recordManager.stopRecording() },
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                                        shape = CircleShape,
                                        modifier = Modifier.size(56.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
                                    }
                                }
                                AudioRecordState.PAUSED -> {
                                    IconButton(
                                        onClick = { recordManager.resumeRecording() },
                                        modifier = Modifier.background(CoralPrimary.copy(alpha = 0.2f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = CoralPrimary)
                                    }
                                    Button(
                                        onClick = { recordManager.stopRecording() },
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                                        shape = CircleShape,
                                        modifier = Modifier.size(56.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
                                    }
                                }
                                AudioRecordState.STOPPED, AudioRecordState.PLAYING -> {
                                    // Playback button
                                    if (recordState == AudioRecordState.PLAYING) {
                                        IconButton(
                                            onClick = { recordManager.stopPlayback() },
                                            modifier = Modifier.background(AmberWarning.copy(alpha = 0.2f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Stop, contentDescription = "Stop playback", tint = AmberWarning)
                                        }
                                    } else {
                                        IconButton(
                                            onClick = { recordManager.startPlayback() },
                                            modifier = Modifier.background(ForestGreen.copy(alpha = 0.2f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Play back", tint = ForestGreen)
                                        }
                                    }

                                    // Delete and re-record
                                    IconButton(
                                        onClick = { recordManager.deleteRecording() },
                                        modifier = Modifier.background(Color.Red.copy(alpha = 0.1f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete recording", tint = Color.Red)
                                    }
                                }
                            }
                        }

                        // Local Attachment Metadata badge
                        if (currentAudioFile != null && currentAudioFile!!.exists()) {
                            val fileSizeKb = currentAudioFile!!.length() / 1024
                            val shaPrefix = AudioRecordManager.computeSha256(currentAudioFile!!).take(8)
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("💾 Audio saved locally ($fileSizeKb KB)", fontSize = 11.sp, color = Color.Gray)
                                    Text("SHA: $shaPrefix... (90d retention)", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                // 3. Spoken Dictation / Transcript Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Spoken Transcript (आवाज़ का विवरण)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    OutlinedTextField(
                        value = transcriptText,
                        onValueChange = { transcriptText = it },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        placeholder = {
                            Text(
                                "Enter or dictate case details, e.g.:\nमरीज ललिता देवी, गांव रामपुर, बीपी 150/100, हीमोग्लोबिन 7.2, तेज सिरदर्द और खून बहना",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                }

                // 4. Extracted Candidate Fields (Review & Edit before Confirmation)
                AnimatedVisibility(visible = candidateDraft.extractedFieldKeys.isNotEmpty() || transcriptText.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CoralSecondary.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Candidate Fields for Confirmation",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Surface(
                                    color = CoralPrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "${candidateDraft.extractedFieldKeys.size} fields extracted",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = CoralPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                "Review and edit every field below before applying to the assessment. Only confirmed fields will affect clinical triage.",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )

                            // Editable Patient Name
                            OutlinedTextField(
                                value = editableName,
                                onValueChange = { editableName = it },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Patient Name")
                                        if (candidateDraft.extractedFieldKeys.contains("patientName")) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("🎙️ Extracted", fontSize = 10.sp, color = CoralPrimary)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Editable Village
                            OutlinedTextField(
                                value = editableVillage,
                                onValueChange = { editableVillage = it },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Village / Area")
                                        if (candidateDraft.extractedFieldKeys.contains("village")) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("🎙️ Extracted", fontSize = 10.sp, color = CoralPrimary)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Vitals Row: BP & Hb
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = editableBp,
                                    onValueChange = { editableBp = it },
                                    label = { Text("Blood Pressure") },
                                    placeholder = { Text("140/90") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = editableHb,
                                    onValueChange = { editableHb = it },
                                    label = { Text("Hb (g/dL)") },
                                    placeholder = { Text("8.5") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            // Triggered Danger Signs
                            val triggeredSigns = candidateDraft.candidateFields.dangerSigns.triggeredList()
                            if (triggeredSigns.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Detected Danger Signs:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        triggeredSigns.forEach { sign ->
                                            Surface(
                                                color = Color.Red.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    "⚠️ $sign",
                                                    fontSize = 11.sp,
                                                    color = Color.Red,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Actions: Confirm or Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            recordManager.cleanUp()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val audioFile = recordManager.stopRecording() ?: currentAudioFile
                            val duration = durationSeconds

                            val confirmedFields = CandidateAssessmentFields(
                                patientName = editableName.ifBlank { null },
                                village = editableVillage.ifBlank { null },
                                gestationalAgeWeeks = editableGa.toIntOrNull(),
                                bloodPressure = editableBp.ifBlank { null },
                                haemoglobin = editableHb.toDoubleOrNull(),
                                dangerSigns = candidateDraft.candidateFields.dangerSigns,
                                travelConstraints = editableTravel.ifBlank { null },
                                notes = "Spoken note: $transcriptText"
                            )

                            onConfirmFields(confirmedFields, audioFile, transcriptText, duration)
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirm & Apply")
                    }
                }
            }
        }
    }
}
