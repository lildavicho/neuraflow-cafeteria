package com.ucacue.bar.erp.vision.infrastructure.persistence.repository;

import com.ucacue.bar.erp.vision.infrastructure.persistence.entity.VisionApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface VisionApiKeyRepository extends JpaRepository<VisionApiKeyEntity, Long> {

    Optional<VisionApiKeyEntity> findByKeyHashAndStatus(String keyHash, VisionApiKeyEntity.Status status);

    @Modifying
    @Query("update VisionApiKeyEntity k set k.lastUsedAt = :lastUsedAt where k.id = :id")
    void markUsed(@Param("id") Long id, @Param("lastUsedAt") OffsetDateTime lastUsedAt);
}
