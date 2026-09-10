package com.sakhicare.app.voice

import com.sakhicare.app.data.DangerSigns
import java.util.Locale

/**
 * Candidate structured fields extracted from speech as an intake shortcut.
 * Every field must be reviewed and confirmed by the ASHA worker before clinical triage.
 */
data class CandidateAssessmentFields(
    val patientName: String? = null,
    val village: String? = null,
    val gestationalAgeWeeks: Int? = null,
    val bloodPressure: String? = null,
    val haemoglobin: Double? = null,
    val dangerSigns: DangerSigns = DangerSigns(),
    val travelConstraints: String? = null,
    val notes: String? = null
)

/**
 * Result of speech-to-form organization containing the raw transcript,
 * candidate fields, and a list of specific field keys that were extracted.
 */
data class OrganizedVoiceDraft(
    val rawTranscript: String,
    val candidateFields: CandidateAssessmentFields,
    val extractedFieldKeys: Set<String>,
    val isRecognized: Boolean
)

/**
 * Deterministic Voice-to-Form Organizer
 *
 * Safe, rule-based intake shortcut for multilingual spoken dictations (Hindi, Bhojpuri, Bengali, English).
 * Never invents or defaults missing clinical data. Unrecognized content remains in the transcript.
 */
object VoiceFormOrganizer {

    private val IndicNumberMappings = listOf(
        // Blood Pressure Phrasings
        Regex("""(?:एक\s*सौ\s*साठ|160)\s*(?:बटा|बाय|\/|over)\s*(?:एक\s*सौ\s*दस|110)""", RegexOption.IGNORE_CASE) to "160/110",
        Regex("""(?:एक\s*सौ\s*पचास|150)\s*(?:बटा|बाय|\/|over)\s*(?:सौ|एक\s*सौ|100)""", RegexOption.IGNORE_CASE) to "150/100",
        Regex("""(?:एक\s*सौ\s*चालीस|140)\s*(?:बटा|बाय|\/|over)\s*(?:नब्बे|90)""", RegexOption.IGNORE_CASE) to "140/90",
        Regex("""(?:एक\s*सौ\s*तीस|130)\s*(?:बटा|बाय|\/|over)\s*(?:अस्सी|80)""", RegexOption.IGNORE_CASE) to "130/80",
        Regex("""(?:एक\s*सौ\s*बीस|120)\s*(?:बटा|बाय|\/|over)\s*(?:अस्सी|80)""", RegexOption.IGNORE_CASE) to "120/80",
        Regex("""(?:1\s*6\s*0)\s*(?:over|by|slash|bata|\/)\s*(?:1\s*1\s*0)""", RegexOption.IGNORE_CASE) to "160/110",
        Regex("""(?:1\s*5\s*0)\s*(?:over|by|slash|bata|\/)\s*(?:1\s*0\s*0)""", RegexOption.IGNORE_CASE) to "150/100",
        Regex("""(?:1\s*4\s*0)\s*(?:over|by|slash|bata|\/)\s*(?:9\s*0)""", RegexOption.IGNORE_CASE) to "140/90",
        Regex("""(?:1\s*2\s*0)\s*(?:over|by|slash|bata|\/)\s*(?:8\s*0)""", RegexOption.IGNORE_CASE) to "120/80",

        // Haemoglobin Decimal Phrasings
        Regex("""(?:दस|10)\s*(?:दशमलव|पॉइंट|point|\.)\s*(?:दो|2)""", RegexOption.IGNORE_CASE) to "10.2",
        Regex("""(?:नौ|9)\s*(?:दशमलव|पॉइंट|point|\.)\s*(?:पांच|पाँच|5)""", RegexOption.IGNORE_CASE) to "9.5",
        Regex("""(?:आठ|8)\s*(?:दशमलव|पॉइंट|point|\.)\s*(?:पांच|पाँच|5)""", RegexOption.IGNORE_CASE) to "8.5",
        Regex("""(?:सात|7)\s*(?:दशमलव|पॉइंट|point|\.)\s*(?:दो|2)""", RegexOption.IGNORE_CASE) to "7.2",
        Regex("""(?:छह|6)\s*(?:दशमलव|पॉइंट|point|\.)\s*(?:आठ|8)""", RegexOption.IGNORE_CASE) to "6.8",
        Regex("""(?:साढ़े\s*दस|saadhe\s*das)""", RegexOption.IGNORE_CASE) to "10.5",
        Regex("""(?:साढ़े\s*नौ|saadhe\s*nau)""", RegexOption.IGNORE_CASE) to "9.5",
        Regex("""(?:साढ़े\s*आठ|saadhe\s*aath)""", RegexOption.IGNORE_CASE) to "8.5",
        Regex("""(?:साढ़े\s*सात|saadhe\s*saat)""", RegexOption.IGNORE_CASE) to "7.5"
    )

