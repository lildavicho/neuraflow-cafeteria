package com.ucacue.bar.dto.loyalty;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class LoyaltyLedgerEntryResponse {
    Long id;
    Integer delta;
    String reason;
    LocalDateTime createdAt;
}
