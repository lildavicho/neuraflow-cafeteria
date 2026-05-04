package com.ucacue.bar.erp.accounting.application;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.InvoiceEmailLogEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.InvoiceEmailLogRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvoiceEmailLogServiceTest {

    private final InvoiceEmailLogRepository repository = mock(InvoiceEmailLogRepository.class);
    private final InvoiceEmailLogService service = new InvoiceEmailLogService(repository);

    @Test
    void bouncedStatusDoesNotRollbackToDeliveredOrSent() {
        InvoiceEmailLogEntity log = log("BOUNCED");
        when(repository.findFirstByProviderMessageIdOrderByCreatedAtDesc("msg-1"))
                .thenReturn(Optional.of(log));

        service.markWebhookStatus("msg-1", "DELIVERED");
        service.markWebhookStatus("msg-1", "SENT");

        assertThat(log.getStatus()).isEqualTo("BOUNCED");
        verify(repository, never()).save(any());
    }

    @Test
    void deliveredStatusDoesNotRollbackToSent() {
        InvoiceEmailLogEntity log = log("DELIVERED");
        when(repository.findFirstByProviderMessageIdOrderByCreatedAtDesc("msg-2"))
                .thenReturn(Optional.of(log));

        service.markWebhookStatus("msg-2", "SENT");

        assertThat(log.getStatus()).isEqualTo("DELIVERED");
        verify(repository, never()).save(any());
    }

    @Test
    void deliveredStatusCanAdvanceToOpened() {
        InvoiceEmailLogEntity log = log("DELIVERED");
        when(repository.findFirstByProviderMessageIdOrderByCreatedAtDesc("msg-3"))
                .thenReturn(Optional.of(log));

        service.markWebhookStatus("msg-3", "OPENED");

        assertThat(log.getStatus()).isEqualTo("OPENED");
        assertThat(log.getOpenedAt()).isNotNull();
        verify(repository).save(log);
    }

    private InvoiceEmailLogEntity log(String status) {
        InvoiceEmailLogEntity log = new InvoiceEmailLogEntity();
        log.setStatus(status);
        return log;
    }
}
