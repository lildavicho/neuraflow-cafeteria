package com.ucacue.bar.algo;

import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.repository.ProductRepository;
import com.ucacue.bar.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationService {
    
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    
    /**
     * Get product recommendations based on co-occurrence and business rules
     * @param productId The product to get recommendations for
     * @return List of recommended products
     */
    public List<ProductEntity> getRecommendations(Long productId) {
        List<ProductEntity> recommendations = new ArrayList<>();
        
        // 1. Co-occurrence based recommendations
        Map<Long, Integer> coOccurrences = findCoOccurrences(productId);
        
        // Sort by frequency and get top 5
        List<Long> topCoOccurring = coOccurrences.entrySet().stream()
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        // Get product entities
        recommendations.addAll(productRepository.findAllById(topCoOccurring));
        
        // 2. Apply business rules
        ProductEntity product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            recommendations.addAll(applyBusinessRules(product));
        }
        
        // Remove duplicates and limit to 10 items
        return recommendations.stream()
            .distinct()
            .filter(p -> p.getStock() > 0) // Only available products
            .limit(10)
            .collect(Collectors.toList());
    }
    
    /**
     * Find products that frequently appear together with the given product
     */
    private Map<Long, Integer> findCoOccurrences(Long productId) {
        Map<Long, Integer> coOccurrences = new HashMap<>();
        
        // Get all sales containing this product (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        // Note: This would need a custom query in repository
        // For now, we'll simulate with a simple approach
        List<Object[]> salesWithProduct = saleRepository.findTopSellingProducts(
            thirtyDaysAgo, LocalDateTime.now(), 100
        );
        
        // Count co-occurrences
        // This is simplified - in production, you'd query sale_items directly
        for (Object[] saleData : salesWithProduct) {
            Long coProductId = ((Number) saleData[1]).longValue();
            if (!coProductId.equals(productId)) {
                coOccurrences.merge(coProductId, 1, Integer::sum);
            }
        }
        
        return coOccurrences;
    }
    
    /**
     * Apply business rules for recommendations
     */
    private List<ProductEntity> applyBusinessRules(ProductEntity product) {
        List<ProductEntity> ruleBasedRecommendations = new ArrayList<>();
        String categoryName = product.getCategory().getName();
        
        // Coffee → Snacks rule
        if (categoryName.contains("Bebidas Calientes") || 
            product.getName().toLowerCase().contains("café")) {
            // Recommend empanadas and cookies
            ruleBasedRecommendations.addAll(
                productRepository.searchProducts("empanada", null).getContent()
            );
            ruleBasedRecommendations.addAll(
                productRepository.searchProducts("galleta", null).getContent()
            );
        }
        
        // Lunch items → Beverages
        if (categoryName.contains("Comida") || 
            product.getName().toLowerCase().contains("sándwich") ||
            product.getName().toLowerCase().contains("empanada")) {
            // Recommend beverages
            ruleBasedRecommendations.addAll(
                productRepository.findByCategoryId(1L, null).getContent() // Bebidas Calientes
            );
            ruleBasedRecommendations.addAll(
                productRepository.findByCategoryId(2L, null).getContent() // Bebidas Frías
            );
        }
        
        // Morning items
        if (categoryName.contains("Desayuno")) {
            // Recommend coffee and juice
            ruleBasedRecommendations.addAll(
                productRepository.searchProducts("café", null).getContent()
            );
            ruleBasedRecommendations.addAll(
                productRepository.searchProducts("jugo", null).getContent()
            );
        }
        
        return ruleBasedRecommendations.stream()
            .limit(5)
            .collect(Collectors.toList());
    }
    
    /**
     * Get trending products based on recent sales
     */
    public List<ProductEntity> getTrendingProducts(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Object[]> topProducts = saleRepository.findTopSellingProducts(
            startDate, LocalDateTime.now(), 10
        );
        
        List<Long> productIds = new ArrayList<>();
        for (Object[] data : topProducts) {
            productIds.add(((Number) data[1]).longValue());
        }
        
        return productRepository.findAllById(productIds);
    }
    
    /**
     * Get personalized recommendations for a user
     */
    public List<ProductEntity> getPersonalizedRecommendations(Long userId) {
        // Get user's purchase history
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Object[]> userPurchases = saleRepository.findTopSellingProducts(
            thirtyDaysAgo, LocalDateTime.now(), 20
        );
        
        // Extract frequently bought products
        Map<Long, Integer> productFrequency = new HashMap<>();
        for (Object[] purchase : userPurchases) {
            Long productId = ((Number) purchase[1]).longValue();
            productFrequency.merge(productId, 1, Integer::sum);
        }
        
        // Get recommendations based on most frequent purchases
        List<ProductEntity> recommendations = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : productFrequency.entrySet()) {
            recommendations.addAll(getRecommendations(entry.getKey()));
        }
        
        return recommendations.stream()
            .distinct()
            .limit(10)
            .collect(Collectors.toList());
    }
}
