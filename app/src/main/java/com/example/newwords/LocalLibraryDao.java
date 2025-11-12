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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLibrary(LocalWordLibrary library);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLibraries(List<LocalWordLibrary> libraries);

    @Update
    void updateLibrary(LocalWordLibrary library);

    @Query("SELECT * FROM local_libraries")
    List<LocalWordLibrary> getAllLibraries();

    @Query("SELECT * FROM local_libraries WHERE isActive = 1")
    List<LocalWordLibrary> getActiveLibraries();

    @Query("SELECT * FROM local_libraries WHERE libraryId = :libraryId")
    LocalWordLibrary getLibraryById(String libraryId);


    @Query("UPDATE local_libraries SET isActive = :isActive WHERE libraryId = :libraryId")
    void updateLibraryActiveStatus(@NonNull String libraryId, boolean isActive);

    @Query("DELETE FROM local_libraries WHERE libraryId = :libraryId")
    void deleteLibrary(@NonNull String libraryId);

    @Query("DELETE FROM local_libraries")
    void clearAllLibraries();
}