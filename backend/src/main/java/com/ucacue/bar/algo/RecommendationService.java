package com.ucacue.bar.algo;

import com.ucacue.bar.entity.ProductEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class RecommendationService {

    public List<ProductEntity> getRecommendations(Long productId) {
        log.debug("RecommendationService.getRecommendations called for {}", productId);
        return Collections.emptyList();
    }

    public List<ProductEntity> getTrendingProducts(int days) {
        log.debug("RecommendationService.getTrendingProducts called for {} days", days);
        return Collections.emptyList();
    }

    public List<ProductEntity> getPersonalizedRecommendations(Long userId) {
        log.debug("RecommendationService.getPersonalizedRecommendations called for {}", userId);
        return Collections.emptyList();
    }
}
