package com.ucacue.bar.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.ucacue.bar.entity.PushTokenEntity;
import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.repository.PushTokenRepository;
import com.ucacue.bar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
    @RequiredArgsConstructor
    @Slf4j
public class PushService {

    private final PushTokenRepository pushTokenRepository;
    private final UserRepository userRepository;
    private final TenantContextResolver tenantContextResolver;

    @Transactional
    public void registerToken(Long userId, String token, String platform, String deviceName, Boolean active) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        UserEntity user = userRepository.findByTenantIdAndId(tenantId, userId)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BadRequestException("Usuario desactivado");
        }

        PushTokenEntity entity = pushTokenRepository.findByFcmToken(token)
            .orElseGet(PushTokenEntity::new);
        if (entity.getId() != null && entity.getTenantId() != null && !entity.getTenantId().equals(tenantId)) {
            throw new BadRequestException("El token FCM ya esta asociado a otro tenant");
        }
        if (entity.getId() != null && entity.getUser() != null && !entity.getUser().getId().equals(userId)) {
            throw new BadRequestException("El token FCM ya esta asociado a otro usuario");
        }
        entity.setTenantId(tenantId);
        entity.setUser(user);
        entity.setFcmToken(token);
        entity.setPlatform(platform);
        entity.setDeviceName(deviceName);
        entity.setActive(active != null ? active : true);
        entity.setLastUsed(java.time.LocalDateTime.now());
        pushTokenRepository.save(entity);
    }

    public int sendNotification(Long userId, String title, String body, Map<String, String> data) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.debug("Firebase not configured, skipping push notification");
            return 0;
        }
        AtomicInteger delivered = new AtomicInteger();
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        pushTokenRepository.findByTenantIdAndUserIdAndActiveTrue(tenantId, userId).forEach(token -> {
            try {
                com.google.firebase.messaging.Notification notification = com.google.firebase.messaging.Notification
                    .builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();
                Message message = Message.builder()
                    .putAllData(data != null ? data : Map.of())
                    .setToken(token.getFcmToken())
                    .setNotification(notification)
                    .build();
                FirebaseMessaging.getInstance().send(message);
                token.setLastUsed(java.time.LocalDateTime.now());
                pushTokenRepository.save(token);
                delivered.incrementAndGet();
            } catch (Exception ex) {
                log.warn("Failed to send push notification: {}", ex.getMessage());
            }
        });
        return delivered.get();
    }
}
