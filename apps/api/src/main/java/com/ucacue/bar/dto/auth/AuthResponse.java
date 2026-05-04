package com.ucacue.bar.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

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
    private String tenantCode;
    private Boolean platformAdmin;
    private String commercialPlan;
    private List<String> enabledModules;
    private boolean requires2FA;
    private Boolean totpEnabled;
    private String message;
}
