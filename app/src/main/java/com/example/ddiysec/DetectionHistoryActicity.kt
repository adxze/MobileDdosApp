package com.example.ddiysec

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

data class DetectionRecord(
    val id: Int,
    val capture_id: String,
    val hostname: String,
    val location: String,
    val timestamp: String,
    val status: String,
    val normal_count: Int,
    val intrusion_count: Int,
    val os: String,
    val result_json: String?,
    val is_critical: Boolean,
    val attack_type: String?
) {
    fun getTotalConnections(): Int = normal_count + intrusion_count

    fun getDDoSPercentage(): Int {
        val total = getTotalConnections()
        return if (total > 0) (intrusion_count * 100) / total else 0
    }

    fun getSeverityLevel(): String {
        val percentage = getDDoSPercentage()
        return when {
            percentage < 20 -> "Safe"
            percentage < 50 -> "Low"
            percentage < 70 -> "Medium"
            else -> "Critical"
        }
    }

    fun getSeverityColor(): Int {
        return when (getSeverityLevel()) {
            "Safe" -> Color.parseColor("#28a745")
            "Low" -> Color.parseColor("#ffc107")
            "Medium" -> Color.parseColor("#fd7e14")
            else -> Color.parseColor("#dc3545")
        }
    }

    fun getSeverityBackground(): String {
        return when (getSeverityLevel()) {
            "Safe" -> "badge_safe_background"
            "Low" -> "badge_warning_background"
            "Medium" -> "badge_warning_background"
            else -> "badge_danger_background"
        }
    }
}

class DetectionHistoryActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnRefresh: Button
    private lateinit var tvCurrentThreatLevel: TextView
    private lateinit var tvTotalDetections: TextView
    private lateinit var tvCriticalCount: TextView
    private lateinit var tvLastUpdated: TextView
    private lateinit var rvDetectionHistory: RecyclerView
    private lateinit var layoutLoading: LinearLayout
    private lateinit var layoutEmpty: LinearLayout

    private lateinit var historyAdapter: DetectionHistoryAdapter
    private var detectionHistory = mutableListOf<DetectionRecord>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detection_history)
        supportActionBar?.hide()

        initViews()
        setupRecyclerView()
        setupListeners()

        // Load data
        loadDetectionHistory()
        loadStatistics()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnRefresh = findViewById(R.id.btnRefresh)
        tvCurrentThreatLevel = findViewById(R.id.tvCurrentThreatLevel)
        tvTotalDetections = findViewById(R.id.tvTotalDetections)
        tvCriticalCount = findViewById(R.id.tvCriticalCount)
        tvLastUpdated = findViewById(R.id.tvLastUpdated)
        rvDetectionHistory = findViewById(R.id.rvDetectionHistory)
        layoutLoading = findViewById(R.id.layoutLoading)
        layoutEmpty = findViewById(R.id.layoutEmpty)
    }

    private fun setupRecyclerView() {
        historyAdapter = DetectionHistoryAdapter(detectionHistory) { record ->
            // Handle detail view click if needed
            showDetailToast(record)
        }
        rvDetectionHistory.layoutManager = LinearLayoutManager(this)
        rvDetectionHistory.adapter = historyAdapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnRefresh.setOnClickListener {
            refreshData()
        }
    }

    private fun refreshData() {
        loadDetectionHistory()
        loadStatistics()
    }

    private fun loadDetectionHistory() {
        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch results from the API
                val results = ApiClient.liveApiService.getResults(
                    ApiClient.getApiKey("live")
                )

                // Convert API response to DetectionRecord objects
                val records = results.mapNotNull { result ->
                    try {
                        DetectionRecord(
                            id = (result["id"] as? Double)?.toInt() ?: 0,
                            capture_id = result["capture_id"] as? String ?: "",
                            hostname = result["hostname"] as? String ?: "Unknown",
                            location = result["location"] as? String ?: "Unknown",
                            timestamp = result["timestamp"] as? String ?: "",
                            status = result["status"] as? String ?: "completed",
                            normal_count = (result["normal_count"] as? Double)?.toInt() ?: 0,
                            intrusion_count = (result["intrusion_count"] as? Double)?.toInt() ?: 0,
                            os = result["os"] as? String ?: "Unknown",
                            result_json = result["result_json"] as? String,
                            is_critical = result["is_critical"] as? Boolean ?: false,
                            attack_type = result["attack_type"] as? String
                        )
                    } catch (e: Exception) {
                        null // Skip malformed records
                    }
                }

                withContext(Dispatchers.Main) {
                    updateDetectionHistory(records)
                    showLoading(false)

                    if (records.isEmpty()) {
                        showEmpty(true)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@DetectionHistoryActivity,
                        "Failed to load history: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    showLoading(false)
                    showEmpty(true)
                }
            }
        }
    }

    private fun loadStatistics() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stats = ApiClient.liveApiService.getStatistics(
                    ApiClient.getApiKey("live")
                )

                withContext(Dispatchers.Main) {
                    updateStatistics(stats)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Stats loading failed, but don't show error to user
                    // Just use default values
                }
            }
        }
    }

    private fun updateDetectionHistory(records: List<DetectionRecord>) {
        detectionHistory.clear()
        detectionHistory.addAll(records)
        historyAdapter.notifyDataSetChanged()

        tvLastUpdated.text = "Last updated: ${getCurrentTime()}"
    }

    private fun updateStatistics(stats: StatisticsResponse) {
        tvTotalDetections.text = NumberFormat.getNumberInstance().format(stats.total_detections)
        tvCriticalCount.text = NumberFormat.getNumberInstance().format(stats.critical_detections)

        // Determine current threat level based on recent activity
        val recentThreats = stats.last_24h["intrusion"] ?: 0
        val recentNormal = stats.last_24h["normal"] ?: 0
        val total = recentThreats + recentNormal

        if (total > 0) {
            val percentage = (recentThreats * 100) / total
            when {
                percentage < 20 -> {
                    tvCurrentThreatLevel.text = "Safe"
                    tvCurrentThreatLevel.setTextColor(Color.parseColor("#28a745"))
                }
                percentage < 50 -> {
                    tvCurrentThreatLevel.text = "Low"
                    tvCurrentThreatLevel.setTextColor(Color.parseColor("#ffc107"))
                }
                percentage < 70 -> {
                    tvCurrentThreatLevel.text = "Medium"
                    tvCurrentThreatLevel.setTextColor(Color.parseColor("#fd7e14"))
                }
                else -> {
                    tvCurrentThreatLevel.text = "Critical"
                    tvCurrentThreatLevel.setTextColor(Color.parseColor("#dc3545"))
                }
            }
        } else {
            tvCurrentThreatLevel.text = "Safe"
            tvCurrentThreatLevel.setTextColor(Color.parseColor("#28a745"))
        }
    }

    private fun showLoading(show: Boolean) {
        layoutLoading.visibility = if (show) View.VISIBLE else View.GONE
        rvDetectionHistory.visibility = if (show) View.GONE else View.VISIBLE
        layoutEmpty.visibility = View.GONE
    }

    private fun showEmpty(show: Boolean) {
        layoutEmpty.visibility = if (show) View.VISIBLE else View.GONE
        rvDetectionHistory.visibility = if (show) View.GONE else View.VISIBLE
        layoutLoading.visibility = View.GONE
    }

    private fun showDetailToast(record: DetectionRecord) {
        val message = """
            Detection Details:
            • ID: ${record.capture_id.take(8)}...
            • Host: ${record.hostname}
            • Connections: ${record.getTotalConnections()}
            • DDoS Traffic: ${record.getDDoSPercentage()}%
            • Attack Type: ${record.attack_type ?: "None"}
        """.trimIndent()

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    private fun formatTimestamp(isoString: String): String {
        return try {
            var timestamp = isoString

            // Handle timestamp format like your web dashboard
            if (timestamp.contains(' ') && !timestamp.contains('T')) {
                timestamp = timestamp.replace(' ', 'T')
            }

            if (!timestamp.contains('Z') && !timestamp.contains('+')) {
                timestamp = timestamp + 'Z'
            }

            val date = Date(timestamp)
            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            "Invalid date"
        }
    }
}