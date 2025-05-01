package com.ser.integration.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PhoneDetection {
    private String id;
    private LocalDateTime timestamp;
    private double confidence;
    private String screenshotUrl;
    private String cameraId;
    
    public PhoneDetection() {
    }
    
    public PhoneDetection(String id, LocalDateTime timestamp, double confidence, String screenshotUrl, String cameraId) {
        this.id = id;
        this.timestamp = timestamp;
        this.confidence = confidence;
        this.screenshotUrl = screenshotUrl;
        this.cameraId = cameraId;
    }
}
