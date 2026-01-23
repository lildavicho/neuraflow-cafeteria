package com.ucacue.bar.security;

import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                // Si no hay token, permitimos conexión anónima (o rechazamos según política).
                // Para este sistema, permitiremos conexión y el SecurityConfig manejará las
                // subscripciones
                // o rechazamos aquí si todo WS requiere auth.
                // El frontend envía token, así que validamos si está presente.
                return message;
            }
            String token = authHeader.substring(7);
            try {
                String email = jwtUtil.extractUsername(token);
                if (email != null && !jwtUtil.isTokenExpired(token)) {
                    UserEntity user = userRepository.findByEmail(email)
                            .filter(UserEntity::getActive)
                            .orElse(null);

                    if (user != null) {
                        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                        accessor.setUser(new UsernamePasswordAuthenticationToken(email, null, authorities));
                    }
                }
            } catch (Exception e) {
                log.warn("Invalid WS token: {}", e.getMessage());
                // No autenticamos, conexión será anónima o fallará si SecurityConfig lo
                // requiere
            }
        }
        return message;
    }
}
