package com.sakhicare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sakhicare.app.data.PatientCase
import com.sakhicare.app.ui.DashboardScreen
import com.sakhicare.app.ui.MyCasesScreen
import com.sakhicare.app.ui.NewAssessmentScreen
import com.sakhicare.app.ui.theme.SakhiCareTheme
import com.sakhicare.app.ui.theme.TealDark
import com.sakhicare.app.ui.theme.TealPrimary

sealed class Screen(val route: String, val title: String) {
    data object Dashboard : Screen("dashboard", "SakhiCare Dashboard")
    data object NewAssessment : Screen("new_assessment", "New Assessment")
    data object MyCases : Screen("my_cases", "My Patient Cases")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SakhiCareTheme {
                SakhiCareApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SakhiCareApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var pendingSyncCount by remember { mutableIntStateOf(2) }
    var isSynced by remember { mutableStateOf(false) }

    val casesList = remember {
        mutableStateListOf<PatientCase>().apply {
            addAll(PatientCase.sampleCases)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = currentScreen.title, color = Color.White) },
                navigationIcon = {
                    if (currentScreen != Screen.Dashboard) {
                        IconButton(onClick = { currentScreen = Screen.Dashboard }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Dashboard",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TealDark)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = TealDark) {
                NavigationBarItem(
                    selected = currentScreen == Screen.Dashboard,
                    onClick = { currentScreen = Screen.Dashboard },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard", tint = Color.White) },
                    label = { Text("Home", color = Color.White) }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.NewAssessment,
                    onClick = { currentScreen = Screen.NewAssessment },
                    icon = { Icon(Icons.Default.Add, contentDescription = "New Assessment", tint = Color.White) },
                    label = { Text("New", color = Color.White) }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.MyCases,
                    onClick = { currentScreen = Screen.MyCases },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "My Cases", tint = Color.White) },
                    label = { Text("Cases", color = Color.White) }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                Screen.Dashboard -> DashboardScreen(
                    pendingSyncCount = pendingSyncCount,
                    isSynced = isSynced,
                    onSyncNowClick = {
                        isSynced = true
                        pendingSyncCount = 0
                    },
                    onNewAssessmentClick = { currentScreen = Screen.NewAssessment },
                    onMyCasesClick = { currentScreen = Screen.MyCases }
                )
                Screen.NewAssessment -> NewAssessmentScreen(
                    onAssessmentSubmitted = { newCase ->
                        casesList.add(0, newCase)
                        if (!isSynced) {
                            pendingSyncCount += 1
                        }
                    }
                )
                Screen.MyCases -> MyCasesScreen(cases = casesList)
            }
        }
    }
}
