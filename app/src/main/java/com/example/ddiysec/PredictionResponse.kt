package com.example.ddiysec

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

data class PredictionResponse(
    val success: Boolean,
    val result_counts: Map<String, Int>,
    val summary: Map<String, Any>,
    val result_file: String,
    val message: String
)

data class DetectionRequest(
    val `interface`: String,
    val duration: Int = 30
)

data class DetectionResponse(
    val capture_id: String,
    val message: String,
    val status: String = "started"
)

data class StatusResponse(
    val status: String,
    val message: String,
    val result_counts: Map<String, Int>? = null,
    val progress: Int? = null,
    val attack_type: String? = "Unknown"
)

data class StatisticsResponse(
    val total_detections: Int,
    val critical_detections: Int,
    val last_24h: Map<String, Int>,
    val hosts: List<HostStats>
)

data class HostStats(
    val hostname: String,
    val count: Int
)

// API service interfaces
interface CsvApiService {
    @Multipart
    @POST("predict_csv")
    suspend fun predictCsv(
        @Part file: MultipartBody.Part,
        @Header("X-API-Key") apiKey: String,
        @Part("hostname") hostname: RequestBody? = null,
        @Part("location") location: RequestBody? = null,
        @Part("os") os: RequestBody? = null
    ): PredictionResponse
}

interface LiveApiService {
    @POST("detect")
    suspend fun startDetection(
        @Body request: DetectionRequest,
        @Header("X-API-Key") apiKey: String
    ): DetectionResponse

    @GET("status/{captureId}")
    suspend fun getStatus(
        @Path("captureId") captureId: String,
        @Header("X-API-Key") apiKey: String
    ): StatusResponse

    @GET("results")
    suspend fun getResults(
        @Header("X-API-Key") apiKey: String
    ): List<Map<String, Any>>

    @GET("stats")
    suspend fun getStatistics(
        @Header("X-API-Key") apiKey: String
    ): StatisticsResponse
}

// API Configuration Object API Keys Will be replaced a mounth or more after the app is submitted
object ApiConfig {
    // API untuk deteksi menggunakan CSV file
    const val CSV_API_BASE = "https://web-production-4a62.up.railway.app/"
    const val CSV_API_KEY = "this-is-api-key-lol"

    // API untuk deteksi real-time/live
    const val LIVE_API_BASE = "https://web-production-8fe18.up.railway.app/"
    const val LIVE_API_KEY = "this-is-api-key-lol"

    fun getApiBase(type: String = "csv"): String {
        return if (type.lowercase() == "live") LIVE_API_BASE else CSV_API_BASE
    }

    fun getApiKey(type: String = "csv"): String {
        return if (type.lowercase() == "live") LIVE_API_KEY else CSV_API_KEY
    }
}

// API Clients
object ApiClient {
    private fun createRetrofit(baseUrl: String): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val csvRetrofit by lazy {
        createRetrofit(ApiConfig.CSV_API_BASE)
    }

    val csvApiService: CsvApiService by lazy {
        csvRetrofit.create(CsvApiService::class.java)
    }

    // Live API Client
    private val liveRetrofit by lazy {
        createRetrofit(ApiConfig.LIVE_API_BASE)
    }

    val liveApiService: LiveApiService by lazy {
        liveRetrofit.create(LiveApiService::class.java)
    }

    fun getApiService(type: String = "csv"): Any {
        return if (type.lowercase() == "live") liveApiService else csvApiService
    }

    fun getApiBase(type: String = "csv"): String {
        return ApiConfig.getApiBase(type)
    }

    fun getApiKey(type: String = "csv"): String {
        return ApiConfig.getApiKey(type)
    }
}