package com.example.newwords;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import android.content.Context;

@Database(entities = {LocalWordLibrary.class, LocalWordItem.class, UserStats.class}, version = 3, exportSchema = false)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {

    public abstract LocalLibraryDao libraryDao();
    public abstract LocalWordDao wordDao();
    public abstract UserStatsDao statsDao(); // ← ДОБАВЬТЕ ЭТУ СТРОКУ

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "newwords_database"
                            )
                            .fallbackToDestructiveMigration() // Пересоздаем при изменении версии
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // Вспомогательный метод для получения или создания статистики
    public UserStats getOrCreateStats(String userId) {
        UserStats stats = statsDao().getStats(userId);
        if (stats == null) {
            stats = new UserStats(userId);
            statsDao().insertStats(stats);
        }
        return stats;
    }
}