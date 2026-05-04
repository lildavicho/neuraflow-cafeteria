package com.ucacue.bar.security;

import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.jsonwebtoken.JwtException;
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
        if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Rejected WS connection without bearer token");
            return null;
        }

        String token = authHeader.substring(7);
        try {
            String email = jwtUtil.extractUsername(token);
            if (email == null || !jwtUtil.validateToken(token, new org.springframework.security.core.userdetails.User(
                    email,
                    "",
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))), "access")) {
                log.warn("Rejected WS connection with expired or missing subject");
                return null;
            }

            UserEntity user = userRepository.findByEmailIgnoreCase(email)
                    .filter(UserEntity::getActive)
                    .orElse(null);
            if (user == null) {
                log.warn("Rejected WS connection for inactive or unknown user");
                return null;
            }

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
            accessor.setUser(new UsernamePasswordAuthenticationToken(email, null, authorities));
            return message;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid WS token: {}", e.getMessage());
            return null;
        }
    }
}
