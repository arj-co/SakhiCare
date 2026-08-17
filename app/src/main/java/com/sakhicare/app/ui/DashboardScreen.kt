package com.sakhicare.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakhicare.app.R
import com.sakhicare.app.data.PatientCase
import com.sakhicare.app.data.PatientRepository
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
    onMyCasesClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    var languageDropdownExpanded by remember { mutableStateOf(false) }
    val totalCases = redCount + amberCount + greenCount
    val lastAssessmentTime = PatientRepository.getLastAssessmentTime()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoft)
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ── Top Row: Logo + App Branding + Language & Network ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Official Logo + Title
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

                Column(
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = "Welcome back 👋",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Neutral500,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = Strings.get("app_name", currentLanguage),
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Neutral900,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right: Language Selector + Network Toggle Pills
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
                            Icon(
                                Icons.Default.Language,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(15.dp)
                            )
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

        // ── Internet Connectivity Sync Alert Banner ──
        AnimatedVisibility(
            visible = isOnline && pendingSyncCount > 0,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = TriageGreenBg,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TriageGreen.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = TriageGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                "Internet Connected",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = TriageGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                "$pendingSyncCount case(s) ready to sync to server",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Neutral700
                                )
                            )
                        }
                    }

                    Button(
                        onClick = onSyncNowClick,
                        colors = ButtonDefaults.buttonColors(containerColor = TriageGreen),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Sync Now", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // ── Hero Banner Card ──
        if (totalCases == 0) {
            // Empty state hero
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Primary.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .background(Brush.linearGradient(listOf(Color(0xFFE8647C), Color(0xFFF472B6))))
                        .padding(24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🩺", fontSize = 22.sp)
                                }
                            }
                            Text(
                                Strings.get("welcome_title", currentLanguage),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Text(
                            Strings.get("welcome_desc", currentLanguage),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.95f),
                                lineHeight = 20.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Button(
                            onClick = onNewAssessmentClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                Strings.get("start_first_assessment", currentLanguage),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = Primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        } else {
            // Active state hero
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = Primary.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .background(Brush.linearGradient(listOf(Color(0xFFE8647C), Color(0xFFF472B6))))
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = Strings.get("app_subtitle", currentLanguage),
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.9f))
                        )
                        if (lastAssessmentTime != null) {
                            val ago = System.currentTimeMillis() - lastAssessmentTime
                            val agoText = when {
                                TimeUnit.MILLISECONDS.toMinutes(ago) < 1 -> "Just now"
                                TimeUnit.MILLISECONDS.toMinutes(ago) < 60 -> "${TimeUnit.MILLISECONDS.toMinutes(ago)} min ago"
                                TimeUnit.MILLISECONDS.toHours(ago) < 24 -> "${TimeUnit.MILLISECONDS.toHours(ago)}h ago"
                                else -> "${TimeUnit.MILLISECONDS.toDays(ago)} days ago"
                            }
                            Text(
                                "${Strings.get("last_assessment", currentLanguage)}: $agoText",
                                style = MaterialTheme.typography.labelMedium.copy(color = Color.White.copy(alpha = 0.75f))
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            StatChip(
                                value = "$totalCases",
                                label = Strings.get("total", currentLanguage),
                                bg = Color.White.copy(alpha = 0.22f),
                                fg = Color.White
                            )
                            StatChip(
                                value = "$pendingSyncCount",
                                label = Strings.get("pending", currentLanguage),
                                bg = Color.White.copy(alpha = 0.22f),
                                fg = Color.White
                            )
                        }
                    }
                }
            }
        }

        // ── Triage Summary (only if cases exist) ──
        if (totalCases > 0) {
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

            // ── Sync Status Bar ──
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            if (pendingSyncCount == 0) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
                            contentDescription = null,
                            tint = if (pendingSyncCount == 0) TriageGreen else TriageAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                if (pendingSyncCount == 0) Strings.get("all_synced", currentLanguage)
                                else "${Strings.get("pending_sync", currentLanguage)}: $pendingSyncCount",
                                style = MaterialTheme.typography.titleMedium.copy(color = Neutral900)
                            )
                            Text(
                                Strings.get("sync_status", currentLanguage),
                                style = MaterialTheme.typography.labelMedium.copy(color = Neutral400)
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onSyncNowClick,
                        enabled = pendingSyncCount > 0 && isOnline,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Strings.get("sync_now", currentLanguage), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // ── Quick Actions ──
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
                icon = Icons.Default.Add,
                label = Strings.get("new_assessment", currentLanguage),
                bgColor = PrimaryLight,
                iconColor = Primary,
                onClick = onNewAssessmentClick
            )
            ActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.List,
                label = Strings.get("my_cases", currentLanguage),
                bgColor = AccentIndigoBg,
                iconColor = AccentIndigo,
                onClick = onMyCasesClick
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun StatChip(value: String, label: String, bg: Color, fg: Color) {
    Surface(color = bg, shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge.copy(color = fg, fontWeight = FontWeight.Bold))
            Text(label, style = MaterialTheme.typography.labelMedium.copy(color = fg.copy(alpha = 0.85f)))
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
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(26.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Neutral900,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
