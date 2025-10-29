package com.ucacue.bar.controller;

import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/debug")
@Profile("dev")
@Tag(name = "Debug", description = "Endpoints de diagnóstico (solo dev)")
public class DebugController {

    private final UserRepository userRepository;

    public DebugController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/whoami")
    @Operation(summary = "Información del usuario autenticado")
    public ResponseEntity<Map<String, Object>> whoAmI(Authentication authentication) {
        Map<String, Object> resp = new HashMap<>();
        if (authentication == null) {
            resp.put("authenticated", false);
            return ResponseEntity.ok(resp);
        }
        String principal = String.valueOf(authentication.getPrincipal());
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        Optional<UserEntity> userOpt = userRepository.findByEmail(principal);
        resp.put("authenticated", true);
        resp.put("email", principal);
        resp.put("roles", roles);
        userOpt.ifPresent(u -> {
            resp.put("userId", u.getId());
            resp.put("role", u.getRole().name());
            resp.put("provider", u.getProvider());
            resp.put("firebaseUid", u.getFirebaseUid());
        });
        return ResponseEntity.ok(resp);
    }
}
