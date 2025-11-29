
#  Anki Widget

  
### Preamble
A minimal Android home screen widget that visualizes your Anki review completion history in a GitHub contribution-style graph. Each dot represents a day, showing whether you completed all your reviews.

**WARNING**: This widget is 100% vibe coded using Claude and Gemini. It may be extremely inefficient, drain your battery, bug out, or otherwise not work at all. I have no android app development experience, thus all issues/feature requests/PRs will be probably ignored. Please fork the project if you have any interest in adding more features. If you do not have a pixel 10, and the app isn't working, see FAQ on how to debug. 
  

###  Installation

1. Install AnkiDroid from the Google Play Store.

2. Go to the **[Releases](../../releases)** page of this repository.

3. Download the latest `.apk` file (e.g., `AnkiWidget-v1.0.apk`).

4. Open the file on your Android device.

-  *Note*: You may need to allow installation from "Unknown Sources" in your browser/file manager settings.

5. Tap **Install**.

6.  **Important**: Go to Apps > Anki Widget > Permissions > Additional permissions > **Enable** "Read and write to the AnkiDroid database".
7. **Important**: Go to AnkiDroid > Settings > Advanced > Plugins > **Enaable AnkiDroid API**

###  Adding the Widget


1. Long-press on your home screen

2. Tap "Widgets"

3. Find and select "Anki Widget", click "Add"

4. Drag to desired location

5. Configure theme, deck, and other options

6. Tap "Add Widget"

  

##  Configuration Options

  

-  **Theme**: Material You, GitHub Green, Monochrome, or Custom

-  **Show Streak**: Optional streak counter below the grid, show which deck the streak is for.

-  **Deck**: Select which deck to show the streak for. You can also select "All Decks" to show the streak for all decks combined.

-  **Transparent Background**: Optional transparent background for the widget.

  

##  FAQ
**Can the widget access historical data?**
No, it can only read today's data, and store history in its own database. This is because AnkiDroid does not open up historical log data for other apps to read. This means that if the widget fails to refresh on time, or otherwise doesn't log today's data correctly, there is no way to get your streak back. I am very sorry. 

**When does the day reset?**
4 AM local time. 

**Privacy?**
All data processing happens locally on your device, only reads AnkiDroid's review database (read-only access)

**How is completion determined?** 
When your deck reaches 0/0/0 (No new cards, no cards schedule for review) for that day.

**Tracking progress from other Anki platforms?**
If you finished today's reviews on another device, you will need to sync that data to AnkiWeb, then sync the data to AnkiDroid. As long as AnkiDroid has the updated data locally, the widget will properly pull from it. 

**How to refresh data?**
Tap the widget to refresh data

**How to debug?**
If the widget displays random/placeholder data, it's likely you did not enable the permissions for it to read AnkiDroid's database data. 

Otherwise, copy paste this repository's link into ChatGPT or your LLM of choice, and ask it to debug for you. 


---

Created with <3, Google Antigravity, Claude, Gemini, and pure vibes by JDY.  

**Note**: This widget is designed and optimized for Pixel devices running Android 12+ to take full advantage of Material You dynamic theming. 
