package com.example.timesense;

import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class BlockerService extends Service {

    private Handler handler = new Handler(); // Handler can schedule tasks to run later
    private Runnable checkerRunnable; // Runnable is task to run repeatedly
    private boolean isRunning = false;
    private boolean onBreak = false;
    private Runnable breakFinisher; // The task that ends the break

    private String targetActivity = "";

    // Banned Apps
    private final String[] BLOCKED_APPS = {
            "com.instagram.android",
            "com.google.android.youtube",
            "com.zhiliaoapp.musically"
    };

    @Override

    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create the Notification Channel
        createNotificationChannel();
        Notification initialNotification = getNotification("Focus Mode Active", "Starting service...");
        startForeground(1, initialNotification);

        // Check if we are starting a BREAK or normal FOCUS
        if (intent != null && intent.getBooleanExtra("START_BREAK", false)) {
            startBreakMode();
        } else {
            startFocusMode();
        }

        // Reads address to return to
        if (intent != null && intent.hasExtra("RETURN_TARGET")) {
            targetActivity = intent.getStringExtra("RETURN_TARGET");
        }

        isRunning = true;
        startBlocking();

        return START_STICKY; // Restart automatically ASAP if problem encountered
    }

    private void startBreakMode() {
        onBreak = true;

        // Update Notification to show "Break"
        startForeground(1, getNotification("Break Time", "You have 5 minutes to scroll."));

        // Schedule the end of the break (5 Minutes)
        if (breakFinisher != null)
            handler.removeCallbacks(breakFinisher);

        breakFinisher = new Runnable() {
            @Override
            public void run() {
                // Time is up! Back to work.
                startFocusMode();
            }
        };
        handler.postDelayed(breakFinisher, 5 * 60 * 1000); // 5 Minutes
    }

    private void startFocusMode() {
        onBreak = false;
        if (breakFinisher != null)
            handler.removeCallbacks(breakFinisher); // Cancel any pending break

        startForeground(1, getNotification("Focus Mode Active", "Monitoring for distractions..."));
    }

    private Notification getNotification(String title, String text) {
        return new NotificationCompat.Builder(this, "BlockerChannel")
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setOnlyAlertOnce(true) // Prevents sound/vibration every time it updates
                .build();
    }

    // Update the notification text dynamically
    private void updateNotification(String title, String text) {
        Notification notification = new NotificationCompat.Builder(this, "BlockerChannel")
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .build();

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null)
            manager.notify(1, notification); // ID 1 updates the existing notification
    }

    // Helper method to create the channel
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "BlockerChannel",
                    "Focus Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void startBlocking() {
        checkerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;

                checkCurrentApp();

                // Check again in 1 second
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(checkerRunnable);
    }

    private void checkCurrentApp() {
        if (onBreak) return; // no need to check if on 5 min break
        if (!PermissionUtils.hasUsageStatsPermission(this)) return; // Safety check for permission

        String topPackageName = "";
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();

        // Query last 10 seconds of usage
        List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time);

        if (stats != null && !stats.isEmpty()) { // If apps were used in the last 10 seconds
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>(); // Treemap sorts automatically
            for (UsageStats usageStats : stats) {
                mySortedMap.put(usageStats.getLastTimeUsed(), usageStats); // getLastTimeUsed() puts newest app at bottom of map
            }
            if (!mySortedMap.isEmpty()) {
                topPackageName = mySortedMap.get(mySortedMap.lastKey()).getPackageName(); // Gets last item in the map
            }
        }

        // Check if the top app is on our Blacklist
        for (String blockedApp : BLOCKED_APPS) {
            if (topPackageName.equals(blockedApp)) {
                // Launch Block Screen
                Intent intent = new Intent(this, BlockedActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("RETURN_TARGET", targetActivity);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        handler.removeCallbacks(checkerRunnable); // Stop the loop
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}