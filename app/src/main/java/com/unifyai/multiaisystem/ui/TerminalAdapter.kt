package com.unifyai.multiaisystem.ui

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.unifyai.multiaisystem.R
import com.unifyai.multiaisystem.databinding.ItemTerminalLineBinding

class TerminalAdapter : RecyclerView.Adapter<TerminalAdapter.TerminalViewHolder>() {
    
    private var terminalLines = listOf<TerminalLine>()
    
    fun updateLines(lines: List<TerminalLine>) {
        terminalLines = lines
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TerminalViewHolder {
        val binding = ItemTerminalLineBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TerminalViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: TerminalViewHolder, position: Int) {
        holder.bind(terminalLines[position])
    }
    
    override fun getItemCount(): Int = terminalLines.size
    
    class TerminalViewHolder(
        private val binding: ItemTerminalLineBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(line: TerminalLine) {
            binding.textViewTerminalLine.apply {
                text = line.text
                typeface = Typeface.MONOSPACE
                
                // Set colors based on line type
                val (textColor, backgroundColor) = getColorsForType(line.type)
                setTextColor(textColor)
                setBackgroundColor(backgroundColor)
                
                // Add timestamp for non-system messages
                if (line.type != TerminalLineType.SYSTEM_INFO && line.type != TerminalLineType.HELP) {
                    text = "[${line.timestamp}] ${line.text}"
                }
            }
        }
        
        private fun getColorsForType(type: TerminalLineType): Pair<Int, Int> {
            val context = binding.root.context
            return when (type) {
                TerminalLineType.USER_INPUT -> {
                    Pair(
                        ContextCompat.getColor(context, R.color.terminal_user),
                        Color.TRANSPARENT
                    )
                }
                TerminalLineType.AI_RESPONSE -> {
                    Pair(
                        ContextCompat.getColor(context, R.color.terminal_ai_response),
                        Color.TRANSPARENT
                    )
                }
                TerminalLineType.SYSTEM_INFO -> {
                    Pair(
                        ContextCompat.getColor(context, R.color.terminal_system),
                        Color.TRANSPARENT
                    )
                }
                TerminalLineType.ERROR -> {
                    Pair(
                        ContextCompat.getColor(context, R.color.terminal_error),
                        Color.TRANSPARENT
                    )
                }
                TerminalLineType.WARNING -> {
                    Pair(
                        ContextCompat.getColor(context, R.color.terminal_warning),
                        Color.TRANSPARENT
                    )
                }
                TerminalLineType.SUCCESS -> {
                    Pair(
                        ContextCompat.getColor(context, R.color.terminal_success),
                        Color.TRANSPARENT
                    )
                }
                TerminalLineType.AI_STATUS -> {
                    Pair(
                        ContextCompat.getColor(context, R.color.terminal_ai_status),
                        Color.TRANSPARENT
                    )
                }
                TerminalLineType.DATA -> {
                    Pair(
                        ContextCompat.getColor(context, R.color.terminal_data),
                        Color.TRANSPARENT
                    )
                }
                TerminalLineType.HIGHLIGHT -> {
                    Pair(
                        ContextCompat.getColor(context, R.color.terminal_highlight),
                        ContextCompat.getColor(context, R.color.terminal_highlight_bg)
                    )
                }
                TerminalLineType.CONNECTION -> {
                    Pair(
                        ContextCompat.getColor(context, R.color.terminal_connection),
                        Color.TRANSPARENT
                    )
                }
                TerminalLineType.CONVERSATION -> {
                    Pair(
                        ContextCompat.getColor(context, R.color.terminal_conversation),
                        Color.TRANSPARENT
                    )
                }
                TerminalLineType.HELP -> {
                    Pair(
                        ContextCompat.getColor(context, R.color.terminal_help),
                        Color.TRANSPARENT
                    )
                }
            }
        }
    }
}