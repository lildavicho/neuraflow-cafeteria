package com.ucacue.bar.erp.vision.application;

import com.ucacue.bar.erp.vision.infrastructure.persistence.entity.VisionApiKeyEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VisionTenantResolver {

    private final VisionApiKeyService visionApiKeyService;

    public ResolvedVisionTenant resolveFromApiKey(String apiKey) {
        VisionApiKeyEntity key = visionApiKeyService.requireActiveKey(apiKey);
        return new ResolvedVisionTenant(key.getTenantId(), key.getId(), key.getKeyPrefix());
    }

    public record ResolvedVisionTenant(
            Long tenantId,
            Long apiKeyId,
            String keyPrefix) {
    }
}
