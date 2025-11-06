package com.ucacue.bar.dto.loyalty;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class LoyaltyBalanceResponse {
    Long userId;
    Integer points;
    String tier;
    LocalDateTime updatedAt;
}
