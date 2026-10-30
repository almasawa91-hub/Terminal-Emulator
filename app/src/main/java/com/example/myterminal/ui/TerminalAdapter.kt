package com.example.myterminal.ui

import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myterminal.model.LogEntry
import com.example.myterminal.model.LogType

class TerminalAdapter : RecyclerView.Adapter<TerminalAdapter.ViewHolder>() {
    private val logs = mutableListOf<LogEntry>()
    class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = TextView(parent.context).apply {
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setPadding(8, 4, 8, 4)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = logs[position]
        holder.textView.text = entry.text
        holder.textView.setTextColor(when (entry.type) {
            LogType.COMMAND -> Color.rgb(129, 199, 132)
            LogType.OUTPUT -> Color.WHITE
            LogType.ERROR -> Color.rgb(255, 90, 90)
        })
    }

    override fun getItemCount() = logs.size
    fun addLog(entry: LogEntry) {
        logs.add(entry)
        notifyItemInserted(logs.lastIndex)
    }
    fun clear() {
        val oldSize = logs.size
        logs.clear()
        if (oldSize > 0) notifyItemRangeRemoved(0, oldSize)
    }
}
