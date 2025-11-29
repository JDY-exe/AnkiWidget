# Anki Widget

A minimal Android home screen widget that visualizes your Anki review completion history in a GitHub contribution-style graph. Each dot represents a day, showing whether you completed all your reviews.

## Features

- **📊 GitHub-Style Visualization**: Clean contribution graph showing review completion over time
- **🎨 Material You Integration**: Automatically adapts to your Pixel's system colors and wallpaper
- **⚙️ Customizable**: Choose from multiple themes, adjust time range (7-90 days), and configure appearance
- **🔄 Auto-Updates**: Refreshes daily to stay current with your progress
- **📱 Pixel-Optimized**: Built specifically for Pixel 10 with dynamic theming support

## Themes

1. **Material You (Default)**: Dynamically adapts to your system wallpaper and color palette
2. **GitHub Green**: Classic green contribution graph aesthetic
3. **Monochrome**: Minimal black and white design

## Installation

### Prerequisites
- Android 8.0 (API 26) or higher
- AnkiDroid installed with review history

### Building from Source

1. Clone this repository
2. Open in Android Studio
3. Build and run:
   ```bash
   ./gradlew assembleDebug
   ```
4. Install the APK on your device
5. Add the widget to your home screen
6. Configure your preferences

### Adding the Widget

1. Long-press on your home screen
2. Tap "Widgets"
3. Find and select "Anki Widget"
4. Drag to desired location
5. Configure theme, days to display, and other options
6. Tap "Add Widget"

## Configuration Options

- **Theme**: Material You, GitHub Green, or Monochrome
- **Days to Display**: 7, 14, 30, 60, or 90 days
- **Show Streak**: Optional streak counter below the grid
- **Tap to Refresh**: Tap the widget to manually refresh data

## How It Works

The widget reads your review history from AnkiDroid's local database and generates a visual grid where:
- **Colored dots**: Days where you completed your reviews
- **Gray dots**: Days with incomplete or no reviews
- **Grid layout**: 7 rows (days of the week) × variable columns (time range)

## Technical Details

- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Dependencies**: Material 3, AndroidX, Kotlin Coroutines
- **Architecture**: Widget Provider + Repository pattern for data access

## Project Structure

```
app/src/main/java/com/ankiwidget/
├── AnkiWidgetProvider.kt          # Main widget provider
├── config/
│   └── WidgetConfigActivity.kt    # Configuration UI
├── data/
│   ├── AnkiRepository.kt          # AnkiDroid data access
│   └── ReviewData.kt              # Data models
├── renderer/
│   ├── ContributionGridRenderer.kt # Grid bitmap generation
│   └── WidgetTheme.kt             # Theme definitions
└── receiver/
    └── DailyUpdateReceiver.kt     # Scheduled updates
```

## Privacy

- All data processing happens locally on your device
- No network requests or data transmission
- Only reads AnkiDroid's review database (read-only access)

## Known Limitations

- Requires AnkiDroid to be installed
- Needs storage permissions to access Anki database
- Currently shows mock data if AnkiDroid database is not accessible (for development/testing)

## Future Enhancements

- [ ] More granular completion criteria (e.g., specific deck targets)
- [ ] Custom color picker for themes
- [ ] Different grid layouts (e.g., monthly calendar view)
- [ ] Export/share widget screenshots
- [ ] Integration with AnkiWeb for cross-device sync

## License

MIT License - feel free to modify and distribute

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

---

**Note**: This widget is designed and optimized for Pixel devices running Android 12+ to take full advantage of Material You dynamic theming. It will work on other Android devices with fallback themes.
