package com.ser.integration.service;

import com.ser.integration.model.EmotionPrediction;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmotionRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(EmotionRecognitionService.class);

    private final Map<String, Process> activeRecordings = new ConcurrentHashMap<>();
    private final Map<String, List<EmotionPrediction>> emotionResults = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> sessionDataMap = new ConcurrentHashMap<>();
    private final String tempDir;
    
    public EmotionRecognitionService() {
        // Create temp directory for audio files
        this.tempDir = new File(System.getProperty("user.dir")).getParent() + "/temp_audio";
        new File(tempDir).mkdirs();
        log.info("Temp audio directory: {}", tempDir);
    }
    
    @Async
    public CompletableFuture<Boolean> startRecording(String sessionId) {
        if (activeRecordings.containsKey(sessionId)) {
            stopRecording(sessionId);
        }
        
        // Clear previous emotion results
        emotionResults.put(sessionId, new ArrayList<>());
        
        // Initialize session data
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("status", "ready");
        sessionDataMap.put(sessionId, sessionData);
        
        log.info("SER recording ready for session {}", sessionId);
        
        // Return success immediately - we'll wait for audio uploads
        return CompletableFuture.completedFuture(true);
    }
    
    public boolean stopRecording(String sessionId) {
        Process process = activeRecordings.remove(sessionId);
        if (process != null) {
            process.destroy();
            
            // Get the audio file path from the session data
            Map<String, String> sessionData = sessionDataMap.get(sessionId);
            if (sessionData != null && sessionData.containsKey("audioFilePath")) {
                String audioFilePath = sessionData.get("audioFilePath");
                sessionData.put("status", "processing");
                
                // Process the recorded audio file
                processAudioFile(sessionId, audioFilePath);
            }
            
            return true;
        }
        return false;
    }
    
    private void processAudioFile(String sessionId, String audioFilePath) {
        try {
            // Check if the audio file exists
            File audioFile = new File(audioFilePath);
            if (!audioFile.exists()) {
                log.error("Audio file not found: {}", audioFilePath);
                return;
            }
            
            log.info("Processing audio file for session {}: {}", sessionId, audioFilePath);
            
            // Build command to run SER processing script
            String pythonPath = "python"; // Adjust based on your environment
            String scriptPath = new File(System.getProperty("user.dir")).getParent() + "/predict_emotion.py";
            String modelPath = new File(System.getProperty("user.dir")).getParent() + "/results_hubert_standard/full/hubert_full_model.pt";
            
            ProcessBuilder processBuilder = new ProcessBuilder(
                pythonPath, 
                scriptPath, 
                "--model_path", modelPath,
                "--fine_tuning_type", "full",
                "--audio_file", audioFilePath
            );
            
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // Read the output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("SER output: {}", line);
                    
                    // Parse emotion results from output
                    if (line.contains("Predicted emotion:")) {
                        // Example format: "Predicted emotion: happy (confidence: 0.85)"
                        String[] parts = line.split("Predicted emotion: ");
                        if (parts.length > 1) {
                            String emotionPart = parts[1].trim();
                            String emotion = emotionPart.split(" \\(confidence:")[0].trim();
                            double confidence = 0.8; // Default
                            
                            // Try to extract confidence
                            if (emotionPart.contains("confidence:")) {
                                String confidencePart = emotionPart.split("confidence: ")[1];
                                confidencePart = confidencePart.replace(")", "").trim();
                                try {
                                    confidence = Double.parseDouble(confidencePart);
                                } catch (NumberFormatException e) {
                                    log.warn("Could not parse confidence: {}", confidencePart);
                                }
                            }
                            
                            EmotionPrediction prediction = new EmotionPrediction(
                                audioFile.getName(),
                                emotion,
                                confidence,
                                LocalDateTime.now()
                            );
                            
                            // Add the prediction to the results
                            List<EmotionPrediction> sessionResults = emotionResults.get(sessionId);
                            if (sessionResults == null) {
                                sessionResults = new ArrayList<>();
                                emotionResults.put(sessionId, sessionResults);
                            }
                            sessionResults.add(prediction);
                        }
                    }
                }
            }
            
            // Wait for the process to complete
            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                log.error("SER processing timed out for session {}", sessionId);
            }
            
            // Update session status
            Map<String, String> sessionData = sessionDataMap.get(sessionId);
            if (sessionData != null) {
                sessionData.put("status", "completed");
            }
            
            // Clean up the audio file
            audioFile.delete();
            
        } catch (Exception e) {
            log.error("Error processing audio file for session {}: {}", sessionId, e.getMessage(), e);
        }
    }
    
    public List<EmotionPrediction> getEmotionResults(String sessionId) {
        return emotionResults.getOrDefault(sessionId, new ArrayList<>());
    }
    
    public EmotionPrediction processAudioFile(String sessionId, MultipartFile audioFile) {
        try {
            // Create a unique filename for the uploaded audio
            String originalFilename = audioFile.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : ".wav";
            
            String audioFilename = sessionId + "_" + UUID.randomUUID().toString() + extension;
            String audioFilePath = tempDir + "/" + audioFilename;
            
            // Save the uploaded file
            File targetFile = new File(audioFilePath);
            audioFile.transferTo(targetFile);
            
            log.info("Processing uploaded audio file for session {}: {}", sessionId, audioFilePath);
            
            // Get paths to script and model
            String scriptPath = new File(System.getProperty("user.dir")).getParent() + "/predict_emotion.py";
            String modelPath = new File(System.getProperty("user.dir")).getParent() + "/results_hubert_standard/full/hubert_full_model.pt";
            
            // Build the command to run the Python script with conda base environment
            // This matches the original implementation that was working
            ProcessBuilder processBuilder = new ProcessBuilder(
                "/bin/bash", "-c",
                "source /opt/anaconda3/etc/profile.d/conda.sh && conda activate base && python " +
                scriptPath + " --model_path " + modelPath +
                " --audio_file " + audioFilePath +
                " --fine_tuning_type full" +
                " --num_emotions 7" +
                " --hidden_size 256"
            );
            
            log.info("Running command: {}", processBuilder.command());
            
            // Redirect error stream to output stream
            processBuilder.redirectErrorStream(true);
            
            // Start the process
            Process process = processBuilder.start();
            
            // Read the output
            String output = new String(process.getInputStream().readAllBytes());
            log.info("Python script output: \n{}", output);
            
            // Wait for the process to complete
            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                log.error("SER processing timed out for session {}", sessionId);
                throw new RuntimeException("SER processing timed out");
            }
            
            // Parse the output to extract emotion and confidence
            String emotion = "unknown";
            double confidence = 0.0;
            
            for (String line : output.split("\n")) {
                if (line.startsWith("Predicted emotion:")) {
                    emotion = line.substring("Predicted emotion:".length()).trim();
                    log.info("Parsed emotion: {}", emotion);
                } else if (line.startsWith("Predicted emotion index:")) {
                    // This is the new format from the updated script
                    String parts = line.substring("Predicted emotion index:".length()).trim();
                    if (parts.contains(", name:")) {
                        emotion = parts.split(", name:")[1].trim();
                        log.info("Parsed emotion from new format: {}", emotion);
                    }
                } else if (line.startsWith("Confidence:")) {
                    String confidenceStr = line.substring("Confidence:".length()).trim();
                    try {
                        confidence = Double.parseDouble(confidenceStr);
                        // The confidence is already a value between 0 and 1
                    } catch (NumberFormatException e) {
                        log.warn("Error parsing confidence value: {}", confidenceStr);
                    }
                    log.info("Parsed confidence: {}", confidence);
                }
            }
            
            // Create the emotion prediction
            EmotionPrediction prediction = new EmotionPrediction(
                audioFilename,
                emotion,
                confidence,
                LocalDateTime.now()
            );
            
            // Add the prediction to the results
            List<EmotionPrediction> sessionResults = emotionResults.get(sessionId);
            if (sessionResults == null) {
                sessionResults = new ArrayList<>();
                emotionResults.put(sessionId, sessionResults);
            }
            sessionResults.add(prediction);
            
            // Clean up the audio file
            try {
                targetFile.delete();
            } catch (Exception e) {
                log.warn("Failed to delete temporary audio file: {}", audioFilePath);
            }
            
            return prediction;
            
        } catch (Exception e) {
            log.error("Error processing audio file for session {}: {}", sessionId, e.getMessage(), e);
            throw new RuntimeException("Error processing audio file: " + e.getMessage(), e);
        }
    }
    
    public boolean isRecordingActive(String sessionId) {
        return activeRecordings.containsKey(sessionId);
    }
    
    @Async
    public CompletableFuture<EmotionPrediction> processAudioFile(MultipartFile file) {
        try {
            // Save the uploaded file temporarily
            String originalFilename = file.getOriginalFilename();
            Path tempFile = Paths.get(tempDir, originalFilename);
            Files.write(tempFile, file.getBytes());
            
            // Build command to run SER prediction script
            String pythonPath = "python"; // Adjust based on your environment
            String scriptPath = new File(System.getProperty("user.dir")).getParent() + "/predict_emotion.py";
            
            ProcessBuilder processBuilder = new ProcessBuilder(
                pythonPath, 
                scriptPath, 
                "--mode", "file",
                "--input", tempFile.toString()
            );
            
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // Read the output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = process.waitFor();
            
            // Clean up temp file
            Files.deleteIfExists(tempFile);
            
            if (exitCode == 0) {
                // Parse the output to get emotion prediction
                String outputStr = output.toString();
                String emotion = "neutral"; // Default
                double confidence = 0.5; // Default
                
                if (outputStr.contains("Predicted emotion:")) {
                    String[] parts = outputStr.split("Predicted emotion: ");
                    if (parts.length > 1) {
                        String emotionPart = parts[1].trim();
                        emotion = emotionPart.split(" \\(confidence:")[0].trim();
                        
                        // Try to extract confidence
                        if (emotionPart.contains("confidence:")) {
                            String confidencePart = emotionPart.split("confidence: ")[1];
                            confidencePart = confidencePart.replace(")", "").trim();
                            try {
                                confidence = Double.parseDouble(confidencePart);
                            } catch (NumberFormatException e) {
                                log.warn("Could not parse confidence: {}", confidencePart);
                            }
                        }
                    }
                }
                
                return CompletableFuture.completedFuture(new EmotionPrediction(
                    originalFilename,
                    emotion,
                    confidence,
                    LocalDateTime.now()
                ));
            } else {
                log.error("SER prediction failed with exit code: {}", exitCode);
                log.error("Output: {}", output.toString());
                return CompletableFuture.failedFuture(new RuntimeException("SER prediction failed"));
            }
        } catch (Exception e) {
            log.error("Error processing audio file", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
