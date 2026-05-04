package com.ucacue.bar.erp.accounting.application;

import com.ucacue.bar.erp.accounting.domain.AccountingTypes.AccountingSourceModule;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.DocumentDirection;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.FiscalDomain;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AccountingDocumentTypeEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.DocumentSequenceEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.AccountingDocumentTypeRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.DocumentSequenceRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(AccountingPostingService.class)
class AccountingPostingServiceConcurrencyTest {

    @Autowired
    private AccountingPostingService accountingPostingService;

    @Autowired
    private AccountingDocumentTypeRepository accountingDocumentTypeRepository;

    @Autowired
    private DocumentSequenceRepository documentSequenceRepository;

    @Autowired
    private TaxDocumentRepository taxDocumentRepository;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldReserveUniqueSequentialNumbersUnderConcurrentPosts() throws Exception {
        long tenantId = 1L;

        AccountingDocumentTypeEntity seededDocumentType = new AccountingDocumentTypeEntity();
        seededDocumentType.setTenantId(tenantId);
        seededDocumentType.setDocumentCode("SRI_INVOICE");
        seededDocumentType.setDocumentName("Factura SRI");
        seededDocumentType.setFiscalDomain(FiscalDomain.SRI);
        seededDocumentType.setDocumentDirection(DocumentDirection.OUTBOUND);
        seededDocumentType.setRequiresSequence(Boolean.TRUE);
        seededDocumentType.setRequiresCounterparty(Boolean.TRUE);
        seededDocumentType.setActive(Boolean.TRUE);
        final AccountingDocumentTypeEntity documentType = accountingDocumentTypeRepository.saveAndFlush(seededDocumentType);

        DocumentSequenceEntity seededSequence = new DocumentSequenceEntity();
        seededSequence.setTenantId(tenantId);
        seededSequence.setDocumentType(documentType);
        seededSequence.setEstablishmentCode("001");
        seededSequence.setEmissionPointCode("001");
        seededSequence.setCurrentNumber(0L);
        seededSequence.setIncrementStep(1);
        seededSequence.setActive(Boolean.TRUE);
        final DocumentSequenceEntity sequence = documentSequenceRepository.saveAndFlush(seededSequence);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Long> firstTask = () -> {
                start.await(2, TimeUnit.SECONDS);
                return postEntry(tenantId, documentType, sequence.getId(), 101L).taxDocumentId();
            };
            Callable<Long> secondTask = () -> {
                start.await(2, TimeUnit.SECONDS);
                return postEntry(tenantId, documentType, sequence.getId(), 102L).taxDocumentId();
            };

            Future<Long> first = executor.submit(firstTask);
            Future<Long> second = executor.submit(secondTask);
            start.countDown();

            Long firstDocumentId = first.get(5, TimeUnit.SECONDS);
            Long secondDocumentId = second.get(5, TimeUnit.SECONDS);

            List<String> sequentialNumbers = taxDocumentRepository.findAllById(List.of(firstDocumentId, secondDocumentId))
                    .stream()
                    .map(TaxDocumentEntity::getSequentialNumber)
                    .sorted()
                    .toList();

            DocumentSequenceEntity persistedSequence = documentSequenceRepository.findById(sequence.getId()).orElseThrow();
            assertThat(sequentialNumbers).containsExactly("000000001", "000000002");
            assertThat(persistedSequence.getCurrentNumber()).isEqualTo(2L);
        } finally {
            executor.shutdownNow();
        }
    }

    private AccountingPostingService.AccountingPostingResult postEntry(Long tenantId,
                                                                       AccountingDocumentTypeEntity documentType,
                                                                       Long sequenceId,
                                                                       Long sourceId) {
        return accountingPostingService.post(new AccountingPostingService.PostAccountingEntryCommand(
                tenantId,
                AccountingSourceModule.POS,
                "ORDER_SALE",
                sourceId,
                LocalDate.now(),
                "Venta concurrente " + sourceId,
                "REF-" + sourceId,
                List.of(
                        new AccountingPostingService.PostAccountingEntryLineCommand(
                                1L,
                                null,
                                new BigDecimal("10.00"),
                                BigDecimal.ZERO,
                                "Debito",
                                "D-" + sourceId
                        ),
                        new AccountingPostingService.PostAccountingEntryLineCommand(
                                2L,
                                null,
                                BigDecimal.ZERO,
                                new BigDecimal("10.00"),
                                "Credito",
                                "C-" + sourceId
                        )
                ),
                null,
                null,
                new AccountingPostingService.CreateTaxDocumentCommand(
                        documentType,
                        sequenceId,
                        "TEST",
                        LocalDate.now(),
                        "0912345678",
                        "Cliente Demo",
                        new BigDecimal("10.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("10.00"),
                        null,
                        null,
                        List.of()
                )
        ));
    }
}
