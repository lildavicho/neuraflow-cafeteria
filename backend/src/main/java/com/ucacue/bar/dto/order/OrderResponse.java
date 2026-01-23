package com.ucacue.bar.dto.order;

import com.ucacue.bar.entity.OrderEntity;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class OrderResponse {
    Long id;
    Long userId;
    String userName;
    String userEmail;
    OrderEntity.OrderStatus status;
    OrderEntity.PaymentMethod paymentMethod;
    String paymentReference;
    OrderEntity.PaymentStatus paymentStatus;
    BigDecimal subtotal;
    BigDecimal tax;
    BigDecimal total;
    Integer loyaltyPoints;
    String notes;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime preparationStartAt;
    LocalDateTime estimatedReadyAt;
    LocalDateTime actualReadyAt;
    List<OrderItemResponse> items;
}
