package com.example.timesense;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "focus_sessions")
public class FocusSession {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String taskName;
    public long durationInMillis; // How long you focused
    public long timestamp; // for sorting by date
    // Constructor
    public FocusSession(String taskName, long durationInMillis, long timestamp) {
        this.taskName = taskName;
        this.durationInMillis = durationInMillis;
        this.timestamp = timestamp;
    }
}