package com.sakhicare.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakhicare.app.i18n.AppLanguage
import com.sakhicare.app.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val badge: String,
    val gradientColors: List<Color>
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onCompleteOnboarding: (ashaName: String, village: String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    var ashaNameInput by remember { mutableStateOf("आशा शांति देवी (Shanti Devi)") }
    var villageInput by remember { mutableStateOf("रामपुर उप-स्वास्थ्य केंद्र (Rampur SC)") }
    var showProfileSetup by remember { mutableStateOf(false) }

    val pages = listOf(
        OnboardingPage(
            title = "सखीकेयर में आपका स्वागत है",
            subtitle = "Welcome to SakhiCare",
            description = "हर गर्भवती मां और नवजात शिशु की सुरक्षा, अब आपकी उंगलियों पर। शत-प्रतिशत ऑफलाइन काम करने वाला डिजिटल स्वास्थ्य साथी।",
            icon = Icons.Default.Favorite,
            badge = "MoHFW & WHO HRP Standard",
            gradientColors = listOf(Color(0xFFE91E63), Color(0xFFFF5252))
        ),
        OnboardingPage(
            title = "स्मार्ट आवाज़ से जांच",
            subtitle = "Speech-LLM Voice Assistant",
            description = "बिना टाइप किए अपनी मातृभाषा में बोलकर मरीज का बीपी, हीमोग्लोबिन और खतरे के लक्षण दर्ज करें। 100% ऑफलाइन एआई।",
            icon = Icons.Default.Mic,
            badge = "Offline Speech-LLM Active",
            gradientColors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
        ),
        OnboardingPage(
            title = "केयर डेस्क और आपातकालीन सहायता",
            subtitle = "Care Desk & 108 Emergency",
            description = "गंभीर स्थिति में डॉक्टरों की तत्काल क्लिनिकल सलाह प्राप्त करें और 1-क्लिक में 108 एम्बुलेंस बुलाएं।",
            icon = Icons.Default.SupportAgent,
            badge = "24/7 Live Tele-Guidance",
            gradientColors = listOf(Color(0xFF0D9488), Color(0xFF14B8A6))
        )
    )

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Header & Language Selector ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = Primary, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(36.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🌸", fontSize = 18.sp)
                        }
                    }
                    Text(
                        "SakhiCare",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Neutral900,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                }

                // Language quick dropdown chip
                Surface(
                    color = SurfaceWhite,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.clickable {
                        val next = when (currentLanguage) {
                            AppLanguage.HINDI -> AppLanguage.ENGLISH
                            AppLanguage.ENGLISH -> AppLanguage.MARATHI
                            AppLanguage.MARATHI -> AppLanguage.BENGALI
                            AppLanguage.BENGALI -> AppLanguage.KANNADA
                            AppLanguage.KANNADA -> AppLanguage.HINDI
                        }
                        onLanguageSelected(next)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                        Text(
                            currentLanguage.displayName,
                            style = MaterialTheme.typography.labelMedium.copy(color = Primary, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Carousel Pager ──
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIdx ->
                val page = pages[pageIdx]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Big Animated Illustration Card
                    Card(
                        shape = RoundedCornerShape(32.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier
                            .size(200.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(page.gradientColors)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    page.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(72.dp)
                                )
                                Surface(
                                    color = Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        page.badge,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        page.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = Neutral900,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        page.subtitle,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Primary,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        page.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Neutral600,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // ── Dots Indicator ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                repeat(3) { idx ->
                    val isSelected = pagerState.currentPage == idx
                    val width by animateDpAsState(targetValue = if (isSelected) 28.dp else 8.dp, label = "dot_width")
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(if (isSelected) Primary else Neutral300)
                    )
                }
            }

            // ── Profile Input Drawer / Step ──
            AnimatedVisibility(visible = showProfileSetup) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "आशा कार्यकर्ता प्रोफाइल (ASHA Profile)",
                            style = MaterialTheme.typography.titleMedium.copy(color = Neutral900, fontWeight = FontWeight.Bold)
                        )
                        OutlinedTextField(
                            value = ashaNameInput,
                            onValueChange = { ashaNameInput = it },
                            label = { Text("आशा कार्यकर्ता का नाम") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = villageInput,
                            onValueChange = { villageInput = it },
                            label = { Text("उप-स्वास्थ्य केंद्र / गांव") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // ── Bottom Action Button ──
            Button(
                onClick = {
                    if (pagerState.currentPage < 2) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else if (!showProfileSetup) {
                        showProfileSetup = true
                    } else {
                        onCompleteOnboarding(ashaNameInput, villageInput)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (pagerState.currentPage < 2) "आगे बढ़ें (Next)" else if (!showProfileSetup) "प्रोफाइल सेट करें (Setup Profile)" else "सखीकेयर शुरू करें (Get Started)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
