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
    private Date createdAt;
    private String createdBy;
    private boolean isActive; // ← ДОБАВЛЯЕМ ЭТО ПОЛЕ

    // Конструктор по умолчанию для Firestore
    public WordLibrary() {
        this.isActive = false; // по умолчанию неактивна
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
        this.createdAt = new Date();
        this.isActive = false; // по умолчанию неактивна
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
    public boolean isActive() { return isActive; } // ← ДОБАВЛЯЕМ ГЕТТЕР

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
    public void setActive(boolean active) { isActive = active; } // ← ДОБАВЛЯЕМ СЕТТЕР

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