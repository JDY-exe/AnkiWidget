package com.ankiwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.ankiwidget.config.WidgetConfigActivity
import com.ankiwidget.data.AnkiRepository
import com.ankiwidget.data.WidgetConfig
import com.ankiwidget.renderer.ContributionGridRenderer
import com.ankiwidget.renderer.WidgetConstants
import com.ankiwidget.renderer.WidgetTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main widget provider for Anki review tracking widget
 */
class AnkiWidgetProvider : AppWidgetProvider() {
    
    companion object {
        private const val TAG = "AnkiWidgetProvider"
        private const val ACTION_REFRESH = "com.ankiwidget.ACTION_REFRESH"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} widgets")
        
        // Update each widget instance
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        Log.d(TAG, "Widget enabled")
        // Schedule periodic updates could go here
    }

    override fun onDisabled(context: Context) {
        Log.d(TAG, "Widget disabled")
        // Clean up resources
    }
    
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // Clean up preferences for deleted widgets
        val prefs = context.getSharedPreferences(WidgetConfig.PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        for (appWidgetId in appWidgetIds) {
            editor.remove("${WidgetConfig.KEY_THEME}$appWidgetId")
            editor.remove("${WidgetConfig.KEY_SHOW_STREAK}$appWidgetId")
        }
        
        editor.apply()
    }
    
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        Log.d(TAG, "Widget $appWidgetId resized")
        // Widget was resized - update it with new dimensions
        updateAppWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    /**
     * Update a single widget instance
     */
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        scope.launch {
            try {
                // Load widget configuration
                val config = loadWidgetConfig(context, appWidgetId)
                
                // Get widget dimensions for column calculation
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val widgetWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 300)
                
                // Calculate how many columns fit (always 7 rows)
                // Estimate ~40dp per column (conservative for varying screen densities)
                val estimatedColumnsPerDp = 1 / 40f
                val columns = (widgetWidthDp * estimatedColumnsPerDp).toInt()
                    .coerceAtLeast(WidgetConstants.MIN_COLUMNS)
                
                // Days = columns × 7 rows
                val daysToShow = (columns * WidgetConstants.ROWS).coerceIn(
                    WidgetConstants.MIN_DAYS,
                    WidgetConstants.MAX_DAYS
                )
                
                // Get review data from repository
                val repository = AnkiRepository(context)
                val reviewData = withContext(Dispatchers.IO) {
                    repository.getReviewData(daysToShow, config.selectedDeckId)
                }
                
                // Get theme
                val theme = WidgetTheme.getTheme(context, config.themeName)
                
                // Calculate bitmap dimensions (always 7 rows)
                val rows = WidgetConstants.ROWS
                val bitmapColumns = kotlin.math.ceil(daysToShow / WidgetConstants.ROWS.toFloat()).toInt()
                
                // Use high resolution
                val cellResolution = 150
                val bitmapWidth = bitmapColumns * cellResolution
                val bitmapHeight = rows * cellResolution
                
                // Render contribution grid
                val renderer = ContributionGridRenderer(context, config, theme)
                val gridBitmap = renderer.render(reviewData, bitmapWidth, bitmapHeight)
                
                // Create RemoteViews
                val views = RemoteViews(context.packageName, R.layout.widget_layout)
                views.setImageViewBitmap(R.id.contribution_grid, gridBitmap)
                
                // Set deck name or "All Decks" - hide on narrow widgets (< ~4 home screen columns)
                // Typical home screen column is ~80dp, so 4 columns ≈ 320dp
                if (widgetWidthDp >= 500) {
                    val displayName = config.selectedDeckName ?: "All Decks"
                    views.setTextViewText(R.id.app_name_text, displayName)
                    views.setViewVisibility(R.id.app_name_text, android.view.View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.app_name_text, android.view.View.GONE)
                }
                
                // Set up stats text visibility based on config
                if (config.showStreak) {
                    views.setViewVisibility(R.id.stats_container, android.view.View.VISIBLE)
                    
                    // Calculate and set real streak
                    val streak = renderer.calculateStreak(reviewData)
                    views.setTextViewText(
                        R.id.streak_text,
                        context.getString(R.string.streak_format, streak)
                    )
                } else {
                    views.setViewVisibility(R.id.stats_container, android.view.View.GONE)
                }
                
                // Set up click handler to refresh
                val refreshIntent = Intent(context, AnkiWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_container, refreshPendingIntent)
                
                // Update the widget
                appWidgetManager.updateAppWidget(appWidgetId, views)
                Log.d(TAG, "Widget $appWidgetId updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widget $appWidgetId", e)
            }
        }
    }
    
    /**
     * Load widget configuration from SharedPreferences
     */
    private fun loadWidgetConfig(context: Context, appWidgetId: Int): WidgetConfig {
        val prefs = context.getSharedPreferences(WidgetConfig.PREFS_NAME, Context.MODE_PRIVATE)
        
        val selectedDeckId = if (prefs.contains("${WidgetConfig.KEY_SELECTED_DECK}$appWidgetId")) {
            prefs.getLong("${WidgetConfig.KEY_SELECTED_DECK}$appWidgetId", -1L).let {
                if (it == -1L) null else it
            }
        } else {
            null
        }
        
        val selectedDeckName = prefs.getString("${WidgetConfig.KEY_DECK_NAME}$appWidgetId", null)
        
        return WidgetConfig(
            widgetId = appWidgetId,
            themeName = prefs.getString("${WidgetConfig.KEY_THEME}$appWidgetId", "material_you") ?: "material_you",
            showStreak = prefs.getBoolean("${WidgetConfig.KEY_SHOW_STREAK}$appWidgetId", false),
            selectedDeckId = selectedDeckId,
            selectedDeckName = selectedDeckName
        )
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == ACTION_REFRESH) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
}
