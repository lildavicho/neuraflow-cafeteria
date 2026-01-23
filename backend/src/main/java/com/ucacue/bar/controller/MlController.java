package com.ucacue.bar.controller;

import com.ucacue.bar.algo.DemandForecastService;
import com.ucacue.bar.algo.PreparationTimePredictor;
import com.ucacue.bar.algo.RecommendationService;
import com.ucacue.bar.dto.ProductDTO;
import com.ucacue.bar.entity.ProductEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/ml")
@RequiredArgsConstructor
@Tag(name = "ML", description = "Machine learning endpoints")
public class MlController {

    private final PreparationTimePredictor preparationTimePredictor;
    private final RecommendationService recommendationService;
    private final DemandForecastService demandForecastService;

    @PostMapping("/predict-preparation-time")
    @PreAuthorize("hasAnyRole('ADMIN','BUYER','CUSTOMER','CASHIER')")
    @Operation(summary = "Predict preparation time (lightweight model)")
    public ResponseEntity<PreparationTimeResponse> predictPreparationTime(@RequestBody PreparationTimeRequest request) {
        int itemsCount = resolveItemsCount(request);
        PreparationTimePredictor.Estimate estimate = preparationTimePredictor.estimateFor(itemsCount, request.concurrentOrders());
        return ResponseEntity.ok(new PreparationTimeResponse(
            estimate.getEstimatedMinutes(),
            estimate.getConfidence(),
            estimate.getItemsCount(),
            estimate.getConcurrentOrders()
        ));
    }

    @GetMapping("/recommendations/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','BUYER','CUSTOMER','CASHIER')")
    @Operation(summary = "Personalized recommendations for a user")
    public ResponseEntity<List<ProductDTO>> getRecommendations(@PathVariable Long userId) {
        List<ProductEntity> products = recommendationService.getPersonalizedRecommendations(userId);
        return ResponseEntity.ok(products.stream().map(this::toDto).toList());
    }

    @GetMapping("/trending")
    @PreAuthorize("hasAnyRole('ADMIN','BUYER','CUSTOMER','CASHIER')")
    @Operation(summary = "Trending products based on recent sales")
    public ResponseEntity<List<ProductDTO>> getTrending(@RequestParam(defaultValue = "30") int days) {
        List<ProductEntity> products = recommendationService.getTrendingProducts(days);
        return ResponseEntity.ok(products.stream().map(this::toDto).toList());
    }

    @GetMapping("/demand-forecast")
    @PreAuthorize("hasAnyRole('ADMIN','BUYER','CUSTOMER','CASHIER')")
    @Operation(summary = "Demand forecast by product")
    public ResponseEntity<?> getDemandForecast(@RequestParam Long productId,
                                               @RequestParam(defaultValue = "daily") String period,
                                               @RequestParam(defaultValue = "7") int daysAhead) {
        return ResponseEntity.ok(demandForecastService.forecastDemand(productId, period, daysAhead));
    }

    @GetMapping("/staffing-recommendation")
    @PreAuthorize("hasAnyRole('ADMIN','BUYER','CUSTOMER','CASHIER')")
    @Operation(summary = "Staffing recommendation based on historical load")
    public ResponseEntity<?> getStaffingRecommendation(@RequestParam(defaultValue = "1") int daysAhead) {
        return ResponseEntity.ok(demandForecastService.getStaffingRecommendation(daysAhead));
    }

    private int resolveItemsCount(PreparationTimeRequest request) {
        if (request.itemsCount() != null && request.itemsCount() > 0) {
            return request.itemsCount();
        }
        if (request.items() == null || request.items().isEmpty()) {
            return 1;
        }
        return request.items().stream()
            .mapToInt(item -> item.quantity() != null ? item.quantity() : 1)
            .sum();
    }

    private ProductDTO toDto(ProductEntity entity) {
        return ProductDTO.builder()
            .id(entity.getId())
            .categoryId(entity.getCategory() != null ? entity.getCategory().getId() : null)
            .categoryName(entity.getCategory() != null ? entity.getCategory().getName() : null)
            .code(entity.getCode())
            .name(entity.getName())
            .description(entity.getDescription())
            .imageUrl(entity.getImageUrl())
            .unit(entity.getUnit())
            .price(entity.getPrice())
            .stock(entity.getStock())
            .status(entity.getStatus() != null ? entity.getStatus().name() : null)
            .build();
    }

    public record PreparationTimeRequest(List<Item> items, Integer itemsCount, Integer concurrentOrders) {}

    public record Item(Integer quantity, String name) {}

    public record PreparationTimeResponse(int estimatedMinutes, double confidence, int itemsCount, int concurrentOrders) {}
}
