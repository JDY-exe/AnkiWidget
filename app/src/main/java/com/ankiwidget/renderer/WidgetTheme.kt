package com.ankiwidget.renderer

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import androidx.annotation.ColorInt

/**
 * Theme configuration for the widget with Material You support
 */
data class WidgetTheme(
    @ColorInt val completedColor: Int,
    @ColorInt val incompleteColor: Int,
    @ColorInt val noDataColor: Int,
    @ColorInt val backgroundColor: Int,
    val themeName: String
) {
    companion object {
        const val THEME_MATERIAL_YOU = "material_you"
        const val THEME_GITHUB = "github"
        const val THEME_MONOCHROME = "monochrome"
        const val THEME_CUSTOM = "custom"

        /**
         * Get theme based on preference
         */
        fun getTheme(
            context: Context, 
            themeName: String,
            customCompleted: Int? = null,
            customIncomplete: Int? = null,
            customBackground: Int? = null,
            customStreak: Int? = null
        ): WidgetTheme {
            return when (themeName) {
                THEME_MATERIAL_YOU -> getMaterialYouTheme(context)
                THEME_GITHUB -> getGitHubTheme(context)
                THEME_MONOCHROME -> getMonochromeTheme(context)
                THEME_CUSTOM -> getCustomTheme(context, customCompleted, customIncomplete, customBackground, customStreak)
                else -> getMaterialYouTheme(context)
            }
        }

        /**
         * Detect if system is in dark mode
         */
        private fun isDarkMode(context: Context): Boolean {
            return (context.resources.configuration.uiMode and 
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }

        /**
         * Material You dynamic theme - extracts colors from system
         */
        private fun getMaterialYouTheme(context: Context): WidgetTheme {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ - Use dynamic colors with dark mode detection
                val isDark = isDarkMode(context)
                
                val colorPrimary = if (isDark) {
                    context.getColor(android.R.color.system_accent1_200) // Lighter for dark mode
                } else {
                    context.getColor(android.R.color.system_accent1_600) // Darker for light mode
                }
                
                val colorOnSurface = if (isDark) {
                    context.getColor(android.R.color.system_neutral1_50) // Light text
                } else {
                    context.getColor(android.R.color.system_neutral1_900) // Dark text
                }
                
                val colorSurface = if (isDark) {
                    context.getColor(android.R.color.system_neutral1_900) // Dark background
                } else {
                    context.getColor(android.R.color.system_neutral1_100) // Light background
                }
                
                val colorIncomplete = if (isDark) {
                    context.getColor(android.R.color.system_accent2_700) // Darker variant for incomplete
                } else {
                    context.getColor(android.R.color.system_accent2_100) // Lighter variant for incomplete
                }

                val colorNoData = if (isDark) {
                    context.getColor(android.R.color.system_neutral2_800) // Dark neutral for empty
                } else {
                    context.getColor(android.R.color.system_neutral2_100) // Light neutral for empty
                }
                
                WidgetTheme(
                    completedColor = colorPrimary,
                    incompleteColor = colorIncomplete,
                    noDataColor = colorNoData,
                    backgroundColor = adjustAlpha(colorSurface, 0.7f),
                    themeName = THEME_MATERIAL_YOU
                )
            } else {
                // Fallback to GitHub theme for older versions
                getGitHubTheme(context)
            }
        }

        /**
         * GitHub green theme - classic contribution graph look
         */
        private fun getGitHubTheme(context: Context): WidgetTheme {
            val isDark = isDarkMode(context)
            
            return if (isDark) {
                // Dark mode - GitHub's dark theme colors
                WidgetTheme(
                    completedColor = Color.parseColor("#39D353"), // GitHub green
                    incompleteColor = Color.parseColor("#0E4429"), // Dark green
                    noDataColor = Color.parseColor("#161B22"), // Almost black
                    backgroundColor = Color.parseColor("#0D1117"), // GitHub dark background
                    themeName = THEME_GITHUB
                )
            } else {
                // Light mode - GitHub's light theme colors
                WidgetTheme(
                    completedColor = Color.parseColor("#216E39"), // Darker green for light bg
                    incompleteColor = Color.parseColor("#C6E6C3"), // Light green
                    noDataColor = Color.parseColor("#EBEDF0"), // Light gray
                    backgroundColor = Color.parseColor("#FFFFFF"), // White background
                    themeName = THEME_GITHUB
                )
            }
        }

        /**
         * Monochrome theme - minimal grayscale design
         */
        private fun getMonochromeTheme(context: Context): WidgetTheme {
            val isDark = isDarkMode(context)
            
            return if (isDark) {
                // Dark mode - inverted grayscale
                WidgetTheme(
                    completedColor = Color.parseColor("#CCCCCC"), // Light gray
                    incompleteColor = Color.parseColor("#555555"), // Mid gray
                    noDataColor = Color.parseColor("#1F1F1F"), // Very dark gray
                    backgroundColor = Color.parseColor("#0A0A0A"), // Almost black
                    themeName = THEME_MONOCHROME
                )
            } else {
                // Light mode - original grayscale
                WidgetTheme(
                    completedColor = Color.parseColor("#333333"), // Dark gray
                    incompleteColor = Color.parseColor("#AAAAAA"), // Mid gray
                    noDataColor = Color.parseColor("#E0E0E0"), // Light gray
                    backgroundColor = Color.parseColor("#F5F5F5"), // Off-white
                    themeName = THEME_MONOCHROME
                )
            }
        }

        /**
         * Custom theme with user-defined colors
         */
        private fun getCustomTheme(
            context: Context,
            customCompleted: Int?,
            customIncomplete: Int?,
            customBackground: Int?,
            customStreak: Int?
        ): WidgetTheme {
            // Default to Material You colors if custom ones aren't provided
            val defaultTheme = getMaterialYouTheme(context)
            
            return WidgetTheme(
                completedColor = customCompleted ?: defaultTheme.completedColor,
                incompleteColor = customIncomplete ?: defaultTheme.incompleteColor,
                noDataColor = customIncomplete ?: defaultTheme.noDataColor, // Use incomplete color for no data
                backgroundColor = customBackground ?: defaultTheme.backgroundColor,
                themeName = THEME_CUSTOM
            )
        }

        /**
         * Helper to adjust alpha of a color
         */
        @ColorInt
        private fun adjustAlpha(@ColorInt color: Int, alphaMod: Float): Int {
            val alpha = (Color.alpha(color) * alphaMod).toInt().coerceIn(0, 255)
            val red = Color.red(color)
            val green = Color.green(color)
            val blue = Color.blue(color)
            return Color.argb(alpha, red, green, blue)
        }
    }
}
