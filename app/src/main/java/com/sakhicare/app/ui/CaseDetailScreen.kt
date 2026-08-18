package com.sakhicare.app.ui

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakhicare.app.data.PatientCase
import com.sakhicare.app.data.RiskLevel
import com.sakhicare.app.fhir.FhirBundleConverter
import com.sakhicare.app.i18n.AppLanguage
import com.sakhicare.app.i18n.Strings
import com.sakhicare.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseDetailScreen(
    patientCase: PatientCase,
    currentLanguage: AppLanguage,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showFhirJson by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val (riskColor, riskBg, riskLabel, riskAdvice) = when (patientCase.riskLevel) {
        RiskLevel.RED -> Tuple4(TriageRed, TriageRedBg, "RED — Emergency Referral", Strings.get("red_advice", currentLanguage))
        RiskLevel.AMBER -> Tuple4(TriageAmber, TriageAmberBg, "AMBER — Urgent Monitoring", Strings.get("amber_advice", currentLanguage))
        RiskLevel.GREEN -> Tuple4(TriageGreen, TriageGreenBg, "GREEN — Normal", Strings.get("green_advice", currentLanguage))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoft)
    ) {
        // ── Header ──
        Surface(color = SurfaceWhite, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Neutral900)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        patientCase.patientName,
                        style = MaterialTheme.typography.titleLarge.copy(color = Neutral900),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Neutral400, modifier = Modifier.size(14.dp))
                        Text(patientCase.village, style = MaterialTheme.typography.labelMedium.copy(color = Neutral500))
                        Text("•", style = MaterialTheme.typography.labelMedium.copy(color = Neutral400))
                        Text(patientCase.relativeTime, style = MaterialTheme.typography.labelMedium.copy(color = Neutral400))
                    }
                }
                Surface(color = riskBg, shape = RoundedCornerShape(10.dp)) {
                    Text(
                        patientCase.riskLevel.name,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge.copy(color = riskColor, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // ── Scrollable content ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Risk Assessment Card ──
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = riskBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        if (patientCase.riskLevel == RiskLevel.GREEN) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = riskColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(riskLabel, style = MaterialTheme.typography.titleMedium.copy(color = riskColor, fontWeight = FontWeight.Bold))
                        Text(riskAdvice, style = MaterialTheme.typography.bodyMedium.copy(color = Neutral700))
                    }
                }
            }

            // ── Care Desk Advisory & Dispatch (If Received) ──
            if (!patientCase.doctorAdvisory.isNullOrBlank()) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(color = Primary, shape = RoundedCornerShape(8.dp), modifier = Modifier.size(28.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                            Text(
                                "SakhiCare Care Desk Advisory",
                                style = MaterialTheme.typography.titleMedium.copy(color = PrimaryDark, fontWeight = FontWeight.Bold)
                            )
                        }
                        Surface(
                            color = SurfaceWhite,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "\"${patientCase.doctorAdvisory}\"",
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(color = Neutral900, fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
            }

            if (!patientCase.ambulanceStatus.isNullOrBlank()) {
                Surface(
                    color = TriageRedBg,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = TriageRed, modifier = Modifier.size(24.dp))
                        Column {
                            Text("108 Emergency Transport Active", style = MaterialTheme.typography.labelMedium.copy(color = TriageRedDark, fontWeight = FontWeight.Bold))
                            Text(patientCase.ambulanceStatus, style = MaterialTheme.typography.bodySmall.copy(color = Neutral800))
                        }
                    }
                }
            }

            // ── Vitals Section ──
            DetailSection(title = Strings.get("vital_measurements", currentLanguage)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VitalCard(
                        modifier = Modifier.weight(1f),
                        label = Strings.get("bp", currentLanguage),
                        value = patientCase.bloodPressure,
                        unit = "mmHg",
                        isAbnormal = PatientCase.calculateRisk(patientCase.dangerSigns, patientCase.bloodPressure) == RiskLevel.RED
                    )
                    VitalCard(
                        modifier = Modifier.weight(1f),
                        label = Strings.get("haemoglobin", currentLanguage),
                        value = patientCase.haemoglobin,
                        unit = "",
                        isAbnormal = false
                    )
                }
            }

            // ── Danger Signs Section ──
            DetailSection(title = Strings.get("danger_signs", currentLanguage)) {
                DangerSignRow(label = Strings.get("bleeding", currentLanguage), active = patientCase.dangerSigns.bleeding)
                DangerSignRow(label = Strings.get("fever", currentLanguage), active = patientCase.dangerSigns.fever)
                DangerSignRow(label = Strings.get("headache", currentLanguage), active = patientCase.dangerSigns.headache)
                DangerSignRow(label = Strings.get("reduced_fetal_movement", currentLanguage), active = patientCase.dangerSigns.reducedFetalMovement)
            }

            // ── Sync & Meta ──
            DetailSection(title = Strings.get("case_info", currentLanguage)) {
                MetaRow(label = "Case ID", value = patientCase.id)
                MetaRow(label = Strings.get("assessment_date", currentLanguage), value = patientCase.formattedDate)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        if (patientCase.syncStatus == "Synced") Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
                        contentDescription = null,
                        tint = if (patientCase.syncStatus == "Synced") TriageGreen else TriageAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "${Strings.get("sync_status", currentLanguage)}: ${patientCase.syncStatus}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Neutral700)
                    )
                }
            }

            // ── FHIR R4 Export ──
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFhirJson = !showFhirJson },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DataObject, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(22.dp))
                            Column {
                                Text(Strings.get("fhir_export", currentLanguage), style = MaterialTheme.typography.titleMedium.copy(color = Neutral900))
                                Text("HL7 FHIR R4 Bundle", style = MaterialTheme.typography.labelMedium.copy(color = Neutral400))
                            }
                        }
                        Icon(
                            if (showFhirJson) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Neutral500
                        )
                    }

                    if (showFhirJson) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Neutral100)
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Neutral50,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = FhirBundleConverter.caseToFhirJson(patientCase),
                                modifier = Modifier
                                    .padding(12.dp)
                                    .horizontalScroll(rememberScrollState()),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Neutral700,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }
                }
            }

            // ── Action Buttons ──
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        val shareText = buildString {
                            appendLine("SakhiCare Case Report")
                            appendLine("═══════════════════")
                            appendLine("Patient: ${patientCase.patientName}")
                            appendLine("Village: ${patientCase.village}")
                            appendLine("Risk: ${patientCase.riskLevel.name}")
                            appendLine("BP: ${patientCase.bloodPressure}")
                            appendLine("Hb: ${patientCase.haemoglobin}")
                            if (patientCase.dangerSigns.hasAny()) {
                                appendLine("Danger Signs:")
                                if (patientCase.dangerSigns.bleeding) appendLine("  ⚠ Vaginal Bleeding")
                                if (patientCase.dangerSigns.fever) appendLine("  ⚠ High Fever")
                                if (patientCase.dangerSigns.headache) appendLine("  ⚠ Severe Headache")
                                if (patientCase.dangerSigns.reducedFetalMovement) appendLine("  ⚠ Reduced Fetal Movement")
                            }
                            appendLine("Date: ${patientCase.formattedDate}")
                            appendLine("ID: ${patientCase.id}")
                            appendLine("═══════════════════")
                            appendLine("Generated by SakhiCare")
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "SakhiCare: ${patientCase.patientName} — ${patientCase.riskLevel.name}")
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Case Report"))
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.get("share_case", currentLanguage))
                }

                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text(Strings.get("back_to_cases", currentLanguage), color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Reusable Detail Components ──

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(color = Primary))
            content()
        }
    }
}

