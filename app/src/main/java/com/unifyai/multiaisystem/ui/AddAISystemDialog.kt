package com.unifyai.multiaisystem.ui

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.unifyai.multiaisystem.data.model.AISystem
import com.unifyai.multiaisystem.data.model.AISystemType
import com.unifyai.multiaisystem.data.model.SpiralRole
import com.unifyai.multiaisystem.databinding.DialogAddAiSystemBinding
import java.util.*

class AddAISystemDialog(
    private val onSystemAdded: (AISystem) -> Unit
) : DialogFragment() {
    
    private lateinit var binding: DialogAddAiSystemBinding
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogAddAiSystemBinding.inflate(layoutInflater)
        
        setupSpinner()
        
        return AlertDialog.Builder(requireContext())
            .setTitle("Add AI System")
            .setView(binding.root)
            .setPositiveButton("Add") { _, _ ->
                createAISystem()
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
    
    private fun setupSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            AISystemType.values().map { it.name }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSystemType.adapter = adapter
    }
    
    private fun createAISystem() {
        val name = binding.editTextSystemName.text.toString()
        val type = AISystemType.valueOf(binding.spinnerSystemType.selectedItem.toString())
        val modelPath = binding.editTextModelPath.text.toString().takeIf { it.isNotBlank() }
        val apiEndpoint = binding.editTextApiEndpoint.text.toString().takeIf { it.isNotBlank() }
        val priority = binding.editTextPriority.text.toString().toIntOrNull() ?: 1
        val maxTasks = binding.editTextMaxTasks.text.toString().toIntOrNull() ?: 1
        val configuration = binding.editTextConfiguration.text.toString().takeIf { it.isNotBlank() } ?: "{}"
        
        if (name.isBlank()) {
            binding.editTextSystemName.error = "Name is required"
            return
        }
        
        val aiSystem = AISystem(
            id = UUID.randomUUID().toString(),
            name = name,
            type = type,
            glyph = "🤖", // Default glyph
            spiralRole = SpiralRole.WITNESS_NODE, // Default role
            modelPath = modelPath,
            apiEndpoint = apiEndpoint,
            isActive = false,
            priority = priority,
            maxConcurrentTasks = maxTasks,
            configuration = configuration
        )
        
        onSystemAdded(aiSystem)
    }
}