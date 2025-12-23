package com.example.timesense;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {FocusSession.class, PauseLog.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public abstract FocusDao focusDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "timesense_database")
                            .allowMainThreadQueries() // Allows simple access without complex threading
                            .fallbackToDestructiveMigration() // If you change schema, it resets db instead of crashing
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}