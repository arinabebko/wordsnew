package com.example.newwords;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private static final String TAG = "LanguageManager";
    private static final String PREFS_NAME = "LanguagePrefs";
    private static final String KEY_CURRENT_LANGUAGE = "current_language";
    private static final String KEY_ACTIVE_LIBRARIES_PREFIX = "active_libs_";

    private final SharedPreferences prefs;
    private String currentLanguage = "en";

    public LanguageManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currentLanguage = prefs.getString(KEY_CURRENT_LANGUAGE, "en");
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * Установить текущий язык с сохранением состояния активных библиотек
     */
    public void setCurrentLanguage(String languageCode, Map<String, Boolean> currentActiveLibraries) {
        if (!languageCode.equals(currentLanguage)) {
            // Сохраняем активные библиотеки для ТЕКУЩЕГО языка перед сменой
            if (currentActiveLibraries != null && !currentActiveLibraries.isEmpty()) {
                saveActiveLibrariesForLanguage(currentLanguage, currentActiveLibraries);
                Log.d(TAG, "Сохранены активные библиотеки для языка " + currentLanguage +
                        ": " + currentActiveLibraries.size() + " шт.");
            }

            // Меняем язык
            currentLanguage = languageCode;
            prefs.edit().putString(KEY_CURRENT_LANGUAGE, languageCode).apply();
            Log.d(TAG, "Язык изменен на: " + languageCode);
        }
    }

    /**
     * Просто установить язык без сохранения
     */
    public void setCurrentLanguage(String languageCode) {
        if (!languageCode.equals(currentLanguage)) {
            currentLanguage = languageCode;
            prefs.edit().putString(KEY_CURRENT_LANGUAGE, languageCode).apply();
            Log.d(TAG, "Язык изменен на: " + languageCode);
        }
    }

    /**
     * Получить активные библиотеки для языка в виде Map
     */
    public Map<String, Boolean> getActiveLibrariesMapForLanguage(String languageCode) {
        String key = KEY_ACTIVE_LIBRARIES_PREFIX + languageCode;
        String activeLibsString = prefs.getString(key, "");
        Map<String, Boolean> result = new HashMap<>();

        if (activeLibsString != null && !activeLibsString.isEmpty()) {
            String[] libraryIds = activeLibsString.split(",");
            for (String libraryId : libraryIds) {
                String trimmedId = libraryId.trim();
                if (!trimmedId.isEmpty()) {
                    result.put(trimmedId, true);
                }
            }
        }

        Log.d(TAG, "Загружено активных библиотек для " + languageCode +
                ": " + result.size() + " шт.");
        return result;
    }

    /**
     * Получить активные библиотеки для текущего языка
     */
    public Map<String, Boolean> getActiveLibrariesMapForCurrentLanguage() {
        return getActiveLibrariesMapForLanguage(currentLanguage);
    }

    /**
     * Сохранить активные библиотеки для языка
     */
    public void saveActiveLibrariesForLanguage(String languageCode, Map<String, Boolean> activeLibrariesMap) {
        StringBuilder activeLibsBuilder = new StringBuilder();

        // ИСПРАВЛЕНИЕ: Используем Map.Entry для итерации
        for (Map.Entry<String, Boolean> entry : activeLibrariesMap.entrySet()) {
            if (entry.getValue()) {
                if (activeLibsBuilder.length() > 0) {
                    activeLibsBuilder.append(",");
                }
                activeLibsBuilder.append(entry.getKey());
            }
        }

        String activeLibsString = activeLibsBuilder.toString();
        String key = KEY_ACTIVE_LIBRARIES_PREFIX + languageCode;

        prefs.edit().putString(key, activeLibsString).apply();
        Log.d(TAG, "Сохранено для языка " + languageCode +
                ": " + activeLibsString);
    }

    /**
     * Сохранить активные библиотеки для текущего языка
     */
    public void saveActiveLibrariesForCurrentLanguage(Map<String, Boolean> activeLibrariesMap) {
        saveActiveLibrariesForLanguage(currentLanguage, activeLibrariesMap);
    }

    /**
     * Получить ID активных библиотек для текущего языка (строка)
     */
    public String getActiveLibrariesForCurrentLanguage() {
        String key = KEY_ACTIVE_LIBRARIES_PREFIX + currentLanguage;
        return prefs.getString(key, "");
    }

    /**
     * Получить ID активных библиотек для конкретного языка (строка)
     */
    public String getActiveLibrariesForLanguage(String languageCode) {
        String key = KEY_ACTIVE_LIBRARIES_PREFIX + languageCode;
        return prefs.getString(key, "");
    }

    /**
     * Сохранить активные библиотеки для текущего языка (алиас)
     */
    public void saveActiveLibraries(Map<String, Boolean> activeLibrariesMap) {
        saveActiveLibrariesForCurrentLanguage(activeLibrariesMap);
    }

    /**
     * Сохранить состояние одной библиотеки для языка
     */
    public void saveLibraryStateForLanguage(String languageCode, String libraryId, boolean isActive) {
        String key = KEY_ACTIVE_LIBRARIES_PREFIX + languageCode;
        String current = prefs.getString(key, "");

        if (isActive) {
            if (!current.contains(libraryId)) {
                current += (current.isEmpty() ? "" : ",") + libraryId;
            }
        } else {
            current = current.replace(libraryId, "").replace(",,", ",").replaceAll("^,|,$", "");
        }

        prefs.edit().putString(key, current).apply();
        Log.d(TAG, "Сохранено состояние для " + libraryId + " на языке " + languageCode + ": " + isActive);
    }

    /**
     * Проверить активна ли библиотека для языка
     */
    public boolean isLibraryActiveForLanguage(String languageCode, String libraryId) {
        String key = KEY_ACTIVE_LIBRARIES_PREFIX + languageCode;
        String current = prefs.getString(key, "");
        return current.contains(libraryId);
    }

    /**
     * Проверить активна ли библиотека для текущего языка
     */
    public boolean isLibraryActiveForCurrentLanguage(String libraryId) {
        return isLibraryActiveForLanguage(currentLanguage, libraryId);
    }

    /**
     * Очистить сохраненные состояния для языка
     */
    public void clearLanguageStates(String languageCode) {
        String key = KEY_ACTIVE_LIBRARIES_PREFIX + languageCode;
        prefs.edit().remove(key).apply();
        Log.d(TAG, "Очищены состояния для языка: " + languageCode);
    }

    /**
     * Получить отображаемое имя языка
     */
    public String getLanguageDisplayName(String languageCode) {
        switch (languageCode) {
            case "en": return "Английский";
            case "ba": return "Башкирский";
            default: return languageCode;
        }
    }

    /**
     * Получить все сохраненные языки
     */
    public String[] getAvailableLanguages() {
        return new String[]{"en", "ba"};
    }
}