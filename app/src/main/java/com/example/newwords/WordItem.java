package com.example.newwords;

import android.util.Log;

import java.util.Date;

public class WordItem {
    private String wordId;
    private String word;
    private String translation;
    private String note;
    private boolean isFavorite;
    private int difficulty;
    private int reviewCount;
    private int correctAnswers;
    private boolean isCustomWord;
    private String libraryId;
    private String userId;
    private Date createdAt;
    private Date lastReviewed;

    // === ПОЛЯ ДЛЯ СИСТЕМЫ ПОВТОРЕНИЙ ===
    private int reviewStage = 0; // текущий этап: 0,1,2,3,4,5,6 (соответствует интервалам)
    private Date nextReviewDate;
    private int consecutiveShows = 0; // сколько раз показано в текущей сессии
    // УБРАТЬ ДУБЛИРУЮЩИЕСЯ ПОЛЯ ОТСЮДА!

    // Конструкторы
    public WordItem() {
    }

    public WordItem(String word, String translation, String note) {
        this.word = word;
        this.translation = translation;
        this.note = note;
        this.isFavorite = false;
        this.difficulty = 3; // ИЗМЕНИТЬ: новые слова должны быть сложности 3
        this.reviewCount = 0;
        this.correctAnswers = 0;
        this.isCustomWord = true;
        this.createdAt = new Date();
        this.reviewStage = 0; // Начинаем с этапа 0
        this.consecutiveShows = 0; // Еще не показывали
        this.nextReviewDate = new Date(); // Готово к изучению сразу
    }

    // === ГЕТТЕРЫ ===
    public String getWordId() { return wordId; }
    public String getWord() { return word; }
    public String getTranslation() { return translation; }
    public String getNote() { return note; }
    public boolean isFavorite() { return isFavorite; }
    public int getDifficulty() { return difficulty; } // ДОБАВИТЬ ЭТОТ ГЕТТЕР
    public int getReviewCount() { return reviewCount; }
    public int getCorrectAnswers() { return correctAnswers; }
    public boolean isCustomWord() { return isCustomWord; }
    public String getLibraryId() { return libraryId; }
    public String getUserId() { return userId; }
    public Date getCreatedAt() { return createdAt; }
    public Date getLastReviewed() { return lastReviewed; }

    // === ГЕТТЕРЫ ДЛЯ СИСТЕМЫ ПОВТОРЕНИЙ ===
    public int getReviewStage() { return reviewStage; }
    public Date getNextReviewDate() { return nextReviewDate; }
    public int getConsecutiveShows() { return consecutiveShows; }

    // === СЕТТЕРЫ ===
    public void setWordId(String wordId) { this.wordId = wordId; }
    public void setWord(String word) { this.word = word; }
    public void setTranslation(String translation) { this.translation = translation; }
    public void setNote(String note) { this.note = note; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; } // ДОБАВИТЬ ЭТОТ СЕТТЕР
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }
    public void setCustomWord(boolean customWord) { isCustomWord = customWord; }
    public void setLibraryId(String libraryId) { this.libraryId = libraryId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setLastReviewed(Date lastReviewed) { this.lastReviewed = lastReviewed; }

    // === СЕТТЕРЫ ДЛЯ СИСТЕМЫ ПОВТОРЕНИЙ ===
    public void setReviewStage(int reviewStage) { this.reviewStage = reviewStage; }
    public void setNextReviewDate(Date nextReviewDate) { this.nextReviewDate = nextReviewDate; }
    public void setConsecutiveShows(int consecutiveShows) { this.consecutiveShows = consecutiveShows; }

    // === ДОБАВЛЯЕМ ТОЛЬКО НУЖНЫЕ ДЛЯ КЕША МЕТОДЫ ===

    /**
     * Для кеширования - геттер с префиксом get
     */
    public boolean getIsFavorite() {
        return isFavorite;
    }

    /**
     * Для кеширования - сеттер с префиксом set
     */
    public void setIsFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    /**
     * Для кеширования - геттер с префиксом get
     */
    public boolean getIsCustomWord() {
        return isCustomWord;
    }

    /**
     * Для кеширования - сеттер с префиксом set
     */
    public void setIsCustomWord(boolean isCustomWord) {
        this.isCustomWord = isCustomWord;
    }

    @Override
    public String toString() {
        return word + " - " + translation;
    }

    // === МЕТОДЫ ДЛЯ СИСТЕМЫ ПОВТОРЕНИЙ ===

    /**
     * Проверяет, готово ли слово к повторению
     */
    public boolean isDueForReview() {
        if (nextReviewDate == null) {
            Log.d("WordItem", "Слово " + word + ": nextReviewDate is null - готово к повторению");
            return true;
        }

        boolean isDue = new Date().after(nextReviewDate);
        Log.d("WordItem", "Слово " + word + ": nextReviewDate=" + nextReviewDate + ", isDue=" + isDue);
        return isDue;
    }

    /**
     * Проверяет, является ли слово новым
     */
    public boolean isNew() {
        return reviewStage == 0 && consecutiveShows == 0;
    }

    /**
     * Нужно ли показывать слово еще раз в текущей сессии
     */
    public boolean needsMoreShows() {
        // Новые слова показываем 3 раза подряд
        if (reviewStage == 0) {
            return consecutiveShows < 3;
        }
        return false;
    }

    /**
     * Обновляет сложность на основе этапа (по твоей логике)
     */
    public void updateDifficultyBasedOnStage() {
        // Сначала сбрасываем сложность
        if (this.reviewStage == 0) {
            this.difficulty = 3; // Новые слова - сложность 3
        }

        // На 4 этапе (14 дней) - средняя сложность
        if (this.reviewStage >= 4) {
            this.difficulty = 2; // Средняя сложность
        }

        // На 6 этапе (60 дней) - выученное
        if (this.reviewStage >= 6) {
            this.difficulty = 1; // Выученное
        }

        Log.d("WordItem", "Слово " + word + ": этап=" + reviewStage + ", сложность=" + difficulty);
    }

    /**
     * Получает текстовое описание статуса
     */
    public String getStatusText() {
        if (difficulty == 3) return "НОВОЕ СЛОВО";
        if (difficulty == 2) return "ИЗУЧАЕТСЯ";
        if (difficulty == 1) return "ВЫУЧЕНО";
        return "НЕИЗВЕСТНО";
    }

    public int getStatusColor() {
        if (difficulty == 3) return 0xFF625fba; // Фиолетовый
        if (difficulty == 2) return 0xFFbabba9; // Серый
        if (difficulty == 1) return 0xFF4CAF50; // Зеленый
        return 0xFF625fba;
    }

    /**
     * Проверяет, выучено ли слово
     */
    public boolean isLearned() {
        return difficulty == 1;
    }
}