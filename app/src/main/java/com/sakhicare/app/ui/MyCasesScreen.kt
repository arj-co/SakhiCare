package com.sakhicare.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakhicare.app.data.PatientCase
import com.sakhicare.app.data.RiskLevel
import com.sakhicare.app.ui.theme.*

@Composable
fun MyCasesScreen(cases: List<PatientCase>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TealBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "My Patient Cases",
            style = MaterialTheme.typography.titleLarge.copy(color = TealDark)
        )

        if (cases.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No recorded patient cases found.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cases) { patientCase ->
                    CaseCard(patientCase = patientCase)
                }
            }
        }
    }
}

@Composable
fun CaseCard(patientCase: PatientCase) {
    val (badgeBg, badgeFg, label) = when (patientCase.riskLevel) {
        RiskLevel.RED -> Triple(RedDangerContainer, RedDanger, "RED RISK")
        RiskLevel.AMBER -> Triple(AmberWarningContainer, AmberWarning, "AMBER RISK")
        RiskLevel.GREEN -> Triple(GreenSuccessContainer, GreenSuccess, "GREEN NORMAL")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = TealLight,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = patientCase.patientName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextDark)
                        )
                        Text(
                            text = "Village: ${patientCase.village}",
                            style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary)
                        )
                    }
                }

                // Risk Badge
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = badgeFg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            HorizontalDivider(color = TealBackground)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "BP: ${patientCase.bloodPressure}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium)
                )
                Text(
                    text = "Hb: ${patientCase.haemoglobin}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium)
                )
                Text(
                    text = "Sync: ${patientCase.syncStatus}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, color = TextSecondary)
                )
            }
        }
    }
}
