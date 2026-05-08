package com.leonjose.tvbooster

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private val apps: MutableList<AppInfo>,
    private val onCheckedChange: (AppInfo, Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLabel: TextView = view.findViewById(R.id.tvAppLabel)
        val tvPackage: TextView = view.findViewById(R.id.tvAppPackage)
        val tvType: TextView = view.findViewById(R.id.tvAppType)
        val checkbox: CheckBox = view.findViewById(R.id.cbSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.tvLabel.text = app.label
        holder.tvPackage.text = app.packageName
        holder.tvType.text = if (app.isSystem) "SYSTEM" else "USER"
        holder.tvType.setTextColor(
            if (app.isSystem) Color.parseColor("#FFA726") else Color.parseColor("#66BB6A")
        )
        holder.checkbox.isChecked = app.isSelected
        holder.checkbox.setOnCheckedChangeListener(null)
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            app.isSelected = isChecked
            onCheckedChange(app, isChecked)
        }
        holder.itemView.setOnClickListener {
            holder.checkbox.isChecked = !holder.checkbox.isChecked
        }
        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            v.alpha = if (hasFocus) 1.0f else 0.85f
            v.scaleX = if (hasFocus) 1.02f else 1.0f
            v.scaleY = if (hasFocus) 1.02f else 1.0f
        }
    }

    override fun getItemCount() = apps.size

    fun updateList(newList: List<AppInfo>) {
        apps.clear()
        apps.addAll(newList)
        notifyDataSetChanged()
    }

    fun getAppList(): List<AppInfo> = apps
}
