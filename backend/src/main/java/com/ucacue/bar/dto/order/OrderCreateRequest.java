package com.ucacue.bar.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class OrderCreateRequest {

    @NotEmpty(message = "Debe incluir al menos un producto")
    @Valid
    List<Item> items;

    @NotNull(message = "El método de pago es requerido")
    OrderPaymentMethod paymentMethod;

    String notes;

    @Value
    @Builder
    public static class Item {
        @NotNull
        Long productId;

        @Min(value = 1, message = "La cantidad mínima es 1")
        Integer quantity;

        @NotNull
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
        BigDecimal unitPrice;
    }

    public enum OrderPaymentMethod {
        CASH,
        CARD,
        TRANSFER
    }
}
