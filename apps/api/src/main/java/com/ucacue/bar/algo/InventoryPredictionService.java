package com.ucacue.bar.algo;

import com.ucacue.bar.dto.ml.DemandForecastDTO;
import com.ucacue.bar.dto.ml.MlResponse;
import com.ucacue.bar.dto.ml.RestockSuggestionDTO;
import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.repository.OrderItemRepository;
import com.ucacue.bar.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryPredictionService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final DemandForecastService demandForecastService;
    private final TenantContextResolver tenantContextResolver;

    public MlResponse<List<RestockSuggestionDTO>> getRestockSuggestions() {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        List<ProductEntity> allProducts = productRepository.findByTenantId(tenantId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        Map<Long, Integer> reservedQuantities = loadReservedQuantities(allProducts);
        List<RestockSuggestionDTO> suggestions = new ArrayList<>();
        List<Double> evaluatedConfidenceScores = new ArrayList<>();

        for (ProductEntity product : allProducts) {
            if (product.getStatus() == ProductEntity.ProductStatus.DISCONTINUED) {
                continue;
            }

            MlResponse<DemandForecastDTO> forecastResult = demandForecastService.forecastDemand(product.getId(), "weekly", 1);
            if (!"ok".equals(forecastResult.status()) || forecastResult.data() == null) {
                continue;
            }

            evaluatedConfidenceScores.add(forecastResult.confidenceScore());
            List<Double> forecastValues = forecastResult.data().forecast();
            if (forecastValues == null || forecastValues.isEmpty()) {
                continue;
            }

            double predictedWeeklyDemand = forecastValues.get(0);
            double dailyBurnRate = predictedWeeklyDemand / 7.0;
            if (dailyBurnRate <= 0.001) {
                continue;
            }

            int physicalStock = product.getStock() != null ? product.getStock() : 0;
            int currentStock = Math.max(0, physicalStock - reservedQuantities.getOrDefault(product.getId(), 0));
            double daysUntilStockout = currentStock / dailyBurnRate;

            if (daysUntilStockout > 14) {
                continue;
            }

            int targetStock = (int) Math.ceil(dailyBurnRate * 14);
            int toOrder = Math.max(0, targetStock - currentStock);
            if (toOrder <= 0) {
                continue;
            }

            suggestions.add(RestockSuggestionDTO.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .currentStock(currentStock)
                    .predictedWeeklyDemand(round(predictedWeeklyDemand))
                    .daysUntilStockout(round(daysUntilStockout))
                    .recommendedRestockAmount(toOrder)
                    .confidenceScore(forecastResult.confidenceScore())
                    .urgency(resolveUrgency(daysUntilStockout))
                    .build());
        }

        if (evaluatedConfidenceScores.isEmpty()) {
            return MlResponse.insufficient();
        }

        suggestions.sort((left, right) -> {
            int urgencyCompare = getUrgencyScore(right.getUrgency()) - getUrgencyScore(left.getUrgency());
            if (urgencyCompare != 0) {
                return urgencyCompare;
            }
            return Double.compare(left.getDaysUntilStockout(), right.getDaysUntilStockout());
        });

        double overallConfidence = evaluatedConfidenceScores.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        return MlResponse.ok(suggestions, overallConfidence);
    }

    private Map<Long, Integer> loadReservedQuantities(List<ProductEntity> products) {
        if (products.isEmpty()) {
            return Map.of();
        }

        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        Map<Long, Integer> reserved = new HashMap<>();
        for (Object[] row : orderItemRepository
                .sumReservedQuantitiesByProductIdsAndTenantId(tenantId, products.stream().map(ProductEntity::getId).toList())) {
            Long productId = ((Number) row[0]).longValue();
            Integer quantity = row[1] != null ? ((Number) row[1]).intValue() : 0;
            reserved.put(productId, quantity);
        }
        return reserved;
    }

    private String resolveUrgency(double daysUntilStockout) {
        if (daysUntilStockout <= 3) {
            return "CRITICAL";
        }
        if (daysUntilStockout <= 7) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private int getUrgencyScore(String urgency) {
        return switch (urgency) {
            case "CRITICAL" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    private double round(double value) {
        return Math.round(Math.max(0.0, value) * 100.0) / 100.0;
    }
}
