package com.example.ddiysec

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class LiveDetectionFragment : Fragment() {

    private lateinit var cardStatus: CardView
    private lateinit var tvStatusTitle: TextView
    private lateinit var tvStatusMessage: TextView
    private lateinit var tvLastUpdated: TextView
    private lateinit var tvLiveBadge: TextView
    private lateinit var ivStatusIcon: ImageView

    private lateinit var cardLiveStatistics: CardView
    private lateinit var tvTotalConnections: TextView
    private lateinit var tvNormalTraffic: TextView
    private lateinit var tvSuspiciousTraffic: TextView
    private lateinit var currentThreatLevel: LinearLayout
    private lateinit var tvCurrentThreatPercentage: TextView
    private lateinit var tvUptime: TextView

    private lateinit var tvTotalScans: TextView
    private lateinit var rvRecentDetections: RecyclerView
    private lateinit var btnViewFullHistory: com.google.android.material.button.MaterialButton

    private var isLiveMonitoring = true
    private val liveUpdateHandler = Handler(Looper.getMainLooper())
    private var currentStats: StatisticsResponse? = null
    private var lastDetectionRecord: DetectionRecord? = null
    private var recentDetections = mutableListOf<DetectionRecord>()
    private lateinit var recentHistoryAdapter: DetectionHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_live_detection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        loadRealData()
        startLiveMonitoring()
    }

    private fun initViews(view: View) {
        // Status views
        cardStatus = view.findViewById(R.id.cardStatus)
        tvStatusTitle = view.findViewById(R.id.tvStatusTitle)
        tvStatusMessage = view.findViewById(R.id.tvStatusMessage)
        tvLastUpdated = view.findViewById(R.id.tvLastUpdated)
        tvLiveBadge = view.findViewById(R.id.tvLiveBadge)
        ivStatusIcon = view.findViewById(R.id.ivStatusIcon)

        // live statistics views
        cardLiveStatistics = view.findViewById(R.id.cardLiveStatistics)
        tvTotalConnections = view.findViewById(R.id.tvTotalConnections)
        tvNormalTraffic = view.findViewById(R.id.tvNormalTraffic)
        tvSuspiciousTraffic = view.findViewById(R.id.tvSuspiciousTraffic)
        currentThreatLevel = view.findViewById(R.id.currentThreatLevel)
        tvCurrentThreatPercentage = view.findViewById(R.id.tvCurrentThreatPercentage)
        tvUptime = view.findViewById(R.id.tvUptime)

        // Detection history views
        tvTotalScans = view.findViewById(R.id.tvTotalScans)
        rvRecentDetections = view.findViewById(R.id.rvRecentDetections)
        btnViewFullHistory = view.findViewById(R.id.btnViewFullHistory)
    }

    private fun setupRecyclerView() {
        // Use the same adapter as DetectionHistoryActivity
        recentHistoryAdapter = DetectionHistoryAdapter(recentDetections) { record ->
            showDetailToast(record)
        }

        rvRecentDetections.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentHistoryAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        btnViewFullHistory.setOnClickListener {
            val intent = Intent(requireContext(), DetectionHistoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadRealData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stats = ApiClient.liveApiService.getStatistics(
                    ApiClient.getApiKey("live")
                )

                val results = ApiClient.liveApiService.getResults(
                    ApiClient.getApiKey("live")
                )

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
                        null
                    }
                }

                withContext(Dispatchers.Main) {
                    currentStats = stats

                    lastDetectionRecord = records.firstOrNull()

                    updateLiveStatisticsFromReal(stats, lastDetectionRecord)
                    updateStatusCardFromReal(lastDetectionRecord, stats)
                    updateRecentDetectionHistory(records.take(3))

                    tvTotalScans.text = "${records.size} scans total"
                    tvLastUpdated.text = "Last updated: ${getCurrentTime()}"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to load live data: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateDefaultValues()
                }
            }
        }
    }

    private fun updateLiveStatisticsFromReal(stats: StatisticsResponse, lastRecord: DetectionRecord?) {

        val totalConnections = stats.total_detections
        val criticalDetections = stats.critical_detections
        val normalDetections = totalConnections - criticalDetections

//        val criticalDetections = stats.last_24h["intrusion"] ?: 0
//        val normalDetections = stats.last_24h["normal"] ?: 0
//        val totalConnections = criticalDetections + normalDetections

        tvTotalConnections.text = formatNumber(totalConnections)
        tvNormalTraffic.text = formatNumber(normalDetections)
        tvSuspiciousTraffic.text = formatNumber(criticalDetections)

        // Calculate threat percentage from real data
        val threatPercentage = if (totalConnections > 0) {
            (criticalDetections * 100) / totalConnections
        } else 0

        updateThreatLevelDisplay(threatPercentage)
    }

    private fun updateStatusCardFromReal(lastRecord: DetectionRecord?, stats: StatisticsResponse) {
        if (lastRecord != null) {
            val ddosPercentage = lastRecord.getDDoSPercentage()
            val totalConnections = lastRecord.getTotalConnections()
            val intrusionCount = lastRecord.intrusion_count

            updateStatusCardDisplay(ddosPercentage, intrusionCount, totalConnections)
        } else {
            val recentThreats = stats.last_24h["intrusion"] ?: 0
            val recentNormal = stats.last_24h["normal"] ?: 0
            val total = recentThreats + recentNormal

            if (total > 0) {
                val percentage = (recentThreats * 100) / total
                updateStatusCardDisplay(percentage, recentThreats, total)
            } else {
                updateDefaultStatus()
            }
        }
    }

    private fun updateRecentDetectionHistory(records: List<DetectionRecord>) {
        recentDetections.clear()
        recentDetections.addAll(records)
        recentHistoryAdapter.notifyDataSetChanged()
    }

    private fun startLiveMonitoring() {
        if (!isLiveMonitoring) return

        //  real-time updates  periodically calling  API
        startRealTimeUpdates()
    }

    private fun startRealTimeUpdates() {
        val updateRunnable = object : Runnable {
            override fun run() {
                if (isLiveMonitoring) {
                    loadRealData()
                    liveUpdateHandler.postDelayed(this, 30000)
                }
            }
        }
        liveUpdateHandler.postDelayed(updateRunnable, 30000)
    }

    private fun updateThreatLevelDisplay(threatPercentage: Int) {
        tvCurrentThreatPercentage.text = "$threatPercentage% Suspicious"

        when {
            threatPercentage < 10 -> {
                currentThreatLevel.setBackgroundColor(Color.parseColor("#e8f5e8"))
                tvCurrentThreatPercentage.setTextColor(Color.parseColor("#155724"))
            }
            threatPercentage < 30 -> {
                currentThreatLevel.setBackgroundColor(Color.parseColor("#fff3cd"))
                tvCurrentThreatPercentage.setTextColor(Color.parseColor("#664d03"))
            }
            threatPercentage < 60 -> {
                currentThreatLevel.setBackgroundColor(Color.parseColor("#ffe8d1"))
                tvCurrentThreatPercentage.setTextColor(Color.parseColor("#df6c0c"))
            }
            else -> {
                currentThreatLevel.setBackgroundColor(Color.parseColor("#f8d7da"))
                tvCurrentThreatPercentage.setTextColor(Color.parseColor("#842029"))
            }
        }
    }

    private fun updateStatusCardDisplay(ddosPercentage: Int, intrusionCount: Int, totalConnections: Int) {
        when {
            ddosPercentage < 10 -> {
                tvStatusTitle.text = "System Secure"
                tvStatusMessage.text = "Excellent - $ddosPercentage% suspicious traffic\n${formatNumber(totalConnections)} total connections"
                cardStatus.setCardBackgroundColor(Color.parseColor("#81C784"))
                ivStatusIcon.setImageResource(R.drawable.ic_shield_check)
                tvLiveBadge.visibility = View.VISIBLE
            }
            ddosPercentage < 30 -> {
                tvStatusTitle.text = "Minor Threats"
                tvStatusMessage.text = "Low Risk - $ddosPercentage% suspicious traffic\n${formatNumber(intrusionCount)} suspicious / ${formatNumber(totalConnections)} total"
                cardStatus.setCardBackgroundColor(Color.parseColor("#ff9800"))
                ivStatusIcon.setImageResource(R.drawable.ic_shield_check)
                tvLiveBadge.visibility = View.VISIBLE
            }
            ddosPercentage < 60 -> {
                tvStatusTitle.text = "Moderate Threats"
                tvStatusMessage.text = "Medium Risk - $ddosPercentage% suspicious traffic\n${formatNumber(intrusionCount)} suspicious / ${formatNumber(totalConnections)} total"
                cardStatus.setCardBackgroundColor(Color.parseColor("#fd7e14"))
                ivStatusIcon.setImageResource(R.drawable.ic_shield_check)
                tvLiveBadge.visibility = View.VISIBLE
            }
            else -> {
                tvStatusTitle.text = "Critical Threats!"
                tvStatusMessage.text = "High Risk - $ddosPercentage% suspicious traffic\n${formatNumber(intrusionCount)} suspicious / ${formatNumber(totalConnections)} total"
                cardStatus.setCardBackgroundColor(Color.parseColor("#e53935"))
                ivStatusIcon.setImageResource(R.drawable.ic_shield_check)
                tvLiveBadge.visibility = View.VISIBLE
            }
        }
    }

    private fun updateDefaultStatus() {
        tvStatusTitle.text = "System Ready"
        tvStatusMessage.text = "Live monitoring active - Ready to detect threats"
        cardStatus.setCardBackgroundColor(Color.parseColor("#81C784"))
        ivStatusIcon.setImageResource(R.drawable.ic_shield_check)
        tvLastUpdated.text = "Last updated: ${getCurrentTime()}"
        tvLiveBadge.visibility = View.VISIBLE

        // Default values
        tvTotalConnections.text = "0"
        tvNormalTraffic.text = "0"
        tvSuspiciousTraffic.text = "0"
        tvCurrentThreatPercentage.text = "0% Suspicious"
        tvTotalScans.text = "0 scans total"
    }

    private fun updateDefaultValues() {
        updateDefaultStatus()
    }

    // Method for upload fragment to update status
    fun updateStatusFromUpload(ddosPercentage: Int, intrusionCount: Int, totalConnections: Int) {
        updateStatusCardDisplay(ddosPercentage, intrusionCount, totalConnections)
        tvLastUpdated.text = "Last updated: ${getCurrentTime()}"

        // Also refresh the live data to get latest from API
        loadRealData()
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

        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun formatNumber(number: Int): String {
        return NumberFormat.getNumberInstance().format(number)
    }

    private fun formatTimestamp(isoString: String): String {
        return try {
            var timestamp = isoString

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

    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    override fun onResume() {
        super.onResume()
        isLiveMonitoring = true
        loadRealData() // Refresh data when resuming
        startLiveMonitoring()

        tvUptime.text = "Uptime: 99.9%"
    }

    override fun onPause() {
        super.onPause()
        isLiveMonitoring = false
        liveUpdateHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isLiveMonitoring = false
        liveUpdateHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        fun newInstance(): LiveDetectionFragment {
            return LiveDetectionFragment()
        }
    }
}