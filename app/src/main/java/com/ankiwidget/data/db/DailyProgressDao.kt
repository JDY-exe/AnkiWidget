package com.ankiwidget.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.time.LocalDate

@Dao
interface DailyProgressDao {
    @Query("SELECT * FROM daily_progress WHERE date BETWEEN :startDate AND :endDate AND deckId = :deckId")
    suspend fun getHistory(startDate: LocalDate, endDate: LocalDate, deckId: Long): List<DailyProgress>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: DailyProgress)
    
    @Query("DELETE FROM daily_progress")
    suspend fun clearAll()
}
