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
import com.ucacue.bar.security.CustomUserDetails;
import com.ucacue.bar.security.JwtUtil;
import com.ucacue.bar.util.EcuadorIdValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EcuadorIdValidator idValidator;
    private final EmailService emailService;
    
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validate identification
        if (!idValidator.isValid(request.getIdentification())) {
            throw new BadRequestException("Cédula ecuatoriana inválida");
        }
        
        // Check if user exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("El email ya está registrado");
        }
        
        if (userRepository.existsByIdentification(request.getIdentification())) {
            throw new BadRequestException("La cédula ya está registrada");
        }
        
        // Validate password strength
        if (!isPasswordStrong(request.getPassword())) {
            throw new BadRequestException("La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial");
        }
        
        // Create new user
        UserEntity user = new UserEntity();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setIdentification(request.getIdentification());
        user.setPhone(request.getPhone());
        user.setRole(UserRole.COMPRADOR); // Default role
        user.setActive(true);
        
        user = userRepository.save(user);
        
        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());
        
        // Generate tokens
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtUtil.generateToken(userDetails, user.getRole().name(), user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(userDetails, user.getRole().name(), user.getId());
        
        return AuthResponse.builder()
            .token(token)
            .refreshToken(refreshToken)
            .userId(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole().name())
            .build();
    }
    
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        
        UserEntity user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> {
                log.error("User not found: {}", request.getEmail());
                return new UnauthorizedException("Credenciales inválidas");
            });
        
        log.info("User found: {}, active: {}, hash length: {}", 
            user.getEmail(), user.getActive(), 
            user.getPasswordHash() != null ? user.getPasswordHash().length() : 0);
        
        if (!user.getActive()) {
            log.error("User is not active: {}", request.getEmail());
            throw new UnauthorizedException("Usuario desactivado");
        }
        
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        log.info("Password matches: {}", passwordMatches);
        
        if (!passwordMatches) {
            log.error("Password mismatch for user: {}", request.getEmail());
            throw new UnauthorizedException("Credenciales inválidas");
        }
        
        // Generate 2FA code if requested
        if (request.isRequire2FA()) {
            String code = generateTwoFactorCode();
            user.setTwoFactorCode(code);
            user.setTwoFactorExpiry(LocalDateTime.now().plusMinutes(5));
            userRepository.save(user);
            
            emailService.sendTwoFactorCode(user.getEmail(), code);
            
            return AuthResponse.builder()
                .requires2FA(true)
                .message("Código de verificación enviado al email")
                .build();
        }
        
        // Generate tokens
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtUtil.generateToken(userDetails, user.getRole().name(), user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(userDetails, user.getRole().name(), user.getId());
        
        return AuthResponse.builder()
            .token(token)
            .refreshToken(refreshToken)
            .userId(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole().name())
            .build();
    }
    
    @Transactional
    public AuthResponse verifyTwoFactor(TwoFactorRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
        
        if (user.getTwoFactorCode() == null || user.getTwoFactorExpiry() == null) {
            throw new UnauthorizedException("No hay código 2FA pendiente");
        }
        
        if (LocalDateTime.now().isAfter(user.getTwoFactorExpiry())) {
            throw new UnauthorizedException("El código ha expirado");
        }
        
        if (!user.getTwoFactorCode().equals(request.getCode())) {
            throw new UnauthorizedException("Código inválido");
        }
        
        // Clear 2FA code
        user.setTwoFactorCode(null);
        user.setTwoFactorExpiry(null);
        userRepository.save(user);
        
        // Generate tokens
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtUtil.generateToken(userDetails, user.getRole().name(), user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(userDetails, user.getRole().name(), user.getId());
        
        return AuthResponse.builder()
            .token(token)
            .refreshToken(refreshToken)
            .userId(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole().name())
            .build();
    }
    
    @Transactional
    public AuthResponse firebaseAuth(FirebaseAuthRequest request) {
        try {
            // Verify Firebase ID token
            FirebaseToken decodedToken = FirebaseAuth.getInstance()
                .verifyIdToken(request.getIdToken());
            
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();
            
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
                            return userRepository.save(newUser);
                        });
                });
            
            if (!user.getActive()) {
                throw new UnauthorizedException("Usuario desactivado");
            }
            
            // Generate tokens
            CustomUserDetails userDetails = new CustomUserDetails(user);
            String token = jwtUtil.generateToken(userDetails, user.getRole().name(), user.getId());
            String refreshToken = jwtUtil.generateRefreshToken(userDetails, user.getRole().name(), user.getId());
            
            return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
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
    
    @Transactional
    public void sendTwoFactorCode(String email) {
        UserEntity user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
        
        String code = generateTwoFactorCode();
        user.setTwoFactorCode(code);
        user.setTwoFactorExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);
        
        emailService.sendTwoFactorCode(email, code);
    }
    
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        try {
            String username = jwtUtil.extractUsername(request.getRefreshToken());
            UserEntity user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
            
            CustomUserDetails userDetails = new CustomUserDetails(user);
            
            if (!jwtUtil.validateToken(request.getRefreshToken(), userDetails)) {
                throw new UnauthorizedException("Refresh token inválido");
            }
            
            // Generate new tokens
            String token = jwtUtil.generateToken(userDetails, user.getRole().name(), user.getId());
            String refreshToken = jwtUtil.generateRefreshToken(userDetails, user.getRole().name(), user.getId());
            
            return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
            
        } catch (Exception e) {
            throw new UnauthorizedException("Refresh token inválido");
        }
    }
    
    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
    
    private String generateTwoFactorCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
    
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
