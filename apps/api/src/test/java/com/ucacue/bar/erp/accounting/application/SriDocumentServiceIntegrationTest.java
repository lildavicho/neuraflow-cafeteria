package com.ucacue.bar.erp.accounting.application;

import com.ucacue.bar.config.RequestTraceFilter;
import com.ucacue.bar.entity.CategoryEntity;
import com.ucacue.bar.entity.OrderEntity;
import com.ucacue.bar.entity.OrderItemEntity;
import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.AccountingSourceModule;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.DocumentDirection;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.FiscalDomain;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxComputationType;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxDocumentStatus;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxScope;
import com.ucacue.bar.erp.accounting.infrastructure.config.SriProperties;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AccountingDocumentTypeEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.SriConfigurationEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentTaxLineEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentTransmissionEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxRuleEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.AccountingDocumentTypeRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.SriConfigurationRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentTaxLineRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentTransmissionRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxRuleRepository;
import com.ucacue.bar.erp.accounting.infrastructure.sri.SriSoapClient;
import com.ucacue.bar.erp.accounting.infrastructure.sri.SriCredentialCryptoService;
import com.ucacue.bar.erp.accounting.infrastructure.sri.SriKeyService;
import com.ucacue.bar.erp.accounting.infrastructure.sri.SriTextSanitizer;
import com.ucacue.bar.erp.accounting.infrastructure.sri.SriXmlMarshaller;
import com.ucacue.bar.erp.accounting.infrastructure.sri.SriXmlSignatureService;
import com.ucacue.bar.erp.accounting.infrastructure.sri.SriXmlValidationService;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantEntity;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.repository.TenantRepository;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.erp.shared.exception.ServiceUnavailableException;
import com.ucacue.bar.repository.CategoryRepository;
import com.ucacue.bar.repository.OrderItemRepository;
import com.ucacue.bar.repository.OrderRepository;
import com.ucacue.bar.repository.ProductRepository;
import com.ucacue.bar.repository.UserRepository;
import org.slf4j.MDC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        SriDocumentService.class,
        SriDocumentPreparationService.class,
        TaxComputationService.class,
        SriKeyService.class,
        SriTextSanitizer.class,
        SriXmlMarshaller.class,
        SriXmlValidationService.class,
        SriCredentialCryptoService.class,
        TenantContextResolver.class,
        SriDocumentServiceIntegrationTest.TestConfig.class
})
class SriDocumentServiceIntegrationTest {

    @Autowired
    private SriDocumentService sriDocumentService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private TaxRuleRepository taxRuleRepository;

    @Autowired
    private AccountingDocumentTypeRepository accountingDocumentTypeRepository;

    @Autowired
    private SriConfigurationRepository sriConfigurationRepository;

    @Autowired
    private TaxDocumentRepository taxDocumentRepository;

    @Autowired
    private TaxDocumentTaxLineRepository taxDocumentTaxLineRepository;

    @Autowired
    private TaxDocumentTransmissionRepository taxDocumentTransmissionRepository;

    @MockitoBean
    private SriSoapClient sriSoapClient;

    @MockitoBean
    private SriXmlSignatureService sriXmlSignatureService;

    private Long tenantId;
    private TaxDocumentEntity draftDocument;

