package com.sakhicare.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sakhicare.app.data.PatientCase
import com.sakhicare.app.data.PatientRepository
import com.sakhicare.app.data.RiskLevel
import com.sakhicare.app.i18n.AppLanguage
import com.sakhicare.app.i18n.Strings
import com.sakhicare.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCasesScreen(
    cases: List<PatientCase>,
    currentLanguage: AppLanguage,
    onBack: () -> Unit,
    onCaseClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<RiskLevel?>(null) }

    val filteredCases = remember(cases, searchQuery, selectedFilter) {
        PatientRepository.filterCases(searchQuery, selectedFilter)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoft)
    ) {
        // ── Header ──
        Surface(color = SurfaceWhite, shadowElevation = 2.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Neutral900)
                    }
                    Text(
                        Strings.get("my_cases", currentLanguage),
                        style = MaterialTheme.typography.titleLarge.copy(color = Neutral900),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${filteredCases.size} ${if (filteredCases.size != cases.size) "of ${cases.size}" else ""} ${Strings.get("cases_recorded", currentLanguage)}",
                        style = MaterialTheme.typography.labelMedium.copy(color = Neutral400)
                    )
                }

                // ── Search Bar ──
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(Strings.get("search_placeholder", currentLanguage), style = MaterialTheme.typography.bodyMedium.copy(color = Neutral400)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Neutral400, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Neutral200,
                        focusedBorderColor = Primary,
                        unfocusedContainerColor = Neutral50,
                        focusedContainerColor = SurfaceWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(50.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // ── Risk Filter Chips ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipItem(
                        label = Strings.get("all_cases", currentLanguage),
                        selected = selectedFilter == null,
                        color = Primary,
                        bgColor = PrimaryLight,
                        onClick = { selectedFilter = null }
                    )
                    FilterChipItem(
                        label = "🔴 ${Strings.get("red_emergency", currentLanguage)}",
                        selected = selectedFilter == RiskLevel.RED,
                        color = TriageRed,
                        bgColor = TriageRedBg,
                        onClick = { selectedFilter = if (selectedFilter == RiskLevel.RED) null else RiskLevel.RED }
                    )
                    FilterChipItem(
                        label = "🟡 ${Strings.get("amber_warning", currentLanguage)}",
                        selected = selectedFilter == RiskLevel.AMBER,
                        color = TriageAmber,
                        bgColor = TriageAmberBg,
                        onClick = { selectedFilter = if (selectedFilter == RiskLevel.AMBER) null else RiskLevel.AMBER }
                    )
                    FilterChipItem(
                        label = "🟢 ${Strings.get("green_normal", currentLanguage)}",
                        selected = selectedFilter == RiskLevel.GREEN,
                        color = TriageGreen,
                        bgColor = TriageGreenBg,
                        onClick = { selectedFilter = if (selectedFilter == RiskLevel.GREEN) null else RiskLevel.GREEN }
                    )
                }
            }
        }

        if (filteredCases.isEmpty()) {
            // ── Empty State ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📋", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        if (searchQuery.isNotBlank() || selectedFilter != null) Strings.get("no_results", currentLanguage)
                        else Strings.get("no_cases", currentLanguage),
                        style = MaterialTheme.typography.titleMedium.copy(color = Neutral500)
                    )
                    Text(
                        if (searchQuery.isNotBlank() || selectedFilter != null) Strings.get("no_results_desc", currentLanguage)
                        else Strings.get("no_cases_desc", currentLanguage),
                        style = MaterialTheme.typography.bodyMedium.copy(color = Neutral400)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredCases, key = { it.id }) { case ->
                    CaseCard(case, currentLanguage, onClick = { onCaseClick(case.id) })
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun FilterChipItem(label: String, selected: Boolean, color: Color, bgColor: Color, onClick: () -> Unit) {
    Surface(
        color = if (selected) bgColor else Neutral50,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (selected) color else Neutral500,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

@Composable
private fun CaseCard(case: PatientCase, currentLanguage: AppLanguage, onClick: () -> Unit) {
    val (riskColor, riskBg, riskLabel) = when (case.riskLevel) {
        RiskLevel.RED -> Triple(TriageRed, TriageRedBg, "RED")
        RiskLevel.AMBER -> Triple(TriageAmber, TriageAmberBg, "AMBER")
        RiskLevel.GREEN -> Triple(TriageGreen, TriageGreenBg, "GREEN")
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Row 1: Name + Risk Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        case.patientName,
                        style = MaterialTheme.typography.titleMedium.copy(color = Neutral900, fontWeight = FontWeight.SemiBold)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Neutral400, modifier = Modifier.size(14.dp))
                        Text(case.village, style = MaterialTheme.typography.labelMedium.copy(color = Neutral500))
                        Text("•", style = MaterialTheme.typography.labelMedium.copy(color = Neutral400))
                        Text(case.relativeTime, style = MaterialTheme.typography.labelMedium.copy(color = Neutral400))
                    }
                }

                Surface(color = riskBg, shape = RoundedCornerShape(10.dp)) {
                    Text(
                        riskLabel,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge.copy(color = riskColor, fontWeight = FontWeight.Bold)
                    )
                }
            }

            HorizontalDivider(color = Neutral100, thickness = 1.dp)

            // Row 2: Vitals + Sync
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                VitalChip(label = "BP", value = case.bloodPressure)
                VitalChip(label = "Hb", value = case.haemoglobin)
                SyncChip(status = case.syncStatus)
            }

            // Row 3: Danger signs
            val activeSigns = mutableListOf<String>()
            if (case.dangerSigns.bleeding) activeSigns.add(Strings.get("bleeding", currentLanguage))
            if (case.dangerSigns.fever) activeSigns.add(Strings.get("fever", currentLanguage))
            if (case.dangerSigns.headache) activeSigns.add(Strings.get("headache", currentLanguage))
            if (case.dangerSigns.reducedFetalMovement) activeSigns.add(Strings.get("reduced_fetal_movement", currentLanguage))

            if (activeSigns.isNotEmpty()) {
                Surface(
                    color = TriageRedBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = TriageRed, modifier = Modifier.size(16.dp))
                        Text(
                            activeSigns.joinToString(" • "),
                            style = MaterialTheme.typography.labelMedium.copy(color = TriageRedDark)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VitalChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium.copy(color = Neutral400))
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(color = Neutral900, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun SyncChip(status: String) {
    val (icon, color) = if (status == "Synced")
        Pair(Icons.Outlined.CloudDone, TriageGreen)
    else
        Pair(Icons.Outlined.CloudOff, TriageAmber)

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(status, style = MaterialTheme.typography.labelMedium.copy(color = color))
    }
}
