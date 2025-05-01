package com.ser.backend.controller;

import com.ser.backend.model.EmotionPrediction;
import com.ser.backend.model.EmotionPredictionHistory;
import com.ser.backend.repository.EmotionPredictionHistoryRepository;
import com.ser.backend.service.EmotionRecognitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/emotion")
@CrossOrigin(origins = "*") // Allow requests from Angular frontend
public class EmotionRecognitionController {

    private final EmotionRecognitionService emotionRecognitionService;
    private final EmotionPredictionHistoryRepository historyRepository;

    @Autowired
    public EmotionRecognitionController(EmotionRecognitionService emotionRecognitionService, EmotionPredictionHistoryRepository historyRepository) {
        this.emotionRecognitionService = emotionRecognitionService;
        this.historyRepository = historyRepository;
    }

    @PostMapping("/predict")
    public ResponseEntity<EmotionPrediction> predictEmotion(
            @RequestParam("file") MultipartFile file) {
        
        try {
            EmotionPrediction prediction = emotionRecognitionService.predictEmotion(file);
            return ResponseEntity.ok(prediction);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/batch-predict")
    public ResponseEntity<List<EmotionPrediction>> batchPredict(
            @RequestParam("files") List<MultipartFile> files) {
        
        try {
            List<EmotionPrediction> predictions = emotionRecognitionService.batchPredict(files);
            return ResponseEntity.ok(predictions);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/history")
    public ResponseEntity<List<EmotionPredictionHistory>> getPredictionHistory() {
        List<EmotionPredictionHistory> history = historyRepository.findAll();
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/models")
    public ResponseEntity<List<String>> getAvailableModels() {
        // Return the available fine-tuning types
        return ResponseEntity.ok(List.of("full", "qkv", "classifier"));
    }
}
