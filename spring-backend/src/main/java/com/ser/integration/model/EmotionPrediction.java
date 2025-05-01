package com.ser.integration.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EmotionPrediction {
    private String fileName;
    private String emotion;
    private double confidence;
    private LocalDateTime timestamp;
    
    public EmotionPrediction() {
    }
    
    public EmotionPrediction(String fileName, String emotion, double confidence, LocalDateTime timestamp) {
        this.fileName = fileName;
        this.emotion = emotion;
        this.confidence = confidence;
        this.timestamp = timestamp;
    }
    
    public String getEmotion() {
        return emotion;
    }
}
