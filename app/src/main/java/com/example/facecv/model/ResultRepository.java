package com.example.facecv.model;

import java.util.ArrayList;

public class ResultRepository {
    private String emotionResult;

    private ArrayList<Result> mResults;
    public ResultRepository(String result, ArrayList<Result> results) {
        this.emotionResult = result;
        this.mResults = results;
    }
    public ResultRepository(ArrayList<Result> results) {
        this.mResults = results;
    }

    public ArrayList<Result> getResults() {
        return mResults;
    }

    public String getEmotionResult() {
        return emotionResult;
    }

    @Override
    public String toString() {
        return "ResultRepository{" +
                "emotionResult='" + emotionResult + '\'' +
                ", mResults=" + mResults +
                '}';
    }

}
