package com.sakhicare.app.sync

import android.content.Context
import com.sakhicare.app.BuildConfig
import com.sakhicare.app.data.preferences.OnboardingPreferences
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

data class SyncBatchRequest(
    @SerializedName("idempotency_key") val idempotencyKey: String,
    @SerializedName("case_id") val caseId: String,
    @SerializedName("patient_name") val patientName: String,
    @SerializedName("village") val village: String,
    @SerializedName("blood_pressure") val bloodPressure: String?,
    @SerializedName("haemoglobin") val haemoglobin: String?,
    @SerializedName("danger_signs") val dangerSigns: Map<String, Boolean>,
    @SerializedName("risk_level") val riskLevel: String,
    @SerializedName("risk_score") val riskScore: Int,
    @SerializedName("rule_pack_version") val rulePackVersion: String,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("location_accuracy_m") val locationAccuracyM: Float? = null,
    @SerializedName("location_captured_at") val locationCapturedAt: Long? = null
)

data class SyncBatchResponse(
    @SerializedName("status") val status: String,
    @SerializedName("case_id") val caseId: String,
    @SerializedName("server_timestamp") val serverTimestamp: Long,
    @SerializedName("doctor_advisory") val doctorAdvisory: String? = null,
    @SerializedName("ambulance_status") val ambulanceStatus: String? = null
)

data class AudioUploadResponse(
    @SerializedName("status") val status: String,
    @SerializedName("artifact_id") val artifactId: String,
    @SerializedName("case_id") val caseId: String,
    @SerializedName("sha256") val sha256: String,
    @SerializedName("file_size_bytes") val fileSizeBytes: Long
)

data class SupabasePasswordLoginRequest(
    val email: String,
    val password: String
)

data class SupabaseSessionResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("user") val user: SupabaseUser?
)

data class SupabaseUser(@SerializedName("id") val id: String)

interface SupabaseAuthService {
    @POST("auth/v1/token?grant_type=password")
    suspend fun passwordLogin(
        @retrofit2.http.Header("apikey") anonKey: String,
        @Body request: SupabasePasswordLoginRequest
    ): Response<SupabaseSessionResponse>
}

interface SakhiCareApiService {
    @POST("/sync/batch")
    suspend fun syncBatch(
        @Header("X-Idempotency-Key") idempotencyKey: String,
        @Body request: SyncBatchRequest
    ): Response<SyncBatchResponse>

    @retrofit2.http.Multipart
    @POST("/api/v1/cases/{case_id}/audio")
    suspend fun uploadCaseAudio(
        @retrofit2.http.Path("case_id") caseId: String,
        @Header("X-Idempotency-Key") idempotencyKey: String,
        @Header("X-Audio-SHA256") sha256: String,
        @retrofit2.http.Part file: okhttp3.MultipartBody.Part,
        @retrofit2.http.Query("duration_seconds") durationSeconds: Int,
        @retrofit2.http.Query("language") language: String
    ): Response<AudioUploadResponse>
}

object SakhiCareApiClient {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val token = appContext?.let { OnboardingPreferences(it).authAccessToken }
                val authenticated = if (token.isNullOrBlank()) request else request.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
                chain.proceed(authenticated)
            }
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val service: SakhiCareApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.SAKHICARE_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SakhiCareApiService::class.java)
    }

    private val supabaseAuthService: SupabaseAuthService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.SUPABASE_URL)
            .client(OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseAuthService::class.java)
    }

    suspend fun signInWithSupabase(email: String, password: String): Response<SupabaseSessionResponse> {
        require(BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) { "Supabase public key is not configured in this APK build" }
        val response = supabaseAuthService.passwordLogin(
            BuildConfig.SUPABASE_ANON_KEY,
            SupabasePasswordLoginRequest(email = email, password = password)
        )
        if (response.isSuccessful) {
            response.body()?.accessToken?.let { token ->
                appContext?.let { OnboardingPreferences(it).authAccessToken = token }
            }
        }
        return response
    }
}
