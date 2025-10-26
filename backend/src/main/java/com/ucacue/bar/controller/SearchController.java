package com.ucacue.bar.controller;

import com.ucacue.bar.service.AlgoliaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Search endpoints (Algolia/DB)")
public class SearchController {

    private final AlgoliaService algoliaService;

    @GetMapping("/products")
    @Operation(summary = "Search products by query")
    @PreAuthorize("hasAnyRole('ADMIN','COMPRADOR')")
    public ResponseEntity<List<Map<String, Object>>> searchProducts(@RequestParam("q") String q) {
        return ResponseEntity.ok(algoliaService.searchProducts(q));
    }
}
