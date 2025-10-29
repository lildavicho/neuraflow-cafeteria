package com.ucacue.bar.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.ucacue.bar.dto.auth.*;
import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.entity.UserEntity.UserRole;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.UnauthorizedException;
import com.ucacue.bar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    
    // Legacy register/login removed in Firebase-only mode

    @Transactional
    public AuthResponse firebaseAuth(FirebaseAuthRequest request) {
        try {
            // Verify Firebase ID token
            FirebaseToken decodedToken = FirebaseAuth.getInstance()
                .verifyIdToken(request.getIdToken());
            
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();
            final String picture = (decodedToken.getClaims() != null && decodedToken.getClaims().get("picture") instanceof String)
                ? (String) decodedToken.getClaims().get("picture")
                : null;
            
            if (email == null) {
                throw new BadRequestException("El token de Firebase no contiene email");
            }
            
            // Find or create user
            UserEntity user = userRepository.findByFirebaseUid(uid)
                .orElseGet(() -> {
                    // Check if user exists with email
                    return userRepository.findByEmail(email)
                        .map(existingUser -> {
                            existingUser.setFirebaseUid(uid);
                            if (existingUser.getFullName() == null && name != null) existingUser.setFullName(name);
                            if (picture != null) existingUser.setAvatarUrl(picture);
                            existingUser.setProvider("firebase");
                            return userRepository.save(existingUser);
                        })
                        .orElseGet(() -> {
                            // Create new user
                            UserEntity newUser = new UserEntity();
                            newUser.setEmail(email);
                            newUser.setFirebaseUid(uid);
                            newUser.setFullName(name != null ? name : email.split("@")[0]);
                            newUser.setPasswordHash(passwordEncoder.encode(generateRandomPassword()));
                            newUser.setRole(UserRole.COMPRADOR); // Default role for Firebase users
                            newUser.setActive(true);
                            newUser.setProvider("firebase");
                            if (picture != null) newUser.setAvatarUrl(picture);
                            return userRepository.save(newUser);
                        });
                });
            
            if (!user.getActive()) {
                throw new UnauthorizedException("Usuario desactivado");
            }
            
            return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
            
        } catch (FirebaseAuthException e) {
            log.error("Firebase authentication error", e);
            throw new UnauthorizedException("Token de Firebase inválido");
        }
    }
    
    
    
    // 2FA eliminado
    
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        Random random = new Random();
        StringBuilder password = new StringBuilder();
        
        for (int i = 0; i < 16; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }
}
