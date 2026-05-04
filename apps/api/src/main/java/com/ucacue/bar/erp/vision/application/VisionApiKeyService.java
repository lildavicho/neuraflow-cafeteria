package com.ucacue.bar.erp.vision.application;

import com.ucacue.bar.erp.vision.infrastructure.persistence.entity.VisionApiKeyEntity;
import com.ucacue.bar.erp.vision.infrastructure.persistence.repository.VisionApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class VisionApiKeyService {

    private final VisionApiKeyRepository visionApiKeyRepository;

    @Transactional
    public VisionApiKeyEntity requireActiveKey(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            throw new VisionApiException(
                    HttpStatus.UNAUTHORIZED,
                    "VISION_API_KEY_REQUIRED",
                    "Vision API key is required");
        }

        String keyHash = hash(rawApiKey.trim());
        VisionApiKeyEntity apiKey = visionApiKeyRepository
                .findByKeyHashAndStatus(keyHash, VisionApiKeyEntity.Status.ACTIVE)
                .orElseThrow(() -> new VisionApiException(
                        HttpStatus.UNAUTHORIZED,
                        "VISION_API_KEY_INVALID",
                        "Invalid or inactive Vision API key"));

        OffsetDateTime now = OffsetDateTime.now();
        if (apiKey.getExpiresAt() != null && !apiKey.getExpiresAt().isAfter(now)) {
            throw new VisionApiException(
                    HttpStatus.UNAUTHORIZED,
                    "VISION_API_KEY_INVALID",
                    "Invalid or inactive Vision API key");
        }
        visionApiKeyRepository.markUsed(apiKey.getId(), now);
        return apiKey;
    }

    public String hash(String rawApiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawApiKey.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo calcular hash de Vision API key", ex);
        }
    }
}
