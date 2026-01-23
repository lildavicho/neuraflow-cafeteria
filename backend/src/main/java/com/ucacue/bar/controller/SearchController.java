package com.ucacue.bar.controller;

import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.repository.ProductRepository;
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
import java.util.HashMap;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Search endpoints (DB)")
public class SearchController {

    private final ProductRepository productRepository;

    @GetMapping("/products")
    @Operation(summary = "Search products by query")
    @PreAuthorize("hasAnyRole('ADMIN','BUYER','CUSTOMER')")
    public ResponseEntity<List<Map<String, Object>>> searchProducts(@RequestParam("q") String q) {
        var page = productRepository.searchProducts(q, org.springframework.data.domain.PageRequest.of(0, 50));
        List<ProductEntity> items = page.getContent();
        List<Map<String, Object>> out = items.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("objectID", String.valueOf(p.getId()));
            m.put("name", p.getName());
            m.put("code", p.getCode());
            m.put("price", p.getPrice());
            m.put("stock", p.getStock());
            m.put("categoryName", p.getCategory() != null ? p.getCategory().getName() : null);
            m.put("description", p.getDescription());
            return m;
        }).toList();
        return ResponseEntity.ok(out);
    }
}


