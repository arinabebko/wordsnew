package com.example.newwords;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class WordLibrary {
    private String libraryId;
    private String name;
    private String description;
    private int wordCount;
    private String category;
    private String languageFrom;
    private String languageTo;
    private boolean isPublic;
    private boolean isActive;
    private Date createdAt;
    private String createdBy;



    // НОВОЕ: храним активность для каждого языка
    private Map<String, Boolean> languageActiveStates = new HashMap<>();


    // === НОВЫЕ МЕТОДЫ ДЛЯ УПРАВЛЕНИЯ АКТИВНОСТЬЮ ПО ЯЗЫКАМ ===

    /**
     * Установить активность для конкретного языка
     */
    public void setActiveForLanguage(String languageCode, boolean active) {
        if (languageActiveStates == null) {
            languageActiveStates = new HashMap<>();
        }
        languageActiveStates.put(languageCode, active);
    }

    /**
     * Получить активность для конкретного языка
     */
    public boolean isActiveForLanguage(String languageCode) {
        if (languageActiveStates == null) {
            return false;
        }
        Boolean active = languageActiveStates.get(languageCode);
        return active != null ? active : false;
    }

    /**
     * Получить все состояния по языкам
     */
    public Map<String, Boolean> getLanguageActiveStates() {
        if (languageActiveStates == null) {
            languageActiveStates = new HashMap<>();
        }
        return languageActiveStates;
    }

    /**
     * Установить все состояния по языкам
     */
    public void setLanguageActiveStates(Map<String, Boolean> states) {
        this.languageActiveStates = states;
    }

    // Конструктор по умолчанию для Firestore
    public WordLibrary() {
        // Инициализируем поля пустыми строками по умолчанию
        this.name = "";
        this.description = "";
        this.category = "custom";
        this.languageFrom = "en";
        this.languageTo = "ru";
        this.languageActiveStates = new HashMap<>();
        this.languageActiveStates.put("en", false); // английский
        this.languageActiveStates.put("ba", false); // башкирский
    }

    // Конструктор с параметрами
    public WordLibrary(String name, String description, int wordCount,
                       String category, String languageFrom, String languageTo) {
        // ЗАЩИТА ОТ NULL: инициализируем все строковые поля
        this.name = name != null ? name : "";
        this.description = description != null ? description : "";
        this.wordCount = wordCount;
        this.category = category != null ? category : "custom";
        this.languageFrom = languageFrom != null ? languageFrom : "en";
        this.languageTo = languageTo != null ? languageTo : "ru";
        this.isPublic = true;
        this.isActive = false;
        this.createdAt = new Date();
    }

    // === ГЕТТЕРЫ ===
    public String getLibraryId() { return libraryId != null ? libraryId : ""; }
    public String getName() { return name != null ? name : ""; }
    public String getDescription() { return description != null ? description : ""; }
    public int getWordCount() { return wordCount; }
    public String getCategory() { return category != null ? category : "custom"; }
    public String getLanguageFrom() { return languageFrom != null ? languageFrom : "en"; }
    public String getLanguageTo() { return languageTo != null ? languageTo : "ru"; }
    public boolean isPublic() { return isPublic; }
    public Date getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy != null ? createdBy : ""; }

    // Добавляем методы для активности
    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    // === СЕТТЕРЫ ===
    public void setLibraryId(String libraryId) { this.libraryId = libraryId != null ? libraryId : ""; }
    public void setName(String name) { this.name = name != null ? name : ""; }
    public void setDescription(String description) { this.description = description != null ? description : ""; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }
    public void setCategory(String category) { this.category = category != null ? category : "custom"; }
    public void setLanguageFrom(String languageFrom) { this.languageFrom = languageFrom != null ? languageFrom : "en"; }
    public void setLanguageTo(String languageTo) { this.languageTo = languageTo != null ? languageTo : "ru"; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy != null ? createdBy : ""; }

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