@Composable
private fun VitalCard(modifier: Modifier, label: String, value: String, unit: String, isAbnormal: Boolean) {
    Surface(
        color = if (isAbnormal) TriageRedBg else Neutral50,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label.split("(").first().trim(), style = MaterialTheme.typography.labelMedium.copy(color = Neutral500), maxLines = 1)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = if (isAbnormal) TriageRed else Neutral900,
                    fontWeight = FontWeight.Bold
                )
            )
            if (unit.isNotBlank()) {
                Text(unit, style = MaterialTheme.typography.labelMedium.copy(color = Neutral400))
            }
        }
    }
}

@Composable
private fun DangerSignRow(label: String, active: Boolean) {
    Surface(
        color = if (active) TriageRedBg else Neutral50,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                if (active) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (active) TriageRed else TriageGreen,
                modifier = Modifier.size(20.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (active) TriageRedDark else Neutral700,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                ),
                modifier = Modifier.weight(1f)
            )
            Text(
                if (active) "⚠ Present" else "✓ Absent",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = if (active) TriageRed else TriageGreen
                )
            )
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium.copy(color = Neutral500))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(color = Neutral900, fontWeight = FontWeight.Medium))
    }
}

private data class Tuple4<A, B, C, D>(val val1: A, val val2: B, val val3: C, val val4: D)
