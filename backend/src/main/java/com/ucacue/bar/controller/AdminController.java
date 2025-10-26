package com.ucacue.bar.controller;

import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.repository.ProductRepository;
import com.ucacue.bar.service.AlgoliaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Endpoints de administración")
public class AdminController {

    private final ProductRepository productRepository;
    private final AlgoliaService algoliaService;

    @PostMapping("/algolia/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reindexar todos los productos en Algolia")
    public ResponseEntity<Map<String, Object>> reindexProducts() {
        try {
            log.info("Starting Algolia reindex...");
            
            List<ProductEntity> products = productRepository.findAll();
            algoliaService.reindexAllProducts(products);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reindexing started");
            response.put("productsCount", products.size());
            
            log.info("Reindexed {} products to Algolia", products.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error reindexing products: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/algolia/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Verificar estado de Algolia")
    public ResponseEntity<Map<String, Object>> algoliaStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("configured", true);
        response.put("productsInDatabase", productRepository.count());
        
        return ResponseEntity.ok(response);
    }
}
