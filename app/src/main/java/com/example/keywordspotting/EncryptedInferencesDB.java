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

    public void addOrUpdateInference(Inference inference) {
        try {
            InferenceEntity entity = InferenceEntity.fromInference(inference);

            if (inference.getId() > 0) {
                database.inferenceDao().updateInference(entity);
                Log.d(TAG, "Inference updated: " + inference.getId());
            } else {
                long id = database.inferenceDao().insertInference(entity);
                inference.setId(id);
                Log.d(TAG, "Inference added with ID: " + id);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding/updating inference", e);
        }
    }

    public List<Inference> getAllInferences() {
        try {
            List<InferenceEntity> entities = database.inferenceDao().getAllInferences();
            List<Inference> inferences = new ArrayList<>();

            for (InferenceEntity entity : entities) {
                inferences.add(entity.toInference());
            }

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