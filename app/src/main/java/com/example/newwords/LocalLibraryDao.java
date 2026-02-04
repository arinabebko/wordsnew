package com.example.newwords;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LocalLibraryDao {
    // 1. Массовая вставка (то, что мы внедряем для SSoT)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<LocalWordLibrary> libraries);

    // 2. Исправленный метод удаления по языку
    // ВАЖНО: Проверь, как называется поле в LocalWordLibrary.
    // Если "languageTo", оставь так. Если "languageFrom", замени ниже.
    @Query("DELETE FROM local_libraries WHERE languageTo = :lang")
    void deleteByLanguage(String lang);

    // 3. Остальные методы (уже были у тебя, оставляем для совместимости)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLibrary(LocalWordLibrary library);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLibraries(List<LocalWordLibrary> libraries);

    @Update
    void updateLibrary(LocalWordLibrary library);

    @Query("SELECT * FROM local_libraries")
    List<LocalWordLibrary> getAllLibraries();

    @Query("SELECT * FROM local_libraries WHERE libraryId = :libraryId")
    LocalWordLibrary getLibraryById(String libraryId);

    @Query("DELETE FROM local_libraries WHERE libraryId = :libraryId")
    void deleteLibrary(@NonNull String libraryId);

    @Query("DELETE FROM local_libraries")
    void clearAllLibraries();

    @Query("SELECT libraryId FROM local_libraries WHERE isActive = 1")
    List<String> getActiveLibraryIds();

    @Query("SELECT * FROM local_libraries WHERE isActive = 1")
    List<LocalWordLibrary> getActiveLibraries();

    @Query("UPDATE local_libraries SET isActive = :isActive WHERE libraryId = :libraryId")
    void updateLibraryActiveStatus(String libraryId, boolean isActive);

    @Query("SELECT COUNT(*) FROM local_libraries WHERE libraryId = :libraryId AND isActive = 1")
    int isLibraryActive(String libraryId);

    @Query("UPDATE local_libraries SET isActive = :active WHERE languageTo = :lang")
    void deactivateAllForLanguage(String lang, boolean active);

    @Query("SELECT * FROM local_libraries WHERE languageTo = :lang AND isActive = 1")
    List<LocalWordLibrary> getActiveLibrariesByLanguage(String lang);

    @Query("UPDATE local_libraries SET wordCount = wordCount + 1 WHERE libraryId = :libraryId")
    void incrementWordCount(String libraryId);

    // А этот метод пригодится при удалении слова:
    @Query("UPDATE local_libraries SET wordCount = CASE WHEN wordCount > 0 THEN wordCount - 1 ELSE 0 END WHERE libraryId = :libraryId")
    void decrementWordCount(String libraryId);


}