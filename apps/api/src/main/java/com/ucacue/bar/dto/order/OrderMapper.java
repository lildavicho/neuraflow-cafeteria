package com.ucacue.bar.dto.order;

import com.ucacue.bar.entity.OrderEntity;
import com.ucacue.bar.entity.OrderItemEntity;

import java.util.List;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponse toResponse(OrderEntity order) {
        return OrderResponse.builder()
            .id(order.getId())
            .userId(order.getUser() != null ? order.getUser().getId() : null)
            .userName(order.getUser() != null ? order.getUser().getName() : null)
            .userEmail(order.getUser() != null ? order.getUser().getEmail() : null)
            .status(order.getStatus())
            .transactionStatus(order.getTransactionStatus())
            .paymentMethod(order.getPaymentMethod())
            .paymentReference(order.getPaymentReference())
            .paymentBreakdownJson(order.getPaymentBreakdownJson())
            .paymentStatus(order.getPaymentStatus())
            .inventoryStatus(order.getInventoryStatus())
            .documentType(order.getDocumentType())
            .subtotal(order.getSubtotal())
            .tax(order.getTax())
            .total(order.getTotal())
            .loyaltyPoints(order.getLoyaltyPointsAwarded())
            .notes(order.getNotes())
            .customerName(order.getCustomerName())
            .customerEmail(order.getCustomerEmail())
            .customerIdentification(order.getCustomerIdentification())
            .customerIdentificationType(order.getCustomerIdentificationType())
            .customerAddress(order.getCustomerAddress())
            .customerPhone(order.getCustomerPhone())
            .createdAt(order.getCreatedAt())
            .paidAt(order.getPaidAt())
            .cancelledAt(order.getCancelledAt())
            .refundedAt(order.getRefundedAt())
            .updatedAt(order.getUpdatedAt())
            .preparationStartAt(order.getPreparationStartAt())
            .estimatedReadyAt(order.getEstimatedReadyAt())
            .actualReadyAt(order.getActualReadyAt())
            .items(order.getItems().stream().map(OrderMapper::toItem).toList())
            .build();
    }

    private static OrderItemResponse toItem(OrderItemEntity item) {
        return OrderItemResponse.builder()
            .id(item.getId())
            .productId(item.getProduct().getId())
            .productName(item.getProduct().getName())
            .productCode(item.getProduct().getCode())
            .quantity(item.getQuantity())
            .unitPrice(item.getUnitPrice())
            .lineTotal(item.getLineTotal())
            .imageUrl(item.getProduct().getImageUrl())
            .build();
    }

    public static List<OrderResponse> toResponseList(List<OrderEntity> orders) {
        return orders.stream().map(OrderMapper::toResponse).toList();
    }
}
