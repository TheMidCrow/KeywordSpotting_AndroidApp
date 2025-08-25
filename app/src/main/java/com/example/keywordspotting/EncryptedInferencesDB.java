package com.example.keywordspotting;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class EncryptedInferencesDB {

    private static final String TAG = "EncryptedInferencesDB";
    private static EncryptedInferencesDB instance;
    private AppDatabase database;

    private EncryptedInferencesDB(Context context) {
        database = AppDatabase.getInstance(context);
    }

    public static synchronized EncryptedInferencesDB getInstance(Context context) {
        if (instance == null) {
            instance = new EncryptedInferencesDB(context);
        }
        return instance;
    }

    public void addOrUpdateInference(InferenceEntity inference) {
        try {
            if (inference.getId() > 0) {
                database.inferenceDao().updateInference(inference);
                Log.d(TAG, "Inference updated: " + inference.getId());
            } else {
                long id = database.inferenceDao().insertInference(inference);
                inference.setId(id);
                Log.d(TAG, "Inference added with ID: " + id);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding/updating inference", e);
        }
    }

    public List<InferenceEntity> getAllInferences() {
        try {
            List<InferenceEntity> inferences = database.inferenceDao().getAllInferences();

            Log.d(TAG, "Retrieved " + inferences.size() + " inferences from database");
            return inferences;
        } catch (Exception e) {
            Log.e(TAG, "Error getting all inferences", e);
            return new ArrayList<>();
        }
    }

    public boolean deleteAllInferences() {
        try {
            database.inferenceDao().deleteAllInferences();
            Log.d(TAG, "All inferences deleted successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting all inferences", e);
            return false;
        }
    }

    public void close() {
        Log.d(TAG, "EncryptedInferencesDB instance closed");
    }
}