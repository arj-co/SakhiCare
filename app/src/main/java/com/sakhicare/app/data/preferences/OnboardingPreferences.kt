package com.sakhicare.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class OnboardingPreferences(
    context: Context,
    customPrefs: SharedPreferences? = null
) {

    private val prefs: SharedPreferences = customPrefs ?: try {
        EncryptedSharedPreferences.create(
            "sakhicare_onboarding_prefs",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Throwable) {
        context.getSharedPreferences("sakhicare_onboarding_prefs_unencrypted", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_COMPLETED = "onboarding_completed"
        private const val KEY_LANGUAGE = "selected_language"
        private const val KEY_WORKER_NAME = "worker_name"
        private const val KEY_WORKER_PHONE = "worker_phone"
        private const val KEY_WORKER_ROLE = "worker_role"
        private const val KEY_FACILITY_NAME = "facility_name"
        private const val KEY_FACILITY_CODE = "facility_code"
        private const val KEY_PIN = "local_security_pin"
        private const val KEY_CONSENT = "consent_acknowledged"
        private const val KEY_PROTOCOL_VERSION = "installed_protocol_version"
        private const val KEY_AUTH_ACCESS_TOKEN = "supabase_access_token"
    }

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_COMPLETED, value).apply()

    var selectedLanguage: String
        get() = prefs.getString(KEY_LANGUAGE, "HINDI") ?: "HINDI"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var workerName: String
        get() = prefs.getString(KEY_WORKER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WORKER_NAME, value).apply()

    var workerPhone: String
        get() = prefs.getString(KEY_WORKER_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WORKER_PHONE, value).apply()

    var workerRole: String
        get() = prefs.getString(KEY_WORKER_ROLE, "ASHA") ?: "ASHA"
        set(value) = prefs.edit().putString(KEY_WORKER_ROLE, value).apply()

    var facilityName: String
        get() = prefs.getString(KEY_FACILITY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FACILITY_NAME, value).apply()

    var facilityCode: String
        get() = prefs.getString(KEY_FACILITY_CODE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FACILITY_CODE, value).apply()

    var localPin: String
        get() = prefs.getString(KEY_PIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PIN, value).apply()

    var hasConsentGiven: Boolean
        get() = prefs.getBoolean(KEY_CONSENT, false)
        set(value) = prefs.edit().putBoolean(KEY_CONSENT, value).apply()

    var protocolPackVersion: String
        get() = prefs.getString(KEY_PROTOCOL_VERSION, "mohfw-hrp-v1.0") ?: "mohfw-hrp-v1.0"
        set(value) = prefs.edit().putString(KEY_PROTOCOL_VERSION, value).apply()

    var authAccessToken: String
        get() = prefs.getString(KEY_AUTH_ACCESS_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_AUTH_ACCESS_TOKEN, value).apply()

    fun completeOnboarding(
        name: String,
        phone: String,
        facility: String,
        facilityCd: String,
        pin: String,
        language: String
    ) {
        prefs.edit()
            .putBoolean(KEY_COMPLETED, true)
            .putString(KEY_WORKER_NAME, name)
            .putString(KEY_WORKER_PHONE, phone)
            .putString(KEY_FACILITY_NAME, facility)
            .putString(KEY_FACILITY_CODE, facilityCd)
            .putString(KEY_PIN, pin)
            .putString(KEY_LANGUAGE, language)
            .putBoolean(KEY_CONSENT, true)
            .putString(KEY_PROTOCOL_VERSION, "mohfw-hrp-v1.0")
            .commit()
    }

    fun clearSession() {
        prefs.edit().remove(KEY_AUTH_ACCESS_TOKEN).apply()
    }
}
