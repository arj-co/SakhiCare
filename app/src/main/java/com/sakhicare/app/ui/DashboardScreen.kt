package com.sakhicare.app.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakhicare.app.R
import com.sakhicare.app.data.PatientCase
import com.sakhicare.app.data.PatientRepository
import com.sakhicare.app.data.RiskLevel
import com.sakhicare.app.i18n.AppLanguage
import com.sakhicare.app.i18n.Strings
import com.sakhicare.app.ui.theme.*
import java.util.concurrent.TimeUnit

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
    onToggleNetworkMode: () -> Unit,
    onSyncNowClick: () -> Unit,
    onNewAssessmentClick: () -> Unit,
    onMyCasesClick: () -> Unit,
    onSakhiAiClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var languageDropdownExpanded by remember { mutableStateOf(false) }
    val totalCases = redCount + amberCount + greenCount
    val lastAssessmentTime = PatientRepository.getLastAssessmentTime()

    // Get the most critical emergency case if any exists
    val activeRedCase = PatientRepository.cases.find { it.riskLevel == RiskLevel.RED }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoft)
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Top Header: Logo + App Branding + Language & Network ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Official Logo + ASHA Welcome Greeting
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
                        text = if (currentLanguage == AppLanguage.HINDI) "नमस्ते आशा दीदी 🌸" else "Namaste ASHA Didi 🌸",
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
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right: Language Selector + Network Toggle
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language dropdown pill
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
                                text = {
                                    Text(
                                        "${lang.nativeName}  •  ${lang.displayName}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    onLanguageSelected(lang)
                                    languageDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Network status pill
                Surface(
                    color = if (isOnline) TriageGreenBg else Neutral100,
                    shape = RoundedCornerShape(18.dp),
                    border = if (isOnline) androidx.compose.foundation.BorderStroke(1.dp, TriageGreen.copy(alpha = 0.3f)) else null,
                    modifier = Modifier.clickable { onToggleNetworkMode() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (isOnline) TriageGreen else Neutral400,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            if (isOnline) "Online" else "Offline",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isOnline) TriageGreen else Neutral500,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // ── Sub-Centre Coverage Tag ──
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
                        "रामपुर उप-स्वास्थ्य केंद्र (Rampur Sub-Centre)",
                        style = MaterialTheme.typography.labelMedium.copy(color = Neutral800, fontWeight = FontWeight.SemiBold)
                    )
                }
                Text(
                    "5 गांव • 24 माताएं",
                    style = MaterialTheme.typography.labelSmall.copy(color = Neutral500)
                )
            }
        }

        // ── Active Emergency Spotlight Card (If RED Case Exists) ──
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
                        "${activeRedCase.patientName} • ${activeRedCase.village} (BP: ${activeRedCase.bloodPressure} | Hb: ${activeRedCase.haemoglobin})",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Neutral900, fontWeight = FontWeight.Bold)
                    )

                    if (!activeRedCase.ambulanceStatus.isNullOrBlank()) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = TriageRed, modifier = Modifier.size(18.dp))
                                Text(
                                    activeRedCase.ambulanceStatus,
                                    style = MaterialTheme.typography.bodySmall.copy(color = TriageRedDark, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Hero Banner Card with Warm Gradient ──
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Primary.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .background(Brush.linearGradient(listOf(Color(0xFFE84364), Color(0xFFF76B8A), Color(0xFFF9A826))))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Strings.get("welcome_title", currentLanguage),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 19.sp
                                )
                            )
                            Text(
                                text = Strings.get("app_subtitle", currentLanguage),
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.95f))
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🩺", fontSize = 22.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatChip(
                            value = "$totalCases",
                            label = Strings.get("total", currentLanguage),
                            bg = Color.White.copy(alpha = 0.22f),
                            fg = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            value = "$redCount",
                            label = "आपातकाल (RED)",
                            bg = Color.Black.copy(alpha = 0.2f),
                            fg = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            value = "$pendingSyncCount",
                            label = Strings.get("pending", currentLanguage),
                            bg = Color.White.copy(alpha = 0.22f),
                            fg = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ── Daily Clinical Care Tip ──
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(color = Primary, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(32.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "🌸 आज का क्लिनिकल संदेश (Daily Maternal Tip)",
                        style = MaterialTheme.typography.labelLarge.copy(color = PrimaryDark, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "तीसरी तिमाही में हर सप्ताह गर्भवती का रक्तचाप और हीमोग्लोबिन अवश्य जांचें। सिस्टोलिक बीपी 140 से ऊपर होने पर तुरंत केयर डेस्क से संपर्क करें।",
                        style = MaterialTheme.typography.bodySmall.copy(color = Neutral800, lineHeight = 18.sp)
                    )
                }
            }
        }

        // ── Triage Summary Cards ──
        Text(
            text = Strings.get("triage_summary", currentLanguage),
            style = MaterialTheme.typography.titleMedium.copy(
                color = Neutral900,
                fontWeight = FontWeight.Bold
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TriageCard(
                modifier = Modifier.weight(1f),
                count = redCount,
                label = Strings.get("red_risk", currentLanguage),
                color = TriageRed,
                bgColor = TriageRedBg
            )
            TriageCard(
                modifier = Modifier.weight(1f),
                count = amberCount,
                label = Strings.get("amber_risk", currentLanguage),
                color = TriageAmber,
                bgColor = TriageAmberBg
            )
            TriageCard(
                modifier = Modifier.weight(1f),
                count = greenCount,
                label = Strings.get("green_risk", currentLanguage),
                color = TriageGreen,
                bgColor = TriageGreenBg
            )
        }

        // ── Quick Actions Grid (4 Tiles) ──
        Text(
            text = Strings.get("quick_actions", currentLanguage),
            style = MaterialTheme.typography.titleMedium.copy(
                color = Neutral900,
                fontWeight = FontWeight.Bold
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Mic,
                badge = "SPEECH-AI",
                label = Strings.get("new_assessment", currentLanguage),
                bgColor = PrimaryLight,
                iconColor = Primary,
                onClick = onNewAssessmentClick
            )
            ActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AutoAwesome,
                badge = "SDG 3",
                label = "SakhiAI Copilot",
                bgColor = AccentIndigoBg,
                iconColor = AccentIndigo,
                onClick = onSakhiAiClick
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.List,
                badge = "${PatientRepository.getTotalCount()} Cases",
                label = Strings.get("my_cases", currentLanguage),
                bgColor = Neutral100,
                iconColor = Neutral800,
                onClick = onMyCasesClick
            )
            ActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Sync,
                badge = if (pendingSyncCount > 0) "$pendingSyncCount Pending" else "All Synced",
                label = Strings.get("sync_now", currentLanguage),
                bgColor = TriageGreenBg,
                iconColor = TriageGreen,
                onClick = onSyncNowClick
            )
        }

        // ── Emergency Helpline Speed-Dial Bar ──
        Text(
            text = "आपातकालीन हेल्पलाइन (Emergency Speed-Dial)",
            style = MaterialTheme.typography.titleMedium.copy(
                color = Neutral900,
                fontWeight = FontWeight.Bold
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HelplineButton(
                modifier = Modifier.weight(1f),
                number = "108",
                label = "108 Ambulance",
                color = TriageRed,
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:108"))
                    context.startActivity(intent)
                }
            )
            HelplineButton(
                modifier = Modifier.weight(1f),
                number = "104",
                label = "104 Health Help",
                color = AccentIndigo,
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:104"))
                    context.startActivity(intent)
                }
            )
            HelplineButton(
                modifier = Modifier.weight(1f),
                number = "CHC",
                label = "PHC/CHC Doctor",
                color = TriageGreen,
                onClick = {
                    Toast.makeText(context, "Dialing CHC Medical Officer...", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun HelplineButton(
    modifier: Modifier,
    number: String,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = SurfaceWhite,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                number,
                style = MaterialTheme.typography.titleLarge.copy(color = color, fontWeight = FontWeight.ExtraBold)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(color = Neutral600),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatChip(value: String, label: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Surface(color = bg, shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge.copy(color = fg, fontWeight = FontWeight.ExtraBold))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(color = fg.copy(alpha = 0.9f)), maxLines = 1)
        }
    }
}

@Composable
private fun TriageCard(modifier: Modifier, count: Int, label: String, color: Color, bgColor: Color) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "$count",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = color,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(label, style = MaterialTheme.typography.labelMedium.copy(color = color), maxLines = 1)
        }
    }
}

@Composable
private fun ActionTile(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badge: String,
    label: String,
    bgColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Neutral900,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                color = bgColor,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    badge,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(color = iconColor, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                )
            }
        }
    }
}
