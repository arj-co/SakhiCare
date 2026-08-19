package com.sakhicare.app.ui

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakhicare.app.i18n.AppLanguage
import com.sakhicare.app.i18n.Strings
import com.sakhicare.app.ui.theme.*
import java.util.Locale

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val protocolTag: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SakhiAiCopilotScreen(
    currentLanguage: AppLanguage
) {
    val context = LocalContext.current
    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Initialize TTS locale
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                sender = "ai",
                text = if (currentLanguage == AppLanguage.HINDI)
                    "नमस्ते आशा दीदी! मैं आपकी सखीकेयर क्लिनिकल सहायक हूँ। गर्भावस्था की जटिलताओं, बीपी, खून की कमी या आपातकालीन प्राथमिक उपचार के बारे में कोई भी सवाल पूछें।"
                else
                    "Namaste ASHA Didi! I am your SakhiCare Clinical Copilot. Ask me any question regarding high-risk maternal symptoms, pre-eclampsia, hemorrhage, or emergency first-aid protocols.",
                protocolTag = "MoHFW & WHO EmOC 2024"
            )
        )
    }

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val quickQueries = listOf(
        "बीपी 160/110 और तेज सिरदर्द",
        "अत्यधिक रक्तस्राव (Hemorrhage)",
        "हीमोग्लोबिन 6.8 g/dL (Severe Anemia)",
        "शिशु की हलचल में कमी (Fetal Distress)",
        "तेज बुखार और कंपकंपी (Sepsis)"
    )

    fun sendUserMessage(query: String) {
        if (query.isBlank()) return
        messages.add(ChatMessage(sender = "user", text = query))
        inputText = ""

        // Offline Clinical AI Inference matching MoHFW / WHO protocol rules
        val clean = query.lowercase(Locale.ROOT)
        val replyText: String
        val tag: String

        if (clean.contains("160") || clean.contains("बीपी") || clean.contains("bp") || clean.contains("सिरदर्द") || clean.contains("headache") || clean.contains("चक्कर")) {
            tag = "Severe Pre-eclampsia Protocol"
            replyText = if (currentLanguage == AppLanguage.HINDI)
                "🚨 **अति-गंभीर उच्च रक्तचाप (Pre-eclampsia)**:\n1) महिला को तुरंत बाईं करवट लिटाएं ताकि बच्चे को ऑक्सीजन मिले।\n2) सांस की नली खुली रखें।\n3) बीपी 160/110 से अधिक होने पर तुरंत 108 एम्बुलेंस बुलाएं।\n4) अस्पताल पहुंचने तक रोशनी और शोर कम रखें।"
            else
                "🚨 **Severe Pre-eclampsia Alert (BP ≥ 160/110)**:\n1) Place mother in left lateral tilt position.\n2) Keep airway clear.\n3) Call 108 emergency transport immediately.\n4) Prepare for Magnesium Sulfate loading dose under Medical Officer guidance."
        } else if (clean.contains("रक्त") || clean.contains("खून") || clean.contains("bleeding") || clean.contains("hemorrhage")) {
            tag = "Obstetric Hemorrhage Protocol"
            replyText = if (currentLanguage == AppLanguage.HINDI)
                "🩸 **आपातकालीन रक्तस्राव (PPH/APH)**:\n1) महिला के पैर ऊंचे रखें (Shock position)।\n2) आईवी ड्रिप (रिंगर लैक्टेट) तुरंत शुरू करें।\n3) प्रसव बाद होने पर गर्भाशय की हल्की मालिश करें।\n4) नजदीकी सीएचसी के ब्लड बैंक को अलर्ट करें और 108 एम्बुलेंस बुलाएं।"
            else
                "🩸 **Emergency Hemorrhage Protocol (APH/PPH)**:\n1) Position patient flat with legs elevated (anti-shock position).\n2) Insert large-bore IV cannula with warm crystalloid infusion.\n3) Uterine massage if postpartum.\n4) Shift to CHC blood storage center immediately."
        } else if (clean.contains("हीमोग्लोबिन") || clean.contains("hb") || clean.contains("anemia") || clean.contains("6.")) {
            tag = "Severe Anemia Protocol"
            replyText = if (currentLanguage == AppLanguage.HINDI)
                "⚠️ **गंभीर एनीमिया (Hb < 7.0 g/dL)**:\n1) यह हृदय पर अत्यधिक दबाव डालता है, महिला को कोई भारी काम न करने दें।\n2) रक्त चढ़ाने (ब्लड ट्रांसफ्यूजन) हेतु तुरंत जिला अस्पताल रेफर करें।"
            else
                "⚠️ **Severe Anemia Protocol (Hb < 7.0 g/dL)**:\n1) Immediate hospital referral for packed red blood cell (PRBC) transfusion.\n2) Maintain complete bed rest to avoid cardiorespiratory decompensation."
        } else if (clean.contains("शिशु") || clean.contains("हलचल") || clean.contains("fetal") || clean.contains("movement")) {
            tag = "Fetal Distress Protocol"
            replyText = if (currentLanguage == AppLanguage.HINDI)
                "👶 **शिशु की हलचल में कमी (Fetal Distress)**:\n1) महिला को 2 गिलास मीठा पानी या दूध पिलाएं और बाईं करवट लिटाएं।\n2) 2 घंटे में कम से कम 4 हलचल होनी चाहिए। यदि हलचल न हो, तो तुरंत सोनोग्राफी हेतु अस्पताल भेजें।"
            else
                "👶 **Fetal Distress Protocol**:\n1) Give maternal oral fluids and rest on left lateral side.\n2) If < 4 movements in 2 hours, arrange urgent ultrasound and Non-Stress Test (NST)."
        } else {
            tag = "General ANC Clinical Support"
            replyText = if (currentLanguage == AppLanguage.HINDI)
                "🌸 **सखीकेयर सहायक**: नियमित एएनसी जांच में बीपी, हीमोग्लोबिन और वजन अवश्य मापें। किसी भी आपात स्थिति में नया मूल्यांकन फॉर्म भरकर 'सबमिट' करें।"
            else
                "🌸 **SakhiCare Copilot**: Always measure BP, Hb, and fetal signs at every ANC visit. Use 'New Assessment' for automated multi-metric triage."
        }

        messages.add(ChatMessage(sender = "ai", text = replyText, protocolTag = tag))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoft)
    ) {
        // ── Top App Bar ──
        Surface(color = SurfaceWhite, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AccentIndigo,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "SakhiAI Clinical Copilot",
                        style = MaterialTheme.typography.titleLarge.copy(color = Neutral900, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "UN SDG 3 • MoHFW & WHO Maternal Intelligence",
                        style = MaterialTheme.typography.labelSmall.copy(color = PrimaryDark, fontWeight = FontWeight.SemiBold)
                    )
                }
                Surface(
                    color = TriageGreenBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "OFFLINE AI",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(color = TriageGreenDark, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // ── Quick Topic Chips ──
        Surface(color = Neutral50, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickQueries.take(2).forEach { chipText ->
                    Surface(
                        color = SurfaceWhite,
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { sendUserMessage(chipText) }
                    ) {
                        Text(
                            text = chipText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall.copy(color = Neutral800, fontWeight = FontWeight.Medium),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // ── Chat Stream ──
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.sender == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!isUser) {
                        Surface(
                            shape = CircleShape,
                            color = AccentIndigo,
                            modifier = Modifier.size(32.dp).padding(top = 4.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Column(
                        modifier = Modifier.widthIn(max = 300.dp),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        Surface(
                            color = if (isUser) Primary else SurfaceWhite,
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 16.dp
                            ),
                            shadowElevation = if (isUser) 0.dp else 1.dp
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                if (msg.protocolTag != null) {
                                    Surface(
                                        color = AccentIndigoBg,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Text(
                                            msg.protocolTag,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(color = AccentIndigo, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        )
                                    }
                                }
                                Text(
                                    msg.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isUser) Color.White else Neutral900,
                                        lineHeight = 20.sp
                                    )
                                )
                            }
                        }

                        if (!isUser) {
                            IconButton(
                                onClick = {
                                    tts?.speak(msg.text.replace("*", ""), TextToSpeech.QUEUE_FLUSH, null, "copilot_tts")
                                },
                                modifier = Modifier.size(28.dp).padding(top = 2.dp)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Read Aloud", tint = Neutral400, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // ── Input Bar ──
        Surface(color = SurfaceWhite, shadowElevation = 8.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask clinical question...", color = Neutral400, fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Neutral200,
                        focusedContainerColor = Neutral50,
                        unfocusedContainerColor = Neutral50
                    ),
                    maxLines = 3
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            sendUserMessage(inputText)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) Primary else Neutral200)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) Color.White else Neutral400,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
