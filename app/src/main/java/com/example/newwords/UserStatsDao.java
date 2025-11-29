package com.example.newwords;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.Date;

@Dao
public interface UserStatsDao {

    // Вставка или замена статистики
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStats(UserStats stats);

    // Получение статистики по userId
    @Query("SELECT * FROM user_stats WHERE userId = :userId")
    UserStats getStats(String userId);

    // Обновление отдельных полей
    @Query("UPDATE user_stats SET streakDays = :streak WHERE userId = :userId")
    void updateStreak(String userId, int streak);

    @Query("UPDATE user_stats SET wordsInProgress = :count WHERE userId = :userId")
    void updateWordsInProgress(String userId, int count);

    @Query("UPDATE user_stats SET wordsLearned = :count WHERE userId = :userId")
    void updateWordsLearned(String userId, int count);

    @Query("UPDATE user_stats SET todayProgress = :progress WHERE userId = :userId")
    void updateTodayProgress(String userId, int progress);

    @Query("UPDATE user_stats SET lastSessionDate = :date WHERE userId = :userId")
    void updateLastSessionDate(String userId, Date date);

    @Query("UPDATE user_stats SET lastUpdated = :date WHERE userId = :userId")
    void updateLastUpdated(String userId, Date date);

    // Комплексные операции
    @Query("UPDATE user_stats SET todayProgress = todayProgress + 1, lastUpdated = :date WHERE userId = :userId")
    void incrementTodayProgress(String userId, Date date);

    @Query("UPDATE user_stats SET streakDays = streakDays + 1, todayProgress = 0, lastSessionDate = :date WHERE userId = :userId")
    void incrementStreak(String userId, Date date);

    @Query("UPDATE user_stats SET streakDays = 0, todayProgress = 0 WHERE userId = :userId")
    void resetStreak(String userId);

    // Проверка существования статистики
    @Query("SELECT COUNT(*) FROM user_stats WHERE userId = :userId")
    int hasStats(String userId);

    // Удаление статистики (для выхода из аккаунта)
    @Query("DELETE FROM user_stats WHERE userId = :userId")
    void deleteStats(String userId);
}