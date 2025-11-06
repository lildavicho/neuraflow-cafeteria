package com.ucacue.bar.controller;

import com.ucacue.bar.dto.auth.*;
import com.ucacue.bar.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {
    private final AuthService authService;
    
    @PostMapping("/firebase")
    @Operation(summary = "Authenticate with Firebase ID token")
    public ResponseEntity<AuthResponse> firebaseAuth(@Valid @RequestBody FirebaseAuthRequest request) {
        return ResponseEntity.ok(authService.firebaseAuth(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user")
    public ResponseEntity<Map<String, String>> logout() {
        // JWT es stateless, el logout se maneja en el cliente
        return ResponseEntity.ok(java.util.Map.of("message", "Sesión cerrada exitosamente"));
    }
}
