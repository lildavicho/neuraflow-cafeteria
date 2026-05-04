package com.ucacue.bar.erp.commercial.application;

import com.ucacue.bar.erp.accounting.infrastructure.config.SriProperties;
import com.ucacue.bar.erp.accounting.infrastructure.email.ResendEmailClient;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.PublicLeadEntity;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.repository.PublicLeadRepository;
import com.ucacue.bar.erp.commercial.interfaces.http.PublicLeadRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicLeadServiceTest {

    private final PublicLeadRepository repository = mock(PublicLeadRepository.class);
    private final ResendEmailClient resendEmailClient = mock(ResendEmailClient.class);
    private final PublicLeadService service = new PublicLeadService(repository, resendEmailClient, new SriProperties());

    @Test
    void createStoresLeadWithoutCreatingErpUser() {
        when(repository.save(any(PublicLeadEntity.class))).thenAnswer(invocation -> {
            PublicLeadEntity lead = invocation.getArgument(0);
            lead.setId(15L);
            return lead;
        });
        when(resendEmailClient.isReady()).thenReturn(false);

        var response = service.create(new PublicLeadRequest(
                " David Mendez ",
                " Demo Corp ",
                "CLIENTE@CORREO.COM ",
                "0999999999",
                "Cuenca",
                "Retail",
                2,
                "Vision AI",
                "Quiero una demo"));

        assertThat(response.id()).isEqualTo(15L);
        assertThat(response.status()).isEqualTo("NEW");
        assertThat(response.message()).contains("recibida");
    }
}
