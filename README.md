# TimeSense
**A data-driven Android productivity tool that enforces focus and improves time-estimation accuracy through system-level distraction blocking.**

<img width="1452" height="815" alt="image" src="https://github.com/user-attachments/assets/1380f9df-796f-434d-9ad7-a8f87850be9e" />


## Overview
TimeSense is a native Android application designed to optimize workflow accuracy and solve digital distraction. Unlike standard timers, TimeSense utilizes a **Foreground Service** to actively monitor user activity. If a user attempts to open a blacklisted app (e.g., Instagram, YouTube) while the timer is active, the app intercepts the intent and launches a blocking overlay.

Crucially, TimeSense also serves as a calibration tool. By tracking the delta between **"Planned Time"** and **"Actual Time"** (recording how often tasks bleed into Overtime), the app provides the data needed for users to audit their efficiency and refine their future time-boxing estimates.

## Tech Stack
* **Language:** Java
* **Architecture:** MVVM (Model-View-ViewModel) pattern principles
* **Database:** Room Persistence Library (SQLite abstraction)
* **Background Processing:** Android Foreground Services & Handlers
* **System APIs:** `UsageStatsManager`, `AppOpsManager`, `WindowManager`

## Key Features
* **Active App Blocking:** Polls system usage stats to detect foreground applications. If a restricted package is detected, a high-priority activity is launched to cover the screen.
* **Estimation Calibration:** Logs the discrepancy between allocated time and actual time spent. This feedback loop allows users to identify tasks they consistently underestimate.
* **Focus Timer & Overtime:** Custom countdown logic that transitions seamlessly into an "Overtime" stopwatch, preserving state across activity lifecycles.
* **Smart Permissions:** Handles sensitive permissions (`USAGE_STATS` and `SYSTEM_ALERT_WINDOW`) with a robust checking utility.
* **Session Tracking:** Persistent local storage of focus metrics and distraction logs using Room Database.


### The Blocking Engine
The core of the application is the `BlockerService`. To prevent the Android OS from killing the monitoring process to save battery, this is implemented as a **Foreground Service** with a persistent notification.

```java
// Snippet: Determining the current foreground app
UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
long time = System.currentTimeMillis();
List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time);

// Logic sorts by 'getLastTimeUsed' to identify the active package
// and triggers the BlockedActivity intent if a match is found.
```

## Installation

* Clone the repo
* Open in Android Studio
* Build and Run on an Emulator (API 28+) or Physical Device

Note: You must grant "Usage Access" and "Display over other apps" permissions when prompted for the blocker to function

## Roadmap & Future Improvements
* **Analytics Dashboard:** Implement data visualization (using `MPAndroidChart`) to display focus trends and a pie chart breakdown of top distractions by category.
* **Dynamic App Selection:** Replace the hardcoded blacklist with a UI that queries the `PackageManager`, allowing users to select their own custom list of distracting apps.
* **Notification Blocking:** Integrate `NotificationListenerService` to temporarily mute or batch notifications while the timer is active, preventing the screen from lighting up.
* **Cloud Sync:** Migrate from a local Room database to Firebase to allow users to sync focus stats across multiple devices.
