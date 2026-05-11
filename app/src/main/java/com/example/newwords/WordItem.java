package com.example.newwords;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.Serializable;
import java.util.Date;

public class WordItem implements Parcelable, Serializable {

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
    private int reviewStage = 0;
    private Date nextReviewDate;
    private int consecutiveShows = 0;

    // Конструкторы
    public WordItem() {
    }

    public WordItem(String word, String translation, String note) {
        this.word = word;
        this.translation = translation;
        this.note = note;
        this.isFavorite = false;
        this.difficulty = 3;
        this.reviewCount = 0;
        this.correctAnswers = 0;
        this.isCustomWord = false;
        this.createdAt = new Date();
        this.reviewStage = 0;
        this.consecutiveShows = 0;
        this.nextReviewDate = new Date();
    }

    // === ГЕТТЕРЫ ===
    public String getWordId() { return wordId; }
    public String getWord() { return word; }
    public String getTranslation() { return translation; }
    public String getNote() { return note; }
    public boolean isFavorite() { return isFavorite; }
    public int getDifficulty() { return difficulty; }
    public int getReviewCount() { return reviewCount; }
    public int getCorrectAnswers() { return correctAnswers; }
    public boolean isCustomWord() { return isCustomWord; }
    public String getLibraryId() { return libraryId; }
    public String getUserId() { return userId; }
    public Date getCreatedAt() { return createdAt; }
    public Date getLastReviewed() { return lastReviewed; }
    public int getReviewStage() { return reviewStage; }
    public Date getNextReviewDate() { return nextReviewDate; }
    public int getConsecutiveShows() { return consecutiveShows; }

    // === СЕТТЕРЫ ===
    public void setWordId(String wordId) { this.wordId = wordId; }
    public void setWord(String word) { this.word = word; }
    public void setTranslation(String translation) { this.translation = translation; }
    public void setNote(String note) { this.note = note; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }
    public void setCustomWord(boolean customWord) { isCustomWord = customWord; }
    public void setLibraryId(String libraryId) { this.libraryId = libraryId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setLastReviewed(Date lastReviewed) { this.lastReviewed = lastReviewed; }
    public void setReviewStage(int reviewStage) { this.reviewStage = reviewStage; }
    public void setNextReviewDate(Date nextReviewDate) { this.nextReviewDate = nextReviewDate; }
    public void setConsecutiveShows(int consecutiveShows) { this.consecutiveShows = consecutiveShows; }

    // === ДОПОЛНИТЕЛЬНЫЕ МЕТОДЫ ===
    public boolean getIsFavorite() { return isFavorite; }
    public void setIsFavorite(boolean isFavorite) { this.isFavorite = isFavorite; }
    public boolean getIsCustomWord() { return isCustomWord; }
    public void setIsCustomWord(boolean isCustomWord) { this.isCustomWord = isCustomWord; }

    @Override
    public String toString() {
        return word + " - " + translation;
    }

    public boolean isDueForReview() {
        if (nextReviewDate == null) {
            return true;
        }
        return new Date().after(nextReviewDate);
    }

    public boolean isNew() {
        return SimpleRepetitionSystem.isNewWord(this);
    }

    public boolean isLearned() {
        return SimpleRepetitionSystem.isLearnedWord(this);
    }

    public boolean needsMoreShows() {
        return reviewStage == 0 && consecutiveShows < 3;
    }

    public int getStatusText() {
        if (difficulty == 3) return R.string.word_status_new;
        if (difficulty == 2) return R.string.word_status_learning;
        if (difficulty == 1) return R.string.word_status_learned;
        return R.string.word_status_unknown;
    }

    public int getStatusColor() {
        if (difficulty == 3) return 0xFF625fba;
        if (difficulty == 2) return 0xFFbabba9;
        if (difficulty == 1) return 0xFF4CAF50;
        return 0xFF625fba;
    }

    // === PARCELABLE Implementation ===
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(wordId);
        dest.writeString(word);
        dest.writeString(translation);
        dest.writeString(note);
        dest.writeByte((byte) (isFavorite ? 1 : 0));
        dest.writeInt(difficulty);
        dest.writeInt(reviewCount);
        dest.writeInt(correctAnswers);
        dest.writeByte((byte) (isCustomWord ? 1 : 0));
        dest.writeString(libraryId);
        dest.writeString(userId);
        dest.writeLong(createdAt != null ? createdAt.getTime() : -1);
        dest.writeLong(lastReviewed != null ? lastReviewed.getTime() : -1);
        dest.writeInt(reviewStage);
        dest.writeLong(nextReviewDate != null ? nextReviewDate.getTime() : -1);
        dest.writeInt(consecutiveShows);
    }

    public static final Creator<WordItem> CREATOR = new Creator<WordItem>() {
        @Override
        public WordItem createFromParcel(Parcel in) {
            return new WordItem(in);
        }

        @Override
        public WordItem[] newArray(int size) {
            return new WordItem[size];
        }
    };

    protected WordItem(Parcel in) {
        wordId = in.readString();
        word = in.readString();
        translation = in.readString();
        note = in.readString();
        isFavorite = in.readByte() != 0;
        difficulty = in.readInt();
        reviewCount = in.readInt();
        correctAnswers = in.readInt();
        isCustomWord = in.readByte() != 0;
        libraryId = in.readString();
        userId = in.readString();
        long createdAtMs = in.readLong();
        createdAt = createdAtMs != -1 ? new Date(createdAtMs) : null;
        long lastReviewedMs = in.readLong();
        lastReviewed = lastReviewedMs != -1 ? new Date(lastReviewedMs) : null;
        reviewStage = in.readInt();
        long nextReviewDateMs = in.readLong();
        nextReviewDate = nextReviewDateMs != -1 ? new Date(nextReviewDateMs) : null;
        consecutiveShows = in.readInt();
    }
}