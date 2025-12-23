package com.example.timesense;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;
import android.app.AlertDialog;
import android.widget.EditText;
import android.content.DialogInterface;

public class OvertimeActivity extends AppCompatActivity {
    // Declare Views
    private TextView timerText;
    private TextView timeElapsedText;
    private TextView taskNameText;
    private Button pauseButton;
    private Button cancelButton;
    private Button stopButton;

    // Logic Variables for Stopwatch
    private Handler handler;
    private long startTime = 0L;
    private long timeInMilliseconds = 0L;
    private long timeSwapBuff = 0L;
    private long updatedTime = 0L;
    private boolean isRunning = true;

    // Data from the previous screen
    private long originalDurationInMillis = 0L;

    private Runnable updateTimerThread;

    private String pauseReason = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overtime);

        startBlocker();

        // 4. Connect Views to XML
        timerText = findViewById(R.id.stopwatch);
        timeElapsedText = findViewById(R.id.timeElapsed);
        taskNameText = findViewById(R.id.timerTaskName);
        pauseButton = findViewById(R.id.pauseOvertimeButton);
        stopButton = findViewById(R.id.stopOvertimeButton);
        cancelButton = findViewById(R.id.cancelTimer);

        // Get Data from TimerActivity
        Intent intent = getIntent();
        String taskName = intent.getStringExtra("TASK_NAME");
        // We receive the original duration (e.g., 25 mins) to calculate the "Total"
        originalDurationInMillis = intent.getLongExtra("ORIGINAL_TIME", 0);

        taskNameText.setText(taskName);

        // Start the Stopwatch Loop
        handler = new Handler();
        startTime = SystemClock.uptimeMillis();

        updateTimerThread = new Runnable() {
            public void run() {
                // Calculate Overtime Duration
                timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
                updatedTime = timeSwapBuff + timeInMilliseconds;

                // Update the timer
                int secs = (int) (updatedTime / 1000);
                int mins = secs / 60;
                secs = secs % 60;
                timerText.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));

                // Update the Total Elapsed Text
                long totalRunTime = originalDurationInMillis + updatedTime;
                int totalSecs = (int) (totalRunTime / 1000);
                int totalMins = totalSecs / 60;
                totalSecs = totalSecs % 60;

                String elapsedString = String.format(Locale.getDefault(), "%02d:%02d elapsed", totalMins, totalSecs);
                timeElapsedText.setText(elapsedString);

                // Repeat every 1000ms (1 second)
                handler.postDelayed(this, 1000);
            }
        };

        // Start the timer immediately
        handler.postDelayed(updateTimerThread, 0);

        // Pause Button Logic
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRunning) {
                    // PAUSE
                    timeSwapBuff += timeInMilliseconds;
                    handler.removeCallbacks(updateTimerThread);
                    pauseButton.setText("Resume");
                    isRunning = false;
                    showPauseReasonDialog();
                } else {
                    // RESUME
                    startBlocker();
                    startTime = SystemClock.uptimeMillis();
                    handler.postDelayed(updateTimerThread, 0);
                    pauseButton.setText("Pause");
                    isRunning = true;
                }
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Stop the stopwatch
                handler.removeCallbacks(updateTimerThread);

                // Stop blocking distracting apps
                stopBlocker();

                // Navigate back to Home
                Intent intent = new Intent(OvertimeActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                // Destroy this screen
                finish();
            }
        });

        // Finish Button
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Stop the background timer
                handler.removeCallbacks(updateTimerThread);

                // Stop blocking distracting apps
                stopBlocker();

                // Calculate final total time
                long finalDuration = originalDurationInMillis + updatedTime;

                // Create the Session object
                String taskName = taskNameText.getText().toString();
                FocusSession session = new FocusSession(taskName, finalDuration, System.currentTimeMillis());

                // Save to Database
                AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
                db.focusDao().insertSession(session);

                // Go directly to Home Screen
                Intent intent = new Intent(OvertimeActivity.this, HomeActivity.class);
                // Clears the back stack
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish(); // Destroy this activity
            }
        });

    }
    // Pause Reason Logic
    private void showPauseReasonDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        android.view.LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_pause_reason, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        // Connect the buttons
        TextView btnClose = dialogView.findViewById(R.id.btnCloseDialog);
        Button btnDistraction = dialogView.findViewById(R.id.btnDistraction);
        Button btnScrolling = dialogView.findViewById(R.id.btnScrolling);
        Button btnEat = dialogView.findViewById(R.id.btnEat);
        Button btnBathroom = dialogView.findViewById(R.id.btnBathroom);
        Button btnOther = dialogView.findViewById(R.id.btnOther);

        // If 'X' is pressed
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                // RESUME
                startTime = SystemClock.uptimeMillis();
                handler.postDelayed(updateTimerThread, 0);
                pauseButton.setText("Pause");
                isRunning = true;
            }
        });

        // Reason Buttons
        View.OnClickListener reasonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                String reasonText = b.getText().toString();

                // If reason is to scroll, blocked apps are allowed
                if (reasonText.equals("Scrolling / Phone")){
                    stopBlocker();
                }

                // Create the Log object
                long timestamp = System.currentTimeMillis();
                PauseLog log = new PauseLog(reasonText, timestamp);

                // Save it to the Database
                AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
                db.focusDao().insertPause(log);

                // Close the dialog
                dialog.dismiss();
            }
        };

        // Attach listener to all buttons
        btnDistraction.setOnClickListener(reasonListener);
        btnScrolling.setOnClickListener(reasonListener);
        btnEat.setOnClickListener(reasonListener);
        btnBathroom.setOnClickListener(reasonListener);
        btnOther.setOnClickListener(reasonListener);

        dialog.show();
    }

    private void startBlocker() {
        // Check permissions first
        if (!PermissionUtils.hasUsageStatsPermission(this) || !PermissionUtils.hasOverlayPermission(this)) {
            return;
        }

        Intent intent = new Intent(this, BlockerService.class);
        intent.putExtra("RETURN_TARGET", OvertimeActivity.class.getName());
        // Android 8+ requires "startForegroundService"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void stopBlocker() {
        Intent intent = new Intent(this, BlockerService.class);
        stopService(intent);
    }
}