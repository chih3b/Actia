package com.ser.integration.controller;

import com.ser.integration.model.DetectionResult;
import com.ser.integration.model.EmotionPrediction;
import com.ser.integration.service.IntegrationService;
import com.ser.integration.service.EmotionRecognitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class IntegrationController {

    private static final Logger log = LoggerFactory.getLogger(IntegrationController.class);

    private final IntegrationService integrationService;
    private final EmotionRecognitionService emotionService;
    private final Path screenshotsDir;
    
    @Autowired
    public IntegrationController(IntegrationService integrationService, EmotionRecognitionService emotionService) {
        this.integrationService = integrationService;
        this.emotionService = emotionService;
        this.screenshotsDir = Paths.get(new File(System.getProperty("user.dir")).getParent(), "screenshots");
    }
    
    @PostMapping("/detection/start")
    public ResponseEntity<String> startDetection(@RequestParam(defaultValue = "session1") String sessionId) {
        try {
            CompletableFuture<Boolean> result = integrationService.startIntegratedDetection(sessionId);
            boolean success = result.get(); // Wait for the result
            
            if (success) {
                return ResponseEntity.ok("Detection started successfully");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to start detection");
            }
        } catch (Exception e) {
            log.error("Error starting detection", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
    
    @PostMapping("/detection/stop")
    public ResponseEntity<String> stopDetection(@RequestParam(defaultValue = "session1") String sessionId) {
        try {
            boolean success = integrationService.stopIntegratedDetection(sessionId);
            
            if (success) {
                return ResponseEntity.ok("Detection stopped successfully");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to stop detection");
            }
        } catch (Exception e) {
            log.error("Error stopping detection", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
    
    @GetMapping("/detection/results")
    public ResponseEntity<DetectionResult> getDetectionResults(@RequestParam(defaultValue = "session1") String sessionId) {
        try {
            DetectionResult results = integrationService.getIntegratedResults(sessionId);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error getting detection results", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    @GetMapping("/detection/status")
    public ResponseEntity<Boolean> getDetectionStatus(@RequestParam(defaultValue = "session1") String sessionId) {
        try {
            boolean isActive = integrationService.isDetectionActive(sessionId);
            return ResponseEntity.ok(isActive);
        } catch (Exception e) {
            log.error("Error getting detection status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    @PostMapping("/emotion/predict")
    public ResponseEntity<EmotionPrediction> predictEmotion(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "session1") String sessionId) {
        try {
            // Process the audio file
            EmotionPrediction prediction = emotionService.processAudioFile(sessionId, file);
            return ResponseEntity.ok(prediction);
        } catch (Exception e) {
            log.error("Error predicting emotion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    @GetMapping("/screenshots/{filename:.+}")
    public ResponseEntity<Resource> getScreenshot(@PathVariable String filename) {
        try {
            Path filePath = screenshotsDir.resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error retrieving screenshot", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/emotion/upload")
    public ResponseEntity<EmotionPrediction> uploadAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "session1") String sessionId) {
        try {
            log.info("Received audio upload for session {}", sessionId);
            
            // Process the audio file
            EmotionPrediction prediction = emotionService.processAudioFile(sessionId, file);
            
            return ResponseEntity.ok(prediction);
        } catch (Exception e) {
            log.error("Error processing audio upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    @GetMapping(value = "/video-feed", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getVideoFeed(@RequestParam(defaultValue = "session1") String sessionId) {
        if (!integrationService.isDetectionActive(sessionId)) {
            log.warn("Video feed requested for inactive session: {}", sessionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        
        try {
            // Get a single frame from the YOLO detection process
            byte[] frame = integrationService.getCurrentFrame(sessionId);
            if (frame != null && frame.length > 0) {
                return ResponseEntity
                    .ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(frame);
            } else {
                log.warn("No frame available for session: {}", sessionId);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
            }
        } catch (Exception e) {
            log.error("Error getting video frame", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
