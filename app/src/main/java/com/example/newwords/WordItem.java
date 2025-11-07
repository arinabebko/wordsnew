
package com.example.newwords;

import java.util.Date;

public class WordItem {
    private String wordId;
    private String word;
    private String translation;
    private String note;
    private boolean isFavorite;
    private int difficulty;
    private Date lastReviewed;
    private int reviewCount;
    private int correctAnswers;
    private Date createdAt;

    // Новые поля для Firestore
    private String libraryId;      // Из какой библиотеки слово
    private boolean isCustomWord;  // true если слово добавлено пользователем
    private String userId;         // Владелец слова

    // Конструктор по умолчанию (ОБЯЗАТЕЛЬНО для Firestore!)
    public WordItem() {
        // Пустой конструктор нужен для Firestore
    }

    // Конструктор для кастомных слов (пользовательских)
    public WordItem(String word, String translation, String note) {
        this.word = word;
        this.translation = translation;
        this.note = note;
        this.isFavorite = false;
        this.difficulty = 1;
        this.reviewCount = 0;
        this.correctAnswers = 0;
        this.isCustomWord = true;
        this.createdAt = new Date();
    }

    // Конструктор для слов из библиотек
    public WordItem(String word, String translation, String note, String libraryId) {
        this(word, translation, note);
        this.libraryId = libraryId;
        this.isCustomWord = false;
    }

    // === ГЕТТЕРЫ ===
    public String getWordId() { return wordId; }
    public String getWord() { return word; }
    public String getTranslation() { return translation; }
    public String getNote() { return note; }
    public boolean isFavorite() { return isFavorite; }
    public int getDifficulty() { return difficulty; }
    public Date getLastReviewed() { return lastReviewed; }
    public int getReviewCount() { return reviewCount; }
    public int getCorrectAnswers() { return correctAnswers; }
    public Date getCreatedAt() { return createdAt; }
    public String getLibraryId() { return libraryId; }
    public boolean isCustomWord() { return isCustomWord; }
    public String getUserId() { return userId; }

    // === СЕТТЕРЫ ===
    public void setWordId(String wordId) { this.wordId = wordId; }
    public void setWord(String word) { this.word = word; }
    public void setTranslation(String translation) { this.translation = translation; }
    public void setNote(String note) { this.note = note; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }
    public void setLastReviewed(Date lastReviewed) { this.lastReviewed = lastReviewed; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setLibraryId(String libraryId) { this.libraryId = libraryId; }
    public void setCustomWord(boolean customWord) { isCustomWord = customWord; }
    public void setUserId(String userId) { this.userId = userId; }

    // Вспомогательный метод для отладки
    @Override
    public String toString() {
        return "WordItem{" +
                "word='" + word + '\'' +
                ", translation='" + translation + '\'' +
                ", libraryId='" + libraryId + '\'' +
                ", isCustom=" + isCustomWord +
                '}';
    }
}

/*

package com.example.newwords;

public class WordItem {
    // Поля класса - это данные, которые мы храним
    private String word;          // английское слово (например "spring")
    private String translation;   // перевод (например "весна")
    private String note;          // примечание (например "Сезон года")
    private boolean isFavorite;   // добавлено в избранное? (true/false)

    // Конструктор - метод для создания нового объекта WordItem
    public WordItem(String word, String translation, String note) {
        this.word = word;
        this.translation = translation;
        this.note = note;
        this.isFavorite = false; // по умолчанию не в избранном
    }

    // ↓↓↓ ГЕТТЕРЫ - методы для получения данных ↓↓↓

    // Получить слово
    public String getWord() {
        return word;
    }

    // Получить перевод
    public String getTranslation() {
        return translation;
    }

    // Получить примечание
    public String getNote() {
        return note;
    }

    // Проверить, в избранном ли слово
    public boolean isFavorite() {
        return isFavorite;
    }

    // ↓↓↓ СЕТТЕРЫ - методы для изменения данных ↓↓↓

    // Изменить слово
    public void setWord(String word) {
        this.word = word;
    }

    // Изменить перевод
    public void setTranslation(String translation) {
        this.translation = translation;
    }

    // Изменить примечание
    public void setNote(String note) {
        this.note = note;
    }

    // Добавить/убрать из избранного
    public void setFavorite(boolean favorite) {
        this.isFavorite = favorite;
    }
}
*/
