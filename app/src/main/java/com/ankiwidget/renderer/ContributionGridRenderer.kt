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
        
        val daysToShow = reviewData.size
        val rows = WidgetConstants.ROWS  // Always 7 rows (Mon-Sun)
        
        // Get today's info
        val today = LocalDate.now()
        val todayDayOfWeek = today.dayOfWeek.value  // 1=Monday, 7=Sunday
        
        // Calculate how many empty cells we need at the start to align with calendar week
        // If today is Monday (1), we need 0 empty cells (today goes at top)
        // If today is Tuesday (2), we need 1 empty cell (Monday empty, Tuesday at top)
        // If today is Sunday (7), we need 6 empty cells (Mon-Sat empty, Sunday at top)
        val emptyCellsInFirstColumn = todayDayOfWeek - 1
        
        // Total cells we need to display = empty cells + actual data
        val totalCells = emptyCellsInFirstColumn + daysToShow
        val columns = ceil(totalCells / rows.toFloat()).toInt()
        
        // Calculate optimal cell size to fill the available space
        val availableWidth = width
        val availableHeight = height
        
        // Calculate cell size based on available space
        val cellWidth = availableWidth / columns
        val cellHeight = availableHeight / rows
        val cellSize = minOf(cellWidth, cellHeight)
        
        // Spacing is proportional to cell size (increased for smaller dots)
        val spacing = (cellSize * 0.30f).toInt().coerceAtLeast(2)
        val dotSize = cellSize - spacing
        val radius = dotSize / 2f // Radius for perfect circles
        
        // Calculate total grid size with new dimensions
        val totalGridWidth = (dotSize * columns) + (spacing * (columns - 1))
        val totalGridHeight = (dotSize * rows) + (spacing * (rows - 1))
        val startX = (width - totalGridWidth) / 2f
        val startY = (height - totalGridHeight) / 2f
        
        // Create paint for dots
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        
        // Create a map of dates to review status for quick lookup
        val reviewMap = reviewData.associateBy { it.date }
        
        // Draw grid column by column (oldest to newest)
        for (col in 0 until columns) {
            for (row in 0 until rows) {
                val cellIndex = (col * rows) + row
                
                // Skip empty cells at the beginning
                if (cellIndex < emptyCellsInFirstColumn) {
                    continue
                }
                
                // Calculate which day this cell represents
                val dayIndex = cellIndex - emptyCellsInFirstColumn
                if (dayIndex >= daysToShow) break
                
                // Calculate the date for this cell (counting backwards from today)
                val date = today.minusDays((daysToShow - 1 - dayIndex).toLong())
                
                // Determine color based on review status
                val status = reviewMap[date]
                paint.color = when {
                    status == null -> theme.noDataColor
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
        
        for (day in sortedData) {
            if (day.allReviewsCompleted) {
                streak++
            } else {
                break
            }
        }
        
        return streak
    }
}
