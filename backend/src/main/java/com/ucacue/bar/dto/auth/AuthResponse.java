package com.ucacue.bar.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String token;
    private String refreshToken;
    private Long userId;
    private String email;
    private String fullName;
    private String role;
    private boolean requires2FA;
    private String message;
}
