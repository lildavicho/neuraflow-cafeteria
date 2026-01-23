package com.ucacue.bar.controller;

import com.ucacue.bar.entity.CategoryEntity;
import com.ucacue.bar.repository.CategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Category endpoints")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private static final java.util.List<String> DEFAULT_CATEGORIES =
            java.util.List.of("Desayunos", "Almuerzos", "Snacks", "Bebidas", "Postres", "Ingredientes");

    @GetMapping
    @Operation(summary = "List all categories")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER','BUYER','CUSTOMER')")
    public ResponseEntity<List<CategoryEntity>> list(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Boolean active) {
        List<CategoryEntity> result = (active != null)
                ? categoryRepository.findByActive(active)
                : categoryRepository.findAll();
        if (result.isEmpty() && (active == null || Boolean.TRUE.equals(active))) {
            result = ensureDefaults();
        }
        result.sort(java.util.Comparator.comparing(CategoryEntity::getName));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/seed")
    @Operation(summary = "Seed default categories if missing")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER','BUYER','CUSTOMER')")
    public ResponseEntity<List<CategoryEntity>> seed() {
        return ResponseEntity.ok(ensureDefaults());
    }

    private List<CategoryEntity> ensureDefaults() {
        for (String name : DEFAULT_CATEGORIES) {
            var opt = categoryRepository.findByName(name);
            if (opt.isPresent()) {
                var cat = opt.get();
                if (Boolean.FALSE.equals(cat.getActive())) {
                    cat.setActive(true);
                    categoryRepository.save(cat);
                }
            } else {
                CategoryEntity c = new CategoryEntity();
                c.setName(name);
                c.setActive(true);
                categoryRepository.save(c);
            }
        }
        List<CategoryEntity> result = categoryRepository.findByActive(true);
        result.sort(java.util.Comparator.comparing(CategoryEntity::getName));
        return result;
    }
}
