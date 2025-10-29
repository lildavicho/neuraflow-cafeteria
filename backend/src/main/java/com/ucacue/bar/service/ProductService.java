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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;
    private final AlgoliaService algoliaService;
    
    public Page<ProductDTO> getAllProducts(String search, Long categoryId, Pageable pageable) {
        Page<ProductEntity> products;
        
        if (search != null && !search.isEmpty()) {
            products = productRepository.searchProducts(search, pageable);
        } else if (categoryId != null) {
            products = productRepository.findByCategoryId(categoryId, pageable);
        } else {
            products = productRepository.findAll(pageable);
        }
        
        return products.map(this::convertToDTO);
    }
    
    public List<ProductDTO> getPublicProducts() {
        List<ProductEntity> products = productRepository.findAvailableProducts();
        return products.stream()
            .map(this::convertToPublicDTO)
            .collect(Collectors.toList());
    }
    
    public ProductDTO getProductById(Long id) {
        ProductEntity product = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        return convertToDTO(product);
    }
    
    @Transactional
    public ProductDTO createProduct(ProductDTO productDTO) {
        // Check if code already exists
        if (productRepository.existsByCode(productDTO.getCode())) {
            throw new BadRequestException("El código de producto ya existe");
        }
        
        CategoryEntity category = categoryRepository.findById(productDTO.getCategoryId())
            .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));
        
        ProductEntity product = new ProductEntity();
        product.setCategory(category);
        product.setCode(productDTO.getCode());
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setImageUrl(productDTO.getImageUrl());
        product.setUnit(productDTO.getUnit());
        product.setPrice(productDTO.getPrice());
        product.setPurchasePrice(productDTO.getPurchasePrice());
        product.setStock(productDTO.getStock());
        product.setMinStock(productDTO.getMinStock());
        product.setStatus(ProductStatus.AVAILABLE);
        
        product = productRepository.save(product);
        
        // Index in Algolia
        algoliaService.indexProduct(product);
        
        log.info("Product created: {} ({})", product.getName(), product.getCode());
        
        return convertToDTO(product);
    }
    
    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        ProductEntity product = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        
        // Check if code is being changed and already exists
        if (!product.getCode().equals(productDTO.getCode()) && 
            productRepository.existsByCode(productDTO.getCode())) {
            throw new BadRequestException("El código de producto ya existe");
        }
        
        if (productDTO.getCategoryId() != null) {
            CategoryEntity category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));
            product.setCategory(category);
        }
        
        product.setCode(productDTO.getCode());
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setImageUrl(productDTO.getImageUrl());
        product.setUnit(productDTO.getUnit());
        product.setPrice(productDTO.getPrice());
        product.setPurchasePrice(productDTO.getPurchasePrice());
        product.setMinStock(productDTO.getMinStock());
        
        // Update status based on stock
        if (product.getStock() <= 0) {
            product.setStatus(ProductStatus.SOLD_OUT);
        } else if (productDTO.getStatus() != null) {
            product.setStatus(ProductStatus.valueOf(productDTO.getStatus()));
        }
        
        product = productRepository.save(product);
        
        // Update in Algolia
        algoliaService.updateProduct(product);
        
        log.info("Product updated: {} ({})", product.getName(), product.getCode());
        
        return convertToDTO(product);
    }
    
    @Transactional
    public void deleteProduct(Long id) {
        ProductEntity product = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        
        product.setStatus(ProductStatus.DISCONTINUED);
        productRepository.save(product);
        
        // Remove from Algolia
        algoliaService.deleteProduct(product.getId().toString());
        
        log.info("Product discontinued: {} ({})", product.getName(), product.getCode());
    }
    
    public List<ProductDTO> getLowStockProducts() {
        List<ProductEntity> products = productRepository.findLowStockProducts();
        
        // Email deshabilitado en esta versión
        
        return products.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public ProductDTO updateStock(Long productId, Integer quantity, String type, String reason) {
        ProductEntity product = productRepository.findById(productId)
            .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        
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
        
        log.info("Stock updated for product {} ({}): {} {} -> {}", 
            product.getName(), product.getCode(), type, stockBefore, stockAfter);
        
        // Email deshabilitado en esta versión
        
        return convertToDTO(product);
    }
    
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
            .created(entity.getCreated())
            .updated(entity.getUpdated())
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
        String email = authentication.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }
}
