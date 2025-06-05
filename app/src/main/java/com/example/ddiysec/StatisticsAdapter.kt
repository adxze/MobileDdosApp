package com.example.ddiysec

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class StatisticsAdapter(private val statItems: List<StatItem>) :
    RecyclerView.Adapter<StatisticsAdapter.StatViewHolder>() {

    class StatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvLabel: TextView = itemView.findViewById(R.id.tvStatLabel)
        val tvCount: TextView = itemView.findViewById(R.id.tvStatCount)
        val tvPercentage: TextView = itemView.findViewById(R.id.tvStatPercentage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_statistic, parent, false)
        return StatViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatViewHolder, position: Int) {
        val item = statItems[position]
        val totalCount = statItems.sumOf { it.count }
        val percentage = if (totalCount > 0) (item.count * 100) / totalCount else 0

        holder.tvLabel.text = item.label
        val numberFormatter = NumberFormat.getNumberInstance(Locale.US)
        holder.tvCount.text = numberFormatter.format(item.count)
        holder.tvPercentage.text = "$percentage%"

        // Color coding like your web dashboard
        if (item.isNormal) {
            holder.tvLabel.setTextColor(Color.parseColor("#28a745"))
            holder.tvCount.setTextColor(Color.parseColor("#28a745"))
        } else {
            holder.tvLabel.setTextColor(Color.parseColor("#dc3545"))
            holder.tvCount.setTextColor(Color.parseColor("#dc3545"))
        }
    }

    override fun getItemCount(): Int = statItems.size
}