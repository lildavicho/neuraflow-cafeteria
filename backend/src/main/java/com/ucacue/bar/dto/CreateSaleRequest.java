package com.ucacue.bar.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSaleRequest {
    
    @NotEmpty(message = "Los items son obligatorios")
    private List<SaleItemRequest> items;
    
    @NotNull(message = "El método de pago es obligatorio")
    private String paymentMethod;
    
    @Builder.Default
    private String orderType = "CARRY";
    
    private String notes;
    
    private String customerName;
    private String customerIdentification;
    
    // For cash payments
    private BigDecimal cashReceived;
    private BigDecimal change;
    
    // For electronic payments
    private String paymentReference;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaleItemRequest {
        @NotNull(message = "El ID del producto es obligatorio")
        private Long productId;
        
        @NotNull(message = "La cantidad es obligatoria")
        private Integer quantity;
        
        @NotNull(message = "El precio unitario es obligatorio")
        private BigDecimal unitPrice;
    }
}
