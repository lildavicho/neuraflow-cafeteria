package com.ucacue.bar.service;

import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${algolia.application-id:}")
    private String applicationId;

    @Value("${algolia.api-key:}")
    private String apiKey; // Write API Key - NEVER expose to frontend

    @Value("${algolia.index-name:products}")
    private String indexName;

    private boolean isAlgoliaConfigured() {
        return applicationId != null && !applicationId.isEmpty() 
            && apiKey != null && !apiKey.isEmpty();
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
            String url = String.format("https://%s-dsn.algolia.net/1/indexes/%s/%s",
                applicationId, indexName, product.getId());

            HttpHeaders headers = createHeaders();
            Map<String, Object> productData = mapProductToAlgolia(product);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(productData, headers);
            restTemplate.exchange(url, HttpMethod.PUT, request, String.class);

            log.info("Product indexed in Algolia: {}", product.getId());
        } catch (Exception e) {
            log.error("Failed to index product in Algolia: {}", e.getMessage());
        }
    }

    /**
     * Update a product in Algolia
     */
    public void updateProduct(ProductEntity product) {
        indexProduct(product); // Same as indexing
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
            String url = String.format("https://%s-dsn.algolia.net/1/indexes/%s/%s",
                applicationId, indexName, productId);

            HttpHeaders headers = createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);

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
                .map(this::mapProductToAlgolia)
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
            String url = String.format("https://%s-dsn.algolia.net/1/indexes/%s/batch",
                applicationId, indexName);

            HttpHeaders headers = createHeaders();
            
            List<Map<String, Object>> requests = products.stream()
                .map(product -> {
                    Map<String, Object> request = new HashMap<>();
                    request.put("action", "addObject");
                    request.put("body", mapProductToAlgolia(product));
                    return request;
                })
                .collect(Collectors.toList());

            Map<String, Object> batchRequest = new HashMap<>();
            batchRequest.put("requests", requests);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(batchRequest, headers);
            restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            log.info("Reindexed {} products in Algolia", products.size());
        } catch (Exception e) {
            log.error("Failed to reindex products in Algolia: {}", e.getMessage());
        }
    }

    /**
     * Get Algolia configuration for frontend (Search API Key only)
     */
    public Map<String, String> getSearchConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("applicationId", applicationId);
        config.put("indexName", indexName);
        // Note: Search API Key should be provided separately via environment variable
        // and exposed through a dedicated endpoint
        return config;
    }

    // ============ PRIVATE HELPER METHODS ============

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Algolia-Application-Id", applicationId);
        headers.set("X-Algolia-API-Key", apiKey); // Write API Key
        return headers;
    }

    private Map<String, Object> mapProductToAlgolia(ProductEntity product) {
        Map<String, Object> map = new HashMap<>();
        map.put("objectID", product.getId());
        map.put("id", product.getId());
        map.put("name", product.getName());
        map.put("code", product.getCode());
        map.put("price", product.getPrice());
        map.put("stock", product.getStock());
        map.put("minStock", product.getMinStock());
        map.put("category", product.getCategory() != null ? product.getCategory().getName() : null);
        map.put("categoryId", product.getCategory() != null ? product.getCategory().getId() : null);
        map.put("description", product.getDescription());
        map.put("status", product.getStatus() != null ? product.getStatus().name() : null);
        map.put("imageUrl", product.getImageUrl());
        return map;
    }
}
