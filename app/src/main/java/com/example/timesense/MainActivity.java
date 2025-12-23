package com.example.timesense;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    // Declares the controls for this screen
    private EditText taskInput;
    private EditText timeInput;
    private SeekBar confidenceSeekBar;
    private Button startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Connects the "Remote Controls" to the actual screen views
        taskInput = findViewById(R.id.taskInput);
        timeInput = findViewById(R.id.timeInput);
        confidenceSeekBar = findViewById(R.id.confidenceSeekBar);
        startButton = findViewById(R.id.startButton);

        // Listens for a button click
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Gets the inputs
                String taskName = taskInput.getText().toString();
                String timeText = timeInput.getText().toString();

                // Creates the Intent
                Intent intent = new Intent(MainActivity.this, TimerActivity.class);

                // Packs the data
                intent.putExtra("TASK_NAME", taskName);
                intent.putExtra("TIME_VALUE", timeText);

                // Launches the new screen
                startActivity(intent);

            }
        });
    }
}