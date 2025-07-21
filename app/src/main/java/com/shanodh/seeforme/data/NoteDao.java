package com.shanodh.seeforme.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Data Access Object for Note entity
 */
@Dao
public interface NoteDao {
    @Insert
    long insert(Note note);

    @Update
    void update(Note note);

    @Delete
    void delete(Note note);

    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    LiveData<List<Note>> getAllNotes();

    @Query("SELECT * FROM notes WHERE id = :id")
    LiveData<Note> getNoteById(int id);

    @Query("UPDATE notes SET isDetected = 1 WHERE id = :id")
    void markAsDetected(int id);

    @Query("SELECT * FROM notes WHERE isDetected = 0")
    List<Note> getUndetectedNotes();

    @Query("SELECT * FROM notes WHERE isSynced = 0")
    List<Note> getUnsyncedNotes();

    @Query("SELECT * FROM notes WHERE firestoreId = :firebaseId")
    Note getNoteByFirebaseId(String firebaseId);

    @Query("SELECT COUNT(*) FROM notes WHERE isSynced = 0")
    int getUnsyncedNotesCount();
}