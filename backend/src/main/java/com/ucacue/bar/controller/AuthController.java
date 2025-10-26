package com.ucacue.bar.controller;

import com.ucacue.bar.config.RateLimitingConfig;
import com.ucacue.bar.dto.auth.*;
import com.ucacue.bar.service.AuthService;
import io.github.bucket4j.Bucket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {
    
    private final AuthService authService;
    private final RateLimitingConfig rateLimitingConfig;
    private final PasswordEncoder passwordEncoder;
    
    @PostMapping("/register")
    @Operation(summary = "Register new user")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }
    
    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        // Rate limiting
        String key = "login:" + httpRequest.getRemoteAddr();
        Bucket bucket = rateLimitingConfig.resolveBucket(key);
        
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(AuthResponse.builder()
                    .message("Demasiados intentos de inicio de sesión. Por favor, espere un minuto.")
                    .build());
        }
        
        return ResponseEntity.ok(authService.login(request));
    }
    
    @PostMapping("/firebase")
    @Operation(summary = "Authenticate with Firebase ID token")
    public ResponseEntity<AuthResponse> firebaseAuth(@Valid @RequestBody FirebaseAuthRequest request) {
        return ResponseEntity.ok(authService.firebaseAuth(request));
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }
    
    @PostMapping("/2fa/verify")
    @Operation(summary = "Verify 2FA code")
    public ResponseEntity<AuthResponse> verifyTwoFactor(@Valid @RequestBody TwoFactorRequest request) {
        return ResponseEntity.ok(authService.verifyTwoFactor(request));
    }
    
    @PostMapping("/2fa/send")
    @Operation(summary = "Send 2FA code")
    public ResponseEntity<Map<String, String>> sendTwoFactorCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        authService.sendTwoFactorCode(email);
        return ResponseEntity.ok(Map.of("message", "Código de verificación enviado"));
    }
    
    @PostMapping("/logout")
    @Operation(summary = "Logout user")
    public ResponseEntity<Map<String, String>> logout() {
        // JWT es stateless, el logout se maneja en el cliente
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada exitosamente"));
    }
    
    @GetMapping("/validate")
    @Operation(summary = "Validate current token")
    public ResponseEntity<Map<String, Boolean>> validateToken() {
        // Si llega aquí, el token es válido (validado por el filtro JWT)
        return ResponseEntity.ok(Map.of("valid", true));
    }
    
    @GetMapping("/generate-hash")
    @Operation(summary = "Generate BCrypt hash for password (TEMPORARY - REMOVE IN PRODUCTION)")
    public ResponseEntity<Map<String, String>> generateHash(@RequestParam String password) {
        String hash = passwordEncoder.encode(password);
        Map<String, String> response = new HashMap<>();
        response.put("password", password);
        response.put("hash", hash);
        response.put("length", String.valueOf(hash.length()));
        return ResponseEntity.ok(response);
    }
}
