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
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class FirebaseTokenFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final CacheManager cacheManager;
    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // Skip if already authenticated (e.g., with local JWT)
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        if (!firebaseEnabled || FirebaseApp.getApps().isEmpty()) {
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
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            Optional<UserEntity> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty() || !Boolean.TRUE.equals(userOpt.get().getActive())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            authenticate(email, userOpt.get().getRole().name(), request);
            chain.doFilter(request, response);
        } catch (FirebaseAuthException ex) {
            log.warn("[Auth] Invalid Firebase token: {}", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        return header.substring(7);
    }

    private String resolveEmailFromToken(String token) throws FirebaseAuthException {
        Cache cache = cacheManager.getCache("firebase-tokens");
        String email = (cache != null) ? cache.get(token, String.class) : null;
        if (email != null) return email;
        FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token);
        email = decoded.getEmail();
        if (cache != null && email != null) cache.put(token, email);
        return email;
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
