package com.example.ddiysec

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class UploadFilesFragment : Fragment() {

    // Upload Views
    private lateinit var uploadArea: RelativeLayout
    private lateinit var fileInfoContainer: LinearLayout
    private lateinit var tvFileName: TextView
    private lateinit var tvFileSize: TextView
    private lateinit var btnBrowse: Button
    private lateinit var btnRemoveFile: Button
    private lateinit var btnAnalyze: Button
    private lateinit var progressBar: ProgressBar

    // Enhanced Results Views
    private lateinit var cardResults: CardView
    private lateinit var ivThreatIcon: ImageView
    private lateinit var tvAnalysisTime: TextView
    private lateinit var threatLevelBanner: LinearLayout
    private lateinit var tvThreatLevel: TextView
    private lateinit var tvThreatPercentage: TextView
    private lateinit var tvResultMessage: TextView
    private lateinit var tvTotalConnections: TextView
    private lateinit var tvNormalTraffic: TextView
    private lateinit var tvSuspiciousTraffic: TextView
    private lateinit var pieChart: PieChart
    private lateinit var rvThreatCategories: RecyclerView
    private lateinit var tvAnalyzedFileName: TextView
    private lateinit var tvAnalyzedFileSize: TextView
    private lateinit var tvProcessingTime: TextView
    private lateinit var layoutRecommendations: LinearLayout
    private lateinit var tvRecommendations: TextView

    // Action Buttons
    private lateinit var btnCloseResults: Button
    private lateinit var btnViewInLiveDetection: Button
    private lateinit var btnAnalyzeAnother: Button
    private lateinit var btnExportResults: Button
    private lateinit var btnShareResults: Button

    private var selectedFile: Uri? = null
    private var analysisStartTime: Long = 0

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedFile = it
            displayFileInfo()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_upload_files, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
    }

    private fun initViews(view: View) {
        // Upload views
        uploadArea = view.findViewById(R.id.uploadArea)
        fileInfoContainer = view.findViewById(R.id.fileInfoContainer)
        tvFileName = view.findViewById(R.id.tvFileName)
        tvFileSize = view.findViewById(R.id.tvFileSize)
        btnBrowse = view.findViewById(R.id.btnBrowse)
        btnRemoveFile = view.findViewById(R.id.btnRemoveFile)
        btnAnalyze = view.findViewById(R.id.btnAnalyze)
        progressBar = view.findViewById(R.id.progressBar)

        // Enhanced Results views
        cardResults = view.findViewById(R.id.cardResults)
        ivThreatIcon = view.findViewById(R.id.ivThreatIcon)
        tvAnalysisTime = view.findViewById(R.id.tvAnalysisTime)
        threatLevelBanner = view.findViewById(R.id.threatLevelBanner)
        tvThreatLevel = view.findViewById(R.id.tvThreatLevel)
        tvThreatPercentage = view.findViewById(R.id.tvThreatPercentage)
        tvResultMessage = view.findViewById(R.id.tvResultMessage)
        tvTotalConnections = view.findViewById(R.id.tvTotalConnections)
        tvNormalTraffic = view.findViewById(R.id.tvNormalTraffic)
        tvSuspiciousTraffic = view.findViewById(R.id.tvSuspiciousTraffic)
        pieChart = view.findViewById(R.id.pieChart)
        rvThreatCategories = view.findViewById(R.id.rvThreatCategories)
        tvAnalyzedFileName = view.findViewById(R.id.tvAnalyzedFileName)
        tvAnalyzedFileSize = view.findViewById(R.id.tvAnalyzedFileSize)
        tvProcessingTime = view.findViewById(R.id.tvProcessingTime)
        layoutRecommendations = view.findViewById(R.id.layoutRecommendations)
        tvRecommendations = view.findViewById(R.id.tvRecommendations)

        // Action buttons
        btnCloseResults = view.findViewById(R.id.btnCloseResults)
        btnViewInLiveDetection = view.findViewById(R.id.btnViewInLiveDetection)
        btnAnalyzeAnother = view.findViewById(R.id.btnAnalyzeAnother)
        btnExportResults = view.findViewById(R.id.btnExportResults)
        btnShareResults = view.findViewById(R.id.btnShareResults)

        rvThreatCategories.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupClickListeners() {
        // Upload area click - open file picker
        uploadArea.setOnClickListener {
            openFilePicker()
        }

        btnBrowse.setOnClickListener {
            openFilePicker()
        }

        btnRemoveFile.setOnClickListener {
            removeSelectedFile()
        }

        btnAnalyze.setOnClickListener {
            selectedFile?.let { analyzeFile(it) }
        }

        btnCloseResults.setOnClickListener {
            hideResults()
        }

        btnAnalyzeAnother.setOnClickListener {
            resetForNewFile()
        }

        btnViewInLiveDetection.setOnClickListener {
            (activity as? HomeActivity)?.switchToLiveDetectionTab()
            Toast.makeText(requireContext(), "Switched to Live Detection tab", Toast.LENGTH_SHORT).show()
        }

        btnExportResults.setOnClickListener {
            exportResults()
        }

        btnShareResults.setOnClickListener {
            shareResults()
        }
    }

    private fun openFilePicker() {
        getContent.launch("*/*")
    }

    private fun displayFileInfo() {
        selectedFile?.let { uri ->
            val fileName = getFileName(uri)
            val fileSize = getFileSize(uri)

            tvFileName.text = fileName
            tvFileSize.text = "${formatFileSize(fileSize)} • Ready to analyze"

            fileInfoContainer.visibility = View.VISIBLE
        }
    }

    private fun removeSelectedFile() {
        selectedFile = null
        fileInfoContainer.visibility = View.GONE
    }

    private fun resetForNewFile() {
        removeSelectedFile()
        hideResults()
    }

    private fun hideResults() {
        cardResults.visibility = View.GONE
    }

    private fun analyzeFile(uri: Uri) {
        analysisStartTime = System.currentTimeMillis()
        progressBar.visibility = View.VISIBLE
        btnAnalyze.isEnabled = false
        btnAnalyze.text = "Analyzing..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val file = createTempFileFromInputStream(inputStream)

                val requestFile = RequestBody.create("text/csv".toMediaTypeOrNull(), file)
                val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val hostname = RequestBody.create("text/plain".toMediaTypeOrNull(), "Mobile-Android")
                val location = RequestBody.create("text/plain".toMediaTypeOrNull(), "Mobile-Device")
                val os = RequestBody.create("text/plain".toMediaTypeOrNull(), "Android")

                val response = ApiClient.csvApiService.predictCsv(
                    file = filePart,
                    apiKey = ApiClient.getApiKey("csv"),
                    hostname = hostname,
                    location = location,
                    os = os
                )

                withContext(Dispatchers.Main) {
                    handleApiResponse(response)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    progressBar.visibility = View.GONE
                    btnAnalyze.isEnabled = true
                    btnAnalyze.text = "Analyze Network Data"
                }
            }
        }
    }

    private fun handleApiResponse(response: PredictionResponse) {
        progressBar.visibility = View.GONE
        btnAnalyze.isEnabled = true
        btnAnalyze.text = "Analyze Network Data"

        val processingTime = System.currentTimeMillis() - analysisStartTime
        val processingTimeSeconds = processingTime / 1000.0

        if (response.success) {
            val totalConnections = response.result_counts.values.sum()
            val intrusionCount = response.result_counts["Intrusion"] ?: 0
            val normalCount = response.result_counts["Normal"] ?: 0
            val ddosPercentage = if (totalConnections > 0) {
                (intrusionCount * 100) / totalConnections
            } else 0

            // Update detailed results
            updateDetailedResults(
                totalConnections,
                normalCount,
                intrusionCount,
                ddosPercentage,
                processingTimeSeconds,
                response.result_counts
            )

            // Update Live Detection status
            (activity as? HomeActivity)?.updateLiveDetectionStatus(ddosPercentage, intrusionCount, totalConnections)

            // Update file size text to show completion
            selectedFile?.let { uri ->
                tvFileSize.text = "${formatFileSize(getFileSize(uri))} • Analysis complete"
            }

        } else {
            // Show error state
            updateErrorResults(response.message, processingTimeSeconds)
        }
    }

    private fun updateDetailedResults(
        totalConnections: Int,
        normalCount: Int,
        intrusionCount: Int,
        threatPercentage: Int,
        processingTime: Double,
        resultCounts: Map<String, Int>
    ) {
        // Update header and timing
        val currentTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        tvAnalysisTime.text = "Completed at $currentTime • ${String.format("%.1f", processingTime)}s"

        // Update threat level and visuals
        when {
            threatPercentage < 30 -> {
                updateThreatLevel("MINIMAL RISK", "#d1e7dd", "#0f5132", R.drawable.ic_shield_check, "#28a745")
                tvResultMessage.text = "Excellent network security posture. Only $threatPercentage% of traffic shows suspicious patterns."
                layoutRecommendations.visibility = View.GONE
            }
            threatPercentage < 50 -> {
                updateThreatLevel("LOW RISK", "#fff3cd", "#664d03", R.drawable.ic_shield_check, "#ffc107")
                tvResultMessage.text = "Network shows minor security concerns with $threatPercentage% suspicious traffic. Monitoring recommended."
                layoutRecommendations.visibility = View.VISIBLE
                tvRecommendations.text = "• Continue monitoring network activity\n• Review unusual traffic patterns\n• Consider updating security policies"
            }
            threatPercentage < 70 -> {
                updateThreatLevel("MEDIUM RISK", "#ffe8d1", "#df6c0c", R.drawable.ic_shield_check, "#fd7e14")
                tvResultMessage.text = "Moderate security threats detected. $threatPercentage% of traffic requires immediate attention."
                layoutRecommendations.visibility = View.VISIBLE
                tvRecommendations.text = "• Implement additional security measures\n• Block suspicious IP addresses\n• Enable DDoS protection\n• Alert security team"
            }
            else -> {
                updateThreatLevel("HIGH RISK", "#f8d7da", "#842029", R.drawable.ic_shield_check, "#dc3545")
                tvResultMessage.text = "Critical security threats detected! $threatPercentage% of traffic shows malicious patterns requiring immediate action."
                layoutRecommendations.visibility = View.VISIBLE
                tvRecommendations.text = "• IMMEDIATE ACTION REQUIRED\n• Block all suspicious traffic\n• Contact security team urgently\n• Implement emergency protocols\n• Review all security measures"
            }
        }

        // Update statistics
        tvThreatPercentage.text = "$threatPercentage% Suspicious"
        tvTotalConnections.text = formatNumber(totalConnections)
        tvNormalTraffic.text = formatNumber(normalCount)
        tvSuspiciousTraffic.text = formatNumber(intrusionCount)

        // Update analysis details
        selectedFile?.let { uri ->
            tvAnalyzedFileName.text = getFileName(uri)
            tvAnalyzedFileSize.text = formatFileSize(getFileSize(uri))
        }
        tvProcessingTime.text = "${String.format("%.1f", processingTime)} seconds"

        // Setup enhanced chart and threat categories
        setupEnhancedPieChart(resultCounts)
        setupThreatCategories(resultCounts)

        cardResults.visibility = View.VISIBLE
    }

    private fun updateThreatLevel(level: String, bgColor: String, textColor: String, iconRes: Int, iconTint: String) {
        tvThreatLevel.text = level
        threatLevelBanner.setBackgroundColor(Color.parseColor(bgColor))
        tvThreatLevel.setTextColor(Color.parseColor(textColor))
        tvThreatPercentage.setTextColor(Color.parseColor(textColor))
        tvResultMessage.setTextColor(Color.parseColor(textColor))
        ivThreatIcon.setImageResource(iconRes)
        ivThreatIcon.setColorFilter(Color.parseColor(iconTint))
    }

    private fun updateErrorResults(errorMessage: String?, processingTime: Double) {
        updateThreatLevel("ANALYSIS ERROR", "#f8d7da", "#842029", R.drawable.ic_shield_check, "#dc3545")
        tvResultMessage.text = "Analysis failed: ${errorMessage ?: "Unknown error occurred"}"
        tvThreatPercentage.text = "Error"

        val currentTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        tvAnalysisTime.text = "Failed at $currentTime • ${String.format("%.1f", processingTime)}s"

        layoutRecommendations.visibility = View.VISIBLE
        tvRecommendations.text = "• Check file format and try again\n• Ensure stable internet connection\n• Contact support if problem persists"

        cardResults.visibility = View.VISIBLE
    }

    private fun setupEnhancedPieChart(counts: Map<String, Int>) {
        val entries = counts.map { (label, count) ->
            PieEntry(count.toFloat(), label)
        }

        val dataSet = PieDataSet(entries, "Detection Results")
        dataSet.colors = counts.map { (label, _) ->
            when (label.lowercase()) {
                "normal" -> Color.parseColor("#28a745")
                "intrusion" -> Color.parseColor("#dc3545")
                "ddos" -> Color.parseColor("#fd7e14")
                "probe" -> Color.parseColor("#ffc107")
                else -> Color.parseColor("#6c757d")
            }
        }
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE
        dataSet.sliceSpace = 2f

        val pieData = PieData(dataSet)

        pieChart.apply {
            data = pieData
            description.isEnabled = false
            legend.isEnabled = true
            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = Legend.LegendOrientation.HORIZONTAL
            isDrawHoleEnabled = true
            holeRadius = 35f
            transparentCircleRadius = 40f
            setUsePercentValues(false)
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(10f)
            animateY(1000)
            invalidate()
        }
    }

    private fun setupThreatCategories(counts: Map<String, Int>) {
        val threatItems = counts.map { (label, count) ->
            val percentage = if (counts.values.sum() > 0) {
                (count * 100) / counts.values.sum()
            } else 0

            ThreatCategoryItem(
                category = label,
                count = count,
                percentage = percentage,
                isNormal = label.lowercase() == "normal"
            )
        }.sortedByDescending { it.count }

        rvThreatCategories.adapter = ThreatCategoryAdapter(threatItems)
    }

    private fun exportResults() {
        Toast.makeText(requireContext(), "Export functionality coming soon!", Toast.LENGTH_SHORT).show()
        // TODO: Implement export to PDF/CSV
    }

    private fun shareResults() {
        val shareText = buildString {
            append("🛡️ Network Security Analysis Results\n\n")
            append("Total Connections: ${tvTotalConnections.text}\n")
            append("Normal Traffic: ${tvNormalTraffic.text}\n")
            append("Threats Detected: ${tvSuspiciousTraffic.text}\n")
            append("Threat Level: ${tvThreatLevel.text}\n\n")
            append("Analysis completed at ${tvAnalysisTime.text}\n")
            append("Generated by DiddySec Network Detection")
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share Results"))
    }

    private fun formatNumber(number: Int): String {
        return when {
            number >= 1000000 -> String.format("%.1fM", number / 1000000.0)
            number >= 1000 -> String.format("%.1fK", number / 1000.0)
            else -> number.toString()
        }
    }

    // ... (keep all existing helper methods: getFileName, getFileSize, formatFileSize, createTempFileFromInputStream)

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result ?: "Unknown file"
    }

    private fun getFileSize(uri: Uri): Long {
        var size = 0L
        try {
            requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            // Handle case where size cannot be determined
        }
        return size
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes bytes"
        }
    }

    private fun createTempFileFromInputStream(inputStream: InputStream?): File {
        if (inputStream == null) throw IOException("InputStream is null")

        val tempFile = File.createTempFile("upload", ".csv", requireContext().cacheDir)
        tempFile.deleteOnExit()

        inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }

    companion object {
        fun newInstance(): UploadFilesFragment {
            return UploadFilesFragment()
        }
    }
}

// Data classes for enhanced UI
data class ThreatCategoryItem(
    val category: String,
    val count: Int,
    val percentage: Int,
    val isNormal: Boolean
)