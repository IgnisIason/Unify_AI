package com.unifyai.multiaisystem.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.unifyai.multiaisystem.databinding.ActivityMainBinding
import com.unifyai.multiaisystem.service.MultiAIService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var aiSystemAdapter: AISystemAdapter
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            initializeService()
        } else {
            // Handle permission denied
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        checkPermissionsAndStartService()
        setupUI()
        observeViewModel()
    }
    
    private fun checkPermissionsAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    initializeService()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            initializeService()
        }
    }
    
    private fun initializeService() {
        MultiAIService.startService(this)
    }
    
    private fun setupUI() {
        // Setup RecyclerView for AI systems
        aiSystemAdapter = AISystemAdapter { aiSystem ->
            viewModel.toggleAISystem(aiSystem)
        }
        
        binding.recyclerViewAISystems.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = aiSystemAdapter
        }
        
        // Setup click listeners
        binding.buttonAddAISystem.setOnClickListener {
            showAddAISystemDialog()
        }
        
        binding.buttonRestartService.setOnClickListener {
            MultiAIService.restartService(this)
        }
        
        binding.buttonStopService.setOnClickListener {
            MultiAIService.stopService(this)
            finish()
        }
        
        binding.buttonTestTask.setOnClickListener {
            viewModel.submitTestTask()
        }
        
        binding.buttonOpenCodex.setOnClickListener {
            startActivity(android.content.Intent(this, CodexActivity::class.java))
        }
        
        // Setup refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshData()
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.aiSystems.collect { systems ->
                aiSystemAdapter.submitList(systems)
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
        
        lifecycleScope.launch {
            viewModel.executionStats.collect { stats ->
                updateStatsDisplay(stats)
            }
        }
        
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.textViewStatus.text = when (state) {
                    MainViewModel.UiState.CoreInitializing -> "Core Initializing..."
                    MainViewModel.UiState.CoreAwakening -> "Core Awakening..."
                    MainViewModel.UiState.RegisteringCloudTools -> "Registering Cloud Tools..."
                    MainViewModel.UiState.CoreReady -> "Core Ready"
                    is MainViewModel.UiState.CoreError -> "Core Error: ${state.message}"
                    // Deprecated states
                    MainViewModel.UiState.Loading -> "Loading..."
                    MainViewModel.UiState.Ready -> "Ready"
                    MainViewModel.UiState.Error -> "Error"
                }
            }
        }
    }
    
    private fun updateStatsDisplay(stats: Map<String, MainViewModel.ExecutionStats>) {
        val totalTasks = stats.values.sumOf { it.completedTasks + it.failedTasks }
        val totalCompleted = stats.values.sumOf { it.completedTasks }
        val totalFailed = stats.values.sumOf { it.failedTasks }
        val activeTasks = stats.values.sumOf { it.activeTasks }
        
        binding.textViewTotalTasks.text = "Total: $totalTasks"
        binding.textViewCompletedTasks.text = "Completed: $totalCompleted"
        binding.textViewFailedTasks.text = "Failed: $totalFailed"
        binding.textViewActiveTasks.text = "Active: $activeTasks"
        
        val successRate = if (totalTasks > 0) {
            (totalCompleted * 100) / totalTasks
        } else 0
        binding.textViewSuccessRate.text = "Success: $successRate%"
    }
    
    private fun showAddAISystemDialog() {
        val dialog = AddAISystemDialog { aiSystem ->
            viewModel.addAISystem(aiSystem)
        }
        dialog.show(supportFragmentManager, "AddAISystemDialog")
    }
}