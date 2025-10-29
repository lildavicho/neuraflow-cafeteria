package com.ucacue.bar.config;

import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL:admin@ucacue.edu.ec}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:Admin123!}")
    private String adminPassword;

    public DevDataLoader(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        final String email = adminEmail;
        userRepository.findByEmail(email).ifPresentOrElse(
            u -> {},
            () -> {
                UserEntity admin = new UserEntity();
                admin.setFullName("Admin Local");
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
}
