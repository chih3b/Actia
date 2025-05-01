package com.ser.integration.model;

import lombok.Data;
import java.util.List;

@Data
public class DetectionResult {
    private List<PhoneDetection> phoneDetections;
    private List<EmotionPrediction> emotionResults;
    private String summary;
    private boolean phoneDetected;
    private String dominantEmotion;
    
    public DetectionResult() {
    }
    
    public DetectionResult(List<PhoneDetection> phoneDetections, List<EmotionPrediction> emotionResults, 
                          String summary, boolean phoneDetected, String dominantEmotion) {
        this.phoneDetections = phoneDetections;
        this.emotionResults = emotionResults;
        this.summary = summary;
        this.phoneDetected = phoneDetected;
        this.dominantEmotion = dominantEmotion;
    }
}
