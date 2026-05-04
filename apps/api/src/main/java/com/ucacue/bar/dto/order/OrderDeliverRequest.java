package com.ucacue.bar.dto.order;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OrderDeliverRequest {

    @Size(max = 100, message = "La referencia de pago no puede exceder 100 caracteres")
    String paymentReference;
}
