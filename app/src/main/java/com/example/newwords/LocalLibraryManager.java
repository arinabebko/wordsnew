package com.example.newwords;

import android.content.Context;
import android.util.Log;
import java.util.List;

public class LocalLibraryManager {
    private static final String TAG = "LocalLibraryManager";
    private AppDatabase localDb;

    public LocalLibraryManager(Context context) {
        this.localDb = AppDatabase.getInstance(context);
    }

    /**
     * Получить ID активных библиотек
     */
    public List<String> getActiveLibraryIds() {
        return localDb.libraryDao().getActiveLibraryIds();
    }

    /**
     * Установить статус активности библиотеки
     */
    public void setLibraryActiveStatus(String libraryId, boolean isActive) {
        Log.d(TAG, "Установка статуса библиотеки " + libraryId + " = " + isActive);
        localDb.libraryDao().updateLibraryActiveStatus(libraryId, isActive);
    }

    /**
     * Проверить активна ли библиотека
     */
    public boolean isLibraryActive(String libraryId) {
        int count = localDb.libraryDao().isLibraryActive(libraryId);
        return count > 0;
    }

    /**
     * Получить все активные библиотеки
     */
    public List<LocalWordLibrary> getActiveLibraries() {
        return localDb.libraryDao().getActiveLibraries();
    }
}