package com.ucacue.bar.controller;

import com.ucacue.bar.service.PushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/push")
@RequiredArgsConstructor
@Tag(name = "Push", description = "Registro de tokens Web Push Firebase")
public class PushController {

    private final PushService pushService;
    private final com.ucacue.bar.repository.UserRepository userRepository;

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('ADMIN','BUYER','CUSTOMER')")
    @Operation(summary = "Registrar token FCM para notificaciones push")
    public ResponseEntity<Void> register(@RequestBody @jakarta.validation.Valid PushRegisterRequest request,
                                         Authentication authentication) {
        Long userId = userRepository.findByEmail(authentication.getName())
            .map(com.ucacue.bar.entity.UserEntity::getId)
            .orElseThrow(() -> new com.ucacue.bar.exception.NotFoundException("Usuario no encontrado"));
        pushService.registerToken(userId, request.token(), request.platform(), request.deviceName(), request.active());
        return ResponseEntity.accepted().build();
    }

    public record PushRegisterRequest(
        @NotBlank
        String token,
        String platform,
        String deviceName,
        Boolean active
    ) {}
}


