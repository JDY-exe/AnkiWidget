package com.ankiwidget.config

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ankiwidget.AnkiWidgetProvider
import com.ankiwidget.R
import com.ankiwidget.data.AnkiRepository
import com.ankiwidget.data.WidgetConfig
import com.ankiwidget.databinding.ConfigLayoutBinding
import com.ankiwidget.renderer.ContributionGridRenderer
import com.ankiwidget.renderer.WidgetConstants
import com.ankiwidget.renderer.WidgetTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Configuration activity shown when adding the widget
 */
class WidgetConfigActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "WidgetConfigActivity"
    }
    
    private lateinit var binding: ConfigLayoutBinding
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var availableDecks = listOf<Pair<Long, String>>()
    private var selectedDeckId: Long? = null
    private var selectedDeckName: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
       // Set result to CANCELED initially
        setResult(RESULT_CANCELED)
        
        // Get widget ID from intent
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "Invalid widget ID")
            finish()
            return
        }
        
        // Setup view binding
        binding = ConfigLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        loadAvailableDecks()
        setupListeners()
        updatePreview()
    }
    
    private fun loadAvailableDecks() {
        scope.launch(Dispatchers.IO) {
            try {
                val repository = AnkiRepository(this@WidgetConfigActivity)
                availableDecks = repository.getAvailableDecks()
                
                withContext(Dispatchers.Main) {
                    setupDeckSpinner()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading decks", e)
            }
        }
    }
    
    private fun setupDeckSpinner() {
        val deckNames = listOf("All Decks") + availableDecks.map { it.second }
        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            deckNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.deckSelector.adapter = adapter
        
        binding.deckSelector.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position == 0) {
                    selectedDeckId = null
                    selectedDeckName = null
                } else {
                    selectedDeckId = availableDecks[position - 1].first
                    selectedDeckName = availableDecks[position - 1].second
                }
                Log.d(TAG, "Deck selected: ${deckNames[position]} (ID=$selectedDeckId, Name=$selectedDeckName)")
                updatePreview()
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        
        Log.d(TAG, "Deck spinner populated with ${deckNames.size} options")
    }
    
    private fun setupListeners() {
        // Theme selection
        binding.themeGroup.setOnCheckedChangeListener { _, _ ->
            updatePreview()
        }
        
        // Show streak toggle
        binding.showStreakSwitch.setOnCheckedChangeListener { _, _ ->
            updatePreview()
        }

        // Frosted glass toggle
        binding.frostedGlassSwitch.setOnCheckedChangeListener { _, _ ->
            updatePreview()
        }
        
        // Save button
        binding.saveButton.setOnClickListener {
            saveConfiguration()
        }
        
        // Cancel button
        binding.cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }
    
    /**
     * Update the live preview
     */
    private fun updatePreview() {
        scope.launch {
            try {
                val config = getCurrentConfig()
                val theme = WidgetTheme.getTheme(this@WidgetConfigActivity, config.themeName)
                val repository = AnkiRepository(this@WidgetConfigActivity)
                
                // Calculate days for preview (use 8 columns as medium size)
                val dummyColumns = 8
                val daysToShow = (dummyColumns * WidgetConstants.ROWS).coerceIn(
                    WidgetConstants.MIN_DAYS,
                    WidgetConstants.MAX_DAYS
                )
                
                withContext(Dispatchers.IO) {
                    val reviewData = repository.getReviewData(daysToShow, selectedDeckId)
                    
                    // Calculate bitmap dimensions (always 7 rows)
                    val columns = kotlin.math.ceil(daysToShow / WidgetConstants.ROWS.toFloat()).toInt()
                    val rows = WidgetConstants.ROWS
                    
                    val cellResolution = 150
                    val bitmapWidth = columns * cellResolution
                    val bitmapHeight = rows * cellResolution
                    
                    val renderer = ContributionGridRenderer(
                        this@WidgetConfigActivity,
                        config,
                        theme
                    )
                    val bitmap = renderer.render(reviewData, bitmapWidth, bitmapHeight)
                    
                    withContext(Dispatchers.Main) {
                        binding.previewImage.setImageBitmap(bitmap)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error updating preview", e)
            }
        }
    }
    
    /**
     * Get current configuration from UI
     */
    private fun getCurrentConfig(): WidgetConfig {
        val themeName = when (binding.themeGroup.checkedRadioButtonId) {
            R.id.theme_github -> WidgetTheme.THEME_GITHUB
            R.id.theme_monochrome -> WidgetTheme.THEME_MONOCHROME
            else -> WidgetTheme.THEME_MATERIAL_YOU
        }
        
        return WidgetConfig(
            widgetId = appWidgetId,
            themeName = themeName,
            showStreak = binding.showStreakSwitch.isChecked,
            selectedDeckId = selectedDeckId,
            selectedDeckName = selectedDeckName,
            dayStartHour = 4, // Default to 4AM for now
            isFrosted = binding.frostedGlassSwitch.isChecked
        )
    }
    
    /**
     * Save configuration and create widget
     */
    private fun saveConfiguration() {
        val config = getCurrentConfig()
        
        // Save to SharedPreferences
        val prefs = getSharedPreferences(WidgetConfig.PREFS_NAME, MODE_PRIVATE)
        prefs.edit().apply {
            putString("${WidgetConfig.KEY_THEME}$appWidgetId", config.themeName)
            putBoolean("${WidgetConfig.KEY_SHOW_STREAK}$appWidgetId", config.showStreak)
            if (config.selectedDeckId != null) {
                putLong("${WidgetConfig.KEY_SELECTED_DECK}$appWidgetId", config.selectedDeckId)
            } else {
                remove("${WidgetConfig.KEY_SELECTED_DECK}$appWidgetId")
            }
            if (config.selectedDeckName != null) {
                putString("${WidgetConfig.KEY_DECK_NAME}$appWidgetId", config.selectedDeckName)
            } else {
                remove("${WidgetConfig.KEY_DECK_NAME}$appWidgetId")
            }
            putInt("${WidgetConfig.KEY_DAY_START}$appWidgetId", config.dayStartHour)
            putBoolean("${WidgetConfig.KEY_IS_FROSTED}$appWidgetId", config.isFrosted)
            apply()
        }
        
        Log.d(TAG, "Saved configuration: theme=${config.themeName}, streak=${config.showStreak}, deck=${config.selectedDeckId}")
        
        // Update the widget
        val appWidgetManager = AppWidgetManager.getInstance(this)
        AnkiWidgetProvider().onUpdate(
            this,
            appWidgetManager,
            intArrayOf(appWidgetId)
        )
        
        // Return success
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cancel any pending coroutines
    }
}