    @BeforeEach
    void setUp() {
        TenantEntity tenant = new TenantEntity();
        tenant.setTenantCode("default");
        tenant.setLegalName("Tenant Default");
        tenant.setTradeName("Tenant Default");
        tenant = tenantRepository.save(tenant);
        tenantId = tenant.getId();

        SriConfigurationEntity sriConfiguration = new SriConfigurationEntity();
        sriConfiguration.setTenantId(tenantId);
        sriConfiguration.setEnvironmentCode("1");
        sriConfiguration.setEmissionCode("1");
        sriConfiguration.setIssuerRuc("0199999999001");
        sriConfiguration.setIssuerLegalName("UCACUE Demo");
        sriConfiguration.setIssuerTradeName("UCACUE Demo");
        sriConfiguration.setMatrixAddress("Matriz 123");
        sriConfiguration.setEstablishmentAddress("Sucursal 456");
        sriConfiguration.setObligatedAccounting("SI");
        sriConfiguration.setActive(Boolean.TRUE);
        sriConfigurationRepository.save(sriConfiguration);

        TaxRuleEntity iva12 = new TaxRuleEntity();
        iva12.setTenantId(tenantId);
        iva12.setTaxCode("IVA12");
        iva12.setTaxName("IVA 12%");
        iva12.setTaxScope(TaxScope.DOCUMENT);
        iva12.setComputationType(TaxComputationType.PERCENTAGE);
        iva12.setRate(new BigDecimal("12.0000"));
        iva12.setTaxAuthorityCode("2");
        iva12.setTaxableObjectCode("2");
        iva12.setIncludedInPrice(Boolean.FALSE);
        iva12.setActive(Boolean.TRUE);
        taxRuleRepository.save(iva12);

        AccountingDocumentTypeEntity invoiceType = new AccountingDocumentTypeEntity();
        invoiceType.setTenantId(tenantId);
        invoiceType.setDocumentCode("SRI_INVOICE");
        invoiceType.setDocumentName("Factura SRI");
        invoiceType.setFiscalDomain(FiscalDomain.SRI);
        invoiceType.setDocumentDirection(DocumentDirection.OUTBOUND);
        invoiceType.setRequiresSequence(Boolean.TRUE);
        invoiceType.setRequiresCounterparty(Boolean.TRUE);
        invoiceType.setActive(Boolean.TRUE);
        invoiceType = accountingDocumentTypeRepository.save(invoiceType);

        CategoryEntity category = new CategoryEntity();
        category.setTenantId(tenantId);
        category.setName("Cafe SRI");
        category.setActive(Boolean.TRUE);
        category = categoryRepository.save(category);

        ProductEntity product = new ProductEntity();
        product.setTenantId(tenantId);
        product.setCategory(category);
        product.setCode("CAF-001");
        product.setName("Cafe Americano");
        product.setDescription("Cafe");
        product.setUnit("UNIDAD");
        product.setPrice(new BigDecimal("10.00"));
        product.setPurchasePrice(new BigDecimal("4.00"));
        product.setStock(20);
        product.setMinStock(5);
        product.setPrepared(Boolean.TRUE);
        product = productRepository.save(product);

        UserEntity user = new UserEntity();
        user.setName("Cliente SRI");
        user.setEmail("cliente-sri@example.com");
        user.setIdentification("0912345678");
        user.setRole(UserEntity.UserRole.CUSTOMER);
        user.setTenant(tenant);
        user.setActive(Boolean.TRUE);
        user = userRepository.save(user);

        OrderEntity order = new OrderEntity();
        order.setTenantId(tenantId);
        order.setUser(user);
        order.setSubtotal(new BigDecimal("10.00"));
        order.setTax(new BigDecimal("1.20"));
        order.setTotal(new BigDecimal("11.20"));
        order.setStatus(OrderEntity.OrderStatus.DELIVERED);
        order.setPaymentMethod(OrderEntity.PaymentMethod.CASH);
        order.setPaymentStatus(OrderEntity.PaymentStatus.PAID);
        order.setTransactionStatus(OrderEntity.TransactionStatus.PAID);
        order.setInventoryStatus(OrderEntity.InventoryStatus.COMMITTED);
        order.setPaidAt(LocalDateTime.now().minusMinutes(5));
        order = orderRepository.save(order);

        OrderItemEntity orderItem = new OrderItemEntity();
        orderItem.setTenantId(tenantId);
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(1);
        orderItem.setUnitPrice(new BigDecimal("10.00"));
        orderItem.setLineTotal(new BigDecimal("10.00"));
        orderItemRepository.save(orderItem);
        order.setItems(List.of(orderItem));

        draftDocument = new TaxDocumentEntity();
        draftDocument.setTenantId(tenantId);
        draftDocument.setSourceModule(AccountingSourceModule.POS);
        draftDocument.setSourceType("ORDER_SALE");
        draftDocument.setSourceId(order.getId());
        draftDocument.setDocumentType(invoiceType);
        draftDocument.setIssueDate(LocalDate.now());
        draftDocument.setBuyerIdentification(user.getIdentification());
        draftDocument.setBuyerName(user.getName());
        draftDocument.setEstablishmentCode("001");
        draftDocument.setEmissionPointCode("002");
        draftDocument.setSequentialNumber("123");
        draftDocument.setSubtotalAmount(new BigDecimal("10.00"));
        draftDocument.setTaxAmount(new BigDecimal("1.20"));
        draftDocument.setTotalAmount(new BigDecimal("11.20"));
        draftDocument.setStatus(TaxDocumentStatus.PENDING_VALIDATION);
        draftDocument = taxDocumentRepository.save(draftDocument);

        TaxDocumentTaxLineEntity taxLine = new TaxDocumentTaxLineEntity();
        taxLine.setTaxDocument(draftDocument);
        taxLine.setTaxRuleId(iva12.getId());
        taxLine.setTaxableBase(new BigDecimal("10.00"));
        taxLine.setTaxAmount(new BigDecimal("1.20"));
        taxLine.setPercentage(new BigDecimal("12.0000"));
        taxDocumentTaxLineRepository.save(taxLine);
    }

