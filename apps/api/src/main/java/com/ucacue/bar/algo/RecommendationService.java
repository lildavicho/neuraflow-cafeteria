package com.ucacue.bar.algo;

import com.ucacue.bar.dto.ml.MlProductInsightDTO;
import com.ucacue.bar.dto.ml.MlResponse;
import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.entity.ProductEntity.ProductStatus;
import com.ucacue.bar.repository.OrderItemRepository;
import com.ucacue.bar.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationService {

    private static final int RECOMMENDATION_LIMIT = 6;

    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public MlResponse<List<MlProductInsightDTO>> getRecommendations(Long productId) {
        log.debug("RecommendationService.getRecommendations called for {}", productId);
        List<Object[]> rows = orderItemRepository.findCoPurchasedProducts(productId, PageRequest.of(0, RECOMMENDATION_LIMIT));
        return buildProductInsights(rows);
    }

    public MlResponse<List<MlProductInsightDTO>> getTrendingProducts(int days) {
        log.debug("RecommendationService.getTrendingProducts called for {} days", days);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime from = now.minusDays(Math.max(1, days));
        List<Object[]> rows = orderItemRepository.findTopProducts(from, now, PageRequest.of(0, RECOMMENDATION_LIMIT));
        return buildProductInsights(rows);
    }

    public MlResponse<List<MlProductInsightDTO>> getPersonalizedRecommendations(Long userId) {
        log.debug("RecommendationService.getPersonalizedRecommendations called for {}", userId);

        List<Object[]> userHistory = orderItemRepository.findTopProductsForUser(userId, PageRequest.of(0, 3));
        if (userHistory.isEmpty()) {
            return MlResponse.insufficient();
        }

        Set<Long> ownedProducts = userHistory.stream()
                .map(row -> ((Number) row[0]).longValue())
                .collect(Collectors.toSet());

        Map<Long, Double> weightedScores = new HashMap<>();
        for (Object[] row : userHistory) {
            Long sourceProductId = ((Number) row[0]).longValue();
            double sourceWeight = toDouble(row[1]);
            List<Object[]> coPurchased = orderItemRepository.findCoPurchasedProducts(
                    sourceProductId,
                    PageRequest.of(0, RECOMMENDATION_LIMIT));

            for (Object[] candidate : coPurchased) {
                Long candidateId = ((Number) candidate[0]).longValue();
                if (ownedProducts.contains(candidateId)) {
                    continue;
                }
                double candidateWeight = toDouble(candidate[1]);
                weightedScores.merge(candidateId, sourceWeight * candidateWeight, Double::sum);
            }
        }

        if (weightedScores.isEmpty()) {
            return MlResponse.insufficient();
        }

        List<Object[]> rankedRows = weightedScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(RECOMMENDATION_LIMIT)
                .map(entry -> new Object[] { entry.getKey(), entry.getValue() })
                .toList();

        return buildProductInsights(rankedRows);
    }

    private MlResponse<List<MlProductInsightDTO>> buildProductInsights(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return MlResponse.insufficient();
        }

        List<Long> ids = rows.stream()
                .map(row -> ((Number) row[0]).longValue())
                .toList();

        Map<Long, ProductEntity> lookup = productRepository.findAllById(ids).stream()
                .filter(product -> product.getStatus() != ProductStatus.DISCONTINUED)
                .collect(Collectors.toMap(ProductEntity::getId, product -> product, (left, right) -> left, LinkedHashMap::new));

        double totalSupport = rows.stream()
                .mapToDouble(row -> toDouble(row[1]))
                .sum();
        double maxSupport = rows.stream()
                .mapToDouble(row -> toDouble(row[1]))
                .max()
                .orElse(0.0);

        List<MlProductInsightDTO> insights = new ArrayList<>();
        for (Object[] row : rows) {
            Long productId = ((Number) row[0]).longValue();
            ProductEntity product = lookup.get(productId);
            if (product == null) {
                continue;
            }

            double support = toDouble(row[1]);
            double itemConfidence = maxSupport > 0
                    ? clamp((support / maxSupport) * confidenceFromSupport(totalSupport, rows.size()))
                    : 0.0;

            insights.add(new MlProductInsightDTO(
                    product.getId(),
                    product.getCode(),
                    product.getName(),
                    product.getCategory() != null ? product.getCategory().getName() : null,
                    product.getImageUrl(),
                    product.getPrice(),
                    Math.round(support),
                    round(itemConfidence)));
        }

        if (insights.isEmpty()) {
            return MlResponse.insufficient();
        }

        return MlResponse.ok(insights, confidenceFromSupport(totalSupport, insights.size()));
    }

    private double confidenceFromSupport(double totalSupport, int sampleSize) {
        double supportFactor = Math.min(1.0, totalSupport / 20.0);
        double sampleFactor = Math.min(1.0, sampleSize / 5.0);
        return round((supportFactor * 0.7) + (sampleFactor * 0.3));
    }

    private double toDouble(Object value) {
        return value == null ? 0.0 : ((Number) value).doubleValue();
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double round(double value) {
        return Math.round(clamp(value) * 10000.0) / 10000.0;
    }
}
