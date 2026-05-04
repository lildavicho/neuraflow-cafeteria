package com.ucacue.bar.service;

import com.ucacue.bar.dto.ProductDTO;
import com.ucacue.bar.entity.CategoryEntity;
import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.entity.ProductEntity.ProductStatus;
import com.ucacue.bar.entity.StockMovementEntity;
import com.ucacue.bar.entity.StockMovementEntity.MovementType;
import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.repository.CategoryRepository;
import com.ucacue.bar.repository.OrderItemRepository;
import com.ucacue.bar.repository.ProductRepository;
import com.ucacue.bar.repository.StockMovementRepository;
import com.ucacue.bar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final String MSG_PRODUCTO_NO_ENCONTRADO = "Producto no encontrado";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;
    private final RealtimeGateway realtimeGateway;
    private final TenantContextResolver tenantContextResolver;
    private final AuditService auditService;

    @Cacheable(cacheNames = "products-list",
            key = "#root.target.currentTenantIdForCache() + ':' + (#search ?: '') + ':' + (#categoryId ?: '') + ':' + (#prepared ?: '') + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<ProductDTO> getAllProducts(String search, Long categoryId, Boolean prepared, Pageable pageable) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        Page<ProductEntity> products;

        if (search != null && !search.isBlank()) {
            products = productRepository.searchProductsWithPreparedByTenantId(tenantId, search.trim(), prepared, pageable);
        } else if (categoryId != null && prepared != null) {
            products = productRepository.findByTenantIdAndCategoryIdAndPrepared(tenantId, categoryId, prepared, pageable);
        } else if (categoryId != null) {
            products = productRepository.findByTenantIdAndCategoryId(tenantId, categoryId, pageable);
        } else if (prepared != null) {
            products = productRepository.findByTenantIdAndPrepared(tenantId, prepared, pageable);
        } else {
            products = productRepository.findByTenantId(tenantId, pageable);
        }

        Map<Long, Integer> reservedQuantities = loadReservedQuantities(products.getContent());
        return products.map(product -> convertToDTO(product, reservedQuantities));
    }

    @Cacheable(cacheNames = "products-public",
            key = "#root.target.currentTenantIdForCache() + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public List<ProductDTO> getPublicProducts(Pageable pageable) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        Page<ProductEntity> products = productRepository.findAvailablePreparedProductsByTenantId(tenantId, pageable);
        Map<Long, Integer> reservedQuantities = loadReservedQuantities(products.getContent());
        return products.stream()
                .map(product -> convertToPublicDTO(product, reservedQuantities))
                .filter(product -> (product.getAvailableStock() != null ? product.getAvailableStock() : 0) > 0)
                .toList();
    }

    public ProductDTO getProductById(Long id) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        ProductEntity product = productRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new NotFoundException(MSG_PRODUCTO_NO_ENCONTRADO));
        return convertToDTO(product, loadReservedQuantities(List.of(product)));
    }

    @Transactional
    @CacheEvict(cacheNames = {"products-list", "products-public", "products-low-stock", "dashboard-snapshots", "sales-reports"}, allEntries = true)
    public ProductDTO createProduct(ProductDTO productDTO) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();

        if (productDTO.getCode() != null && !productDTO.getCode().isBlank()
                && productRepository.existsByTenantIdAndCode(tenantId, productDTO.getCode())) {
            throw new BadRequestException("El codigo de producto ya existe");
        }

        CategoryEntity category = categoryRepository.findByTenantIdAndId(tenantId, productDTO.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Categoria no encontrada"));

        ProductEntity product = new ProductEntity();
        product.setTenantId(tenantId);
        product.setCategory(category);
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setImageUrl(productDTO.getImageUrl());
        product.setUnit(productDTO.getUnit() != null && !productDTO.getUnit().isBlank()
                ? productDTO.getUnit()
                : "UNIDAD");
        product.setPrice(productDTO.getPrice());
        product.setPurchasePrice(productDTO.getPurchasePrice());
        product.setStock(productDTO.getStock() != null ? productDTO.getStock() : 0);
        product.setMinStock(productDTO.getMinStock() != null ? productDTO.getMinStock() : 5);
        product.setStatus(product.getStock() > 0 ? ProductStatus.AVAILABLE : ProductStatus.SOLD_OUT);
        boolean defaultPrepared = !"Ingredientes".equalsIgnoreCase(category.getName());
        product.setPrepared(productDTO.getPrepared() != null ? productDTO.getPrepared() : defaultPrepared);

        if (productDTO.getCode() != null && !productDTO.getCode().isBlank()) {
            product.setCode(productDTO.getCode());
        } else {
            String provisional;
            do {
                provisional = "TMP-"
                        + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            } while (productRepository.existsByTenantIdAndCode(tenantId, provisional));
            product.setCode(provisional);
        }

        product = productRepository.save(product);

        if (product.getCode() != null && product.getCode().startsWith("TMP-")) {
            String slug = slugify(product.getName());
            String idStr = String.valueOf(product.getId());
            int maxLen = Math.max(1, 20 - 1 - idStr.length());
            if (slug.length() > maxLen) {
                slug = slug.substring(0, maxLen);
            }
            String finalCode = slug + "-" + idStr;
            if (!productRepository.existsByTenantIdAndCode(tenantId, finalCode)) {
                product.setCode(finalCode);
                product = productRepository.save(product);
            }
        }

        ProductDTO dto = convertToDTO(product, Map.of(product.getId(), 0));
        log.info("Product created: {} ({})", product.getName(), product.getCode());
        auditService.logAction(currentUserEmail(), "PRODUCT_CREATE", "Product", product.getId(),
                "code=" + product.getCode() + " name=" + product.getName());
        publishProductsRefresh();
        broadcastProductChange("created", dto);
        return dto;
    }

    @Transactional
    @CacheEvict(cacheNames = {"products-list", "products-public", "products-low-stock", "dashboard-snapshots", "sales-reports"}, allEntries = true)
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        ProductEntity product = productRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new NotFoundException(MSG_PRODUCTO_NO_ENCONTRADO));

        if (productDTO.getCode() != null && !productDTO.getCode().isBlank()
                && !Objects.equals(product.getCode(), productDTO.getCode())
                && productRepository.existsByTenantIdAndCode(tenantId, productDTO.getCode())) {
            throw new BadRequestException("El codigo de producto ya existe");
        }

        if (productDTO.getCategoryId() != null) {
            CategoryEntity category = categoryRepository.findByTenantIdAndId(tenantId, productDTO.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Categoria no encontrada"));
            product.setCategory(category);
        }
        if (productDTO.getCode() != null && !productDTO.getCode().isBlank()) {
            product.setCode(productDTO.getCode());
        }
        if (productDTO.getName() != null && !productDTO.getName().isBlank()) {
            product.setName(productDTO.getName());
        }
        if (productDTO.getDescription() != null) {
            product.setDescription(productDTO.getDescription());
        }
        if (productDTO.getImageUrl() != null) {
            product.setImageUrl(productDTO.getImageUrl());
        }
        if (productDTO.getUnit() != null && !productDTO.getUnit().isBlank()) {
            product.setUnit(productDTO.getUnit());
        }
        if (productDTO.getPrice() != null) {
            product.setPrice(productDTO.getPrice());
        }
        if (productDTO.getPurchasePrice() != null) {
            product.setPurchasePrice(productDTO.getPurchasePrice());
        }
        if (productDTO.getMinStock() != null) {
            product.setMinStock(productDTO.getMinStock());
        }
        if (productDTO.getPrepared() != null) {
            product.setPrepared(productDTO.getPrepared());
        }
        if (productDTO.getStatus() != null) {
            try {
                product.setStatus(ProductStatus.valueOf(productDTO.getStatus()));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Estado de producto invalido: " + productDTO.getStatus());
            }
        }
        if (product.getStock() <= 0) {
            product.setStatus(ProductStatus.SOLD_OUT);
        }

        product = productRepository.save(product);

        ProductDTO dto = convertToDTO(product, loadReservedQuantities(List.of(product)));
        log.info("Product updated: {} ({})", product.getName(), product.getCode());
        auditService.logAction(currentUserEmail(), "PRODUCT_UPDATE", "Product", product.getId(),
                "code=" + product.getCode() + " name=" + product.getName());
        publishProductsRefresh();
        broadcastProductChange("updated", dto);
        return dto;
    }

    @Transactional
    @CacheEvict(cacheNames = {"products-list", "products-public", "products-low-stock", "dashboard-snapshots", "sales-reports"}, allEntries = true)
    public void deleteProduct(Long id) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        ProductEntity product = productRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new NotFoundException(MSG_PRODUCTO_NO_ENCONTRADO));

        product.setStatus(ProductStatus.DISCONTINUED);
        productRepository.save(product);

        log.info("Product discontinued: {} ({})", product.getName(), product.getCode());
        auditService.logAction(currentUserEmail(), "PRODUCT_DELETE", "Product", product.getId(),
                "code=" + product.getCode() + " name=" + product.getName());
        broadcastProductChange("deleted",
                ProductDTO.builder().id(product.getId()).status(product.getStatus().name()).build());
    }

    @Cacheable(cacheNames = "products-low-stock", key = "#root.target.currentTenantIdForCache() + ':default'")
    public List<ProductDTO> getLowStockProducts() {
        return loadLowStockProducts(50);
    }

    @Cacheable(cacheNames = "products-low-stock", key = "#root.target.currentTenantIdForCache() + ':' + #limit")
    public List<ProductDTO> getLowStockProducts(int limit) {
        return loadLowStockProducts(limit);
    }

    @Cacheable(cacheNames = "products-low-stock", key = "#root.target.currentTenantIdForCache() + ':count'")
    public long countLowStockProducts() {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return productRepository.countLowStockProductsByTenantId(tenantId);
    }

    private List<ProductDTO> loadLowStockProducts(int limit) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        List<ProductEntity> products = productRepository.findLowStockProductsByTenantId(
                tenantId,
                PageRequest.of(0, safeLimit));
        Map<Long, Integer> reservedQuantities = loadReservedQuantities(products);
        return products.stream()
                .map(product -> convertToDTO(product, reservedQuantities))
                .filter(product -> Boolean.TRUE.equals(product.getLowStock()))
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = {"products-list", "products-public", "products-low-stock", "dashboard-snapshots", "sales-reports"}, allEntries = true)
    public ProductDTO updateStock(Long productId, Integer quantity, String type, String reason) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        ProductEntity product = productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new NotFoundException(MSG_PRODUCTO_NO_ENCONTRADO));

        MovementType movementType;
        try {
            movementType = MovementType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Tipo de movimiento invalido");
        }
        int stockBefore = product.getStock();
        int stockAfter;

        switch (movementType) {
            case IN -> stockAfter = stockBefore + quantity;
            case OUT -> {
                if (stockBefore < quantity) {
                    throw new BadRequestException("Stock insuficiente");
                }
                stockAfter = stockBefore - quantity;
            }
            case ADJUST -> stockAfter = quantity;
            default -> throw new BadRequestException("Tipo de movimiento invalido");
        }

        product.setStock(stockAfter);
        if (stockAfter <= 0) {
            product.setStatus(ProductStatus.SOLD_OUT);
        } else if (product.getStatus() == ProductStatus.SOLD_OUT) {
            product.setStatus(ProductStatus.AVAILABLE);
        }

        product = productRepository.save(product);

        StockMovementEntity movement = new StockMovementEntity();
        movement.setTenantId(tenantId);
        movement.setProduct(product);
        movement.setType(movementType);
        movement.setQuantity(Math.abs(quantity));
        movement.setStockBefore(stockBefore);
        movement.setStockAfter(stockAfter);
        movement.setReason(reason != null ? reason : "Ajuste manual");
        movement.setUser(getCurrentUser());
        stockMovementRepository.save(movement);

        ProductDTO dto = convertToDTO(product, loadReservedQuantities(List.of(product)));
        log.info("Stock updated for product {} ({}): {} {} -> {}", product.getName(), product.getCode(), type,
                stockBefore, stockAfter);
        auditService.logAction(currentUserEmail(), "PRODUCT_STOCK_UPDATE", "Product", product.getId(),
                "type=" + type + " qty=" + quantity + " " + stockBefore + "->" + stockAfter
                        + (reason != null ? " reason=" + reason : ""));
        publishProductsRefresh();
        broadcastProductChange("stock", dto);
        return dto;
    }

    public Long currentTenantIdForCache() {
        return tenantContextResolver.resolveCurrent().getId();
    }

    private ProductDTO convertToDTO(ProductEntity entity, Map<Long, Integer> reservedQuantities) {
        int reservedStock = reservedQuantities.getOrDefault(entity.getId(), 0);
        int availableStock = Math.max(0, entity.getStock() - reservedStock);

        ProductDTO dto = ProductDTO.builder()
                .id(entity.getId())
                .categoryId(entity.getCategory().getId())
                .categoryName(entity.getCategory().getName())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .imageUrl(entity.getImageUrl())
                .unit(entity.getUnit())
                .price(entity.getPrice())
                .purchasePrice(entity.getPurchasePrice())
                .stock(entity.getStock())
                .reservedStock(reservedStock)
                .availableStock(availableStock)
                .minStock(entity.getMinStock())
                .status(entity.getStatus().name())
                .prepared(entity.getPrepared())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .lowStock(availableStock <= entity.getMinStock())
                .build();

        if (entity.getPurchasePrice() != null && entity.getPurchasePrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal profit = entity.getPrice().subtract(entity.getPurchasePrice());
            BigDecimal margin = profit.divide(entity.getPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            dto.setProfitMargin(margin);
        }

        return dto;
    }

    private ProductDTO convertToPublicDTO(ProductEntity entity, Map<Long, Integer> reservedQuantities) {
        int reservedStock = reservedQuantities.getOrDefault(entity.getId(), 0);
        int availableStock = Math.max(0, entity.getStock() - reservedStock);

        return ProductDTO.builder()
                .id(entity.getId())
                .categoryId(entity.getCategory().getId())
                .categoryName(entity.getCategory().getName())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .imageUrl(entity.getImageUrl())
                .unit(entity.getUnit())
                .price(entity.getPrice())
                .stock(availableStock)
                .reservedStock(reservedStock)
                .availableStock(availableStock)
                .status(entity.getStatus().name())
                .prepared(entity.getPrepared())
                .build();
    }

    private Map<Long, Integer> loadReservedQuantities(List<ProductEntity> products) {
        if (products == null || products.isEmpty()) {
            return Map.of();
        }

        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        List<Long> productIds = products.stream()
                .map(ProductEntity::getId)
                .toList();

        Map<Long, Integer> reserved = new HashMap<>();
        for (Object[] row : orderItemRepository.sumReservedQuantitiesByProductIdsAndTenantId(tenantId, productIds)) {
            Long productId = ((Number) row[0]).longValue();
            Integer quantity = row[1] != null ? ((Number) row[1]).intValue() : 0;
            reserved.put(productId, quantity);
        }
        return reserved;
    }

    private UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new NotFoundException("Usuario no autenticado");
        }
        String email = authentication.getName();
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private String currentUserEmail() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null ? authentication.getName() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private String slugify(String input) {
        if (input == null) {
            return "";
        }
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private void publishProductsRefresh() {
        try {
            realtimeGateway.products("PRODUCTS_CHANGED");
        } catch (Exception ignore) {
            log.warn("Unable to publish product refresh", ignore);
        }
    }

    private void broadcastProductChange(String type, ProductDTO payload) {
        var message = java.util.Map.of(
                "type", type,
                "product", payload);
        try {
            realtimeGateway.products(message);
            realtimeGateway.inventory(message);
        } catch (Exception ex) {
            log.warn("Unable to broadcast product change {}", type, ex);
        }
    }
}
