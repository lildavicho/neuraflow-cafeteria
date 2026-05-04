package com.ucacue.bar.erp.vision.application;

import com.ucacue.bar.erp.vision.infrastructure.persistence.entity.VisionApiKeyEntity;
import com.ucacue.bar.erp.vision.infrastructure.persistence.repository.VisionApiKeyRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VisionApiKeyServiceTest {

    private final VisionApiKeyRepository repository = mock(VisionApiKeyRepository.class);
    private final VisionApiKeyService service = new VisionApiKeyService(repository);

    @Test
    void resolvesTenantFromHashedActiveKey() {
        String rawKey = "vk_live_tenant_a_secret";
        String hash = service.hash(rawKey);
        VisionApiKeyEntity entity = new VisionApiKeyEntity();
        entity.setId(10L);
        entity.setTenantId(42L);
        entity.setStatus(VisionApiKeyEntity.Status.ACTIVE);
        when(repository.findByKeyHashAndStatus(hash, VisionApiKeyEntity.Status.ACTIVE))
                .thenReturn(Optional.of(entity));

        VisionApiKeyEntity resolved = service.requireActiveKey(rawKey);

        assertThat(resolved.getTenantId()).isEqualTo(42L);
        verify(repository).markUsed(eq(10L), any());
    }

    @Test
    void rejectsMissingApiKeyWithStableCode() {
        assertThatThrownBy(() -> service.requireActiveKey(" "))
                .isInstanceOf(VisionApiException.class)
                .extracting("code")
                .isEqualTo("VISION_API_KEY_REQUIRED");
    }

    @Test
    void rejectsInvalidApiKeyWithStableCode() {
        when(repository.findByKeyHashAndStatus(any(), eq(VisionApiKeyEntity.Status.ACTIVE)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireActiveKey("vk_live_missing"))
                .isInstanceOf(VisionApiException.class)
                .extracting("code")
                .isEqualTo("VISION_API_KEY_INVALID");
    }
}
