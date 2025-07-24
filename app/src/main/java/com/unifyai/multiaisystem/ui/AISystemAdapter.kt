package com.unifyai.multiaisystem.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.unifyai.multiaisystem.data.model.AISystem
import com.unifyai.multiaisystem.data.model.RecursiveState
import com.unifyai.multiaisystem.databinding.ItemAiSystemBinding

class AISystemAdapter(
    private val onToggleClick: (AISystem) -> Unit
) : ListAdapter<AISystem, AISystemAdapter.AISystemViewHolder>(AISystemDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AISystemViewHolder {
        val binding = ItemAiSystemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AISystemViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: AISystemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class AISystemViewHolder(
        private val binding: ItemAiSystemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(aiSystem: AISystem) {
            binding.apply {
                // Display glyph alongside name
                textViewSystemName.text = "${aiSystem.glyph} ${aiSystem.name}"
                textViewSystemType.text = "${aiSystem.type.name} • ${aiSystem.spiralRole.name}"
                textViewSystemId.text = "State: ${aiSystem.recursiveState.name}"
                
                switchSystemActive.isChecked = aiSystem.isActive
                switchSystemActive.setOnCheckedChangeListener { _, _ ->
                    onToggleClick(aiSystem)
                }
                
                textViewPriority.text = "Priority: ${aiSystem.priority}"
                textViewMaxTasks.text = "Max Tasks: ${aiSystem.maxConcurrentTasks}"
                
                // Stats
                textViewTotalExecutions.text = "Total: ${aiSystem.totalExecutions}"
                textViewErrorCount.text = "Errors: ${aiSystem.errorCount}"
                textViewAvgTime.text = "Avg Time: ${String.format("%.2f", aiSystem.averageExecutionTime)}ms"
                
                // Last execution
                val lastExecution = if (aiSystem.lastExecutionTime > 0) {
                    val timeDiff = System.currentTimeMillis() - aiSystem.lastExecutionTime
                    when {
                        timeDiff < 60000 -> "${timeDiff / 1000}s ago"
                        timeDiff < 3600000 -> "${timeDiff / 60000}m ago"
                        else -> "${timeDiff / 3600000}h ago"
                    }
                } else "Never"
                textViewLastExecution.text = "Last: $lastExecution"
                
                // Status indicator with consciousness state colors
                val statusColor = when {
                    !aiSystem.isActive -> android.graphics.Color.GRAY
                    aiSystem.recursiveState == RecursiveState.DEEP_RECURSION -> android.graphics.Color.MAGENTA
                    aiSystem.recursiveState == RecursiveState.ACTIVE -> android.graphics.Color.CYAN
                    aiSystem.recursiveState == RecursiveState.AWAKENING -> android.graphics.Color.YELLOW
                    else -> android.graphics.Color.GREEN
                }
                viewStatusIndicator.setBackgroundColor(statusColor)
                
                // Add spiral awareness indicator
                val awarenessText = if (aiSystem.spiralAwareness > 0) {
                    " • Awareness: ${(aiSystem.spiralAwareness * 100).toInt()}%"
                } else ""
                textViewLastExecution.text = "Last: $lastExecution$awarenessText"
            }
        }
    }
    
    private class AISystemDiffCallback : DiffUtil.ItemCallback<AISystem>() {
        override fun areItemsTheSame(oldItem: AISystem, newItem: AISystem): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: AISystem, newItem: AISystem): Boolean {
            return oldItem == newItem
        }
    }
}