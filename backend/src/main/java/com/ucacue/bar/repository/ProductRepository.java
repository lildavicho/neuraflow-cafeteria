package com.ucacue.bar.repository;

import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.entity.ProductEntity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    Optional<ProductEntity> findByCode(String code);
    
    boolean existsByCode(String code);
    
    Page<ProductEntity> findByStatus(ProductStatus status, Pageable pageable);
    
    Page<ProductEntity> findByCategoryId(Long categoryId, Pageable pageable);
    Page<ProductEntity> findByPrepared(Boolean prepared, Pageable pageable);
    Page<ProductEntity> findByCategoryIdAndPrepared(Long categoryId, Boolean prepared, Pageable pageable);
    
    @Query("SELECT p FROM ProductEntity p WHERE p.stock <= p.minStock AND p.status = 'AVAILABLE'")
    List<ProductEntity> findLowStockProducts();
    
    @Query("SELECT p FROM ProductEntity p WHERE " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ProductEntity> searchProducts(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM ProductEntity p WHERE " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:prepared IS NULL OR p.prepared = :prepared)")
    Page<ProductEntity> searchProductsWithPrepared(@Param("search") String search,
                                                   @Param("prepared") Boolean prepared,
                                                   Pageable pageable);
    
    @Query("SELECT p FROM ProductEntity p WHERE p.status = 'AVAILABLE' AND p.stock > 0")
    List<ProductEntity> findAvailableProducts();
}
