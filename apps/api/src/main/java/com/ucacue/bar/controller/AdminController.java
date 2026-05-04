package com.ucacue.bar.controller;

import com.ucacue.bar.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Endpoints de administración")
public class AdminController {

    private final ProductRepository productRepository;

    @PostMapping("/algolia/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reindexar todos los productos en Algolia")
    public ResponseEntity<Map<String, Object>> reindexProducts() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Algolia deshabilitado. No hay reindexación");
        response.put("productsCount", productRepository.count());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/algolia/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Verificar estado de Algolia")
    public ResponseEntity<Map<String, Object>> algoliaStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("configured", false);
        response.put("productsInDatabase", productRepository.count());
        
        return ResponseEntity.ok(response);
    }
}
