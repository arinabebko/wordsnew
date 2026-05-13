package com.example.newwords;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import androidx.annotation.NonNull;

import java.util.Date;
import java.util.Map;

@Entity(tableName = "local_libraries")
@TypeConverters(Converters.class)
public class LocalWordLibrary {
    @PrimaryKey
    @NonNull
    private String libraryId;
    private Map<String, String> name;
    private Map<String, String> description;
    private Map<String, String> subcategory;

    private int wordCount;
    private String category;
    private String languageFrom;
    private String languageTo;
    private boolean isPublic;
    private boolean isActive;
    private Date createdAt;
    private String createdBy;
    private Date lastSynced;

    // Конструкторы, геттеры и сеттеры
    public LocalWordLibrary() {
        this.libraryId = ""; // Инициализируем пустой строкой
    }

    public LocalWordLibrary(WordLibrary library) {
        this.libraryId = library.getLibraryId() != null ? library.getLibraryId() : "";
        this.name = library.getName();
        this.description = library.getDescription();
        this.wordCount = library.getWordCount();
        this.category = library.getCategory();
        this.languageFrom = library.getLanguageFrom();  // ✅ ДОБАВИТЬ!
        this.languageTo = library.getLanguageTo();
        this.isPublic = library.isPublic();
        this.createdAt = library.getCreatedAt();
        this.createdBy = library.getCreatedBy();
        this.lastSynced = new Date();
        this.isActive = library.getIsActive();
    }

    // === ГЕТТЕРЫ ===
    @NonNull
    public String getLibraryId() { return libraryId; }
    public Map<String, String> getName() { return name; }
    public Map<String, String> getDescription() { return description; }
    public Map<String, String> getSubcategory() { return subcategory; }
    public int getWordCount() { return wordCount; }
    public String getCategory() { return category; }
    public String getLanguageFrom() { return languageFrom; }
    public String getLanguageTo() { return languageTo; }
    public boolean isPublic() { return isPublic; }
    public boolean isActive() { return isActive; }
    public Date getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
    public Date getLastSynced() { return lastSynced; }

    // === СЕТТЕРЫ ===
    public void setLibraryId(@NonNull String libraryId) { this.libraryId = libraryId; }
    public void setName(Map<String, String> name) { this.name = name; }
    public void setDescription(Map<String, String> description) { this.description = description; }
    public void setSubcategory(Map<String, String> subcategory) { this.subcategory = subcategory; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }
    public void setCategory(String category) { this.category = category; }
    public void setLanguageFrom(String languageFrom) { this.languageFrom = languageFrom; }
    public void setLanguageTo(String languageTo) { this.languageTo = languageTo; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }
    public void setActive(boolean active) { isActive = active; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setLastSynced(Date lastSynced) { this.lastSynced = lastSynced; }
}