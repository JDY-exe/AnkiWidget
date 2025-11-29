package com.ankiwidget.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import java.time.LocalDate

/**
 * Repository for accessing AnkiDroid review data via Content Provider
 */
class AnkiRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "AnkiRepository"
        
        // AnkiDroid Content Provider
        private const val AUTHORITY = "com.ichi2.anki.flashcards"
        private val DECK_LIST_URI = Uri.parse("content://$AUTHORITY/decks")
        private val SCHEDULE_URI = Uri.parse("content://$AUTHORITY/schedule")
    }

    /**
     * Get review data for the specified number of days
     * Currently only supports checking if reviews were done today
     */
    fun getReviewData(daysToShow: Int, selectedDeckId: Long? = null): List<DayReviewStatus> {
        Log.d(TAG, "Getting review data for $daysToShow days (deck=${selectedDeckId ?: "ALL"})")
        
        // Check if AnkiDroid is installed
        if (!isAnkiDroidInstalled()) {
            Log.w(TAG, "AnkiDroid not installed, using mock data")
            return generateMockData(daysToShow)
        }
        
        // Check if we have the required permission
        val hasPermission = context.checkSelfPermission("com.ichi2.anki.permission.READ_WRITE_DATABASE")
        Log.d(TAG, "Permission check result: $hasPermission (GRANTED=${android.content.pm.PackageManager.PERMISSION_GRANTED})")
        
        if (hasPermission != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "❌ PERMISSION DENIED: com.ichi2.anki.permission.READ_WRITE_DATABASE")
            return generateMockData(daysToShow)
        }
        
        Log.d(TAG, "✓ Permission granted! Checking review status...")
        
        // Check if today's reviews are complete
        val todayComplete = isReviewsComplete(selectedDeckId)
        
        // Generate status for all days
        val today = LocalDate.now()
        return (0 until daysToShow).map { i ->
            val date = today.minusDays(i.toLong())
            val isToday = i == 0
            
            DayReviewStatus(
                date = date,
                allReviewsCompleted = isToday && todayComplete,
                reviewCount = if (isToday && todayComplete) 1 else 0
            )
        }
    }
    
    /**
     * Check if AnkiDroid is installed
     */
    private fun isAnkiDroidInstalled(): Boolean {
        Log.d(TAG, "Checking if AnkiDroid is installed...")
        
        return try {
            // Try the standard package name
            val packageInfo = context.packageManager.getPackageInfo("com.ichi2.anki", 0)
            Log.d(TAG, "✓ AnkiDroid IS installed! Package: ${packageInfo.packageName}, Version: ${packageInfo.versionName}")
            true
        } catch (e: Exception) {
            Log.w(TAG, "✗ AnkiDroid detection failed with package 'com.ichi2.anki': ${e.message}")
            
            // Try to find any package with 'anki' in the name
            try {
                val allPackages = context.packageManager.getInstalledPackages(0)
                val ankiPackages = allPackages.filter { 
                    it.packageName.contains("anki", ignoreCase = true) 
                }
                
                if (ankiPackages.isNotEmpty()) {
                    Log.d(TAG, "Found ${ankiPackages.size} package(s) with 'anki' in name:")
                    ankiPackages.forEach { pkg ->
                        Log.d(TAG, "  - ${pkg.packageName} (version ${pkg.versionName})")
                    }
                } else {
                    Log.w(TAG, "No packages with 'anki' in name found")
                }
            } catch (listException: Exception) {
                Log.e(TAG, "Failed to list installed packages: ${listException.message}")
            }
            
            false
        }
    }
    /**
     * Get list of available decks
     */
    fun getAvailableDecks(): List<Pair<Long, String>> {
        val decks = mutableListOf<Pair<Long, String>>()
        var cursor: Cursor? = null
        
        try {
            // Check permission first
            val hasPermission = context.checkSelfPermission("com.ichi2.anki.permission.READ_WRITE_DATABASE")
            if (hasPermission != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "No permission to access AnkiDroid decks")
                return emptyList()
            }
            
            val decksUri = Uri.parse("content://com.ichi2.anki.flashcards/decks")
            cursor = context.contentResolver.query(
                decksUri,
                arrayOf("deck_id", "deck_name"),
                null,
                null,
                null
            )
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val deckId = cursor.getLong(cursor.getColumnIndex("deck_id"))
                    val deckName = cursor.getString(cursor.getColumnIndex("deck_name"))
                    decks.add(Pair(deckId, deckName))
                    Log.d(TAG, "Found deck: $deckName (ID: $deckId)")
                } while (cursor.moveToNext())
                
                Log.d(TAG, "Loaded ${decks.size} decks from AnkiDroid")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading decks", e)
        } finally {
            cursor?.close()
        }
        
        return decks
    }
    
    /**
     * Check if all cards are done today (deck_count = [0, 0, 0] for all decks)
     * If selectedDeckId is provided, only check that specific deck
     */
    fun isReviewsComplete(selectedDeckId: Long? = null): Boolean {
        var cursor: Cursor? = null
        var allClear = true
        var totalPending = 0
        
        try {
            val decksUri = Uri.parse("content://com.ichi2.anki.flashcards/decks")
            
            cursor = context.contentResolver.query(
                decksUri,
                arrayOf("deck_name", "deck_id", "deck_count"),
                null,
                null,
                null
            )
            
            if (cursor == null) {
                Log.w(TAG, "Failed to query decks")
                return false
            }
            
            Log.d(TAG, "Checking review completion (selectedDeck=${selectedDeckId ?: "ALL"})")
            
            while (cursor.moveToNext()) {
                try {
                    val deckId = cursor.getLong(cursor.getColumnIndex("deck_id"))
                    val deckName = cursor.getString(cursor.getColumnIndex("deck_name"))
                    val deckCountStr = cursor.getString(cursor.getColumnIndex("deck_count"))
                    
                    // Skip this deck if we're filtering by a specific deck
                    if (selectedDeckId != null && deckId != selectedDeckId) {
                        continue
                    }
                    
                    // Parse deck_count array: [learning, review, new]
                    val counts = parseDeckCount(deckCountStr)
                    if (counts != null && counts.size >= 3) {
                        val learning = counts[0]
                        val review = counts[1]
                        val newCards = counts[2]
                        
                        val deckPending = learning.toInt() + review + newCards
                        totalPending += deckPending
                        
                        if (deckPending > 0) {
                            allClear = false
                            if (selectedDeckId != null) {
                                // If tracking specific deck, log and break
                                Log.d(TAG, "Deck '$deckName': [L=$learning, R=$review, N=$newCards] = $deckPending pending")
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing deck data", e)
                    allClear = false
                }
            }
            
            Log.d(TAG, if (allClear) "✓ Reviews complete! (0 pending)" else "✗ $totalPending cards pending")
            return allClear
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking reviews", e)
            return false
        } finally {
            cursor?.close()
        }
    }
    
    /**
     * Parse deck_count string like "[0,0,1]" into list of integers
     */
    private fun parseDeckCount(deckCountStr: String?): List<Int>? {
        if (deckCountStr == null) return null
        
        return try {
            deckCountStr.trim('[', ']')
                .split(',')
                .map { it.trim().toInt() }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Generate mock data for testing/demo purposes
     */
    private fun generateMockData(daysToShow: Int): List<DayReviewStatus> {
        Log.d(TAG, "Generating mock data for $daysToShow days")
        val today = LocalDate.now()
        return (0 until daysToShow).map { i ->
            val date = today.minusDays(i.toLong())
            // Mock pattern: completed on weekdays, incomplete on weekends
            val isWeekday = date.dayOfWeek.value in 1..5
            DayReviewStatus(
                date = date,
                allReviewsCompleted = isWeekday && (i % 3 != 0), // Some variety
                reviewCount = if (isWeekday) 20 else 0
            )
        }
    }
}
