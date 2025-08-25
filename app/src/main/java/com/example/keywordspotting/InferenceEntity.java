package com.example.keywordspotting;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "inferences")
public class InferenceEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private String type;
    private String results;
    private String timestamp;

    // Costruttori
    public InferenceEntity() {}

    public InferenceEntity(String type, String results, String timestamp) {
        this.type = type;
        this.results = results;
        this.timestamp = timestamp;
    }

    public InferenceEntity(long id, String type, String results, String timestamp) {
        this.id = id;
        this.type = type;
        this.results = results;
        this.timestamp = timestamp;
    }

    // Getter e Setter
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getResults() { return results; }
    public void setResults(String results) { this.results = results; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
