package com.ucacue.bar.controller;

import com.ucacue.bar.entity.CategoryEntity;
import com.ucacue.bar.repository.CategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
    @PreAuthorize("hasAnyRole('ADMIN','COMPRADOR')")
    public ResponseEntity<List<CategoryEntity>> list() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }
}
