package com.sakhicare.app.sync

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
    @SerializedName("created_at") val createdAt: Long
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
    // Default loopback URL for Android emulator / local network
    private const val DEFAULT_BASE_URL = "http://10.0.2.2:8000"

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val service: SakhiCareApiService by lazy {
        Retrofit.Builder()
            .baseUrl(DEFAULT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SakhiCareApiService::class.java)
    }
}
