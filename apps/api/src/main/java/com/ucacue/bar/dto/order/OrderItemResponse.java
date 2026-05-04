package com.ucacue.bar.dto.order;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class OrderItemResponse {
    Long id;
    Long productId;
    String productName;
    String productCode;
    Integer quantity;
    BigDecimal unitPrice;
    BigDecimal lineTotal;
    String imageUrl;
}
