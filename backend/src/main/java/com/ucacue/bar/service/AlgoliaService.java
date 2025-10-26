package com.ucacue.bar.service;

import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlgoliaService {
    private final ProductRepository productRepository;

    public void indexProduct(ProductEntity product) {
        // No-op (fallback sin Algolia)
    }

    public void updateProduct(ProductEntity product) {
        // No-op (fallback sin Algolia)
    }

    public void deleteProduct(String productId) {
        // No-op (fallback sin Algolia)
    }

    public List<Map<String, Object>> searchProducts(String query) {
        try {
            var page = productRepository.searchProducts(query, PageRequest.of(0, 50));
            return page.getContent().stream().map(p -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", p.getId());
                m.put("name", p.getName());
                m.put("code", p.getCode());
                m.put("price", p.getPrice());
                m.put("stock", p.getStock());
                m.put("category", p.getCategory() != null ? p.getCategory().getName() : null);
                m.put("description", p.getDescription());
                return m;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("DB search error: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public void reindexAllProducts(List<ProductEntity> products) {
        // No-op (fallback sin Algolia)
    }
}
