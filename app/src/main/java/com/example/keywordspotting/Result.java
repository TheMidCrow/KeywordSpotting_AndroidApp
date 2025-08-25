package com.example.keywordspotting;

public class Result {
    private String keyword;
    private float confidence;

    public Result(String k, float c){
        this.keyword = k;
        this.confidence = c;
    }

    public String getKeyword(){return this.keyword;}
    public float getConfidence(){return this.confidence;}

}
