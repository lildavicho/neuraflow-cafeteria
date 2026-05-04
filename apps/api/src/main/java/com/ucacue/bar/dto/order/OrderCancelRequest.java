package com.ucacue.bar.dto.order;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OrderCancelRequest {

    @Size(max = 200)
    String reason;
}
