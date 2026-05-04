package com.ucacue.bar.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JwtResponse {
    private String token;
    private String refreshToken;
}
