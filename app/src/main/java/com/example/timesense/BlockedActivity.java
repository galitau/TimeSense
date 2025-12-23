package com.example.timesense;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class BlockedActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked);

        // Disables the clicking back to get back to the blocked app
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Do nothing
            }
        });

        Button btnReturn = findViewById(R.id.btnReturn);
        btnReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // Get the class name we passed along (TimerActivity or OvertimeActivity)
                    String targetClassName = getIntent().getStringExtra("RETURN_TARGET");
                    // Convert string back into a real Class
                    Class<?> targetClass = Class.forName(targetClassName);
                    // Create the intent dynamically
                    Intent intent = new Intent(BlockedActivity.this, targetClass);
                    // Use SINGLE_TOP so it resumes without restarting
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                    startActivity(intent);
                    finish();

                } catch (Exception e) {
                    // If something breaks, just go to TimerActivity
                    Intent intent = new Intent(BlockedActivity.this, TimerActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }
}