    @Test
    void validateShouldPrepareXmlAndMarkDocumentReadyToSend() {
        SriDocumentService.TaxDocumentSummary summary = sriDocumentService.validate("default", draftDocument.getId());

        TaxDocumentEntity persisted = taxDocumentRepository.findById(draftDocument.getId()).orElseThrow();
        assertThat(summary.status()).isEqualTo(TaxDocumentStatus.READY_TO_SEND.name());
        assertThat(summary.accessKey()).hasSize(49);
        assertThat(persisted.getXmlPayload()).contains("<factura");
        assertThat(persisted.getXmlPayload()).contains("<razonSocialComprador>Cliente SRI</razonSocialComprador>");
        assertThat(persisted.getXmlPayload()).contains("<importeTotal>11.20</importeTotal>");
        assertThat(persisted.getValidationErrors()).isNull();
        assertThat(taxDocumentTransmissionRepository.findByTaxDocumentIdOrderByAttemptedAtDesc(draftDocument.getId())).isEmpty();
    }

    @Test
    void emitShouldPersistReceptionAndAuthorizationTransmissions() {
        MDC.put(RequestTraceFilter.TRACE_ID_KEY, "trace-sri-e2e");
        doReturn("<signed-xml/>").when(sriXmlSignatureService)
                .sign(anyString(), any(SriXmlSignatureService.SigningCredentials.class));
        doReturn(new SriSoapClient.ReceiptResponse(
                "https://test-recepcion.local",
                "<receipt-request/>",
                "<receipt-response/>",
                200,
                true,
                false,
                "RECIBIDA",
                "Recibida por el SRI",
                List.of()
        )).when(sriSoapClient).submitReceipt(anyString(), anyString());
        doReturn(new SriSoapClient.AuthorizationResponse(
                "https://test-autorizacion.local",
                "<auth-request/>",
                "<auth-response/>",
                200,
                true,
                false,
                "AUTORIZADO",
                "Autorizado",
                "AUTH-001",
                LocalDateTime.now(),
                "<autorizado/>",
                List.of()
        )).when(sriSoapClient).queryAuthorization(anyString(), anyString());

        SriDocumentService.TaxDocumentSummary summary;
        try {
            summary = sriDocumentService.emit("default", draftDocument.getId());
        } finally {
            MDC.remove(RequestTraceFilter.TRACE_ID_KEY);
        }

        List<TaxDocumentTransmissionEntity> transmissions =
                taxDocumentTransmissionRepository.findByTaxDocumentIdOrderByAttemptedAtDesc(draftDocument.getId());
        TaxDocumentEntity persisted = taxDocumentRepository.findById(draftDocument.getId()).orElseThrow();

        assertThat(summary.status()).isEqualTo(TaxDocumentStatus.AUTHORIZED.name());
        assertThat(summary.authorizationCode()).isEqualTo("AUTH-001");
        assertThat(persisted.getSignedXmlPayload()).isEqualTo("<autorizado/>");
        assertThat(transmissions).hasSize(2);
        assertThat(transmissions)
                .extracting(TaxDocumentTransmissionEntity::getPhase)
                .containsExactlyInAnyOrder(
                        TaxDocumentTransmissionEntity.TransmissionPhase.RECEPTION,
                        TaxDocumentTransmissionEntity.TransmissionPhase.AUTHORIZATION
                );
        assertThat(transmissions).allSatisfy(item -> {
            assertThat(item.getRequestPayload()).isNotBlank();
            assertThat(item.getResponsePayload()).isNotBlank();
            assertThat(item.getCompletedAt()).isNotNull();
            assertThat(item.getTraceId()).isEqualTo("trace-sri-e2e");
            assertThat(item.getAttemptNumber()).isEqualTo(1);
        });
        verify(sriXmlSignatureService)
                .sign(anyString(), any(SriXmlSignatureService.SigningCredentials.class));
    }

