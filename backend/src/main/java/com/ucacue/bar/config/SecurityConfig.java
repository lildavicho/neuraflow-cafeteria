package com.ucacue.bar.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private com.ucacue.bar.security.FirebaseTokenFilter firebaseTokenFilter;
    @Autowired
    private com.ucacue.bar.security.RateLimitingFilter rateLimitingFilter;

    @Value("${app.allowed-origins:http://localhost:5173}")
    private String allowedOriginsCsv;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        List<String> allowedOrigins = parseAllowedOrigins();

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // --- PUBLIC ROUTES ---
                .requestMatchers("/", "/index.html", "/error", "/favicon.ico").permitAll()
                .requestMatchers(HttpMethod.GET, "/pages/**", "/css/**", "/js/**", "/images/**", "/assets/**").permitAll()
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/health", "/health").permitAll()

                .requestMatchers(HttpMethod.GET, "/products/public").permitAll()

                // --- ADMIN ONLY ---
                .requestMatchers("/users/**", "/inventory/**", "/reports/**", "/camera/**",
                                 "/settings/**", "/dashboard/metrics/sse", "/analytics/**", "/people-counts/**")
                .hasRole("ADMIN")

                // --- WebSocket ---
                .requestMatchers("/ws/**", "/api/ws/**").permitAll()
                
                // --- ADMIN + BUYER ---
                .requestMatchers("/orders/**", "/products/**", "/categories/**", "/loyalty/**", "/push/**", "/search/**")
                .hasAnyRole("ADMIN", "BUYER")

                // --- OTHER REQUESTS ---
                .anyRequest().authenticated()
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .xssProtection(xss -> xss.disable())
                .contentTypeOptions(content -> content.disable())
                .addHeaderWriter(new StaticHeadersWriter("X-Content-Type-Options", "nosniff"))
                .addHeaderWriter(new StaticHeadersWriter("Content-Security-Policy",
                    buildContentSecurityPolicy(allowedOrigins)))
            )
            .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitingFilter, com.ucacue.bar.security.FirebaseTokenFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = parseAllowedOrigins();
        List<String> exactOrigins = new ArrayList<>();
        List<String> originPatterns = new ArrayList<>();
        for (String origin : origins) {
            if (origin.contains("*")) {
                originPatterns.add(origin);
            } else {
                exactOrigins.add(origin);
            }
        }
        if (!exactOrigins.isEmpty()) {
            configuration.setAllowedOrigins(exactOrigins);
        }
        if (!originPatterns.isEmpty()) {
            configuration.setAllowedOriginPatterns(originPatterns);
        }
        if (exactOrigins.isEmpty() && originPatterns.isEmpty()) {
            configuration.addAllowedOriginPattern("*");
        }
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Cache-Control", "Pragma"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition", "Link", "Location"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> parseAllowedOrigins() {
        return Arrays.stream(allowedOriginsCsv.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .distinct()
            .toList();
    }

    private String buildContentSecurityPolicy(List<String> allowedOrigins) {
        List<String> baseConnectSources = Arrays.asList(
            "'self'",
            "https://identitytoolkit.googleapis.com",
            "https://securetoken.googleapis.com",
            "https://accounts.google.com",
            "https://www.googleapis.com",
            "https://firebasestorage.googleapis.com"
        );

        List<String> wsSources = allowedOrigins.stream()
            .map(origin -> origin.replaceAll("/+$", ""))
            .flatMap(origin -> {
                if (origin.startsWith("http://")) {
                    return Stream.of(origin, origin.replaceFirst("^http", "ws"));
                }
                if (origin.startsWith("https://")) {
                    return Stream.of(origin, origin.replaceFirst("^https", "wss"));
                }
                return Stream.of(origin);
            })
            .collect(Collectors.toList());

        String connectSrc = Stream.concat(baseConnectSources.stream(), wsSources.stream())
            .distinct()
            .collect(Collectors.joining(" "));

        return "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net https://www.gstatic.com https://apis.google.com https://accounts.google.com https://unpkg.com; " +
            "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; " +
            "font-src 'self' https://fonts.gstatic.com data:; " +
            "img-src 'self' data: https:; " +
            "connect-src " + connectSrc + "; " +
            "frame-src 'self' https://accounts.google.com https://*.firebaseapp.com";
    }
}
