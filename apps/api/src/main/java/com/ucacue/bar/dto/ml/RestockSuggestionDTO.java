package com.ucacue.bar.dto.ml;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RestockSuggestionDTO {
    private Long productId;
    private String productName;
    private Integer currentStock;
    private Double predictedWeeklyDemand;
    private Double daysUntilStockout;
    private Integer recommendedRestockAmount;
    private Double confidenceScore;
    private String urgency; // LOW, MEDIUM, CRITICAL
}
