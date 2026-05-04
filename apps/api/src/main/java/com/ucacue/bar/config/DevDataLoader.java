package com.ucacue.bar.config;

import com.ucacue.bar.entity.CategoryEntity;
import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.repository.CategoryRepository;
import com.ucacue.bar.repository.ProductRepository;
import com.ucacue.bar.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Profile("dev")
@Slf4j
public class DevDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    public DevDataLoader(
        UserRepository userRepository,
        CategoryRepository categoryRepository,
        ProductRepository productRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedAdmin();
        seedSampleCatalog();
    }

    private void seedAdmin() {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.warn("ADMIN_EMAIL y ADMIN_PASSWORD no estan configurados; se omite el admin seed de dev");
            return;
        }

        final String email = adminEmail.trim();
        userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(
            user -> {
            },
            () -> {
                UserEntity admin = new UserEntity();
                admin.setName("Admin Local");
                admin.setEmail(email);
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                admin.setPhone("0000000000");
                admin.setIdentification("9999999999");
                admin.setRole(UserEntity.UserRole.ADMIN);
                admin.setActive(true);
                userRepository.save(admin);
            }
        );
    }

    private void seedSampleCatalog() {
        if (productRepository.count() > 0) {
            return;
        }

        CategoryEntity bebidas = ensureCategory("Bebidas");
        CategoryEntity desayunos = ensureCategory("Desayunos");
        CategoryEntity almuerzos = ensureCategory("Almuerzos");
        CategoryEntity snacks = ensureCategory("Snacks");
        CategoryEntity postres = ensureCategory("Postres");

        productRepository.saveAll(List.of(
            buildProduct("CAF-001", "Cafe americano", "Cafe negro recien preparado", bebidas, "1.50", 120, 10, true),
            buildProduct("JUG-001", "Jugo natural", "Jugo de frutas del dia", bebidas, "2.25", 60, 8, true),
            buildProduct("DES-001", "Tostada mixta", "Pan tostado con huevo y queso", desayunos, "3.75", 40, 6, true),
            buildProduct("ALM-001", "Almuerzo ejecutivo", "Menu del dia con sopa y plato fuerte", almuerzos, "5.50", 35, 5, true),
            buildProduct("SNA-001", "Empanada de queso", "Empanada horneada lista para servir", snacks, "1.25", 50, 8, true),
            buildProduct("POS-001", "Cheesecake", "Porcion individual de cheesecake", postres, "3.00", 20, 4, true)
        ));
    }

    private CategoryEntity ensureCategory(String name) {
        return categoryRepository.findByName(name).orElseGet(() -> {
            CategoryEntity category = new CategoryEntity();
            category.setName(name);
            category.setActive(true);
            return categoryRepository.save(category);
        });
    }

    private ProductEntity buildProduct(
        String code,
        String name,
        String description,
        CategoryEntity category,
        String price,
        int stock,
        int minStock,
        boolean prepared
    ) {
        ProductEntity product = new ProductEntity();
        product.setCode(code);
        product.setName(name);
        product.setDescription(description);
        product.setCategory(category);
        product.setPrice(new BigDecimal(price));
        product.setPurchasePrice(new BigDecimal(price).multiply(new BigDecimal("0.55")));
        product.setStock(stock);
        product.setMinStock(minStock);
        product.setPrepared(prepared);
        product.setStatus(ProductEntity.ProductStatus.AVAILABLE);
        return product;
    }
}
