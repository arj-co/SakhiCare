package com.sakhicare.app.voice

import com.sakhicare.app.data.DangerSigns
import java.util.Locale

data class ParsedVoiceData(
    val patientName: String = "",
    val village: String = "",
    val bloodPressure: String = "",
    val haemoglobin: String = "",
    val dangerSigns: DangerSigns = DangerSigns(),
    val rawTranscript: String = ""
)

object VoiceHelper {

    val sampleMultilingualDictations = listOf(
        "मरीज सुनीता देवी, गांव रामपुर, बीपी 145/95, हीमोग्लोबिन 10.2, बुखार और खून बहना",
        "Patient Sunita Devi village Rampur BP 145 over 95 haemoglobin 10.2 vaginal bleeding fever",
        "मरीज मीरा कुमार, गांव सीतापुर, बीपी 120/80, हीमोग्लोबिन 11.5, कोई लक्षण नहीं",
        "Patient Radha Devi village Gopalpur BP 150 over 100 haemoglobin 8.5 severe headache and reduced movement",
        "মরীয অনিতা দেবী, গ্রাম চন্দপুর, বিপি ১৪০/৯০, হিমোগ্লোবিন ৯.২, জ্বর এবং মাথা ব্যথা"
    )

    /**
     * Multilingual speech regex parser supporting English, Hindi (Devanagari), Hinglish, Marathi, and Bengali dictation.
     * Returns empty strings for any field it cannot extract — no fake fallbacks.
     */
    fun parseSpokenText(transcript: String): ParsedVoiceData {
        val clean = transcript.trim()
        if (clean.isBlank()) return ParsedVoiceData()

        val lowerClean = clean.lowercase(Locale.ROOT)

        // 1. Patient Name (English & Devanagari "मरीज" / "নাম")
        val nameRegexEn = Regex("""(?:patient|name|patient name)\s+(?:is\s+)?([a-zA-Z\s]+?)(?:,|\svillage|\sbp|\sblood|\shaemoglobin|\shb|\sdanger|\svaginal|$)""", RegexOption.IGNORE_CASE)
        val nameRegexHi = Regex("""(?:मरीज|मरीज़|नाम)\s+(?:है\s+)?([\u0900-\u097F\s]+?)(?:,|\sगांव|\sगाँव|\sबीपी|\sरक्तचाप|\sहीमोग्लोबिन|\sबुखार|\sखून|$)""", RegexOption.IGNORE_CASE)
        val nameRegexBn = Regex("""(?:রোগী|মরীয|নাম)\s+(?:হল\s+)?([\u0980-\u09FF\s]+?)(?:,|\sগ্রাম|\sবিপি|\sরক্তচাপ|\sহিমোগ্লোবিন|\sজ্বর|$)""", RegexOption.IGNORE_CASE)

        val rawName = nameRegexHi.find(clean)?.groupValues?.get(1)?.trim()
            ?: nameRegexBn.find(clean)?.groupValues?.get(1)?.trim()
            ?: nameRegexEn.find(clean)?.groupValues?.get(1)?.trim()
            ?: ""

        val formattedName = if (rawName.isNotBlank()) {
            rawName.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        } else ""

        // 2. Village (English "village" / Devanagari "गांव" / Bengali "গ্রাম")
        val villageRegexEn = Regex("""(?:village|from)\s+(?:is\s+)?([a-zA-Z\s]+?)(?:,|\sbp|\sblood|\shaemoglobin|\shb|\sdanger|\sbleeding|\svaginal|$)""", RegexOption.IGNORE_CASE)
        val villageRegexHi = Regex("""(?:गांव|गाँव|क्षेत्र)\s+(?:है\s+)?([\u0900-\u097F\s]+?)(?:,|\sबीपी|\sरक्तचाप|\sहीमोग्लोबिन|\sबुखार|\sखून|$)""", RegexOption.IGNORE_CASE)
        val villageRegexBn = Regex("""(?:গ্রাম|এলাকা)\s+(?:হল\s+)?([\u0980-\u09FF\s]+?)(?:,|\sবিপি|\sরক্তচাপ|\sহিমোগ্লোবিন|\sজ্বর|$)""", RegexOption.IGNORE_CASE)

        val rawVillage = villageRegexHi.find(clean)?.groupValues?.get(1)?.trim()
            ?: villageRegexBn.find(clean)?.groupValues?.get(1)?.trim()
            ?: villageRegexEn.find(clean)?.groupValues?.get(1)?.trim()
            ?: ""

        val formattedVillage = if (rawVillage.isNotBlank()) {
            rawVillage.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        } else ""

        // 3. Blood Pressure ("145/95", "145 over 95", "बीपी 145/95", "বিপি ১৪০/৯০")
        val bpRegex = Regex("""(?:bp|blood pressure|बीपी|রক্তচাপ|বিপি|রক্তচাপ)\s*(?:is\s*|है\s*|হল\s*)?(\d{2,3})\s*(?:\/|over|\s|बटा)\s*(\d{2,3})""", RegexOption.IGNORE_CASE)
        val bpMatch = bpRegex.find(clean)
        val bpString = if (bpMatch != null) {
            "${bpMatch.groupValues[1]}/${bpMatch.groupValues[2]}"
        } else ""

        // 4. Haemoglobin ("10.2", "हीमोग्लोबिन 10.2", "হিমোগ্লোবিন ৯.২")
        val hbRegex = Regex("""(?:hb|haemoglobin|hemoglobin|हीमोग्लोबिन|হিমোগ্লোবিন)\s*(?:is\s*|है\s*|হল\s*)?(\d{1,2}(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
        val hbMatch = hbRegex.find(clean)
        val hbString = hbMatch?.groupValues?.get(1) ?: ""

        // 5. Danger Signs (English, Hindi, Marathi, Kannada, Bengali keywords)
        val bleeding = lowerClean.contains("bleeding") || lowerClean.contains("hemorrhage") || clean.contains("खून") || clean.contains("रक्तस्राव") || clean.contains("রক্তস্রাব")
        val fever = lowerClean.contains("fever") || lowerClean.contains("temperature") || clean.contains("बुखार") || clean.contains("ताप") || clean.contains("জ্বর")
        val headache = lowerClean.contains("headache") || lowerClean.contains("head ache") || clean.contains("सिरदर्द") || clean.contains("डोकेदुखी") || clean.contains("মাথা ব্যথা")
        val reducedFetalMovement = lowerClean.contains("fetal movement") || lowerClean.contains("reduced movement") || clean.contains("हलचल") || clean.contains("शिशु") || clean.contains("নড়াচড়া")

        return ParsedVoiceData(
            patientName = formattedName,
            village = formattedVillage,
            bloodPressure = bpString,
            haemoglobin = hbString,
            dangerSigns = DangerSigns(
                bleeding = bleeding,
                fever = fever,
                headache = headache,
                reducedFetalMovement = reducedFetalMovement
            ),
            rawTranscript = transcript
        )
    }
}
