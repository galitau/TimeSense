package com.example.timesense;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;
import android.app.AlertDialog;
import android.widget.EditText;
import android.content.DialogInterface;

public class TimerActivity extends AppCompatActivity {

    // Controlled options
    private TextView timerText;
    private TextView taskNameText;
    private ProgressBar progressBar;
    private Button pauseButton;
    private Button stopButton;
    private Button cancelButton;
    private TextView timeElapsed;

    // Timer Variables
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis; // How much time is left right now?
    private long totalTimeInMillis; // How much time did we start with?
    private boolean isTimerRunning; // Is the clock ticking?
    private double warningThresholdTime; // What time is it when 85% of time has passed?

    private String pauseReason = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        PermissionUtils.requestPermissions(this); // Permission to block distracting apps

        // Connect the views
        timerText = findViewById(R.id.timerCountdown);
        taskNameText = findViewById(R.id.timerTaskName);
        progressBar = findViewById(R.id.timerProgressBar);
        pauseButton = findViewById(R.id.pauseButton);
        stopButton = findViewById(R.id.stopButton);
        timeElapsed = findViewById(R.id.timeElapsed);
        cancelButton = findViewById(R.id.cancelTimer);

        // Get data from previous screen
        Intent intent = getIntent();
        String taskName = intent.getStringExtra("TASK_NAME");
        String timeString = intent.getStringExtra("TIME_VALUE");

        // Set the task name on screen
        taskNameText.setText(taskName);

        // Convert the input to Milliseconds
        // Default to 10 minutes if they typed nothing
        long minutesInput = 10;
        if (timeString != null && !timeString.isEmpty()) {
            minutesInput = Long.parseLong(timeString);
        }

        totalTimeInMillis = minutesInput * 60 * 1000; // Minutes to Milliseconds
        timeLeftInMillis = totalTimeInMillis;
        warningThresholdTime = (totalTimeInMillis * 0.15); // Time until warning color is set

        // Setup the Progress Bar
        progressBar.setMax((int) totalTimeInMillis);
        progressBar.setProgress(0);

        // Start the Timer immediately!
        startTimer();

        // Handle the Pause Button
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isTimerRunning) {
                    pauseTimer();
                    showPauseReasonDialog();
                } else {
                    startTimer();
                }
            }
        });

        // Cancel Button
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Stop the countdown
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }

                stopBlocker();

                // Navigate back to Home
                Intent intent = new Intent(TimerActivity.this, HomeActivity.class);
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
                // Cancel the timer
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }

                stopBlocker();

                // Calculate how much time spent
                long timeSpent = totalTimeInMillis - timeLeftInMillis;

                // Only save if time spent over 10 seconds
                if (timeSpent > 10000) {
                    String taskName = taskNameText.getText().toString();
                    FocusSession session = new FocusSession(taskName, timeSpent, System.currentTimeMillis());

                    // Save to Database
                    AppDatabase.getDatabase(getApplicationContext()).focusDao().insertSession(session);
                }

                // Go to Home Screen
                Intent intent = new Intent(TimerActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void startTimer() {
        startBlocker();

        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                    if (timeLeftInMillis <= warningThresholdTime){
                        int orangeColor = android.graphics.Color.parseColor("#F39C12");
                        timerText.setTextColor(orangeColor);
                        progressBar.getProgressDrawable().setTint(orangeColor);
                    }

                updateCountDownText();
                updateProgressBar();

                int minutes = (int) ((totalTimeInMillis - timeLeftInMillis) / 1000) / 60;
                int seconds = (int) ((totalTimeInMillis - timeLeftInMillis) / 1000) % 60;

                String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
                timeFormatted += " Elapsed";
                timeElapsed.setText(timeFormatted);
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;

                // Create Intent for Overtime
                Intent intent = new Intent(TimerActivity.this, OvertimeActivity.class);

                // Pass the Task Name & the Original Time
                String currentTask = taskNameText.getText().toString();
                intent.putExtra("TASK_NAME", currentTask);
                intent.putExtra("ORIGINAL_TIME", totalTimeInMillis);

                // Go to Overtime
                startActivity(intent);
                finish();
            }
        }.start();

        isTimerRunning = true;
        pauseButton.setText("Pause");
    }

    private void pauseTimer() {
        countDownTimer.cancel();
        isTimerRunning = false;
        pauseButton.setText("Resume");
    }

    private void updateCountDownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;

        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        timerText.setText(timeFormatted);
    }

    private void updateProgressBar() {
        progressBar.setProgress((int) (totalTimeInMillis - timeLeftInMillis));
    }

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

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                startTimer();
            }
        });

        // Reason Buttons
        View.OnClickListener reasonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                String reasonText = b.getText().toString();

                // If reason is to scroll, blocked apps are allowed
                if (reasonText.equals("Scrolling / Phone (5 min)")){
                    // Start the service in break mode
                    startBlockerWithBreak();
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
        intent.putExtra("RETURN_TARGET", TimerActivity.class.getName());

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

    @Override
    protected void onResume() {
        super.onResume();
        // To check if there is still a second permission missing
        PermissionUtils.requestPermissions(this);
    }

    private void startBlockerWithBreak() {
        // Permissions check
        if (!PermissionUtils.hasUsageStatsPermission(this) || !PermissionUtils.hasOverlayPermission(this)) return;

        Intent intent = new Intent(this, BlockerService.class);
        intent.putExtra("RETURN_TARGET", TimerActivity.class.getName());

        // TRIGGER THE BREAK
        intent.putExtra("START_BREAK", true);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }
}