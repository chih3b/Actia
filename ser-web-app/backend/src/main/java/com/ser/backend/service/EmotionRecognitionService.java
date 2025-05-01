package com.ser.backend.service;

import com.ser.backend.model.EmotionPrediction;
import com.ser.backend.model.EmotionPredictionHistory;
import com.ser.backend.repository.EmotionPredictionHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class EmotionRecognitionService {
    
    @Autowired
    private EmotionPredictionHistoryRepository historyRepository;

    private static final String UPLOAD_DIR = "uploads";
    private static final String PYTHON_SCRIPT = "/Users/chihebnouri/SER/predict_emotion.py";
    private static final String MODEL_PATH = "/Users/chihebnouri/SER/results_hubert_standard/full/hubert_full_model.pt";
    
    public EmotionRecognitionService() {
        // Create uploads directory if it doesn't exist
        File directory = new File(UPLOAD_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }
    
    public EmotionPrediction predictEmotion(MultipartFile audioFile) throws IOException {
        // Always use the full fine-tuning model as it performs best
        String fineTuningType = "full";
        int numEmotions = 7;
        int hiddenSize = 256;
        
        // Save the uploaded file temporarily
        String fileName = UUID.randomUUID().toString() + "_" + audioFile.getOriginalFilename();
        Path filePath = Paths.get(UPLOAD_DIR, fileName);
        Files.write(filePath, audioFile.getBytes());
        
        try {
            System.out.println("Processing audio file: " + fileName);
            System.out.println("Using model path: " + MODEL_PATH);
            
            // Build the command to run the Python script with conda base environment
            ProcessBuilder processBuilder = new ProcessBuilder(
                "/bin/bash", "-c",
                "source /opt/anaconda3/etc/profile.d/conda.sh && conda activate base && python " +
                PYTHON_SCRIPT + " --model_path " + MODEL_PATH +
                " --audio_file " + filePath.toString() +
                " --fine_tuning_type " + fineTuningType +
                " --num_emotions " + String.valueOf(numEmotions) +
                " --hidden_size " + String.valueOf(hiddenSize)
            );
            
            // Redirect error stream to output stream
            processBuilder.redirectErrorStream(true);
            
            // Start the process
            Process process = processBuilder.start();
            
            // Read the output
            String output = new String(process.getInputStream().readAllBytes());
            System.out.println("Python script output: \n" + output);
            
            // Wait for the process to complete
            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                throw new IOException("Process timed out");
            }
            
            // Parse the output to extract emotion and confidence
            String emotion = "unknown";
            double confidence = 0.0;
            
            System.out.println("Python script output:\n" + output);
            
            for (String line : output.split("\\n")) {
                if (line.startsWith("Predicted emotion:")) {
                    emotion = line.substring("Predicted emotion:".length()).trim();
                    System.out.println("Parsed emotion: " + emotion);
                } else if (line.startsWith("Predicted emotion index:")) {
                    // This is the new format from our updated script
                    String parts = line.substring("Predicted emotion index:".length()).trim();
                    if (parts.contains(", name:")) {
                        emotion = parts.split(", name:")[1].trim();
                        System.out.println("Parsed emotion from new format: " + emotion);
                    }
                } else if (line.startsWith("Confidence:")) {
                    String confidenceStr = line.substring("Confidence:".length()).trim();
                    try {
                        confidence = Double.parseDouble(confidenceStr);
                        // The confidence is already a value between 0 and 1, we'll keep it as is
                        // and let the frontend handle the percentage display
                    } catch (NumberFormatException e) {
                        System.out.println("Error parsing confidence value: " + confidenceStr);
                    }
                    System.out.println("Parsed confidence: " + confidence);
                }
            }
            
            // Create the prediction object
            EmotionPrediction prediction = new EmotionPrediction();
            prediction.setFileName(audioFile.getOriginalFilename());
            prediction.setEmotion(emotion);
            prediction.setConfidence(confidence);
            
            // Save to history
            historyRepository.save(new EmotionPredictionHistory(audioFile.getOriginalFilename(), emotion, confidence));
            
            return prediction;
            
        } catch (Exception e) {
            throw new IOException("Failed to process audio file: " + e.getMessage(), e);
        } finally {
            // Clean up the temporary file
            Files.deleteIfExists(filePath);
        }
    }
    
    public List<EmotionPrediction> batchPredict(List<MultipartFile> audioFiles) throws IOException {
        List<EmotionPrediction> results = new ArrayList<>();
        
        for (MultipartFile audioFile : audioFiles) {
            results.add(predictEmotion(audioFile));
        }
        
        return results;
    }
}
