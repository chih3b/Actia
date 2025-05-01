package com.ser.integration.service;

import com.ser.integration.model.PhoneDetection;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class YoloDetectionService {

    private static final Logger log = LoggerFactory.getLogger(YoloDetectionService.class);

    private final ResourceLoader resourceLoader;
    private final Map<String, Process> activeDetections = new ConcurrentHashMap<>();
    private final Map<String, List<PhoneDetection>> detectionResults = new ConcurrentHashMap<>();
    private final Map<String, byte[]> currentFrames = new ConcurrentHashMap<>();
    private final String screenshotsDir;
    
    public YoloDetectionService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        // Create screenshots directory
        this.screenshotsDir = new File(System.getProperty("user.dir")).getParent() + "/screenshots";
        new File(screenshotsDir).mkdirs();
        log.info("Screenshots directory: {}", screenshotsDir);
    }
    
    @Async
    public CompletableFuture<Boolean> startDetection(String cameraId) {
        if (activeDetections.containsKey(cameraId)) {
            stopDetection(cameraId);
        }
        
        // Clear previous detection results
        detectionResults.put(cameraId, new ArrayList<>());
        
        try {
            // Build command to run YOLO detection script
            String pythonPath = "python"; // Adjust based on your environment
            String scriptPath = new File(System.getProperty("user.dir")).getParent() + "/main.py";
            
            // Use absolute path to the model file
            String modelPath = "/Users/chihebnouri/SER/best.pt";
            
            ProcessBuilder processBuilder = new ProcessBuilder(
                pythonPath, 
                scriptPath, 
                "--camera_id", cameraId,
                "--output_dir", screenshotsDir,
                "--model_path", modelPath
            );
            
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            activeDetections.put(cameraId, process);
            
            // Start a thread to read the output
            new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    boolean readingFrameData = false;
                    ByteArrayOutputStream frameDataBuffer = new ByteArrayOutputStream();
                    
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("FRAME_DATA_START")) {
                            // Start capturing frame data
                            readingFrameData = true;
                            frameDataBuffer.reset();
                            continue;
                        } else if (line.contains("FRAME_DATA_END")) {
                            // End of frame data, process it
                            readingFrameData = false;
                            byte[] frameData = frameDataBuffer.toByteArray();
                            if (frameData.length > 0) {
                                // Update the current frame
                                updateCurrentFrame(cameraId, frameData);
                            }
                            continue;
                        }
                        
                        if (readingFrameData) {
                            // Read binary data directly from the process input stream
                            // This won't work well with text-based reading, so we'll just log it
                            log.debug("Reading frame data...");
                            // In a real implementation, we would read the binary data here
                        } else {
                            // Regular text output
                            log.info("YOLO output: {}", line);
                            
                            // Parse detection results from output
                            if (line.contains("Screenshot saved:")) {
                                // Example format: "Screenshot saved: /path/to/screenshots/camera1_uuid.jpg with confidence 0.85"
                                String[] parts = line.split("Screenshot saved: ");
                                if (parts.length > 1) {
                                    String filepath = parts[1].split(" with confidence ")[0].trim();
                                    String confidenceStr = parts[1].split(" with confidence ")[1].trim();
                                    double confidence = Double.parseDouble(confidenceStr);
                                    
                                    String filename = new File(filepath).getName();
                                    String id = filename.replace(cameraId + "_", "").replace(".jpg", "");
                                    
                                    PhoneDetection detection = new PhoneDetection(
                                        id,
                                        LocalDateTime.now(),
                                        confidence,
                                        "/api/screenshots/" + filename,
                                        cameraId
                                    );
                                    
                                    detectionResults.get(cameraId).add(detection);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Error reading YOLO output", e);
                } finally {
                    // When the process ends, remove it from active detections
                    activeDetections.remove(cameraId);
                }
            }).start();
            
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Failed to start YOLO detection", e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    public boolean stopDetection(String cameraId) {
        Process process = activeDetections.remove(cameraId);
        // Also remove the current frame
        currentFrames.remove(cameraId);
        if (process != null) {
            process.destroy();
            return true;
        }
        return false;
    }
    
    public List<PhoneDetection> getDetectionResults(String cameraId) {
        return detectionResults.getOrDefault(cameraId, new ArrayList<>());
    }
    
    public boolean isDetectionActive(String cameraId) {
        return activeDetections.containsKey(cameraId);
    }
    
    /**
     * Gets the current frame from the YOLO detection process
     * @param cameraId The camera ID
     * @return The current frame as a byte array, or null if no frame is available
     */
    public byte[] getCurrentFrame(String cameraId) {
        // Return the current frame for the camera
        byte[] frame = currentFrames.get(cameraId);
        
        // If no frame is available, return a default frame
        if (frame == null) {
            try {
                // Create a simple black image as a placeholder
                BufferedImage img = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "jpg", baos);
                baos.flush();
                frame = baos.toByteArray();
                baos.close();
                
                // Cache the frame
                currentFrames.put(cameraId, frame);
            } catch (Exception e) {
                log.error("Error creating default frame", e);
            }
        }
        
        return frame;
    }
    
    /**
     * Updates the current frame for a camera
     * @param cameraId The camera ID
     * @param frame The new frame
     */
    private void updateCurrentFrame(String cameraId, byte[] frame) {
        currentFrames.put(cameraId, frame);
    }
}