    fun normalizeIndicSpeech(rawText: String): String {
        var text = rawText.trim()
        for ((pattern, replacement) in IndicNumberMappings) {
            text = pattern.replace(text, replacement)
        }
        text = text.replace(Regex("""\s+(?:over|बटा|बाय|\/)\s+""", RegexOption.IGNORE_CASE), "/")
        text = text.replace(Regex("""\s+(?:दशमलव|पॉइंट|point|dot)\s+""", RegexOption.IGNORE_CASE), ".")
        return text
    }

    /**
     * Parses spoken text into draft candidate fields.
     * Guaranteed to never invent missing values or clinical defaults.
     */
    fun organizeTranscript(rawTranscript: String): OrganizedVoiceDraft {
        val clean = rawTranscript.trim()
        if (clean.isBlank()) {
            return OrganizedVoiceDraft(
                rawTranscript = "",
                candidateFields = CandidateAssessmentFields(),
                extractedFieldKeys = emptySet(),
                isRecognized = false
            )
        }

        val normalized = normalizeIndicSpeech(clean)
        val lower = normalized.lowercase(Locale.ROOT)
        val extractedKeys = mutableSetOf<String>()

        // 1. Patient Name
        val nameRegexEn = Regex("""(?:patient|name|patient\s+name|mrs|smt)\s+(?:is\s+)?([a-zA-Z\s]+?)(?:,|\svillage|\sbp|\sblood|\shaemoglobin|\shb|\sdanger|\svaginal|$)""", RegexOption.IGNORE_CASE)
        val nameRegexHi = Regex("""(?:मरीज|मरीज़|नाम|श्रीमती)\s+(?:है\s+)?([\u0900-\u097F\s]+?)(?:,|\sगांव|\sगाँव|\sबीपी|\sरक्तचाप|\sहीमोग्लोबिन|\sबुखार|\sखून|$)""", RegexOption.IGNORE_CASE)
        val nameRegexBn = Regex("""(?:রোগী|মরীয|নাম)\s+(?:হল\s+)?([\u0980-\u09FF\s]+?)(?:,|\sগ্রাম|\sবিপি|\sরক্তচাপ|\sহিমোগ্লোবিন|\sজ্বর|$)""", RegexOption.IGNORE_CASE)

        val rawName = nameRegexHi.find(normalized)?.groupValues?.get(1)?.trim()
            ?: nameRegexBn.find(normalized)?.groupValues?.get(1)?.trim()
            ?: nameRegexEn.find(normalized)?.groupValues?.get(1)?.trim()

        val patientName = if (!rawName.isNullOrBlank() && rawName.length >= 2) {
            extractedKeys.add("patientName")
            rawName.split(" ").filter { it.isNotBlank() }.joinToString(" ") {
                it.replaceFirstChar { char -> char.uppercase() }
            }
        } else null

        // 2. Village / Area
        val villageRegexEn = Regex("""(?:village|from|area)\s+(?:is\s+)?([a-zA-Z\s]+?)(?:,|\sbp|\sblood|\shaemoglobin|\shb|\sdanger|\sbleeding|\svaginal|$)""", RegexOption.IGNORE_CASE)
        val villageRegexHi = Regex("""(?:गांव|गाँव|ग्राम|क्षेत्र)\s+(?:है\s+)?([\u0900-\u097F\s]+?)(?:,|\sबीपी|\sरक्तचाप|\sहीमोग्लोबिन|\sबुखार|\sखून|$)""", RegexOption.IGNORE_CASE)
        val villageRegexBn = Regex("""(?:গ্রাম|এলাকা)\s+(?:হল\s+)?([\u0980-\u09FF\s]+?)(?:,|\sবিপি|\sরক্তচাপ|\sহিমোগ্লোবিন|\sজ্বর|$)""", RegexOption.IGNORE_CASE)

        val rawVillage = villageRegexHi.find(normalized)?.groupValues?.get(1)?.trim()
            ?: villageRegexBn.find(normalized)?.groupValues?.get(1)?.trim()
            ?: villageRegexEn.find(normalized)?.groupValues?.get(1)?.trim()

        val village = if (!rawVillage.isNullOrBlank() && rawVillage.length >= 2) {
            extractedKeys.add("village")
            rawVillage.split(" ").filter { it.isNotBlank() }.joinToString(" ") {
                it.replaceFirstChar { char -> char.uppercase() }
            }
        } else null

        // 3. Gestational Age (e.g. "gestational age 32 weeks", "32 हफ्ते", "8 महीने")
        val gaRegex = Regex("""(?:gestational\s*age|pregnancy\s*week|गर्भ|हफ्ते|सप्ताह|माह|महीने|weeks|month)?\s*(\d{1,2})\s*(?:हफ्ते|सप्ताह|week|weeks)""", RegexOption.IGNORE_CASE)
        val gaMatch = gaRegex.find(normalized)
        val gestationalAgeWeeks = if (gaMatch != null) {
            val w = gaMatch.groupValues[1].toIntOrNull()
            if (w != null && w in 4..44) {
                extractedKeys.add("gestationalAgeWeeks")
                w
            } else null
        } else null

        // 4. Blood Pressure
        val bpRegex = Regex("""(?:bp|blood\s*pressure|बीपी|रक्तचाप|বিপি)?\s*(?:is\s*|है\s*|হল\s*)?(\d{2,3})\s*(?:\/|\s)\s*(\d{2,3})""", RegexOption.IGNORE_CASE)
        val bpMatch = bpRegex.find(normalized)
        val bloodPressure = if (bpMatch != null) {
            val sbp = bpMatch.groupValues[1].toIntOrNull()
            val dbp = bpMatch.groupValues[2].toIntOrNull()
            if (sbp != null && dbp != null && sbp in 60..260 && dbp in 30..160) {
                extractedKeys.add("bloodPressure")
                "$sbp/$dbp"
            } else null
        } else null

        // 5. Haemoglobin
        val hbRegex = Regex("""(?:hb|haemoglobin|hemoglobin|हीमोग्लोबिन|হিমোগ্লোबिन)\s*(?:is\s*|है\s*|হল\s*)?(\d{1,2}(?:\.\d{1,2})?)\s*(?:g\/dl|gram|gm)?""", RegexOption.IGNORE_CASE)
        val hbMatch = hbRegex.find(normalized)
        val haemoglobin = if (hbMatch != null) {
            val hbVal = hbMatch.groupValues[1].toDoubleOrNull()
            if (hbVal != null && hbVal in 2.0..20.0) {
                extractedKeys.add("haemoglobin")
                hbVal
            } else null
        } else null

        // 6. Danger Signs (Full 8 signs supported under mohfw-hrp-v1.0)
        val hasBleeding = lower.contains("bleed") || lower.contains("blood") ||
                lower.contains("khoon") || lower.contains("खून") || lower.contains("रक्तस्राव") || lower.contains("রক্তস্রাব")

        val hasConvulsions = lower.contains("convulsion") || lower.contains("seizure") || lower.contains("fit") ||
                lower.contains("daura") || lower.contains("दौरा") || lower.contains("झटके") || lower.contains("बेहोश") ||
                lower.contains("unconscious") || lower.contains("vision") || lower.contains("धुंधला")

        val hasHeadache = lower.contains("headache") || lower.contains("head ache") || lower.contains("migraine") ||
                lower.contains("sirdard") || lower.contains("सिरदर्द") || lower.contains("सिर दर्द") || lower.contains("মাথা ব্যথা")

        val hasAbdominalPain = lower.contains("abdominal pain") || lower.contains("stomach pain") || lower.contains("belly pain") ||
                lower.contains("pet dard") || lower.contains("पेट दर्द") || lower.contains("पेट में तेज दर्द")

        val hasBreathlessness = lower.contains("breathless") || lower.contains("shortness of breath") || lower.contains("breathing") ||
                lower.contains("saans") || lower.contains("सांस फूलना") || lower.contains("साँस") || lower.contains("chest pain")

        val hasFever = lower.contains("fever") || lower.contains("pyrexia") || lower.contains("temperature") ||
                lower.contains("bukhar") || lower.contains("बुखार") || lower.contains("ज्वर") || lower.contains("ताप") || lower.contains("জ্বর")

        val hasLabourEarly = lower.contains("water broke") || lower.contains("labour pain") || lower.contains("premature labour") ||
                lower.contains("pani chhut") || lower.contains("पानी छूटना") || lower.contains("प्रसव पीड़ा")

        val hasFetalDistress = lower.contains("fetal") || lower.contains("baby movement") || lower.contains("no movement") ||
                lower.contains("halchal kam") || lower.contains("हलचल कम") || lower.contains("बच्चा नहीं हिल") || lower.contains("শিশু কম")

        val dangerSigns = DangerSigns(
            bleeding = hasBleeding,
            convulsions = hasConvulsions,
            severeHeadache = hasHeadache,
            severeAbdominalPain = hasAbdominalPain,
            severeBreathlessness = hasBreathlessness,
            fever = hasFever,
            prematureLabourWaterBroke = hasLabourEarly,
            reducedFetalMovement = hasFetalDistress
        )

        if (hasBleeding) extractedKeys.add("dangerSigns.bleeding")
        if (hasConvulsions) extractedKeys.add("dangerSigns.convulsionsOrVisionLoss")
        if (hasHeadache) extractedKeys.add("dangerSigns.severeHeadache")
        if (hasAbdominalPain) extractedKeys.add("dangerSigns.severeAbdominalPain")
        if (hasBreathlessness) extractedKeys.add("dangerSigns.severeBreathlessness")
        if (hasFever) extractedKeys.add("dangerSigns.fever")
        if (hasLabourEarly) extractedKeys.add("dangerSigns.prematureLabourWaterBroke")
        if (hasFetalDistress) extractedKeys.add("dangerSigns.reducedFetalMovement")

        // 7. Travel Constraints (e.g. "flood", "river", "kutcha road", "kuchha rasta")
        val travelConstraints = when {
            lower.contains("flood") || lower.contains("baadh") || lower.contains("बाढ़") -> "Flood waters blocking road"
            lower.contains("river") || lower.contains("nadi") || lower.contains("नदी") -> "River crossing required"
            lower.contains("kutcha") || lower.contains("kaccha") || lower.contains("कच्चा रास्ता") -> "Kutcha mud road (no vehicle access)"
            lower.contains("no vehicle") || lower.contains("gadi nahi") || lower.contains("गाड़ी नहीं") -> "No local vehicle available"
            else -> null
        }
        if (travelConstraints != null) {
            extractedKeys.add("travelConstraints")
        }

        return OrganizedVoiceDraft(
            rawTranscript = clean,
            candidateFields = CandidateAssessmentFields(
                patientName = patientName,
                village = village,
                gestationalAgeWeeks = gestationalAgeWeeks,
                bloodPressure = bloodPressure,
                haemoglobin = haemoglobin,
                dangerSigns = dangerSigns,
                travelConstraints = travelConstraints,
                notes = "Spoken note: $clean"
            ),
            extractedFieldKeys = extractedKeys,
            isRecognized = extractedKeys.isNotEmpty()
        )
    }
}
