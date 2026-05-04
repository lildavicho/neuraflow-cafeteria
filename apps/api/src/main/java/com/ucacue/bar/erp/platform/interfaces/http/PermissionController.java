package com.ucacue.bar.erp.platform.interfaces.http;

import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.repository.UserRepository;
import com.ucacue.bar.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/erp/permissions")
@RequiredArgsConstructor
@Tag(name = "ERP Permissions", description = "Consulta de permisos del usuario autenticado")
public class PermissionController {

    private final PermissionService permissionService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener permisos efectivos del usuario autenticado")
    public ResponseEntity<Map<String, Object>> myPermissions(Authentication auth) {
        String email = auth.getName();
        UserEntity user = userRepository.findByEmailIgnoreCaseWithTenant(email).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(Map.of(
                    "role", "UNKNOWN",
                    "permissions", List.of(),
                    "platformAdmin", false
            ));
        }

        Set<String> permissions = permissionService.resolvePermissionsFor(user);
        return ResponseEntity.ok(Map.of(
                "role", user.getRole() != null ? user.getRole().name() : "UNKNOWN",
                "permissions", permissions.stream().sorted().toList(),
                "platformAdmin", Boolean.TRUE.equals(user.getPlatformAdmin())
        ));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todos los permisos disponibles en el sistema")
    public ResponseEntity<List<PermissionDto>> listAll() {
        List<PermissionDto> result = permissionService.listAll().stream()
                .map(p -> new PermissionDto(p.getId(), p.getCode(), p.getDescription(), p.getModuleCode()))
                .toList();
        return ResponseEntity.ok(result);
    }

    public record PermissionDto(Long id, String code, String description, String moduleCode) {}
}
