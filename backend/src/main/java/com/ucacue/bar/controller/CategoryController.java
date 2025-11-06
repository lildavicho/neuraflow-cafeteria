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

    @GetMapping
    @Operation(summary = "List all categories")
    @PreAuthorize("hasAnyRole('ADMIN','BUYER')")
    public ResponseEntity<List<CategoryEntity>> list(@org.springframework.web.bind.annotation.RequestParam(required = false) Boolean active) {
        List<CategoryEntity> result = (active != null)
            ? categoryRepository.findByActive(active)
            : categoryRepository.findAll();
        result.sort(java.util.Comparator.comparing(CategoryEntity::getName));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/seed")
    @Operation(summary = "Seed default categories if missing")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CategoryEntity>> seed() {
        java.util.List<String> names = java.util.List.of("Bebidas", "Ingredientes", "Postres", "Comidas");
        for (String name : names) {
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
        return ResponseEntity.ok(result);
    }
}
