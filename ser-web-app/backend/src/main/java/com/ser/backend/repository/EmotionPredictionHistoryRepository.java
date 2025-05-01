package com.ser.backend.repository;

import com.ser.backend.model.EmotionPredictionHistory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class EmotionPredictionHistoryRepository {
    private final List<EmotionPredictionHistory> predictions = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong idCounter = new AtomicLong(1);

    public EmotionPredictionHistory save(EmotionPredictionHistory prediction) {
        prediction.setId(idCounter.getAndIncrement());
        predictions.add(prediction);
        return prediction;
    }

    public List<EmotionPredictionHistory> findAll() {
        // Return a copy of the list to avoid concurrent modification issues
        return new ArrayList<>(predictions);
    }

    public void deleteAll() {
        predictions.clear();
    }
}
