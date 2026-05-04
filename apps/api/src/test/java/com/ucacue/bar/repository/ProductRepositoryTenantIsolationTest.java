package com.ucacue.bar.repository;

import com.ucacue.bar.entity.CategoryEntity;
import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.entity.ProductEntity.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garantiza que las queries scoped por tenantId no retornan datos de otros tenants.
 * Si este test falla, hay un leak de aislamiento multi-tenant.
 */
@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTenantIsolationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void findByTenantId_returnsOnlyProductsOfThatTenant() {
        Long tenantA = 100L;
        Long tenantB = 200L;
        CategoryEntity catA = categoryFor(tenantA);
        CategoryEntity catB = categoryFor(tenantB);

        productRepository.save(buildProduct(tenantA, catA, "A-1", "Producto A1"));
        productRepository.save(buildProduct(tenantA, catA, "A-2", "Producto A2"));
        productRepository.save(buildProduct(tenantB, catB, "B-1", "Producto B1"));

        Page<ProductEntity> aPage = productRepository.findByTenantId(tenantA, PageRequest.of(0, 10));
        Page<ProductEntity> bPage = productRepository.findByTenantId(tenantB, PageRequest.of(0, 10));

        assertThat(aPage.getTotalElements()).isEqualTo(2);
        assertThat(aPage.getContent()).allMatch(p -> p.getTenantId().equals(tenantA));
        assertThat(bPage.getTotalElements()).isEqualTo(1);
        assertThat(bPage.getContent()).allMatch(p -> p.getTenantId().equals(tenantB));
    }

    @Test
    void findByTenantIdAndCode_doesNotLeakAcrossTenants() {
        Long tenantA = 300L;
        Long tenantB = 400L;
        CategoryEntity catA = categoryFor(tenantA);
        CategoryEntity catB = categoryFor(tenantB);

        productRepository.save(buildProduct(tenantA, catA, "SHARED", "Producto de A"));
        productRepository.save(buildProduct(tenantB, catB, "SHARED", "Producto de B"));

        Optional<ProductEntity> forA = productRepository.findByTenantIdAndCode(tenantA, "SHARED");
        Optional<ProductEntity> forB = productRepository.findByTenantIdAndCode(tenantB, "SHARED");

        assertThat(forA).isPresent();
        assertThat(forA.get().getName()).isEqualTo("Producto de A");
        assertThat(forB).isPresent();
        assertThat(forB.get().getName()).isEqualTo("Producto de B");
        assertThat(forA.get().getId()).isNotEqualTo(forB.get().getId());
    }

    @Test
    void findByTenantIdAndId_rejectsCrossTenantReads() {
        Long tenantA = 500L;
        Long tenantB = 600L;
        CategoryEntity catA = categoryFor(tenantA);

        ProductEntity saved = productRepository.save(buildProduct(tenantA, catA, "X-1", "Solo de A"));

        Optional<ProductEntity> asA = productRepository.findByTenantIdAndId(tenantA, saved.getId());
        Optional<ProductEntity> asB = productRepository.findByTenantIdAndId(tenantB, saved.getId());

        assertThat(asA).isPresent();
        assertThat(asB).isEmpty();
    }

    private CategoryEntity categoryFor(Long tenantId) {
        CategoryEntity c = new CategoryEntity();
        c.setTenantId(tenantId);
        c.setName("Cat-" + tenantId + "-" + System.nanoTime());
        c.setActive(true);
        return categoryRepository.save(c);
    }

    private ProductEntity buildProduct(Long tenantId, CategoryEntity category, String code, String name) {
        ProductEntity p = new ProductEntity();
        p.setTenantId(tenantId);
        p.setCategory(category);
        p.setCode(code);
        p.setName(name);
        p.setPrice(BigDecimal.valueOf(9.99));
        p.setStock(10);
        p.setMinStock(1);
        p.setMaxStock(100);
        p.setReorderPoint(2);
        p.setStatus(ProductStatus.ACTIVE);
        p.setPrepared(Boolean.FALSE);
        return p;
    }
}
