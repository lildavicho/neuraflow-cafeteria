package com.ucacue.bar.service;

import com.ucacue.bar.dto.ProductDTO;
import com.ucacue.bar.entity.CategoryEntity;
import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.entity.ProductEntity.ProductStatus;
import com.ucacue.bar.entity.StockMovementEntity;
import com.ucacue.bar.entity.StockMovementEntity.MovementType;
import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.repository.CategoryRepository;
import com.ucacue.bar.repository.ProductRepository;
import com.ucacue.bar.repository.StockMovementRepository;
import com.ucacue.bar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private static final String MSG_PRODUCTO_NO_ENCONTRADO = "Producto no encontrado";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;
    private final RealtimeGateway realtimeGateway;

    /**
     * Retrieves a page of products based on the provided search criteria.
     *
     * @param search     the search query
     * @param categoryId the ID of the category to filter by
     * @param prepared   whether to filter by prepared products
     * @param pageable   the pagination information
     * @return a page of products
     */
    public Page<ProductDTO> getAllProducts(String search, Long categoryId, Boolean prepared, Pageable pageable) {
        Page<ProductEntity> products;

        if (search != null && !search.isEmpty()) {
            products = productRepository.searchProductsWithPrepared(search, prepared, pageable);
        } else if (categoryId != null && prepared != null) {
            products = productRepository.findByCategoryIdAndPrepared(categoryId, prepared, pageable);
        } else if (categoryId != null) {
            products = productRepository.findByCategoryId(categoryId, pageable);
        } else if (prepared != null) {
            products = productRepository.findByPrepared(prepared, pageable);
        } else {
            products = productRepository.findAll(pageable);
        }

        return products.map(this::convertToDTO);
    }

    /**
     * Retrieves a list of public products.
     *
     * @param pageable the pagination information
     * @return a list of public products
     */
    @Cacheable(cacheNames = "products-public")
    public List<ProductDTO> getPublicProducts(Pageable pageable) {
        // Ignoramos pageable para POS porque necesitamos todos los productos preparados
        // o implementamos paginación real si son muchos. Por ahora devolvemos lista
        // completa filtrada por DB.
        List<ProductEntity> products = productRepository.findAvailablePreparedProducts();
        return products.stream()
                .map(this::convertToPublicDTO)
                .toList();
    }

    /**
     * Retrieves a product by its ID.
     *
     * @param id the ID of the product
     * @return the product
     */
    public ProductDTO getProductById(Long id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(MSG_PRODUCTO_NO_ENCONTRADO));
        return convertToDTO(product);
    }

    /**
     * Creates a new product.
     *
     * @param productDTO the product data
     * @return the created product
     */
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames = "products-public", allEntries = true)
    public ProductDTO createProduct(ProductDTO productDTO) {
        // If client provided a code, ensure uniqueness
        if (productDTO.getCode() != null && !productDTO.getCode().isBlank()) {
            if (productRepository.existsByCode(productDTO.getCode())) {
                throw new BadRequestException("El código de producto ya existe");
            }
        }

        CategoryEntity category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));

        ProductEntity product = new ProductEntity();
        product.setCategory(category);
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setImageUrl(productDTO.getImageUrl());
        product.setUnit(
                productDTO.getUnit() != null && !productDTO.getUnit().isBlank() ? productDTO.getUnit() : "UNIDAD");
        product.setPrice(productDTO.getPrice());
        product.setPurchasePrice(productDTO.getPurchasePrice());
        product.setStock(productDTO.getStock() != null ? productDTO.getStock() : 0);
        product.setMinStock(productDTO.getMinStock() != null ? productDTO.getMinStock() : 5);
        product.setStatus(ProductStatus.AVAILABLE);
        boolean defaultPrepared = !"Ingredientes".equalsIgnoreCase(category.getName());
        product.setPrepared(productDTO.getPrepared() != null ? productDTO.getPrepared() : defaultPrepared);

        // Determine code: if provided use it, else generate
        if (productDTO.getCode() != null && !productDTO.getCode().isBlank()) {
            product.setCode(productDTO.getCode());
        } else {
            // provisional unique code to satisfy NOT NULL + UNIQUE constraints
            String provisional;
            do {
                provisional = "TMP-"
                        + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            } while (productRepository.existsByCode(provisional));
            product.setCode(provisional);
        }

        product = productRepository.save(product);

        // If code was provisional, compute final SKU as slug(name)+"-"+id and update
        if (product.getCode() != null && product.getCode().startsWith("TMP-")) {
            String slug = slugify(product.getName());
            String idStr = String.valueOf(product.getId());
            int maxLen = Math.max(1, 20 - 1 - idStr.length());
            if (slug.length() > maxLen)
                slug = slug.substring(0, maxLen);
            String finalCode = slug + "-" + idStr;
            // In rare case of collision, keep provisional
            if (!productRepository.existsByCode(finalCode)) {
                product.setCode(finalCode);
                product = productRepository.save(product);
            }
        }

        ProductDTO dto = convertToDTO(product);
        log.info("Product created: {} ({})", product.getName(), product.getCode());
        // Broadcast generic change for storefront refresh
        try {
            realtimeGateway.products("PRODUCTS_CHANGED");
        } catch (Exception ignore) {
        }
        broadcastProductChange("created", dto);
        return dto;
    }

    /**
     * Updates a product.
     *
     * @param id         the ID of the product
     * @param productDTO the updated product data
     * @return the updated product
     */
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames = "products-public", allEntries = true)
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(MSG_PRODUCTO_NO_ENCONTRADO));

        // Check if code is being changed and already exists
        if (productDTO.getCode() != null && !productDTO.getCode().isBlank() &&
                !product.getCode().equals(productDTO.getCode()) &&
                productRepository.existsByCode(productDTO.getCode())) {
            throw new BadRequestException("El código de producto ya existe");
        }

        if (productDTO.getCategoryId() != null) {
            CategoryEntity category = categoryRepository.findById(productDTO.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));
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

        // Update status based on stock
        if (product.getStock() <= 0) {
            product.setStatus(ProductStatus.SOLD_OUT);
        } else if (productDTO.getStatus() != null) {
            product.setStatus(ProductStatus.valueOf(productDTO.getStatus()));
        }

        product = productRepository.save(product);

        ProductDTO dto = convertToDTO(product);
        log.info("Product updated: {} ({})", product.getName(), product.getCode());
        try {
            realtimeGateway.products("PRODUCTS_CHANGED");
        } catch (Exception ignore) {
        }
        broadcastProductChange("updated", dto);
        return dto;
    }

    /**
     * Deletes a product.
     *
     * @param id the ID of the product
     */
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames = "products-public", allEntries = true)
    public void deleteProduct(Long id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(MSG_PRODUCTO_NO_ENCONTRADO));

        product.setStatus(ProductStatus.DISCONTINUED);
        productRepository.save(product);

        log.info("Product discontinued: {} ({})", product.getName(), product.getCode());
        broadcastProductChange("deleted",
                ProductDTO.builder().id(product.getId()).status(product.getStatus().name()).build());
    }

    /**
     * Retrieves a list of products with low stock.
     *
     * @return a list of products with low stock
     */
    public List<ProductDTO> getLowStockProducts() {
        List<ProductEntity> products = productRepository.findLowStockProducts();

        // Email deshabilitado en esta versión

        return products.stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * Updates the stock of a product.
     *
     * @param productId the ID of the product
     * @param quantity  the quantity to add or remove
     * @param type      the type of stock movement (IN, OUT, ADJUST)
     * @param reason    the reason for the stock movement
     * @return the updated product
     */
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames = "products-public", allEntries = true)
    public ProductDTO updateStock(Long productId, Integer quantity, String type, String reason) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException(MSG_PRODUCTO_NO_ENCONTRADO));

        MovementType movementType = MovementType.valueOf(type.toUpperCase());
        int stockBefore = product.getStock();
        int stockAfter;

        switch (movementType) {
            case IN:
                stockAfter = stockBefore + quantity;
                break;
            case OUT:
                if (stockBefore < quantity) {
                    throw new BadRequestException("Stock insuficiente");
                }
                stockAfter = stockBefore - quantity;
                break;
            case ADJUST:
                stockAfter = quantity;
                break;
            default:
                throw new BadRequestException("Tipo de movimiento inválido");
        }

        // Update product stock
        product.setStock(stockAfter);

        // Update status based on stock
        if (stockAfter <= 0) {
            product.setStatus(ProductStatus.SOLD_OUT);
        } else if (product.getStatus() == ProductStatus.SOLD_OUT) {
            product.setStatus(ProductStatus.AVAILABLE);
        }

        product = productRepository.save(product);

        // Record stock movement
        StockMovementEntity movement = new StockMovementEntity();
        movement.setProduct(product);
        movement.setType(movementType);
        movement.setQuantity(Math.abs(quantity));
        movement.setStockBefore(stockBefore);
        movement.setStockAfter(stockAfter);
        movement.setReason(reason != null ? reason : "Ajuste manual");
        movement.setUser(getCurrentUser());

        stockMovementRepository.save(movement);

        ProductDTO dto = convertToDTO(product);
        log.info("Stock updated for product {} ({}): {} {} -> {}",
                product.getName(), product.getCode(), type, stockBefore, stockAfter);
        broadcastProductChange("stock", dto);

        // Email deshabilitado en esta versión

        return dto;
    }

    /**
     * Converts a product entity to a DTO.
     *
     * @param entity the product entity
     * @return the product DTO
     */
    private ProductDTO convertToDTO(ProductEntity entity) {
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
                .minStock(entity.getMinStock())
                .status(entity.getStatus().name())
                .prepared(entity.getPrepared())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .lowStock(entity.getStock() <= entity.getMinStock())
                .build();

        // Calculate profit margin if purchase price is available
        if (entity.getPurchasePrice() != null && entity.getPurchasePrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal profit = entity.getPrice().subtract(entity.getPurchasePrice());
            BigDecimal margin = profit.divide(entity.getPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            dto.setProfitMargin(margin);
        }

        return dto;
    }

    private ProductDTO convertToPublicDTO(ProductEntity entity) {
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
                .stock(entity.getStock())
                .status(entity.getStatus().name())
                .build();
    }

    private UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new NotFoundException("Usuario no autenticado");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private String slugify(String input) {
        if (input == null)
            return "";
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = normalized.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
        return slug;
    }

    private void broadcastProductChange(String type, ProductDTO payload) {
        var message = java.util.Map.of(
                "type", type,
                "product", payload);
        realtimeGateway.products(message);
        realtimeGateway.inventory(message);
    }
}
