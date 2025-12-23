package com.example.timesense;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface FocusDao {

    @Insert
    void insertSession(FocusSession session);

    @Query("SELECT * FROM focus_sessions ORDER BY timestamp DESC")
    List<FocusSession> getAllSessions();

    // Gets Time Since ___
    @Query("SELECT SUM(durationInMillis) FROM focus_sessions WHERE timestamp >= :startTime")
    long getTotalFocusTimeSince(long startTime);

    // Count Total Sessions
    @Query("SELECT COUNT(id) FROM focus_sessions WHERE timestamp >= :startTime")
    int getSessionCountSince(long startTime);

    // Top Pause Reason
    @Query("SELECT reason FROM pause_log WHERE timestamp >= :startTime GROUP BY reason ORDER BY COUNT(reason) DESC LIMIT 1")
    String getTopDistractionSince(long startTime);

    // Log a pause
    @Insert
    void insertPause(PauseLog pauseLog);

}