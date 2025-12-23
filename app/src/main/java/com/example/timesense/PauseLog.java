package com.example.timesense;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pause_log")
public class PauseLog {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String reason;    // e.g., "Scrolling"
    public long timestamp;   // When it happened

    public PauseLog(String reason, long timestamp) {
        this.reason = reason;
        this.timestamp = timestamp;
    }
}