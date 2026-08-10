package com.sakhicare.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakhicare.app.data.DangerSigns
import com.sakhicare.app.data.PatientCase
import com.sakhicare.app.data.RiskLevel
import com.sakhicare.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAssessmentScreen(
    onAssessmentSubmitted: (PatientCase) -> Unit
) {
    var patientName by remember { mutableStateOf("") }
    var village by remember { mutableStateOf("") }
    var bloodPressure by remember { mutableStateOf("") }
    var haemoglobin by remember { mutableStateOf("") }

    var bleeding by remember { mutableStateOf(false) }
    var fever by remember { mutableStateOf(false) }
    var headache by remember { mutableStateOf(false) }
    var reducedFetalMovement by remember { mutableStateOf(false) }

    var submittedResult by remember { mutableStateOf<RiskLevel?>(null) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TealBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Maternal Health Assessment",
            style = MaterialTheme.typography.titleLarge.copy(color = TealDark)
        )

        // Patient Identification Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "1. Patient Details",
                    style = MaterialTheme.typography.titleMedium.copy(color = TealPrimary, fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = patientName,
                    onValueChange = { patientName = it },
                    label = { Text("Patient Name") },
                    placeholder = { Text("e.g. Sunita Devi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = village,
                    onValueChange = { village = it },
                    label = { Text("Village") },
                    placeholder = { Text("e.g. Rampur") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Clinical Vitals Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "2. Vital Measurements",
                    style = MaterialTheme.typography.titleMedium.copy(color = TealPrimary, fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = bloodPressure,
                    onValueChange = { bloodPressure = it },
                    label = { Text("Blood Pressure (mmHg)") },
                    placeholder = { Text("e.g. 140/90 or 120/80") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = haemoglobin,
                    onValueChange = { haemoglobin = it },
                    label = { Text("Haemoglobin (g/dL)") },
                    placeholder = { Text("e.g. 10.5") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Danger Signs Selection Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "3. Danger Signs",
                    style = MaterialTheme.typography.titleMedium.copy(color = TealPrimary, fontWeight = FontWeight.Bold)
                )

                DangerSignCheckbox(
                    label = "Bleeding",
                    checked = bleeding,
                    onCheckedChange = { bleeding = it }
                )

                DangerSignCheckbox(
                    label = "Fever",
                    checked = fever,
                    onCheckedChange = { fever = it }
                )

                DangerSignCheckbox(
                    label = "Headache",
                    checked = headache,
                    onCheckedChange = { headache = it }
                )

                DangerSignCheckbox(
                    label = "Reduced Fetal Movement",
                    checked = reducedFetalMovement,
                    onCheckedChange = { reducedFetalMovement = it }
                )
            }
        }

        // Submit Button
        Button(
            onClick = {
                val dangerSigns = DangerSigns(
                    bleeding = bleeding,
                    fever = fever,
                    headache = headache,
                    reducedFetalMovement = reducedFetalMovement
                )
                val calculatedRisk = PatientCase.calculateRisk(dangerSigns, bloodPressure)
                submittedResult = calculatedRisk

                val newCase = PatientCase(
                    id = "SC-${(100..999).random()}",
                    patientName = if (patientName.isBlank()) "Anonymous Patient" else patientName,
                    village = if (village.isBlank()) "Unspecified Village" else village,
                    bloodPressure = if (bloodPressure.isBlank()) "120/80" else bloodPressure,
                    haemoglobin = if (haemoglobin.isBlank()) "11.0 g/dL" else "$haemoglobin g/dL",
                    dangerSigns = dangerSigns,
                    riskLevel = calculatedRisk,
                    syncStatus = "Pending"
                )
                onAssessmentSubmitted(newCase)
            },
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Submit Assessment",
                style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
            )
        }

        // Assessment Result Card
        submittedResult?.let { risk ->
            val (containerColor, contentColor, icon, titleText, adviceText) = when (risk) {
                RiskLevel.RED -> Tuple5(
                    RedDangerContainer,
                    RedDanger,
                    Icons.Default.Warning,
                    "RED - High Emergency Risk",
                    "Immediate referral required! High Blood Pressure (>=140/90) or Vaginal Bleeding observed."
                )
                RiskLevel.AMBER -> Tuple5(
                    AmberWarningContainer,
                    AmberWarning,
                    Icons.Default.Warning,
                    "AMBER - Moderate Risk",
                    "High priority observation. Fever or severe headache reported. Monitor closely."
                )
                RiskLevel.GREEN -> Tuple5(
                    GreenSuccessContainer,
                    GreenSuccess,
                    Icons.Default.CheckCircle,
                    "GREEN - Normal Assessment",
                    "No immediate clinical danger signs. Continue standard antenatal checkup schedule."
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = containerColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Triage Result",
                        tint = contentColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = contentColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = adviceText,
                            style = MaterialTheme.typography.bodyLarge.copy(color = TextDark, fontSize = 14.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DangerSignCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(color = TextDark)
        )
    }
}

private data class Tuple5<A, B, C, D, E>(
    val val1: A,
    val val2: B,
    val val3: C,
    val val4: D,
    val val5: E
)
