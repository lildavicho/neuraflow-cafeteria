package com.ucacue.bar.algo;

import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.entity.ProductEntity.ProductStatus;
import com.ucacue.bar.repository.OrderItemRepository;
import com.ucacue.bar.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationService {

    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public List<ProductEntity> getRecommendations(Long productId) {
        log.debug("RecommendationService.getRecommendations called for {}", productId);
        List<Object[]> rows = orderItemRepository.findCoPurchasedProducts(productId, PageRequest.of(0, 6));
        if (rows.isEmpty()) {
            return getTrendingProducts(30);
        }
        return resolveProducts(rows);
    }

    public List<ProductEntity> getTrendingProducts(int days) {
        log.debug("RecommendationService.getTrendingProducts called for {} days", days);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusDays(Math.max(1, days));
        List<Object[]> rows = orderItemRepository.findTopProducts(from, now, PageRequest.of(0, 6));
        return resolveProducts(rows);
    }

    public List<ProductEntity> getPersonalizedRecommendations(Long userId) {
        log.debug("RecommendationService.getPersonalizedRecommendations called for {}", userId);
        List<Object[]> rows = orderItemRepository.findTopProductsForUser(userId, PageRequest.of(0, 6));
        if (rows.isEmpty()) {
            return getTrendingProducts(14);
        }
        return resolveProducts(rows);
    }

    private List<ProductEntity> resolveProducts(List<Object[]> rows) {
        List<Long> ids = rows.stream()
            .map(row -> ((Number) row[0]).longValue())
            .toList();

        Map<Long, ProductEntity> lookup = productRepository.findAllById(ids).stream()
            .filter(product -> product.getStatus() != ProductStatus.DISCONTINUED)
            .collect(Collectors.toMap(ProductEntity::getId, p -> p, (a, b) -> a, HashMap::new));

        List<ProductEntity> ordered = new ArrayList<>();
        for (Long id : ids) {
            ProductEntity product = lookup.get(id);
            if (product != null) {
                ordered.add(product);
            }
        }
        return ordered;
    }
}
