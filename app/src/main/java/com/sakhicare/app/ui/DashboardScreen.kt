package com.sakhicare.app.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakhicare.app.R
import com.sakhicare.app.data.DemoConfig
import com.sakhicare.app.data.PatientCase
import com.sakhicare.app.data.PatientRepository
import com.sakhicare.app.data.RiskLevel
import com.sakhicare.app.data.preferences.OnboardingPreferences
import com.sakhicare.app.i18n.AppLanguage
import com.sakhicare.app.i18n.Strings
import com.sakhicare.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    pendingSyncCount: Int,
    isOnline: Boolean,
    redCount: Int,
    amberCount: Int,
    greenCount: Int,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onSyncNowClick: () -> Unit,
    onNewAssessmentClick: () -> Unit,
    onMyCasesClick: () -> Unit,
    onToggleNetworkMode: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var languageDropdownExpanded by remember { mutableStateOf(false) }
    val totalCases = redCount + amberCount + greenCount
    val lastAssessmentTime = PatientRepository.getLastAssessmentTime()

    val prefs = remember { OnboardingPreferences(context) }
    val registeredFacility = prefs.facilityName.ifBlank { "Primary Health Catchment" }
    val workerDisplayName = prefs.workerName.ifBlank { "ASHA Didi" }

    // Most critical emergency case
    val activeRedCase = PatientRepository.cases.find { it.riskLevel == RiskLevel.RED }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoft)
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Top Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceWhite,
                    shadowElevation = 3.dp,
                    modifier = Modifier.size(46.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.sakhicare_logo),
                        contentDescription = "SakhiCare Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                    )
                }

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "नमस्ते $workerDisplayName 🌸",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = PrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = Strings.get("app_name", currentLanguage),
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Neutral900,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Language Selector Pill
            Box {
                Surface(
                    color = SurfaceWhite,
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 1.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Neutral200),
                    modifier = Modifier.clickable { languageDropdownExpanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = Primary, modifier = Modifier.size(15.dp))
                        Text(
                            currentLanguage.nativeName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Neutral800,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
                DropdownMenu(
                    expanded = languageDropdownExpanded,
                    onDismissRequest = { languageDropdownExpanded = false }
                ) {
                    AppLanguage.entries.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text("${lang.nativeName}  •  ${lang.displayName}") },
                            onClick = {
                                onLanguageSelected(lang)
                                languageDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // ── Demo Data Indicator (Visible ONLY when demo mode enabled) ──
        if (DemoConfig.isDemoEnabled) {
            Surface(
                color = Color(0xFFFFF3CD),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFEEBA)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFF856404), modifier = Modifier.size(18.dp))
                    Text(
                        "Demo Mode Active: Simulated records shown. Not clinical patient data.",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF856404), fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // ── Honest Connectivity & Outbox Strip ──
        Surface(
            color = SurfaceWhite,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Neutral200),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = if (isOnline) TriageGreenBg else Neutral100,
                        shape = CircleShape,
                        modifier = Modifier.size(12.dp)
                    ) {
                        Box(modifier = Modifier.background(if (isOnline) TriageGreen else Neutral400))
                    }
                    Column {
                        Text(
                            text = if (isOnline) "Connected to Care Desk" else "Working Offline (Phone Storage)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Neutral900)
                        )
                        Text(
                            text = if (pendingSyncCount > 0) "$pendingSyncCount pending in outbox" else "All local records up to date",
                            style = MaterialTheme.typography.labelSmall.copy(color = if (pendingSyncCount > 0) TriageAmber else Neutral500)
                        )
                    }
                }

                if (pendingSyncCount > 0 && isOnline) {
                    FilledTonalButton(
                        onClick = onSyncNowClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync Outbox", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // ── Facility Tag ──
        Surface(
            color = SurfaceWhite,
            shape = RoundedCornerShape(14.dp),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                    Text(
                        registeredFacility,
                        style = MaterialTheme.typography.labelMedium.copy(color = Neutral800, fontWeight = FontWeight.SemiBold)
                    )
                }
                Text(
                    "$totalCases Records",
                    style = MaterialTheme.typography.labelSmall.copy(color = Neutral500)
                )
            }
        }

        // ── Primary Action: START MATERNAL CHECK (Always first) ──
        Button(
            onClick = onNewAssessmentClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .shadow(6.dp, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                Text(
                    "नई गर्भावस्था जांच (Start Maternal Check)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // ── Active RED Emergency Case (If Exists) ──
        if (activeRedCase != null) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = TriageRedBg),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, TriageRed.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(color = TriageRed, shape = CircleShape, modifier = Modifier.size(24.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                "🚨 सक्रिय आपातकालीन मामला (Active Emergency)",
                                style = MaterialTheme.typography.labelLarge.copy(color = TriageRedDark, fontWeight = FontWeight.Bold)
                            )
                        }
                        Surface(color = TriageRed, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                "RED",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Text(
                        "${activeRedCase.patientName} • ${activeRedCase.village} (${activeRedCase.relativeTime})",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Neutral900, fontWeight = FontWeight.Bold)
                    )

                    Text(
                        activeRedCase.clinicalRationale ?: "Critical risk detected",
                        style = MaterialTheme.typography.bodySmall.copy(color = Neutral700)
                    )
                }
            }
        }

        // ── Empty State or Summary Stats ──
        if (totalCases == 0) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.MedicalServices, contentDescription = null, tint = Primary, modifier = Modifier.size(40.dp))
                    Text(
                        "कोई जांच दर्ज नहीं (No Assessments Yet)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Neutral900)
                    )
                    Text(
                        "Tap 'Start Maternal Check' above to conduct your first offline maternal danger-sign screening.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Neutral500, textAlign = TextAlign.Center)
                    )
                }
            }
        } else {
            // Case Stats Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "आपातकाल\n(RED)",
                    count = redCount,
                    bg = TriageRedBg,
                    fg = TriageRed,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "निगरानी\n(AMBER)",
                    count = amberCount,
                    bg = TriageAmberBg,
                    fg = TriageAmber,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "सामान्य\n(GREEN)",
                    count = greenCount,
                    bg = TriageGreenBg,
                    fg = TriageGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── View My Cases Button ──
        OutlinedButton(
            onClick = onMyCasesClick,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = Primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("सभी दर्ज मामले देखें (View All My Cases)", style = MaterialTheme.typography.labelLarge.copy(color = Primary, fontWeight = FontWeight.Bold))
        }

        // Protocol Version Badge Footer
        Text(
            text = "Protocol Pack: mohfw-hrp-v1.0 • Offline AES-256 Storage",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall.copy(color = Neutral400, textAlign = TextAlign.Center)
        )
    }
}

@Composable
fun StatCard(title: String, count: Int, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "$count",
                style = MaterialTheme.typography.titleLarge.copy(color = fg, fontWeight = FontWeight.ExtraBold)
            )
            Text(
                title,
                style = MaterialTheme.typography.labelSmall.copy(color = Neutral700, textAlign = TextAlign.Center)
            )
        }
    }
}
