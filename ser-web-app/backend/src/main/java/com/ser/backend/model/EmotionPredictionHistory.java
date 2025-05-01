package com.ser.backend.model;

import java.time.LocalDateTime;

public class EmotionPredictionHistory {
    private Long id;
    private String fileName;
    private String emotion;
    private double confidence;
    private LocalDateTime timestamp;

    public EmotionPredictionHistory() {
    }

    public EmotionPredictionHistory(String fileName, String emotion, double confidence) {
        this.fileName = fileName;
        this.emotion = emotion;
        this.confidence = confidence;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
