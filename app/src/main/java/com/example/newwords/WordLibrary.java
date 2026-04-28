package com.example.newwords;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WordLibrary {
    private String libraryId;

    // СЛОВАРИ: теперь соответствуют структуре в Firebase и LocalWordLibrary
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

    // Состояния активности по языкам
    private Map<String, Boolean> languageActiveStates = new HashMap<>();

    // Конструктор по умолчанию для Firestore
    public WordLibrary() {
        this.name = new HashMap<>();
        this.description = new HashMap<>();
        this.subcategory = new HashMap<>();
        this.category = "custom";
        this.languageActiveStates = new HashMap<>();
    }

    // === УМНЫЕ МЕТОДЫ ДЛЯ АДАПТЕРА (UI) ===

    public String getLocalizedName() {
        if (name == null || name.isEmpty()) return "";

        String lang = Locale.getDefault().getLanguage();
        // 1. Если есть название на языке телефона, берем его
        if (name.containsKey(lang)) return name.get(lang);

        // 2. Если нет (для пользовательских либ), берем самое первое, что ввел юзер
        // values().iterator().next() достает первую попавшуюся строку из словаря
        return name.values().iterator().next();
    }

    public String getLocalizedDescription() {
        if (description == null || description.isEmpty()) return "";

        String lang = Locale.getDefault().getLanguage();
        if (description.containsKey(lang)) return description.get(lang);

        // Берем любое доступное описание
        return description.values().iterator().next();
    }



    // === ГЕТТЕРЫ И СЕТТЕРЫ ДЛЯ МАР (Чтобы не было ошибок в Repository) ===

    public Map<String, String> getName() { return name; }
    public void setName(Map<String, String> name) { this.name = name; }

    public Map<String, String> getDescription() { return description; }
    public void setDescription(Map<String, String> description) { this.description = description; }

    public Map<String, String> getSubcategory() { return subcategory; }
    public void setSubcategory(Map<String, String> subcategory) { this.subcategory = subcategory; }

    // === МЕТОДЫ АКТИВНОСТИ (Важно для совместимости с твоим Room и Repository) ===

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    // Дублируем для кеширования (так как твой код ищет именно эти названия)
    public boolean getIsActive() { return isActive; }
    public void setIsActive(boolean isActive) { this.isActive = isActive; }

    // === ОСТАЛЬНЫЕ ПОЛЯ ===

    public String getLibraryId() { return libraryId != null ? libraryId : ""; }
    public void setLibraryId(String libraryId) { this.libraryId = libraryId; }

    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLanguageFrom() { return languageFrom; }
    public void setLanguageFrom(String languageFrom) { this.languageFrom = languageFrom; }

    public String getLanguageTo() { return languageTo; }
    public void setLanguageTo(String languageTo) { this.languageTo = languageTo; }
    public void setActiveForLanguage(String languageCode, boolean active) {
        if (languageActiveStates == null) languageActiveStates = new HashMap<>();
        languageActiveStates.put(languageCode, active);
    }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Map<String, Boolean> getLanguageActiveStates() { return languageActiveStates; }
    public void setLanguageActiveStates(Map<String, Boolean> states) { this.languageActiveStates = states; }
    public String getLocalizedSubcategory() {
        // 1. Если словаря нет или он пустой — возвращаем пустоту
        if (subcategory == null || subcategory.isEmpty()) return "";

        String lang = Locale.getDefault().getLanguage();

        // 2. Сначала ищем на языке телефона (например, "ru")
        if (subcategory.containsKey(lang)) return subcategory.get(lang);

        // 3. Если нет, ищем на русском (запасной вариант для системных либ)
        if (subcategory.containsKey("ru")) return subcategory.get("ru");

        // 4. Если и этого нет, берем любое первое значение, которое там есть
        return subcategory.values().iterator().next();
    }
    @Override
    public String toString() {
        return "WordLibrary{" +
                "id='" + libraryId + '\'' +
                ", localizedName='" + getLocalizedName() + '\'' +
                ", category='" + category + '\'' +
                '}';
    }
}