package com.ucacue.bar.controller;

import com.ucacue.bar.dto.ProductDTO;
import com.ucacue.bar.erp.commercial.interfaces.http.RequireTenantModule;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.ModuleCode;
import com.ucacue.bar.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management endpoints")
public class ProductController {
    
    private final ProductService productService;
    
    @GetMapping
    @RequireTenantModule(ModuleCode.INVENTORY)
    @PreAuthorize("hasAnyRole('ADMIN', 'BUYER', 'CUSTOMER')")
    @Operation(summary = "Get all products with pagination")
    public ResponseEntity<Page<ProductDTO>> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean prepared,
            @PageableDefault(size = 50, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(search, categoryId, prepared, pageable));
    }
    
    @GetMapping("/public")
    @RequireTenantModule(ModuleCode.POS)
    @Operation(summary = "Get public product list for POS")
    public ResponseEntity<List<ProductDTO>> getPublicProducts(
            @PageableDefault(size = 100, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productService.getPublicProducts(pageable));
    }
    
    @GetMapping("/{id}")
    @RequireTenantModule(ModuleCode.INVENTORY)
    @PreAuthorize("hasAnyRole('ADMIN', 'BUYER', 'CUSTOMER')")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }
    
    @PostMapping
    @RequireTenantModule(ModuleCode.INVENTORY)
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('products:create')")
    @Operation(summary = "Create new product")
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody ProductDTO productDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(productService.createProduct(productDTO));
    }

    @PutMapping("/{id}")
    @RequireTenantModule(ModuleCode.INVENTORY)
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('products:update')")
    @Operation(summary = "Update product")
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductDTO productDTO) {
        return ResponseEntity.ok(productService.updateProduct(id, productDTO));
    }

    @DeleteMapping("/{id}")
    @RequireTenantModule(ModuleCode.INVENTORY)
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('products:delete')")
    @Operation(summary = "Delete product")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/low-stock")
    @RequireTenantModule(ModuleCode.INVENTORY)
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('inventory:alerts')")
    @Operation(summary = "Get products with low stock")
    public ResponseEntity<List<ProductDTO>> getLowStockProducts(
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(productService.getLowStockProducts(size));
    }

    @PostMapping("/{id}/update-stock")
    @RequireTenantModule(ModuleCode.INVENTORY)
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('inventory:move')")
    @Operation(summary = "Update product stock")
    public ResponseEntity<ProductDTO> updateStock(
            @PathVariable Long id,
            @RequestParam Integer quantity,
            @RequestParam String type,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(productService.updateStock(id, quantity, type, reason));
    }
}

