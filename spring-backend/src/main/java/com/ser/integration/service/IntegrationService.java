package com.ser.integration.service;

import com.ser.integration.model.DetectionResult;
import com.ser.integration.model.EmotionPrediction;
import com.ser.integration.model.PhoneDetection;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class IntegrationService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationService.class);

    private final YoloDetectionService yoloService;
    private final EmotionRecognitionService emotionService;
    
    @Autowired
    public IntegrationService(YoloDetectionService yoloService, EmotionRecognitionService emotionService) {
        this.yoloService = yoloService;
        this.emotionService = emotionService;
    }
    
    public CompletableFuture<Boolean> startIntegratedDetection(String sessionId) {
        // Start both YOLO detection and SER recording simultaneously
        CompletableFuture<Boolean> yoloFuture = yoloService.startDetection(sessionId);
        CompletableFuture<Boolean> serFuture = emotionService.startRecording(sessionId);
        
        // Return true if both started successfully
        return CompletableFuture.allOf(yoloFuture, serFuture)
                .thenApply(v -> yoloFuture.join() && serFuture.join());
    }
    
    public boolean stopIntegratedDetection(String sessionId) {
        // Stop both YOLO detection and SER recording
        boolean yoloStopped = yoloService.stopDetection(sessionId);
        boolean serStopped = emotionService.stopRecording(sessionId);
        
        // Return true if both stopped successfully
        return yoloStopped && serStopped;
    }
    
    public DetectionResult getIntegratedResults(String sessionId) {
        // Get results from both services
        List<PhoneDetection> phoneDetections = yoloService.getDetectionResults(sessionId);
        List<EmotionPrediction> emotionResults = emotionService.getEmotionResults(sessionId);
        
        // Determine if phone was detected
        boolean phoneDetected = !phoneDetections.isEmpty();
        
        // Find dominant emotion
        String dominantEmotion = "neutral"; // Default
        if (!emotionResults.isEmpty()) {
            Map<String, Long> emotionCounts = emotionResults.stream()
                    .collect(Collectors.groupingBy(EmotionPrediction::getEmotion, Collectors.counting()));
            
            dominantEmotion = emotionCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("neutral");
        }
        
        // Generate summary
        String summary = String.format(
            "Detection Summary: %s. Dominant emotion: %s.",
            phoneDetected ? "Phone usage detected" : "No phone usage detected",
            dominantEmotion
        );
        
        return new DetectionResult(
            phoneDetections,
            emotionResults,
            summary,
            phoneDetected,
            dominantEmotion
        );
    }
    
    public boolean isDetectionActive(String sessionId) {
        return yoloService.isDetectionActive(sessionId) || emotionService.isRecordingActive(sessionId);
    }
    
    /**
     * Gets the current frame from the YOLO detection process
     * @param sessionId The session ID
     * @return The current frame as a byte array, or null if no frame is available
     */
    public byte[] getCurrentFrame(String sessionId) {
        // Delegate to the YOLO service to get the current frame
        return yoloService.getCurrentFrame(sessionId);
    }
}
