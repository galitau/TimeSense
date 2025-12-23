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

    private String targetActivity = "";

    // Banned Apps
    private final String[] BLOCKED_APPS = {
            "com.instagram.android",
            "com.google.android.youtube"
    };

    @Override

    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create the Notification Channel
        createNotificationChannel();

        // Build the notification to show that the app is running in the background per Android requirements
        Notification notification = new NotificationCompat.Builder(this, "BlockerChannel")
                .setContentTitle("Focus Mode Active")
                .setContentText("Monitoring for distractions...")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .build();

        startForeground(1, notification);

        // Reads address to return to
        if (intent != null && intent.hasExtra("RETURN_TARGET")) {
            targetActivity = intent.getStringExtra("RETURN_TARGET");
        }

        // Start your logic
        isRunning = true;
        startBlocking();

        return START_STICKY; // Restart automatically ASAP if problem encountered
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

                // Check again in 5 second
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(checkerRunnable);
    }

    private void checkCurrentApp() {
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