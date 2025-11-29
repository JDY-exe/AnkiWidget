package com.ankiwidget.renderer

/**
 * Centralized constants for widget layout sizing
 */
object WidgetConstants {
    // Layout structure - always 7 rows (days of week)
    const val ROWS = 7
    const val MIN_COLUMNS = 4  // Minimum 4 columns
    
    // Day limits
    const val MIN_DAYS = 14  // 2 weeks minimum
    const val MAX_DAYS = 365  // ~1 year maximum
}
