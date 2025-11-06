package com.ucacue.bar.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class OrderPriceRequest {
    @NotEmpty
    @Valid
    List<OrderCreateRequest.Item> items;
}
