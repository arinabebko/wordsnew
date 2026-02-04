package com.example.newwords;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

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


}