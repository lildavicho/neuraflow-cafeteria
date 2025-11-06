package com.ucacue.bar.repository;

import com.ucacue.bar.entity.PushTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushTokenRepository extends JpaRepository<PushTokenEntity, Long> {
    Optional<PushTokenEntity> findByFcmToken(String fcmToken);

    List<PushTokenEntity> findByUserId(Long userId);
}
