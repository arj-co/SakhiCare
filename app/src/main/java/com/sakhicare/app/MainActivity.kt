package com.sakhicare.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sakhicare.app.data.PatientRepository
import com.sakhicare.app.data.preferences.OnboardingPreferences
import com.sakhicare.app.i18n.AppLanguage
import com.sakhicare.app.i18n.Strings
import com.sakhicare.app.sync.NetworkMonitor
import com.sakhicare.app.sync.SakhiCareApiClient
import com.sakhicare.app.ui.CaseDetailScreen
import com.sakhicare.app.ui.DashboardScreen
import com.sakhicare.app.ui.MyCasesScreen
import com.sakhicare.app.ui.NewAssessmentScreen
import com.sakhicare.app.ui.OnboardingScreen
import com.sakhicare.app.ui.theme.*

sealed class Screen {
    data object Onboarding : Screen()
    data object Dashboard : Screen()
    data object NewAssessment : Screen()
    data object MyCases : Screen()
    data class CaseDetail(val caseId: String) : Screen()
}

class MainActivity : ComponentActivity() {

    private lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize offline Room repository
        SakhiCareApiClient.initialize(this)
        PatientRepository.initialize(this)

        networkMonitor = NetworkMonitor(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 200)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                201
            )
        }

        setContent {
            SakhiCareTheme {
                SakhiCareApp(networkMonitor = networkMonitor)
            }
        }
    }
}

@Composable
fun SakhiCareApp(networkMonitor: NetworkMonitor? = null) {
    val context = LocalContext.current
    val prefs = remember { OnboardingPreferences(context) }

    val initialScreen = if (prefs.isOnboardingCompleted) Screen.Dashboard else Screen.Onboarding
    var currentScreen by remember { mutableStateOf<Screen>(initialScreen) }
    var isUnlocked by remember { mutableStateOf(!prefs.isOnboardingCompleted) }

    val initialLang = try {
        AppLanguage.valueOf(prefs.selectedLanguage)
    } catch (e: Exception) {
        AppLanguage.HINDI
    }
    var currentLanguage by remember { mutableStateOf(initialLang) }

    val autoOnlineState by (networkMonitor?.isConnected?.collectAsState() ?: remember { mutableStateOf(false) })
    val isOnline = autoOnlineState

    // Start network callback
    LaunchedEffect(networkMonitor) {
        networkMonitor?.startMonitoring()
    }

    if (prefs.isOnboardingCompleted && !isUnlocked) {
        LocalPinLockScreen(
            currentLanguage = currentLanguage,
            onUnlock = { isUnlocked = true }
        )
    } else {
        Scaffold(
            containerColor = BackgroundSoft,
            bottomBar = {
                val showBottomNav = currentScreen is Screen.Dashboard || currentScreen is Screen.NewAssessment || currentScreen is Screen.MyCases
                if (showBottomNav) {
                    ModernBottomNav(
                        currentScreen = currentScreen,
                        currentLanguage = currentLanguage,
                        onNavigate = { currentScreen = it }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (val screen = currentScreen) {
                Screen.Onboarding -> OnboardingScreen(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = { currentLanguage = it },
                    onCompleteOnboarding = {
                        isUnlocked = true
                        currentScreen = Screen.Dashboard
                    }
                )
                Screen.Dashboard -> DashboardScreen(
                    pendingSyncCount = PatientRepository.getPendingSyncCount(),
                    isOnline = isOnline,
                    redCount = PatientRepository.getRedRiskCount(),
                    amberCount = PatientRepository.getAmberRiskCount(),
                    greenCount = PatientRepository.getGreenRiskCount(),
                    currentLanguage = currentLanguage,
                    onLanguageSelected = { currentLanguage = it },
                    onSyncNowClick = { PatientRepository.syncAllPending(context) },
                    onNewAssessmentClick = { currentScreen = Screen.NewAssessment },
                    onMyCasesClick = { currentScreen = Screen.MyCases }
                )
                Screen.NewAssessment -> NewAssessmentScreen(
                    currentLanguage = currentLanguage,
                    onAssessmentSubmitted = { newCase, audioFile, transcript, duration ->
                        PatientRepository.addCase(
                            patientCase = newCase,
                            context = context,
                            audioFile = audioFile,
                            voiceTranscript = transcript,
                            audioDurationSeconds = duration
                        )
                    },
                    onNavigateBack = { currentScreen = Screen.Dashboard }
                )
                Screen.MyCases -> MyCasesScreen(
                    cases = PatientRepository.cases,
                    currentLanguage = currentLanguage,
                    onBack = { currentScreen = Screen.Dashboard },
                    onCaseClick = { caseId -> currentScreen = Screen.CaseDetail(caseId) }
                )
                is Screen.CaseDetail -> {
                    val patientCase = PatientRepository.getCaseById(screen.caseId)
                    if (patientCase != null) {
                        CaseDetailScreen(
                            patientCase = patientCase,
                            currentLanguage = currentLanguage,
                            onBack = { currentScreen = Screen.MyCases }
                        )
                    } else {
                        currentScreen = Screen.MyCases
                    }
                }
            }
        }
    }
}

}


@Composable
private fun LocalPinLockScreen(
    currentLanguage: AppLanguage,
    onUnlock: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { OnboardingPreferences(context) }
    var pin by remember { mutableStateOf("") }
    var invalidAttempt by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoft)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "SakhiCare",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = if (currentLanguage == AppLanguage.HINDI) "स्थानीय PIN दर्ज करें" else "Enter your local PIN",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Neutral900,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = "This keeps saved maternal-health records protected on this phone.",
                style = MaterialTheme.typography.bodyMedium.copy(color = Neutral500),
            )
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 6 && it.all(Char::isDigit)) {
                        pin = it
                        invalidAttempt = false
                    }
                },
                label = { Text("PIN") },
                singleLine = true,
                isError = invalidAttempt,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            if (invalidAttempt) {
                Text(
                    text = "Incorrect PIN. Try again.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                onClick = {
                    if (pin.isNotBlank() && pin == prefs.localPin) {
                        onUnlock()
                    } else {
                        invalidAttempt = true
                    }
                },
                enabled = pin.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Unlock", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ModernBottomNav(
    currentScreen: Screen,
    currentLanguage: AppLanguage,
    onNavigate: (Screen) -> Unit
) {
    Surface(
        color = SurfaceWhite,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                selected = currentScreen is Screen.Dashboard,
                icon = Icons.Default.Home,
                label = Strings.get("home", currentLanguage),
                onClick = { onNavigate(Screen.Dashboard) }
            )
            NavItem(
                selected = currentScreen is Screen.NewAssessment,
                icon = Icons.Default.Add,
                label = Strings.get("new_assessment", currentLanguage),
                onClick = { onNavigate(Screen.NewAssessment) }
            )
            NavItem(
                selected = currentScreen is Screen.MyCases || currentScreen is Screen.CaseDetail,
                icon = Icons.Outlined.FolderOpen,
                label = Strings.get("cases", currentLanguage),
                onClick = { onNavigate(Screen.MyCases) }
            )
        }
    }
}

@Composable
private fun NavItem(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val color = if (selected) Primary else Neutral400
    val bgColor = if (selected) PrimaryLight else Color.Transparent

    Surface(
        onClick = onClick,
        color = bgColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            if (selected) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}
