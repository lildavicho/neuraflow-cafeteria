package com.ucacue.bar.controller;

import com.ucacue.bar.dto.auth.*;
import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.erp.commercial.application.TenantFeatureService;
import com.ucacue.bar.exception.UnauthorizedException;
import com.ucacue.bar.repository.UserRepository;
import com.ucacue.bar.security.JwtUtil;
import com.ucacue.bar.service.AuthService;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {
    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final TenantFeatureService tenantFeatureService;

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        String ua = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(authService.loginWithContext(request, ip, ua));
    }

    @PostMapping("/register-tenant")
    @Operation(summary = "Register a new business tenant with its administrator user")
    public ResponseEntity<AuthResponse> registerTenant(@Valid @RequestBody RegisterTenantRequest request,
                                                        HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        String ua = httpRequest.getHeader("User-Agent");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerTenant(request, ip, ua));
    }

    @PostMapping("/2fa/verify")
    @Operation(summary = "Verify 2FA TOTP code")
    public ResponseEntity<AuthResponse> verify2FA(@Valid @RequestBody TwoFactorRequest request,
                                                   HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        String ua = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(authService.verify2FA(request, ip, ua));
    }

    @PostMapping("/2fa/setup")
    @Operation(summary = "Setup 2FA for authenticated user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> setup2FA() {
        return ResponseEntity.ok(authService.setup2FA());
    }

    @PostMapping("/2fa/confirm")
    @Operation(summary = "Confirm 2FA setup with TOTP code")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> confirm2FA(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.confirm2FA(body.get("code")));
    }

    @PostMapping("/2fa/disable")
    @Operation(summary = "Disable 2FA")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> disable2FA() {
        return ResponseEntity.ok(authService.disable2FA());
    }

    @PostMapping("/firebase")
    @Operation(summary = "Authenticate with Firebase ID token")
    public ResponseEntity<AuthResponse> firebaseAuth(@Valid @RequestBody FirebaseAuthRequest request) {
        return ResponseEntity.ok(authService.firebaseAuth(request));
    }

    @PostMapping("/password-reset/sms")
    @Operation(summary = "Reset password after Firebase SMS verification")
    public ResponseEntity<Map<String, String>> resetPasswordWithSms(
            @Valid @RequestBody SmsPasswordResetRequest request) {
        return ResponseEntity.ok(authService.resetPasswordWithSms(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(java.util.Map.of("message", "Sesion cerrada exitosamente"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT token")
    public ResponseEntity<JwtResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            String username = jwtUtil.extractUsername(refreshToken);
            UserEntity user = userRepository.findByEmailIgnoreCase(username)
                    .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
            var userDetails = userDetailsService.loadUserByUsername(username);
            if (!jwtUtil.validateToken(refreshToken, userDetails, "refresh")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String newToken = jwtUtil.generateToken(userDetails, user.getRole().name(), user.getId());
            String newRefreshToken = jwtUtil.generateRefreshToken(userDetails, user.getRole().name(), user.getId());

            return ResponseEntity.ok(JwtResponse.builder()
                    .token(newToken)
                    .refreshToken(newRefreshToken)
                    .build());
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException | UnauthorizedException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("Usuario no autenticado");
        }
        UserEntity user = userRepository.findByEmailIgnoreCaseWithTenant(email)
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
        TenantFeatureService.TenantPlanView commercialPlan = tenantFeatureService.resolvePlanForTenant(user.getTenant());

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", user.getId());
        resp.put("email", user.getEmail());
        resp.put("name", user.getName());
        resp.put("role", user.getRole().name());
        resp.put("tenantCode", commercialPlan.tenantCode());
        resp.put("platformAdmin", Boolean.TRUE.equals(user.getPlatformAdmin()));
        resp.put("active", user.getActive());
        resp.put("avatarUrl", user.getAvatarUrl());
        resp.put("totpEnabled", user.getTotpEnabled());
        resp.put("lastLoginAt", user.getLastLoginAt());
        resp.put("commercialPlan", commercialPlan.commercialPlan());
        resp.put("enabledModules", commercialPlan.enabledModules());
        return ResponseEntity.ok(resp);
    }

}
