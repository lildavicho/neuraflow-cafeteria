package com.ucacue.bar.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    OrderDocumentType documentType;

    @Size(max = 200, message = "El nombre del cliente es muy largo")
    String customerName;

    @Email(message = "El correo del cliente no es valido")
    @Size(max = 180, message = "El correo del cliente es muy largo")
    String customerEmail;

    @Size(max = 20, message = "La identificacion del cliente es muy larga")
    String customerIdentification;

    @Size(max = 300, message = "La direccion del cliente es muy larga")
    String customerAddress;

    @Size(max = 30, message = "El telefono del cliente es muy largo")
    String customerPhone;

    @Value
    @Builder
    public static class Item {
        @NotNull
        Long productId;

        @Min(value = 1, message = "La cantidad mínima es 1")
        Integer quantity;

        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
        BigDecimal unitPrice;

        @DecimalMin(value = "0.00", message = "El descuento no puede ser negativo")
        BigDecimal discountAmount;
    }

    public enum OrderPaymentMethod {
        CASH,
        CARD,
        TRANSFER
    }

    public enum OrderDocumentType {
        NOTA_VENTA,
        FACTURA
    }
}
