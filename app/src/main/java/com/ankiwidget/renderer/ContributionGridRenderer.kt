package com.ankiwidget.renderer

import android.content.Context
import android.graphics.*
import com.ankiwidget.data.DayReviewStatus
import com.ankiwidget.data.WidgetConfig
import java.time.LocalDate
import kotlin.math.ceil

/**
 * Renders the contribution grid as a bitmap with layout mode support
 */
class ContributionGridRenderer(
    private val context: Context,
    private val config: WidgetConfig,
    private val theme: WidgetTheme
) {

    /**
     * Render the contribution grid to a bitmap
     */
    fun render(reviewData: List<DayReviewStatus>, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val rows = WidgetConstants.ROWS  // Always 7 rows (Mon-Sun)
        val today = LocalDate.now()
        
        // Create a map of dates to review status for quick lookup
        val reviewMap = reviewData.associateBy { it.date }
        
        // Calculate the date range we're displaying
        val oldestDate = reviewData.minByOrNull { it.date }?.date ?: today
        val newestDate = today
        
        // Calculate how many weeks we need to display
        // We need to go from the Monday of the week containing oldestDate
        // to the Sunday of the week containing newestDate (today)
        val startOfFirstWeek = oldestDate.minusDays((oldestDate.dayOfWeek.value - 1).toLong())  // Go back to Monday
        val endOfLastWeek = newestDate.plusDays((7 - newestDate.dayOfWeek.value).toLong())  // Go forward to Sunday
        
        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(startOfFirstWeek, endOfLastWeek).toInt() + 1
        val columns = ceil(totalDays / rows.toFloat()).toInt()
        
        // Calculate optimal cell size to fill the available space
        val availableWidth = width
        val availableHeight = height
        
        val cellWidth = availableWidth / columns
        val cellHeight = availableHeight / rows
        val cellSize = minOf(cellWidth, cellHeight)
        
        // Spacing is proportional to cell size
        val spacing = (cellSize * 0.30f).toInt().coerceAtLeast(2)
        val dotSize = cellSize - spacing
        val radius = dotSize / 2f
        
        // Calculate total grid size
        val totalGridWidth = (dotSize * columns) + (spacing * (columns - 1))
        val totalGridHeight = (dotSize * rows) + (spacing * (rows - 1))
        val startX = (width - totalGridWidth) / 2f
        val startY = (height - totalGridHeight) / 2f
        
        // Create paint for dots
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        
        // Draw grid column by column
        for (col in 0 until columns) {
            for (row in 0 until rows) {
                // Calculate which date this cell represents
                val dayIndex = (col * rows) + row
                val date = startOfFirstWeek.plusDays(dayIndex.toLong())
                
                // Skip if this date is in the future
                if (date.isAfter(newestDate)) {
                    continue
                }
                
                // Get the review status for this date
                val status = reviewMap[date]
                
                // Determine color based on review status
                paint.color = when {
                    status == null -> theme.incompleteColor // Treat no data as incomplete
                    status.allReviewsCompleted -> theme.completedColor
                    else -> theme.incompleteColor
                }
                
                // Calculate center position for circle
                val centerX = startX + (col * (dotSize + spacing)) + radius
                val centerY = startY + (row * (dotSize + spacing)) + radius
                
                // Draw perfect circle
                canvas.drawCircle(centerX, centerY, radius, paint)
            }
        }
        
        return bitmap
    }
    
    /**
     * Calculate the current streak from review data
     */
    fun calculateStreak(reviewData: List<DayReviewStatus>): Int {
        val sortedData = reviewData.sortedByDescending { it.date }
        var streak = 0
        
        for ((index, day) in sortedData.withIndex()) {
            if (day.allReviewsCompleted) {
                streak++
            } else {
                // If the most recent day (today) is incomplete, don't break the streak yet.
                // Just skip it and check if yesterday was complete.
                if (index == 0) {
                    continue
                }
                break
            }
        }
        
        return streak
    }
}