    @Test
    void emitShouldPersistNetworkErrorAllowRetryAndManualFallback() {
        doReturn("<signed-xml/>").when(sriXmlSignatureService)
                .sign(anyString(), any(SriXmlSignatureService.SigningCredentials.class));
        doReturn(new SriSoapClient.ReceiptResponse(
                "https://test-recepcion.local",
                "<receipt-request/>",
                null,
                null,
                false,
                true,
                "NETWORK_ERROR",
                "timeout",
                List.of()
        )).when(sriSoapClient).submitReceipt(anyString(), anyString());

        assertThatThrownBy(() -> sriDocumentService.emit("default", draftDocument.getId()))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("timeout");

        TaxDocumentEntity afterFailure = taxDocumentRepository.findById(draftDocument.getId()).orElseThrow();
        List<TaxDocumentTransmissionEntity> firstAttempt =
                taxDocumentTransmissionRepository.findByTaxDocumentIdOrderByAttemptedAtDesc(draftDocument.getId());
        assertThat(afterFailure.getStatus()).isEqualTo(TaxDocumentStatus.READY_TO_SEND);
        assertThat(afterFailure.getReceptionStatus()).isEqualTo("NETWORK_ERROR");
        assertThat(afterFailure.getLastProviderMessage()).contains("timeout");
        assertThat(firstAttempt).hasSize(1);
        assertThat(firstAttempt.get(0).getSuccess()).isFalse();

        doReturn(new SriSoapClient.ReceiptResponse(
                "https://test-recepcion.local",
                "<receipt-request-2/>",
                "<receipt-response-2/>",
                200,
                true,
                false,
                "RECIBIDA",
                "Recibida por el SRI",
                List.of()
        )).when(sriSoapClient).submitReceipt(anyString(), anyString());
        doReturn(new SriSoapClient.AuthorizationResponse(
                "https://test-autorizacion.local",
                "<auth-request/>",
                "<auth-processing/>",
                200,
                true,
                false,
                "PROCESSING",
                "Aun en proceso",
                null,
                null,
                null,
                List.of()
        )).when(sriSoapClient).queryAuthorization(anyString(), anyString());

        SriDocumentService.TaxDocumentSummary retried = sriDocumentService.emit("default", draftDocument.getId());
        SriDocumentService.TaxDocumentSummary manual = sriDocumentService.markAuthorized(
                "default",
                draftDocument.getId(),
                "AUTH-MANUAL-001"
        );

        List<TaxDocumentTransmissionEntity> transmissions =
                taxDocumentTransmissionRepository.findByTaxDocumentIdOrderByAttemptedAtDesc(draftDocument.getId());
        assertThat(retried.status()).isEqualTo(TaxDocumentStatus.SENT.name());
        assertThat(manual.status()).isEqualTo(TaxDocumentStatus.AUTHORIZED.name());
        assertThat(manual.authorizationCode()).isEqualTo("AUTH-MANUAL-001");
        assertThat(manual.authorizationStatus()).isEqualTo("MANUAL");
        assertThat(manual.providerMessage()).contains("Autorizacion registrada manualmente");
        assertThat(transmissions).hasSize(3);
        verify(sriXmlSignatureService, times(2))
                .sign(anyString(), any(SriXmlSignatureService.SigningCredentials.class));
        verify(sriSoapClient).queryAuthorization(anyString(), anyString());
    }

