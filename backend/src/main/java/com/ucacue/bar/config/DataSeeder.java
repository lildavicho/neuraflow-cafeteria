package com.ucacue.bar.config;

import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Data Seeder - Creates default users on application startup
 * ADMIN: admin@ucacue.edu.ec / Admin123!
 * COMPRADOR: comprador@ucacue.edu.ec / Comprador123!
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedDefaultUsers() {
        return args -> {
            log.info("🌱 Starting data seeding...");
            
            // Seed ADMIN user
            seedUser(
                "admin@ucacue.edu.ec",
                "Admin123!",
                "Administrador UCACUE",
                UserEntity.UserRole.ADMIN,
                "0000000000"
            );
            
            // Seed COMPRADOR user
            seedUser(
                "comprador@ucacue.edu.ec",
                "Comprador123!",
                "Comprador Demo",
                UserEntity.UserRole.COMPRADOR,
                "0000000001"
            );
            
            log.info("✅ Data seeding completed successfully");
        };
    }

    private void seedUser(String email, String password, String fullName, 
                         UserEntity.UserRole role, String identification) {
        
        // Check if user already exists
        if (userRepository.findByEmail(email).isPresent()) {
            log.info("ℹ️  User {} already exists, skipping...", email);
            return;
        }

        try {
            UserEntity user = new UserEntity();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setFullName(fullName);
            user.setRole(role);
            user.setIdentification(identification);
            user.setActive(true);
            user.setProvider("email");
            
            userRepository.save(user);
            log.info("✅ Created {} user: {} (password: {})", role, email, password);
            
        } catch (Exception e) {
            log.error("❌ Failed to create user {}: {}", email, e.getMessage());
        }
    }
}
