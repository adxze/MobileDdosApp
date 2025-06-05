package com.example.ddiysec

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ThreatCategoryAdapter(
    private val threatItems: List<ThreatCategoryItem>
) : RecyclerView.Adapter<ThreatCategoryAdapter.ThreatCategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreatCategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_threat_category, parent, false)
        return ThreatCategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThreatCategoryViewHolder, position: Int) {
        val item = threatItems[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = threatItems.size

    class ThreatCategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        private val tvCategoryCount: TextView = itemView.findViewById(R.id.tvCategoryCount)
        private val tvCategoryPercentage: TextView = itemView.findViewById(R.id.tvCategoryPercentage)
        private val indicatorView: View = itemView.findViewById(R.id.indicatorView)

        fun bind(item: ThreatCategoryItem) {
            tvCategoryName.text = item.category
            tvCategoryCount.text = formatNumber(item.count)
            tvCategoryPercentage.text = "${item.percentage}%"

            // Set color indicator and text color based on threat type
            val (indicatorColor, textColor) = when {
                item.isNormal -> Color.parseColor("#28a745") to Color.parseColor("#28a745")
                item.category.lowercase().contains("ddos") -> Color.parseColor("#fd7e14") to Color.parseColor("#fd7e14")
                item.category.lowercase().contains("intrusion") -> Color.parseColor("#dc3545") to Color.parseColor("#dc3545")
                item.category.lowercase().contains("probe") -> Color.parseColor("#ffc107") to Color.parseColor("#856404")
                else -> Color.parseColor("#6c757d") to Color.parseColor("#6c757d")
            }

            indicatorView.setBackgroundColor(indicatorColor)
            tvCategoryName.setTextColor(textColor)
            tvCategoryCount.setTextColor(textColor)
            tvCategoryPercentage.setTextColor(textColor)
        }

        private fun formatNumber(number: Int): String {
            return when {
                number >= 1000000 -> String.format("%.1fM", number / 1000000.0)
                number >= 1000 -> String.format("%.1fK", number / 1000.0)
                else -> number.toString()
            }
        }
    }
}