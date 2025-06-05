package com.example.ddiysec

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DetectionHistoryAdapter(
    private val detectionHistory: List<DetectionRecord>,
    private val onItemClick: (DetectionRecord) -> Unit
) : RecyclerView.Adapter<DetectionHistoryAdapter.DetectionViewHolder>() {

    class DetectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThreatIcon: ImageView = itemView.findViewById(R.id.ivThreatIcon)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        val tvSeverityBadge: TextView = itemView.findViewById(R.id.tvSeverityBadge)
        val tvStatusMessage: TextView = itemView.findViewById(R.id.tvStatusMessage)
        val tvConnectionDetails: TextView = itemView.findViewById(R.id.tvConnectionDetails)
        val layoutAttackType: LinearLayout = itemView.findViewById(R.id.layoutAttackType)
        val tvAttackType: TextView = itemView.findViewById(R.id.tvAttackType)
        val btnViewDetails: Button = itemView.findViewById(R.id.btnViewDetails)
        val layoutExpandedDetails: LinearLayout = itemView.findViewById(R.id.layoutExpandedDetails)
        val tvDetailNormalCount: TextView = itemView.findViewById(R.id.tvDetailNormalCount)
        val tvDetailIntrusionCount: TextView = itemView.findViewById(R.id.tvDetailIntrusionCount)
        val tvDetailPercentage: TextView = itemView.findViewById(R.id.tvDetailPercentage)
        val tvDetailCaptureId: TextView = itemView.findViewById(R.id.tvDetailCaptureId)
        val tvDetailHostname: TextView = itemView.findViewById(R.id.tvDetailHostname)

        var isExpanded = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_detection_history, parent, false)
        return DetectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: DetectionViewHolder, position: Int) {
        val record = detectionHistory[position]
        val ddosPercentage = record.getDDoSPercentage()
        val severity = record.getSeverityLevel()
        val totalConnections = record.getTotalConnections()

        // Format timestamp
        holder.tvTimestamp.text = formatTimestamp(record.timestamp)

        // Set severity badge
        holder.tvSeverityBadge.text = severity
        val badgeColor = record.getSeverityColor()
        holder.tvSeverityBadge.setBackgroundColor(badgeColor)

        // Set threat icon and color
        if (ddosPercentage >= 20) {
            holder.ivThreatIcon.setImageResource(R.drawable.ic_exclamation_triangle)
            holder.ivThreatIcon.setColorFilter(Color.parseColor("#dc3545"))
        } else {
            holder.ivThreatIcon.setImageResource(R.drawable.ic_shield_check)
            holder.ivThreatIcon.setColorFilter(Color.parseColor("#28a745"))
        }

        // Set status message
        if (ddosPercentage >= 20) {
            holder.tvStatusMessage.text = "$ddosPercentage% DDoS traffic detected"
            holder.tvStatusMessage.setTextColor(Color.parseColor("#dc3545"))
        } else {
            holder.tvStatusMessage.text = "Safe - $ddosPercentage% DDoS traffic"
            holder.tvStatusMessage.setTextColor(Color.parseColor("#28a745"))
        }

        // Set connection details
        val numberFormatter = NumberFormat.getNumberInstance()
        if (ddosPercentage >= 20) {
            holder.tvConnectionDetails.text = "${numberFormatter.format(record.intrusion_count)} suspicious / ${numberFormatter.format(totalConnections)} total connections"
        } else {
            holder.tvConnectionDetails.text = "${numberFormatter.format(totalConnections)} connections analyzed"
        }

        // Show/hide attack type
        if (ddosPercentage > 0 && !record.attack_type.isNullOrEmpty() &&
            record.attack_type != "Unknown" && record.attack_type != "Mixed / Generic DDoS") {
            holder.layoutAttackType.visibility = View.VISIBLE
            holder.tvAttackType.text = record.attack_type
        } else {
            holder.layoutAttackType.visibility = View.GONE
        }

        // Setup expanded details
        holder.tvDetailNormalCount.text = numberFormatter.format(record.normal_count)
        holder.tvDetailIntrusionCount.text = numberFormatter.format(record.intrusion_count)
        holder.tvDetailPercentage.text = "$ddosPercentage%"
        holder.tvDetailCaptureId.text = record.capture_id.take(8) + "..."
        holder.tvDetailHostname.text = record.hostname

        // Set percentage color
        holder.tvDetailPercentage.setTextColor(
            when {
                ddosPercentage < 20 -> Color.parseColor("#28a745")
                ddosPercentage < 50 -> Color.parseColor("#ffc107")
                else -> Color.parseColor("#dc3545")
            }
        )

        holder.btnViewDetails.text = if (holder.isExpanded) "Hide Details" else "View Details"
        holder.layoutExpandedDetails.visibility = if (holder.isExpanded) View.VISIBLE else View.GONE

        holder.btnViewDetails.setOnClickListener {
            holder.isExpanded = !holder.isExpanded
            holder.btnViewDetails.text = if (holder.isExpanded) "Hide Details" else "View Details"
            holder.layoutExpandedDetails.visibility = if (holder.isExpanded) View.VISIBLE else View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick(record)
        }

        if (ddosPercentage >= 20) {
            holder.itemView.setBackgroundResource(R.drawable.item_danger_background)
        } else {
            holder.itemView.setBackgroundResource(R.drawable.item_safe_background)
        }
    }

    override fun getItemCount(): Int = detectionHistory.size

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

            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).parse(timestamp)

            // Format to Jakarta time
            val outputFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getTimeZone("Asia/Jakarta")

            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            "Invalid date"
        }
    }
}