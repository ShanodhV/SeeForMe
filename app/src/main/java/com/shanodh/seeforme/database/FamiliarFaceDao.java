package com.shanodh.seeforme.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.shanodh.seeforme.models.FamiliarFace;

import java.util.List;

@Dao
public interface FamiliarFaceDao {
    
    @Query("SELECT * FROM familiar_faces ORDER BY createdAt DESC")
    List<FamiliarFace> getAllFaces();

    @Query("SELECT * FROM familiar_faces WHERE id = :id")
    FamiliarFace getFaceById(int id);

    @Query("SELECT * FROM familiar_faces WHERE name LIKE :name")
    List<FamiliarFace> getFacesByName(String name);

    @Insert
    long insertFace(FamiliarFace face);

    @Update
    void updateFace(FamiliarFace face);

    @Delete
    void deleteFace(FamiliarFace face);

    @Query("DELETE FROM familiar_faces WHERE id = :id")
    void deleteFaceById(int id);

    @Query("SELECT COUNT(*) FROM familiar_faces")
    int getFaceCount();
}
