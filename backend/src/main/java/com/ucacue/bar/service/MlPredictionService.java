package com.ucacue.bar.service;

import com.ucacue.bar.entity.MlPredictionEntity;
import com.ucacue.bar.entity.MlPredictionEntity.PredictionType;
import com.ucacue.bar.entity.OrderEntity;
import com.ucacue.bar.repository.MlPredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MlPredictionService {

    private final MlPredictionRepository mlPredictionRepository;

    @Transactional
    public MlPredictionEntity createPrediction(OrderEntity order, PredictionType type, String predictedValue, Double confidence) {
        MlPredictionEntity entity = new MlPredictionEntity();
        entity.setOrder(order);
        entity.setPredictionType(type);
        entity.setPredictedValue(predictedValue);
        if (confidence != null) {
            entity.setConfidenceScore(BigDecimal.valueOf(confidence));
        }
        return mlPredictionRepository.save(entity);
    }

    @Transactional
    public void updateActualValue(Long orderId, PredictionType type, String actualValue) {
        Optional<MlPredictionEntity> prediction = mlPredictionRepository.findLatestByOrderAndType(orderId, type);
        prediction.ifPresent(p -> {
            p.setActualValue(actualValue);
            mlPredictionRepository.save(p);
        });
    }
}
