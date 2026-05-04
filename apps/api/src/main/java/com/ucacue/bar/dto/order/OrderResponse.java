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
    OrderEntity.TransactionStatus transactionStatus;
    OrderEntity.PaymentMethod paymentMethod;
    String paymentReference;
    String paymentBreakdownJson;
    OrderEntity.PaymentStatus paymentStatus;
    OrderEntity.InventoryStatus inventoryStatus;
    OrderEntity.DocumentType documentType;
    BigDecimal subtotal;
    BigDecimal tax;
    BigDecimal total;
    Integer loyaltyPoints;
    String notes;
    String customerName;
    String customerEmail;
    String customerIdentification;
    String customerIdentificationType;
    String customerAddress;
    String customerPhone;
    LocalDateTime createdAt;
    LocalDateTime paidAt;
    LocalDateTime cancelledAt;
    LocalDateTime refundedAt;
    LocalDateTime updatedAt;
    LocalDateTime preparationStartAt;
    LocalDateTime estimatedReadyAt;
    LocalDateTime actualReadyAt;
    List<OrderItemResponse> items;
}
