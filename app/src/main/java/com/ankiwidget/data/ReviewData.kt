package com.ankiwidget.data

import java.time.LocalDate

/**
 * Represents the review completion status for a single day
 */
data class DayReviewStatus(
    val date: LocalDate,
    val allReviewsCompleted: Boolean,
    val reviewCount: Int = 0,
    val newCardCount: Int = 0
)

/**
 * Widget configuration preferences
 */
data class WidgetConfig(
    val widgetId: Int,
    val themeName: String = "material_you",
    val showStreak: Boolean = false,
    val selectedDeckId: Long? = null,  // null = track all decks
    val selectedDeckName: String? = null  // null = "All Decks"
) {
    companion object {
        const val PREFS_NAME = "AnkiWidgetPrefs"
        const val KEY_THEME = "theme_"
        const val KEY_SHOW_STREAK = "show_streak_"
        const val KEY_SELECTED_DECK = "selected_deck_"
        const val KEY_DECK_NAME = "deck_name_"
    }
}
