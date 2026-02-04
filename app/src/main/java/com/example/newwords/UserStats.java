package com.example.newwords;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import java.util.Date;

@Entity(tableName = "user_stats")
@TypeConverters(DateConverter.class)
public class UserStats {
    @PrimaryKey
    @NonNull
    private String userId;

    private int streakDays;
    private int wordsInProgress;
    private int wordsLearned;
    private int todayProgress;
    private int dailyGoal;
    private Date lastSessionDate;
    private Date lastUpdated;

    // Конструкторы
    public UserStats() {
        this.userId = "";
        this.dailyGoal = 10;
    }

    public UserStats(@NonNull String userId) {
        this.userId = userId;
        this.streakDays = 0;
        this.wordsInProgress = 0;
        this.wordsLearned = 0;
        this.todayProgress = 0;
        this.dailyGoal = 10;
        this.lastSessionDate = new Date();
        this.lastUpdated = new Date();
    }

    // Геттеры и сеттеры
    @NonNull
    public String getUserId() { return userId; }
    public void setUserId(@NonNull String userId) { this.userId = userId; }

    public int getStreakDays() { return streakDays; }
    public void setStreakDays(int streakDays) { this.streakDays = streakDays; }

    public int getWordsInProgress() { return wordsInProgress; }
    public void setWordsInProgress(int wordsInProgress) { this.wordsInProgress = wordsInProgress; }

    public int getWordsLearned() { return wordsLearned; }
    public void setWordsLearned(int wordsLearned) { this.wordsLearned = wordsLearned; }

    public int getTodayProgress() { return todayProgress; }
    public void setTodayProgress(int todayProgress) { this.todayProgress = todayProgress; }

    public int getDailyGoal() { return dailyGoal; }
    public void setDailyGoal(int dailyGoal) { this.dailyGoal = dailyGoal; }

    public Date getLastSessionDate() { return lastSessionDate; }
    public void setLastSessionDate(Date lastSessionDate) { this.lastSessionDate = lastSessionDate; }

    public Date getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Date lastUpdated) { this.lastUpdated = lastUpdated; }
}