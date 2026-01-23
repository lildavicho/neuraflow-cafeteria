package com.ucacue.bar.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.ucacue.bar.entity.PushTokenEntity;
import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.repository.PushTokenRepository;
import com.ucacue.bar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushService {

    private final PushTokenRepository pushTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registerToken(Long userId, String token, String platform, String deviceName, Boolean active) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        PushTokenEntity entity = pushTokenRepository.findByFcmToken(token)
            .orElseGet(PushTokenEntity::new);
        entity.setUser(user);
        entity.setFcmToken(token);
        entity.setPlatform(platform);
        entity.setDeviceName(deviceName);
        entity.setActive(active != null ? active : true);
        entity.setLastUsed(java.time.LocalDateTime.now());
        pushTokenRepository.save(entity);
    }

    public void sendNotification(Long userId, String title, String body, Map<String, String> data) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.debug("Firebase not configured, skipping push notification");
            return;
        }
        pushTokenRepository.findByUserIdAndActiveTrue(userId).forEach(token -> {
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
                FirebaseMessaging.getInstance().sendAsync(message);
                token.setLastUsed(java.time.LocalDateTime.now());
                pushTokenRepository.save(token);
            } catch (Exception ex) {
                log.warn("Failed to send push notification: {}", ex.getMessage());
            }
        });
    }
}
