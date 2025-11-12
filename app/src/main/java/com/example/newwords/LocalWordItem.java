package com.example.newwords;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import androidx.annotation.NonNull;

import java.util.Date;

@Entity(tableName = "local_words")
@TypeConverters(Converters.class)
public class LocalWordItem {
    @PrimaryKey
    @NonNull
    private String wordId;
    private String word;
    private String translation;
    private String note;
    private boolean isFavorite;
    private String difficulty;
    private int reviewCount;
    private int correctAnswers;
    private boolean isCustomWord;
    private String libraryId;
    private String userId;
    private Date createdAt;
    private Date lastReviewed;
    private Date lastSynced;


    // ДОБАВЬ ЭТИ ПОЛЯ:
    private int reviewStage;
    private Date nextReviewDate;
    private int consecutiveShows;

    // ДОБАВЬ ГЕТТЕРЫ И СЕТТЕРЫ:
    public int getReviewStage() { return reviewStage; }
    public void setReviewStage(int reviewStage) { this.reviewStage = reviewStage; }

    public Date getNextReviewDate() { return nextReviewDate; }
    public void setNextReviewDate(Date nextReviewDate) { this.nextReviewDate = nextReviewDate; }

    public int getConsecutiveShows() { return consecutiveShows; }
    public void setConsecutiveShows(int consecutiveShows) { this.consecutiveShows = consecutiveShows; }



    // Конструкторы, геттеры и сеттеры
    public LocalWordItem() {
        this.wordId = ""; // Инициализируем пустой строкой
    }

    public LocalWordItem(WordItem word) {
        this.wordId = word.getWordId() != null ? word.getWordId() : "";
        this.word = word.getWord();
        this.translation = word.getTranslation();
        this.note = word.getNote();
        // ДОБАВЬ ЭТИ СТРОКИ:
        this.reviewStage = word.getReviewStage();
        this.nextReviewDate = word.getNextReviewDate();
        this.consecutiveShows = word.getConsecutiveShows();
        // Используем правильные геттеры с префиксом get
        this.isFavorite = word.getIsFavorite(); // было: word.isFavorite()
        this.difficulty = String.valueOf(word.getDifficulty());
        this.reviewCount = word.getReviewCount();
        this.correctAnswers = word.getCorrectAnswers();
        this.isCustomWord = word.getIsCustomWord(); // было: word.isCustomWord()

        this.libraryId = word.getLibraryId();
        this.userId = word.getUserId();
        this.createdAt = word.getCreatedAt();
        this.lastReviewed = word.getLastReviewed();
        this.lastSynced = new Date();
    }

    // === ГЕТТЕРЫ ===
    @NonNull
    public String getWordId() { return wordId; }
    public String getWord() { return word; }
    public String getTranslation() { return translation; }
    public String getNote() { return note; }
    public boolean isFavorite() { return isFavorite; }
    public String getDifficulty() { return difficulty; }
    public int getReviewCount() { return reviewCount; }
    public int getCorrectAnswers() { return correctAnswers; }
    public boolean isCustomWord() { return isCustomWord; }
    public String getLibraryId() { return libraryId; }
    public String getUserId() { return userId; }
    public Date getCreatedAt() { return createdAt; }
    public Date getLastReviewed() { return lastReviewed; }
    public Date getLastSynced() { return lastSynced; }

    // === СЕТТЕРЫ ===
    public void setWordId(@NonNull String wordId) { this.wordId = wordId; }
    public void setWord(String word) { this.word = word; }
    public void setTranslation(String translation) { this.translation = translation; }
    public void setNote(String note) { this.note = note; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }
    public void setCustomWord(boolean customWord) { isCustomWord = customWord; }
    public void setLibraryId(String libraryId) { this.libraryId = libraryId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setLastReviewed(Date lastReviewed) { this.lastReviewed = lastReviewed; }
    public void setLastSynced(Date lastSynced) { this.lastSynced = lastSynced; }
}