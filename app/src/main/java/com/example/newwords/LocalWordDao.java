package com.example.newwords;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.Date;
import java.util.List;

@Dao
public interface LocalWordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWord(LocalWordItem word);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWords(List<LocalWordItem> words);

    @Update
    void updateWord(LocalWordItem word);

    @Query("SELECT * FROM local_words")
    List<LocalWordItem> getAllWords();

    @Query("SELECT * FROM local_words WHERE libraryId = :libraryId")
    List<LocalWordItem> getWordsByLibrary(String libraryId);

    @Query("SELECT * FROM local_words WHERE isFavorite = 1")
    List<LocalWordItem> getFavoriteWords();

    @Query("SELECT w.* FROM local_words w " +
            "JOIN local_libraries l ON w.libraryId = l.libraryId " +
            "WHERE l.isActive = 1")
    List<LocalWordItem> getWordsFromActiveLibraries();

    @Query("SELECT * FROM local_words WHERE wordId = :wordId")
    LocalWordItem getWordById(String wordId);

    @Query("DELETE FROM local_words WHERE wordId = :wordId")
    void deleteWord(@NonNull String wordId);
    @Query("DELETE FROM local_words WHERE libraryId = :libraryId")
    void deleteWordsByLibrary(String libraryId);

    @Query("DELETE FROM local_words")
    void clearAllWords();

    @Query("UPDATE local_words SET isFavorite = :isFav WHERE wordId = :id")
    void updateFavoriteStatus(String id, boolean isFav);





    @Query("SELECT * FROM local_libraries WHERE languageFrom = :language")
    List<LocalWordLibrary> getLibrariesByLanguage(String language);


    // ========== НОВЫЕ МЕТОДЫ ДЛЯ ПРОГРЕССА ==========

    /**
     * Обновляет прогресс слова (для системы повторений)
     */
    @Query("UPDATE local_words SET " +
            "reviewStage = :reviewStage, " +
            "consecutiveShows = :consecutiveShows, " +
            "nextReviewDate = :nextReviewDate, " +
            "lastReviewed = :lastReviewed " +
            "WHERE wordId = :wordId")
    void updateWordProgress(String wordId,
                            int reviewStage,
                            int consecutiveShows,
                            Date nextReviewDate,
                            Date lastReviewed);

    /**
     * Обновляет только этап и дату следующего повторения
     */
    @Query("UPDATE local_words SET " +
            "reviewStage = :stage, " +
            "nextReviewDate = :nextDate " +
            "WHERE wordId = :wordId")
    void updateReviewStageAndDate(String wordId, int stage, Date nextDate);

    /**
     * Увеличивает счетчик consecutiveShows на 1
     */
    @Query("UPDATE local_words SET consecutiveShows = consecutiveShows + 1 WHERE wordId = :wordId")
    void incrementConsecutiveShows(String wordId);

    /**
     * Сбрасывает счетчик consecutiveShows (когда слово не выучено)
     */
    @Query("UPDATE local_words SET consecutiveShows = 0 WHERE wordId = :wordId")
    void resetConsecutiveShows(String wordId);

    /**
     * Получить слова, готовые к повторению (nextReviewDate <= сейчас И не выучены)
     */
    @Query("SELECT * FROM local_words WHERE " +
            "reviewStage < 6 AND " +
            "nextReviewDate <= :currentDate " +
            "ORDER BY nextReviewDate ASC")
    List<LocalWordItem> getDueWords(Date currentDate);
    // LocalLibraryDao.java
    @Query("UPDATE local_libraries SET wordCount = wordCount + 1 WHERE libraryId = :libraryId")
    void incrementWordCount(String libraryId);
    /**
     * Получить слова в процессе изучения (stage 1-5)
     */
    @Query("SELECT * FROM local_words WHERE reviewStage BETWEEN 1 AND 5")
    List<LocalWordItem> getLearningWords();

    /**
     * Получить количество слов в процессе изучения
     */
    @Query("SELECT COUNT(*) FROM local_words WHERE reviewStage BETWEEN 1 AND 5")
    int getLearningWordsCount();

    /**
     * Получить количество выученных слов (stage >= 6)
     */
    @Query("SELECT COUNT(*) FROM local_words WHERE reviewStage >= 6")
    int getLearnedWordsCount();

    /**
     * Получить все слова для конкретной библиотеки с сортировкой по прогрессу
     */
    @Query("SELECT * FROM local_words WHERE libraryId = :libraryId ORDER BY reviewStage ASC, nextReviewDate ASC")
    List<LocalWordItem> getWordsByLibrarySorted(String libraryId);
}
