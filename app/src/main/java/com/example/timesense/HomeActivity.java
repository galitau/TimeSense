package com.example.timesense;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private TextView statFocusTime, statSessionCount, statDistraction;
    private RadioGroup tabGroup;
    private Button btnNewTask;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize Database
        db = AppDatabase.getDatabase(this);

        // Connect Views
        statFocusTime = findViewById(R.id.statFocusTime);
        statSessionCount = findViewById(R.id.statSessionCount);
        statDistraction = findViewById(R.id.statDistraction);
        tabGroup = findViewById(R.id.tabGroup);
        btnNewTask = findViewById(R.id.btnNewTask);

        // Handle Tab Switching
        tabGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.tabDaily) {
                    loadStats(1); // 1 Day
                } else if (checkedId == R.id.tabWeekly) {
                    loadStats(7); // 7 Days
                } else if (checkedId == R.id.tabMonthly) {
                    loadStats(30); // 30 Days
                }
            }
        });

        // 4. Handle "New Task" Button
        btnNewTask.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // To reload data when we come back to this screen
        if (tabGroup.getCheckedRadioButtonId() == R.id.tabWeekly) {
            loadStats(7);
        } else if (tabGroup.getCheckedRadioButtonId() == R.id.tabMonthly) {
            loadStats(30);
        } else {
            loadStats(1);
        }
    }

    private void loadStats(int daysAgo) {
        // Calculate the Start Time
        Calendar calendar = Calendar.getInstance();
        if (daysAgo == 1) {
            // Set to midnight today
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
        } else {
            // Go back X days
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo);
        }
        long startTime = calendar.getTimeInMillis();

        // Query the Database
        long totalTime = db.focusDao().getTotalFocusTimeSince(startTime);
        int sessionCount = db.focusDao().getSessionCountSince(startTime);
        String topDistraction = db.focusDao().getTopDistractionSince(startTime);

        // Update the UI
        long totalMinutes = totalTime / 1000 / 60;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        statFocusTime.setText(String.format(Locale.getDefault(), "%dh %dm", hours, minutes));

        statSessionCount.setText(String.valueOf(sessionCount));

        if (topDistraction == null) {
            statDistraction.setText("None");
        } else {
            statDistraction.setText(topDistraction);
        }
    }
}