package com.shanodh.seeforme.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Data Access Object for FamiliarFace entity
 */
@Dao
public interface FamiliarFaceDao {
    @Insert
    long insert(FamiliarFace face);

    @Update
    void update(FamiliarFace face);

    @Delete
    void delete(FamiliarFace face);

    @Query("SELECT * FROM familiar_faces ORDER BY timestamp DESC")
    LiveData<List<FamiliarFace>> getAllFaces();

    @Query("SELECT * FROM familiar_faces WHERE id = :id")
    LiveData<FamiliarFace> getFaceById(int id);

    @Query("UPDATE familiar_faces SET isSynced = 1 WHERE id = :id")
    void markAsSynced(int id);

    @Query("SELECT * FROM familiar_faces WHERE isSynced = 0")
    List<FamiliarFace> getUnsyncedFaces();

    @Query("SELECT * FROM familiar_faces WHERE firebaseId = :firebaseId")
    FamiliarFace getFaceByFirebaseId(String firebaseId);
}