    @Test
    void emitShouldPollAuthorizationWhenDocumentAlreadySent() {
        draftDocument.setStatus(TaxDocumentStatus.SENT);
        draftDocument.setAccessKey("1234567890123456789012345678901234567890123456789");
        draftDocument.setReceptionStatus("RECIBIDA");
        taxDocumentRepository.save(draftDocument);

        doReturn(new SriSoapClient.AuthorizationResponse(
                "https://test-autorizacion.local",
                "<auth-request/>",
                "<auth-response/>",
                200,
                true,
                false,
                "AUTORIZADO",
                "Autorizado",
                "AUTH-SENT-001",
                LocalDateTime.now(),
                "<autorizado/>",
                List.of()
        )).when(sriSoapClient).queryAuthorization(anyString(), anyString());

        SriDocumentService.TaxDocumentSummary summary = sriDocumentService.emit("default", draftDocument.getId());

        List<TaxDocumentTransmissionEntity> transmissions =
                taxDocumentTransmissionRepository.findByTaxDocumentIdOrderByAttemptedAtDesc(draftDocument.getId());
        assertThat(summary.status()).isEqualTo(TaxDocumentStatus.AUTHORIZED.name());
        assertThat(transmissions).hasSize(1);
        assertThat(transmissions.get(0).getPhase()).isEqualTo(TaxDocumentTransmissionEntity.TransmissionPhase.AUTHORIZATION);
        verify(sriSoapClient, never()).submitReceipt(anyString(), anyString());
        verify(sriXmlSignatureService, never())
                .sign(anyString(), any(SriXmlSignatureService.SigningCredentials.class));
    }

    @Test
    void retryAutoShouldProbeAuthorizationBeforeRetryingReception() {
        doReturn("<signed-xml/>").when(sriXmlSignatureService)
                .sign(anyString(), any(SriXmlSignatureService.SigningCredentials.class));
        doReturn(new SriSoapClient.ReceiptResponse(
                "https://test-recepcion.local",
                "<receipt-request/>",
                null,
                null,
                false,
                true,
                "NETWORK_ERROR",
                "timeout",
                List.of()
        )).when(sriSoapClient).submitReceipt(anyString(), anyString());

        assertThatThrownBy(() -> sriDocumentService.emit("default", draftDocument.getId()))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("timeout");

        doReturn(new SriSoapClient.AuthorizationResponse(
                "https://test-autorizacion.local",
                "<auth-request/>",
                "<auth-processing/>",
                200,
                true,
                false,
                "PROCESSING",
                "Aun en proceso",
                null,
                null,
                null,
                List.of()
        )).when(sriSoapClient).queryAuthorization(anyString(), anyString());

        SriDocumentService.TaxDocumentSummary retried = sriDocumentService.retry("default", draftDocument.getId(), "AUTO");

        List<TaxDocumentTransmissionEntity> transmissions =
                taxDocumentTransmissionRepository.findByTaxDocumentIdOrderByAttemptedAtDesc(draftDocument.getId());
        assertThat(retried.status()).isEqualTo(TaxDocumentStatus.SENT.name());
        assertThat(retried.authorizationStatus()).isEqualTo("PROCESSING");
        assertThat(transmissions).hasSize(2);
        assertThat(transmissions)
                .extracting(TaxDocumentTransmissionEntity::getPhase)
                .containsExactly(
                        TaxDocumentTransmissionEntity.TransmissionPhase.AUTHORIZATION,
                        TaxDocumentTransmissionEntity.TransmissionPhase.RECEPTION
                );
        verify(sriSoapClient, times(1)).submitReceipt(anyString(), anyString());
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        SriProperties sriProperties() {
            SriProperties properties = new SriProperties();
            properties.setAuthorizationPollDelayMs(0);
            properties.setAutoQueryAfterReception(true);
            properties.getSignature().setPasswordMasterKey("test-sri-signature-master-key-32b");

            SriProperties.Endpoint test = new SriProperties.Endpoint();
            test.setReceptionUrl("https://test-recepcion.local");
            test.setAuthorizationUrl("https://test-autorizacion.local");
            properties.setTest(test);

            SriProperties.Endpoint production = new SriProperties.Endpoint();
            production.setReceptionUrl("https://prod-recepcion.local");
            production.setAuthorizationUrl("https://prod-autorizacion.local");
            properties.setProduction(production);

            return properties;
        }
    }
}
