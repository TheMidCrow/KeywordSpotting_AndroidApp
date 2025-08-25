package com.example.keywordspotting;

public class Inference {
    private long id;
    private String type;
    private String results;
    private String timestamp;

    public Inference(String type, String results, String timestamp){
        this.id = 0;
        this.type = type;
        this.results = results;
        this.timestamp = timestamp;
    }

    public Inference(long id, String type, String results, String timestamp){
        this.id = id;
        this.type = type;
        this.results = results;
        this.timestamp = timestamp;
    }

    public long getId(){return this.id;}
    public String getType(){return this.type;}
    public String getResults(){return this.results;}
    public String getTimestamp(){return this.timestamp;}

    public void setId(long id){this.id = id;}

}
