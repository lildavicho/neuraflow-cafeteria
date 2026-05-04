package com.ucacue.bar.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.FirebaseAuthException;
import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.RequiredArgsConstructor;
import com.google.firebase.FirebaseApp;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class FirebaseTokenFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final CacheManager cacheManager;
    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;
    @Value("${firebase.auth-enabled:${FIREBASE_AUTH_ENABLED:false}}")
    private boolean firebaseAuthEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // Skip if already authenticated (e.g., with local JWT)
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        if (!firebaseEnabled || !firebaseAuthEnabled || FirebaseApp.getApps().isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        String token = extractBearerToken(request);
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String email = resolveEmailFromToken(token);
            if (email == null) {
                chain.doFilter(request, response);
                return;
            }

            Optional<UserEntity> userOpt = userRepository.findByEmailIgnoreCaseWithTenant(email);
            if (userOpt.isEmpty()
                    || !Boolean.TRUE.equals(userOpt.get().getActive())
                    || userOpt.get().getTenant() == null
                    || !Boolean.TRUE.equals(userOpt.get().getTenant().getActive())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            authenticate(email, userOpt.get().getRole().name(), request);
        } catch (FirebaseAuthException ex) {
            log.debug("[Auth] Firebase token not applicable: {}", ex.getMessage());
        }

        chain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        return header.substring(7);
    }

    private String resolveEmailFromToken(String token) throws FirebaseAuthException {
        FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token);
        String email = decoded.getEmail();
        if (email == null || email.isBlank()) {
            email = firstStringClaim(decoded, "email", "preferred_username", "upn");
        }
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
        Cache cache = cacheManager.getCache("firebase-tokens");
        if (cache != null && email != null) cache.put(token, email);
        return email;
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

    private void authenticate(String email, String role, HttpServletRequest request) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(email, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
