package com.sakhicare.app.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakhicare.app.data.db.AppDatabase
import com.sakhicare.app.data.db.entities.WorkerEntity
import com.sakhicare.app.data.preferences.OnboardingPreferences
import com.sakhicare.app.i18n.AppLanguage
import com.sakhicare.app.i18n.Strings
import com.sakhicare.app.ui.theme.*
import com.sakhicare.app.sync.SakhiCareApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onCompleteOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { OnboardingPreferences(context) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var currentStep by remember { mutableIntStateOf(1) }

    // Step 2 Inputs: Worker, facility assignment, and issued Supabase account
    var ashaName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var facilityName by remember { mutableStateOf("") }
    var facilityCode by remember { mutableStateOf("") }
    var authEmail by remember { mutableStateOf("") }
    var authPassword by remember { mutableStateOf("") }
    var authLoading by remember { mutableStateOf(false) }

    // Step 3 Inputs: Security PIN
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    // Step 4 Consent
    var consentAgreed by remember { mutableStateOf(false) }

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
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Progress Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SakhiCare",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryLight
                ) {
                    Text(
                        text = "Step $currentStep of 4",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = PrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // Step Content
            when (currentStep) {
                1 -> {
                    // Step 1: Language Selection
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryLight,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Translate, contentDescription = null, tint = Primary, modifier = Modifier.size(36.dp))
                            }
                        }

                        Text(
                            text = "अपनी भाषा चुनें\nChoose Your Language",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Neutral900,
                                textAlign = TextAlign.Center
                            )
                        )

                        Text(
                            text = "Select the language you will use for maternal screenings.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Neutral500,
                                textAlign = TextAlign.Center
                            )
                        )

                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), border = CardDefaults.outlinedCardBorder()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("What SakhiCare helps you do", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Neutral900))
                                Text("• Record pregnancy details safely, even without internet\n• Capture a voice note in your local language\n• Get on-device danger-sign guidance\n• Send the complete case, voice note, and Gemini review when online", style = MaterialTheme.typography.bodyMedium.copy(color = Neutral700))
                                Text("Your data stays on this protected phone until a connection is available.", style = MaterialTheme.typography.bodySmall.copy(color = PrimaryDark, fontWeight = FontWeight.SemiBold))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val languages = listOf(
                            Triple(AppLanguage.HINDI, "हिंदी", "Hindi"),
                            Triple(AppLanguage.ENGLISH, "English", "English"),
                            Triple(AppLanguage.MARATHI, "मराठी", "Marathi"),
                            Triple(AppLanguage.KANNADA, "ಕನ್ನಡ", "Kannada"),
                            Triple(AppLanguage.BENGALI, "বাংলা", "Bengali")
                        )

                        languages.forEach { (lang, nativeName, englishName) ->
                            val isSelected = currentLanguage == lang
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLanguageSelected(lang) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) PrimaryLight else SurfaceWhite
                                ),
                                border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Primary)) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(nativeName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Neutral900))
                                        Text(englishName, style = MaterialTheme.typography.bodySmall.copy(color = Neutral500))
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Primary)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { currentStep = 2 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text("Continue", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                        }
                    }
                }

                2 -> {
                    // Step 2: Worker Identity & Facility Assignment
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "स्वास्थ्य कार्यकर्ता प्रोफ़ाइल\nWorker & Facility Setup",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Neutral900
                            )
                        )
                        Text(
                            text = "Enter the worker and facility details issued by your programme supervisor. These are used to label offline cases before sync.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Neutral500)
                        )

                        OutlinedTextField(
                            value = ashaName,
                            onValueChange = { ashaName = it },
                            label = { Text("ASHA Worker Name (आशा कार्यकर्ता का नाम)") },
                            placeholder = { Text("e.g. Shanti Devi") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) phone = it },
                            label = { Text("Mobile Phone (10-digit)") },
                            placeholder = { Text("e.g. 9876543210") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = facilityName,
                            onValueChange = { facilityName = it },
                            label = { Text("Facility / Sub-Centre (स्वास्थ्य उप-केंद्र)") },
                            placeholder = { Text("e.g. Rampur Sub-Centre") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = facilityCode,
                            onValueChange = { facilityCode = it },
                            label = { Text("Assigned Facility Code") },
                            placeholder = { Text("e.g. FAC-RAM-01") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = authEmail,
                            onValueChange = { authEmail = it },
                            label = { Text("Care Desk email (Optional for offline)") },
                            placeholder = { Text("Issued by programme administrator") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = authPassword,
                            onValueChange = { authPassword = it },
                            label = { Text("Care Desk password (Optional for offline)") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { currentStep = 1 },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = {
                                    if (ashaName.isBlank() || phone.length < 10 || facilityName.isBlank() || facilityCode.isBlank()) {
                                        Toast.makeText(context, "Enter your assigned worker name, 10-digit phone, and facility details", Toast.LENGTH_SHORT).show()
                                    } else {
                                        currentStep = 3
                                    }
                                },
                                modifier = Modifier.weight(1.5f).height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Text("Continue", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }

                3 -> {
                    // Step 3: Local Unlock PIN
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "सुरक्षा पिन सेट करें\nSet Local Security PIN",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Neutral900
                            )
                        )
                        Text(
                            text = "Create a 4-digit PIN to protect sensitive maternal patient data on this device.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Neutral500)
                        )

                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                            label = { Text("4-Digit Security PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
                            label = { Text("Confirm 4-Digit Security PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { currentStep = 2 },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = {
                                    if (pin.length != 4 || pin != confirmPin) {
                                        Toast.makeText(context, "PIN must be 4 digits and match confirm PIN", Toast.LENGTH_SHORT).show()
                                    } else {
                                        currentStep = 4
                                    }
                                },
                                modifier = Modifier.weight(1.5f).height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Text("Continue", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }

                4 -> {
                    // Step 4: Consent, Offline Explanation & Protocol Version
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ऑफलाइन उपयोग एवं सहमति\nOffline Protocol & Consent",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Neutral900
                            )
                        )

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Primary)
                                    Text("Protocol Pack: mohfw-hrp-v1.0", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                }
                                Text(
                                    "Adheres to Indian MoHFW, WHO, and ACOG High-Risk Pregnancy guidelines. Deterministic on-device clinical triage.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Neutral500)
                                )
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PrimaryLight)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = Primary)
                                    Text("100% Offline Capable", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = PrimaryDark))
                                }
                                Text(
                                    "You can perform complete maternal assessments in remote areas without internet. Data is encrypted using hardware-backed keys on this phone and sent to the Care Desk when network resumes.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Neutral900)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { consentAgreed = !consentAgreed },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Checkbox(
                                checked = consentAgreed,
                                onCheckedChange = { consentAgreed = it },
                                colors = CheckboxDefaults.colors(checkedColor = Primary)
                            )
                            Text(
                                "I understand and agree to manage maternal health records securely in compliance with clinical protocols.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Neutral900)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { currentStep = 3 },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = {
                                    if (!consentAgreed) {
                                        Toast.makeText(context, "Please accept the consent agreement", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    // Complete onboarding & persist worker in Room
                                    authLoading = true
                                    coroutineScope.launch {
                                        var workerId = "WKR-${phone.trim()}"
                                        var workerStatus = "VERIFICATION_PENDING"

                                        if (authEmail.isNotBlank() && authPassword.isNotBlank()) {
                                            try {
                                                val authResponse = SakhiCareApiClient.signInWithSupabase(authEmail.trim(), authPassword)
                                                if (authResponse.isSuccessful && !authResponse.body()?.accessToken.isNullOrBlank()) {
                                                    prefs.authAccessToken = authResponse.body()?.accessToken ?: ""
                                                    workerId = authResponse.body()?.user?.id ?: workerId
                                                    workerStatus = "ACTIVE"
                                                } else {
                                                    Toast.makeText(context, "Care Desk account unverified. Working in offline mode.", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                // Offline or network error: proceed with local offline onboarding
                                                Toast.makeText(context, "Network unavailable. Working offline; verification pending.", Toast.LENGTH_SHORT).show()
                                            }
                                        }

                                        try {
                                            prefs.completeOnboarding(
                                                name = ashaName.trim(),
                                                phone = phone.trim(),
                                                facility = facilityName.trim(),
                                                facilityCd = facilityCode.trim(),
                                                pin = pin.trim(),
                                                language = currentLanguage.name
                                            )
                                            val db = AppDatabase.getInstance(context.applicationContext)
                                            db.workerDao().insertWorker(
                                                WorkerEntity(
                                                    id = workerId,
                                                    name = ashaName.trim(),
                                                    role = "ASHA",
                                                    phone = phone.trim(),
                                                    facilityId = facilityCode.trim(),
                                                    facilityName = facilityName.trim(),
                                                    locale = when (currentLanguage) {
                                                        AppLanguage.HINDI -> "hi-IN"
                                                        AppLanguage.MARATHI -> "mr-IN"
                                                        AppLanguage.KANNADA -> "kn-IN"
                                                        AppLanguage.BENGALI -> "bn-IN"
                                                        else -> "en-IN"
                                                    },
                                                    status = workerStatus
                                                )
                                            )
                                            onCompleteOnboarding()
                                        } catch (error: Exception) {
                                            Toast.makeText(context, error.message ?: "Unable to complete setup", Toast.LENGTH_LONG).show()
                                        } finally {
                                            authLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1.5f).height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Text(if (authLoading) "Setting up…" else "Complete Setup", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
    }
}
