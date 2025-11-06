package com.ucacue.bar.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class OrderPaymentRequest {
    @NotNull
    String method; // CASH, CARD, TRANSFER
    String reference;
}
