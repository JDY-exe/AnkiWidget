package com.ankiwidget.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "daily_progress", primaryKeys = ["date", "deckId"])
data class DailyProgress(
    val date: LocalDate,
    val deckId: Long, // -1 for all decks, or specific deck ID
    val isComplete: Boolean
)
