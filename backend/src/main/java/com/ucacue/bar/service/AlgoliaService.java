package com.ucacue.bar.service;

import com.algolia.search.SearchIndex;
import com.ucacue.bar.dto.ProductDTO;
import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Algolia Search Service
 * Uses Write API Key on backend for indexing operations
 * Frontend uses Search API Key for read-only operations
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AlgoliaService {
    private final ProductRepository productRepository;

    @Autowired(required = false)
    private SearchIndex<ProductDTO> productIndex;

    private boolean isAlgoliaConfigured() {
        return productIndex != null;
    }

    /**
     * Index a single product in Algolia
     */
    public void indexProduct(ProductEntity product) {
        if (!isAlgoliaConfigured()) {
            log.debug("Algolia not configured, skipping indexing");
            return;
        }

        try {
            ProductDTO dto = mapProductToDTO(product);
            productIndex.saveObjectAsync(dto);
            log.info("Product indexed in Algolia: {}", product.getId());
        } catch (Exception e) {
            log.error("Failed to index product in Algolia: {}", e.getMessage());
        }
    }

    /**
     * Update a product in Algolia
     */
    public void updateProduct(ProductEntity product) {
        indexProduct(product); // Same as indexing with saveObjectAsync
    }

    /**
     * Delete a product from Algolia
     */
    public void deleteProduct(String productId) {
        if (!isAlgoliaConfigured()) {
            log.debug("Algolia not configured, skipping deletion");
            return;
        }

        try {
            productIndex.deleteObjectAsync(productId);
            log.info("Product deleted from Algolia: {}", productId);
        } catch (Exception e) {
            log.error("Failed to delete product from Algolia: {}", e.getMessage());
        }
    }

    /**
     * Search products - fallback to database if Algolia is not configured
     */
    public List<Map<String, Object>> searchProducts(String query) {
        try {
            var page = productRepository.searchProducts(query, PageRequest.of(0, 50));
            return page.getContent().stream()
                .map(this::mapProductToDTO)
                .map(dto -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("objectID", dto.getObjectID());
                    map.put("id", dto.getId());
                    map.put("name", dto.getName());
                    map.put("code", dto.getCode());
                    map.put("price", dto.getPrice());
                    map.put("stock", dto.getStock());
                    map.put("categoryName", dto.getCategoryName());
                    map.put("description", dto.getDescription());
                    return map;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("DB search error: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Reindex all products in Algolia
     */
    public void reindexAllProducts(List<ProductEntity> products) {
        if (!isAlgoliaConfigured()) {
            log.debug("Algolia not configured, skipping reindexing");
            return;
        }

        try {
            List<ProductDTO> dtos = products.stream()
                .map(this::mapProductToDTO)
                .collect(Collectors.toList());
            
            productIndex.saveObjectsAsync(dtos);
            log.info("Reindexed {} products in Algolia", products.size());
        } catch (Exception e) {
            log.error("Failed to reindex products in Algolia: {}", e.getMessage());
        }
    }

    // ============ PRIVATE HELPER METHODS ============

    private ProductDTO mapProductToDTO(ProductEntity product) {
        ProductDTO dto = new ProductDTO();
        dto.setObjectID(String.valueOf(product.getId())); // Algolia requires String objectID
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setCode(product.getCode());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());
        dto.setMinStock(product.getMinStock());
        dto.setDescription(product.getDescription());
        dto.setImageUrl(product.getImageUrl());
        
        if (product.getCategory() != null) {
            dto.setCategoryName(product.getCategory().getName());
            dto.setCategoryId(product.getCategory().getId());
        }
        
        if (product.getStatus() != null) {
            dto.setStatus(product.getStatus().name());
        }
        
        return dto;
    }
}
