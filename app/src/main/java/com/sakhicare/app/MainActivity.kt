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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sakhicare.app.data.PatientRepository
import com.sakhicare.app.i18n.AppLanguage
import com.sakhicare.app.i18n.Strings
import com.sakhicare.app.sync.NetworkMonitor
import com.sakhicare.app.ui.CaseDetailScreen
import com.sakhicare.app.ui.DashboardScreen
import com.sakhicare.app.ui.MyCasesScreen
import com.sakhicare.app.ui.NewAssessmentScreen
import com.sakhicare.app.ui.theme.*

sealed class Screen {
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

        networkMonitor = NetworkMonitor(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 200)
            }
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
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var currentLanguage by remember { mutableStateOf(AppLanguage.HINDI) }

    val autoOnlineState by (networkMonitor?.isConnected?.collectAsState() ?: remember { mutableStateOf(false) })
    var manualOnlineOverride by remember { mutableStateOf<Boolean?>(null) }
    val isOnline = manualOnlineOverride ?: autoOnlineState

    // Start network callback
    LaunchedEffect(networkMonitor) {
        networkMonitor?.startMonitoring()
    }

    Scaffold(
        containerColor = BackgroundSoft,
        bottomBar = {
            val showBottomNav = currentScreen is Screen.Dashboard || currentScreen is Screen.NewAssessment
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
                Screen.Dashboard -> DashboardScreen(
                    pendingSyncCount = PatientRepository.getPendingSyncCount(),
                    isOnline = isOnline,
                    redCount = PatientRepository.getRedRiskCount(),
                    amberCount = PatientRepository.getAmberRiskCount(),
                    greenCount = PatientRepository.getGreenRiskCount(),
                    currentLanguage = currentLanguage,
                    onLanguageSelected = { currentLanguage = it },
                    onToggleNetworkMode = { manualOnlineOverride = !isOnline },
                    onSyncNowClick = { PatientRepository.syncAllPending() },
                    onNewAssessmentClick = { currentScreen = Screen.NewAssessment },
                    onMyCasesClick = { currentScreen = Screen.MyCases }
                )
                Screen.NewAssessment -> NewAssessmentScreen(
                    currentLanguage = currentLanguage,
                    onAssessmentSubmitted = { newCase ->
                        PatientRepository.addCase(newCase)
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
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
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
