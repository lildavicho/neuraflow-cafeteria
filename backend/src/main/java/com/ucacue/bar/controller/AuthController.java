package com.ucacue.bar.controller;

import com.ucacue.bar.dto.auth.*;
import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.exception.UnauthorizedException;
import com.ucacue.bar.repository.UserRepository;
import com.ucacue.bar.security.JwtUtil;
import com.ucacue.bar.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
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

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/firebase")
    @Operation(summary = "Authenticate with Firebase ID token")
    public ResponseEntity<AuthResponse> firebaseAuth(@Valid @RequestBody FirebaseAuthRequest request) {
        return ResponseEntity.ok(authService.firebaseAuth(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user")
    public ResponseEntity<Map<String, String>> logout() {
        // JWT is stateless, logout is handled on the client
        return ResponseEntity.ok(java.util.Map.of("message", "Sesion cerrada exitosamente"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT token")
    public ResponseEntity<JwtResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            String username = jwtUtil.extractUsername(request.getRefreshToken());
            if (username == null || username.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (userDetails == null || !jwtUtil.validateToken(request.getRefreshToken(), userDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            UserEntity user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

            String newToken = jwtUtil.generateToken(userDetails, user.getRole().name(), user.getId());
            String refreshToken = jwtUtil.generateRefreshToken(userDetails, user.getRole().name(), user.getId());

            return ResponseEntity.ok(JwtResponse.builder()
                    .token(newToken)
                    .refreshToken(refreshToken)
                    .build());
        } catch (Exception ex) {
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
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", user.getId());
        resp.put("email", user.getEmail());
        resp.put("name", user.getName());
        resp.put("role", user.getRole().name());
        resp.put("active", user.getActive());
        resp.put("avatarUrl", user.getAvatarUrl());
        return ResponseEntity.ok(resp);
    }
}

