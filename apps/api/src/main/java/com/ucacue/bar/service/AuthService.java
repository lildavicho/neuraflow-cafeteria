package com.ucacue.bar.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.ucacue.bar.dto.auth.*;
import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.entity.UserEntity.UserRole;
import com.ucacue.bar.erp.commercial.application.TenantFeatureService;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantEntity;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.UnauthorizedException;
import com.ucacue.bar.repository.UserRepository;
import com.ucacue.bar.security.JwtUtil;
import com.ucacue.bar.security.TotpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TenantFeatureService tenantFeatureService;
    private final TotpService totpService;
    private final AuditService auditService;

    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;

    @Value("${firebase.auth-enabled:${FIREBASE_AUTH_ENABLED:false}}")
    private boolean firebaseAuthEnabled;

    @Value("${app.public-tenant-registration-enabled:${PUBLIC_TENANT_REGISTRATION_ENABLED:false}}")
    private boolean publicTenantRegistrationEnabled;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        return loginInternal(request, null, null);
    }

    @Transactional
    public AuthResponse loginWithContext(LoginRequest request, String ipAddress, String userAgent) {
        return loginInternal(request, ipAddress, userAgent);
    }

    private AuthResponse loginInternal(LoginRequest request, String ipAddress, String userAgent) {
        UserEntity user = userRepository.findByEmailIgnoreCase(request.getEmail())
            .orElseThrow(() -> {
                auditService.logLogin(null, request.getEmail(), "LOGIN", false, ipAddress, userAgent, "User not found");
                return new UnauthorizedException("Credenciales inválidas");
            });

        if (!Boolean.TRUE.equals(user.getActive())) {
            auditService.logLogin(user.getId(), request.getEmail(), "LOGIN", false, ipAddress, userAgent, "Account disabled");
            throw new UnauthorizedException("Usuario desactivado");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            auditService.logLogin(user.getId(), request.getEmail(), "LOGIN", false, ipAddress, userAgent, "Account locked");
            throw new UnauthorizedException("Cuenta bloqueada. Intente de nuevo después de "
                    + user.getLockedUntil().toString());
        }

        if (user.getPasswordHash() == null ||
            !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user, ipAddress, userAgent);
            throw new UnauthorizedException("Credenciales inválidas");
        }

        if (Boolean.TRUE.equals(user.getTotpEnabled()) && Boolean.TRUE.equals(user.getTotpVerified())) {
            TenantFeatureService.TenantPlanView commercialPlan = resolveCommercialPlan(user);
            AuthResponse pending = AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getName())
                .role(user.getRole().name())
                .requires2FA(true)
                .tenantCode(commercialPlan.tenantCode())
                .platformAdmin(Boolean.TRUE.equals(user.getPlatformAdmin()))
                .commercialPlan(commercialPlan.commercialPlan())
                .enabledModules(commercialPlan.enabledModules())
                .build();
            return pending;
        }

        return completeLogin(user, resolveCommercialPlan(user), ipAddress, userAgent);
    }

    @Transactional
    public AuthResponse verify2FA(TwoFactorRequest request, String ipAddress, String userAgent) {
        UserEntity user = userRepository.findByEmailIgnoreCase(request.getEmail())
            .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new UnauthorizedException("Usuario desactivado");
        }

        if (!Boolean.TRUE.equals(user.getTotpEnabled())) {
            throw new BadRequestException("2FA no está habilitado para este usuario");
        }

        if (!totpService.verifyCode(user.getTotpSecret(), request.getCode())) {
            auditService.logLogin(user.getId(), request.getEmail(), "2FA_VERIFY", false, ipAddress, userAgent, "Invalid TOTP code");
            throw new UnauthorizedException("Código 2FA inválido");
        }

        TenantFeatureService.TenantPlanView commercialPlan = resolveCommercialPlan(user);
        auditService.logLogin(user.getId(), request.getEmail(), "2FA_VERIFY", true, ipAddress, userAgent, null);
        return completeLogin(user, commercialPlan, ipAddress, userAgent);
    }

    @Transactional
    public Map<String, String> setup2FA() {
        UserEntity user = currentAuthenticatedUser();

        String secret = totpService.generateSecret();
        user.setTotpSecret(secret);
        user.setTotpEnabled(true);
        user.setTotpVerified(false);
        userRepository.save(user);

        String otpAuthUri = totpService.buildOtpAuthUri(secret, user.getEmail(), "UCACUE-Bar");

        return Map.of(
            "secret", secret,
            "otpAuthUri", otpAuthUri
        );
    }

    @Transactional
    public Map<String, String> confirm2FA(String code) {
        UserEntity user = currentAuthenticatedUser();

        if (!Boolean.TRUE.equals(user.getTotpEnabled()) || user.getTotpSecret() == null) {
            throw new BadRequestException("Primero configure 2FA");
        }

        if (!totpService.verifyCode(user.getTotpSecret(), code)) {
            throw new BadRequestException("Código inválido. Verifique su app autenticadora");
        }

        user.setTotpVerified(true);
        userRepository.save(user);

        return Map.of("message", "2FA habilitado exitosamente");
    }

    @Transactional
    public Map<String, String> disable2FA() {
        UserEntity user = currentAuthenticatedUser();

        user.setTotpEnabled(false);
        user.setTotpVerified(false);
        user.setTotpSecret(null);
        userRepository.save(user);

        return Map.of("message", "2FA deshabilitado");
    }

    @Transactional
    public AuthResponse firebaseAuth(FirebaseAuthRequest request) {
        if (!firebaseEnabled || !firebaseAuthEnabled || FirebaseApp.getApps().isEmpty()) {
            throw new BadRequestException("Firebase no esta habilitado en el backend");
        }
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(request.getIdToken());
            String uid = decodedToken.getUid();
            String email = firebaseEmail(decodedToken);
            String name = firebaseDisplayName(decodedToken, email);
            String picture = firebasePicture(decodedToken);

            if (email == null) throw new BadRequestException("El token de Firebase no contiene email");
            if (!decodedToken.isEmailVerified()) {
                throw new UnauthorizedException("El correo de Firebase no esta verificado");
            }

            UserEntity user = findFirebaseOrEmailUser(uid, email);
            if (user == null) {
                throw new UnauthorizedException("Acceso no autorizado. Solicita una demo o contacta a soporte.");
            }
            if (!Boolean.TRUE.equals(user.getActive())) {
                throw new UnauthorizedException("Usuario desactivado");
            }
            TenantEntity tenant = user.getTenant();
            if (tenant == null || !Boolean.TRUE.equals(tenant.getActive())) {
                throw new UnauthorizedException("El negocio asociado no esta activo");
            }
            if (request.getTenantCode() != null
                    && !request.getTenantCode().isBlank()
                    && !tenant.getTenantCode().equalsIgnoreCase(request.getTenantCode().trim())) {
                throw new UnauthorizedException("Tu correo no tiene acceso a este negocio");
            }
            updateAuthorizedFirebaseProfile(user, uid, name, picture);
            TenantFeatureService.TenantPlanView commercialPlan = resolveCommercialPlan(user);

            UserDetails userDetails = User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash() != null ? user.getPasswordHash() : "")
                .authorities(Collections.emptyList())
                .build();

            String token = jwtUtil.generateToken(userDetails, user.getRole().name(), user.getId());
            String refreshToken = jwtUtil.generateRefreshToken(userDetails, user.getRole().name(), user.getId());

            auditService.logLogin(user.getId(), user.getEmail(), "FIREBASE_LOGIN", true, null, null, null);

            return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getName())
                .role(user.getRole().name())
                .tenantCode(commercialPlan.tenantCode())
                .platformAdmin(Boolean.TRUE.equals(user.getPlatformAdmin()))
                .commercialPlan(commercialPlan.commercialPlan())
                .enabledModules(commercialPlan.enabledModules())
                .build();
        } catch (FirebaseAuthException e) {
            log.error("Firebase authentication error", e);
            throw new UnauthorizedException("Token de Firebase inválido");
        }
    }

    @Transactional
    public Map<String, String> resetPasswordWithSms(SmsPasswordResetRequest request) {
        FirebaseToken decodedToken = verifyFirebaseIdToken(request.getIdToken());
        String verifiedPhone = verifiedPhone(decodedToken);
        if (verifiedPhone == null || verifiedPhone.isBlank()) {
            throw new UnauthorizedException("El token de Firebase no contiene telefono verificado");
        }

        UserEntity user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new UnauthorizedException("Usuario desactivado");
        }
        String userPhone = normalizePhoneForComparison(user.getPhone());
        String tokenPhone = normalizePhoneForComparison(verifiedPhone);
        if (userPhone.isBlank() || !userPhone.equals(tokenPhone)) {
            throw new UnauthorizedException("El telefono verificado no coincide con la cuenta");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        auditService.logLogin(user.getId(), user.getEmail(), "PASSWORD_RESET_SMS", true, null, null, null);
        return Map.of("message", "Contrasena actualizada correctamente");
    }

    private AuthResponse completeLogin(UserEntity user,
                                        TenantFeatureService.TenantPlanView commercialPlan,
                                        String ipAddress, String userAgent) {
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);

        UserDetails userDetails = User.builder()
            .username(user.getEmail())
            .password(user.getPasswordHash())
            .authorities(Collections.emptyList())
            .build();

        String token = jwtUtil.generateToken(userDetails, user.getRole().name(), user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(userDetails, user.getRole().name(), user.getId());

        log.info("Usuario {} autenticado exitosamente", user.getEmail());
        auditService.logLogin(user.getId(), user.getEmail(), "LOGIN", true, ipAddress, userAgent, null);

        return AuthResponse.builder()
            .token(token)
            .refreshToken(refreshToken)
            .userId(user.getId())
            .email(user.getEmail())
            .fullName(user.getName())
            .role(user.getRole().name())
            .totpEnabled(user.getTotpEnabled())
            .tenantCode(commercialPlan.tenantCode())
            .platformAdmin(Boolean.TRUE.equals(user.getPlatformAdmin()))
            .commercialPlan(commercialPlan.commercialPlan())
            .enabledModules(commercialPlan.enabledModules())
            .build();
    }

    /**
     * Alta pública de negocio + usuario administrador.
     * Valida unicidad de email, crea tenant con plan START y retorna tokens.
     */
    @Transactional
    public AuthResponse registerTenant(RegisterTenantRequest request, String ipAddress, String userAgent) {
        if (!publicTenantRegistrationEnabled) {
            throw new UnauthorizedException("El registro publico al ERP esta deshabilitado. Solicita una demo o contacta a soporte.");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("El email es obligatorio");
        }
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(existing -> {
            throw new BadRequestException("Ya existe una cuenta con ese email");
        });

        TenantEntity tenant = tenantFeatureService.registerPublicTenant(
                request.getTenantCode(), request.getLegalName(), request.getTradeName());

        UserEntity admin = new UserEntity();
        admin.setTenant(tenant);
        admin.setEmail(normalizedEmail);
        admin.setName(request.getFullName().trim());
        admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        admin.setRole(UserRole.ADMIN);
        admin.setActive(Boolean.TRUE);
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            admin.setPhone(request.getPhone().trim());
        }
        admin = userRepository.save(admin);

        log.info("Registered tenant {} with admin {}", tenant.getTenantCode(), admin.getEmail());
        auditService.logLogin(admin.getId(), admin.getEmail(), "REGISTER", true, ipAddress, userAgent, null);

        return completeLogin(admin, resolveCommercialPlan(admin), ipAddress, userAgent);
    }

    private void updateAuthorizedFirebaseProfile(UserEntity user, String uid, String name, String picture) {
        boolean changed = false;
        if ((user.getFirebaseUid() == null || user.getFirebaseUid().isBlank()) && uid != null && !uid.isBlank()) {
            user.setFirebaseUid(uid);
            changed = true;
        }
        if ((user.getProvider() == null || user.getProvider().isBlank()) && uid != null && !uid.isBlank()) {
            user.setProvider("firebase");
            changed = true;
        }
        if ((user.getName() == null || user.getName().isBlank()) && name != null && !name.isBlank()) {
            user.setName(name);
            changed = true;
        }
        if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) && picture != null && !picture.isBlank()) {
            user.setAvatarUrl(picture);
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
        }
    }

    private void handleFailedLogin(UserEntity user, String ipAddress, String userAgent) {
        int attempts = (user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0) + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            log.warn("Account locked for user {} after {} failed attempts", user.getEmail(), attempts);
        }

        userRepository.save(user);
        auditService.logLogin(user.getId(), user.getEmail(), "LOGIN", false, ipAddress, userAgent,
                "Bad credentials (attempt " + attempts + ")");
    }

    private TenantFeatureService.TenantPlanView resolveCommercialPlan(UserEntity user) {
        return tenantFeatureService.resolvePlanForTenant(user != null ? user.getTenant() : null);
    }

    private FirebaseToken verifyFirebaseIdToken(String idToken) {
        if (!firebaseEnabled || !firebaseAuthEnabled || FirebaseApp.getApps().isEmpty()) {
            throw new BadRequestException("Firebase no esta habilitado en el backend");
        }
        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            log.error("Firebase token verification error", e);
            throw new UnauthorizedException("Token de Firebase invalido");
        }
    }

    private String verifiedPhone(FirebaseToken token) {
        Object phoneClaim = token.getClaims() != null ? token.getClaims().get("phone_number") : null;
        return phoneClaim instanceof String phone ? phone : "";
    }

    private String normalizePhoneForComparison(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }
        String digits = phone.trim().replaceAll("[^0-9]", "");
        if (digits.startsWith("00")) {
            digits = digits.substring(2);
        }
        if (digits.length() == 10 && digits.startsWith("0")) {
            return "593" + digits.substring(1);
        }
        return digits;
    }

    private UserEntity currentAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new UnauthorizedException("Usuario no autenticado");
        }
        return userRepository.findByEmailIgnoreCaseWithTenant(authentication.getName())
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
    }

    private UserEntity findFirebaseOrEmailUser(String uid, String email) {
        UserEntity byUid = userRepository.findByFirebaseUid(uid).orElse(null);
        if (byUid != null) {
            if (!Boolean.TRUE.equals(byUid.getActive())) {
                throw new UnauthorizedException("Usuario desactivado");
            }
            return byUid;
        }

        UserEntity byEmail = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (byEmail != null && !Boolean.TRUE.equals(byEmail.getActive())) {
            throw new UnauthorizedException("Usuario desactivado");
        }
        return byEmail;
    }

    private UserEntity createOrUpdateUser(String uid,
                                          String email,
                                          String name,
                                          String picture,
                                          TenantEntity tenant,
                                          boolean firstTenantUser) {
        return userRepository.findByFirebaseUid(uid)
            .map(existing -> {
                if (!Boolean.TRUE.equals(existing.getActive())) {
                    throw new UnauthorizedException("Usuario desactivado");
                }
                if (existing.getName() == null && name != null) existing.setName(name);
                if (picture != null) existing.setAvatarUrl(picture);
                existing.setProvider("firebase");
                if (existing.getTenant() == null) existing.setTenant(tenant);
                if (firstTenantUser && existing.getRole() != UserRole.ADMIN) existing.setRole(UserRole.ADMIN);
                if (existing.getPlatformAdmin() == null) existing.setPlatformAdmin(false);
                return userRepository.save(existing);
            })
            .orElseGet(() -> userRepository.findByEmailIgnoreCase(email)
                .map(existing -> {
                    if (!Boolean.TRUE.equals(existing.getActive())) {
                        throw new UnauthorizedException("Usuario desactivado");
                    }
                    existing.setFirebaseUid(uid);
                    if (existing.getName() == null && name != null) existing.setName(name);
                    if (picture != null) existing.setAvatarUrl(picture);
                    existing.setProvider("firebase");
                    if (existing.getTenant() == null) existing.setTenant(tenant);
                    if (firstTenantUser && existing.getRole() != UserRole.ADMIN) existing.setRole(UserRole.ADMIN);
                    if (existing.getPlatformAdmin() == null) existing.setPlatformAdmin(false);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    UserEntity u = new UserEntity();
                    u.setEmail(email);
                    u.setFirebaseUid(uid);
                    u.setName(name != null ? name : email.split("@")[0]);
                    u.setPasswordHash(passwordEncoder.encode(generateRandomPassword()));
                    u.setRole(firstTenantUser ? UserRole.ADMIN : UserRole.CUSTOMER);
                    u.setTenant(tenant);
                    u.setPlatformAdmin(false);
                    u.setActive(true);
                    u.setProvider("firebase");
                    if (picture != null) u.setAvatarUrl(picture);
                    return userRepository.save(u);
                }));
    }

    private String firebaseEmail(FirebaseToken token) {
        String email = token.getEmail();
        if (email == null || email.isBlank()) {
            email = firstStringClaim(token, "email", "preferred_username", "upn");
        }
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String firebaseDisplayName(FirebaseToken token, String email) {
        String name = token.getName();
        if (name == null || name.isBlank()) {
            name = firstStringClaim(token, "name", "displayName");
        }
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
    }

    private String firebasePicture(FirebaseToken token) {
        return firstStringClaim(token, "picture", "avatar", "photo_url");
    }

    private String firstStringClaim(FirebaseToken token, String... claimNames) {
        Map<String, Object> claims = token.getClaims();
        if (claims == null || claims.isEmpty()) {
            return null;
        }
        for (String claimName : claimNames) {
            Object value = claims.get(claimName);
            if (value instanceof String text && !text.isBlank()) {
                return text.trim();
            }
        }
        return null;
    }

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
