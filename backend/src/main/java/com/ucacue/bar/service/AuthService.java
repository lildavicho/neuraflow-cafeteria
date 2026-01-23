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
import com.ucacue.bar.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Collections;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    /**
     * Login con email y contraseña tradicional
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));
        
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new UnauthorizedException("Usuario desactivado");
        }
        
        if (user.getPasswordHash() == null || 
            !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Credenciales inválidas");
        }
        
        // Crear UserDetails para generar token
        UserDetails userDetails = User.builder()
            .username(user.getEmail())
            .password(user.getPasswordHash())
            .authorities(Collections.emptyList())
            .build();
        
        String token = jwtUtil.generateToken(userDetails, user.getRole().name(), user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(userDetails, user.getRole().name(), user.getId());
        
        log.info("Usuario {} autenticado exitosamente", user.getEmail());
        
        return AuthResponse.builder()
            .token(token)
            .refreshToken(refreshToken)
            .userId(user.getId())
            .email(user.getEmail())
            .fullName(user.getName())
            .role(user.getRole().name())
            .build();
    }
    
    
    // Legacy register/login removed in Firebase-only mode

    @Transactional
    public AuthResponse firebaseAuth(FirebaseAuthRequest request) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(request.getIdToken());
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();
            Object pictureObj = decodedToken.getClaims() != null ? decodedToken.getClaims().get("picture") : null;
            String picture = (pictureObj instanceof String pic) ? pic : null;

            if (email == null) throw new BadRequestException("El token de Firebase no contiene email");

            UserEntity user = createOrUpdateUser(uid, email, name, picture);
            boolean active = Boolean.TRUE.equals(user.getActive());
            if (!active) throw new UnauthorizedException("Usuario desactivado");

            UserDetails userDetails = User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash() != null ? user.getPasswordHash() : "")
                .authorities(Collections.emptyList())
                .build();

            String token = jwtUtil.generateToken(userDetails, user.getRole().name(), user.getId());
            String refreshToken = jwtUtil.generateRefreshToken(userDetails, user.getRole().name(), user.getId());

            return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getName())
                .role(user.getRole().name())
                .build();
        } catch (FirebaseAuthException e) {
            log.error("Firebase authentication error", e);
            throw new UnauthorizedException("Token de Firebase inválido");
        }
    }

    private UserEntity createOrUpdateUser(String uid, String email, String name, String picture) {
        return userRepository.findByFirebaseUid(uid)
            .orElseGet(() -> userRepository.findByEmail(email)
                .map(existing -> {
                    existing.setFirebaseUid(uid);
                    if (existing.getName() == null && name != null) existing.setName(name);
                    if (picture != null) existing.setAvatarUrl(picture);
                    existing.setProvider("firebase");
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    UserEntity u = new UserEntity();
                    u.setEmail(email);
                    u.setFirebaseUid(uid);
                    u.setName(name != null ? name : email.split("@")[0]);
                    u.setPasswordHash(passwordEncoder.encode(generateRandomPassword()));
                    u.setRole(UserRole.CUSTOMER);
                    u.setActive(true);
                    u.setProvider("firebase");
                    if (picture != null) u.setAvatarUrl(picture);
                    return userRepository.save(u);
                }));
    }

    // 2FA eliminado
    
    private static final SecureRandom RNG = new SecureRandom();
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            password.append(chars.charAt(RNG.nextInt(chars.length())));
        }
        return password.toString();
    }
}
