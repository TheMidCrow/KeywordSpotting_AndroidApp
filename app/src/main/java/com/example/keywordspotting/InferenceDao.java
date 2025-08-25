package com.example.keywordspotting;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface InferenceDao {

    @Query("SELECT * FROM inferences ORDER BY id DESC")
    List<InferenceEntity> getAllInferences();

    @Insert
    long insertInference(InferenceEntity inference);

    @Update
    void updateInference(InferenceEntity inference);

    @Query("DELETE FROM inferences")
    void deleteAllInferences();
}