package com.ser.backend.model;

import lombok.Data;

@Data
public class AudioRequest {
    private String fineTuningType = "full"; // Default value
    private Integer numEmotions = 7; // Default value
    private Integer hiddenSize = 256; // Default value
}
