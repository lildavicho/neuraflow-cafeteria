package com.ucacue.bar.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SaleDTO {
    
    private Long id;
    private String invoiceNumber;
    private Long userId;
    private String userName;
    private String userEmail;
    
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal iva;
    private BigDecimal total;
    
    private String paymentMethod;
    private String orderType;
    private String status;
    private String notes;
    private String paymentReference;
    
    private List<SaleItemDTO> items;
    
    private LocalDateTime created;
    private LocalDateTime updated;
    
    // Additional fields for receipts
    private BigDecimal cashReceived;
    private BigDecimal change;
    private String customerName;
    private String customerIdentification;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaleItemDTO {
        private Long id;
        private Long productId;
        private String productCode;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
        private BigDecimal subtotalDiscount;
    }
}
