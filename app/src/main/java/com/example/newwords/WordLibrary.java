package com.example.newwords;

import java.util.Date;

public class WordLibrary {
    private String libraryId;
    private String name;
    private String description;
    private int wordCount;
    private String category;
    private String languageFrom;
    private String languageTo;
    private boolean isPublic;
    private boolean isActive; // Добавляем поле isActive
    private Date createdAt;
    private String createdBy;

    // Конструктор по умолчанию для Firestore
    public WordLibrary() {
    }

    // Конструктор с параметрами
    public WordLibrary(String name, String description, int wordCount,
                       String category, String languageFrom, String languageTo) {
        this.name = name;
        this.description = description;
        this.wordCount = wordCount;
        this.category = category;
        this.languageFrom = languageFrom;
        this.languageTo = languageTo;
        this.isPublic = true;
        this.isActive = false; // По умолчанию неактивна
        this.createdAt = new Date();
    }

    // === ГЕТТЕРЫ ===
    public String getLibraryId() { return libraryId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getWordCount() { return wordCount; }
    public String getCategory() { return category; }
    public String getLanguageFrom() { return languageFrom; }
    public String getLanguageTo() { return languageTo; }
    public boolean isPublic() { return isPublic; }
    public Date getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }

    // Добавляем методы для активности
    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    // === СЕТТЕРЫ ===
    public void setLibraryId(String libraryId) { this.libraryId = libraryId; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }
    public void setCategory(String category) { this.category = category; }
    public void setLanguageFrom(String languageFrom) { this.languageFrom = languageFrom; }
    public void setLanguageTo(String languageTo) { this.languageTo = languageTo; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    // === ДОБАВЛЯЕМ ТОЛЬКО НУЖНЫЕ ДЛЯ КЕША МЕТОДЫ ===

    /**
     * Для кеширования - проверка активности библиотеки
     */
    public boolean getIsActive() {
        return isActive;
    }

    /**
     * Для кеширования - установка активности
     */
    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public String toString() {
        return "WordLibrary{" +
                "name='" + name + '\'' +
                ", wordCount=" + wordCount +
                ", category='" + category + '\'' +
                ", isActive=" + isActive +
                '}';
    }

}