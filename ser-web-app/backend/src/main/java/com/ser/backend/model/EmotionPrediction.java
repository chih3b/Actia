package com.ser.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmotionPrediction {
    private String fileName;
    private String emotion;
    private double confidence;
}
