package com.ankiwidget.receiver

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ankiwidget.AnkiWidgetProvider

/**
 * Receiver for daily widget updates
 */
class DailyUpdateReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "DailyUpdateReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Daily update triggered: ${intent.action}")
        
        // Update all widget instances
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, AnkiWidgetProvider::class.java)
        )
        
        if (widgetIds.isNotEmpty()) {
            val updateIntent = Intent(context, AnkiWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            }
            context.sendBroadcast(updateIntent)
        }
    }
}
