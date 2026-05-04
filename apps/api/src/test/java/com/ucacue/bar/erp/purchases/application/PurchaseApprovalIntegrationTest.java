package com.ucacue.bar.erp.purchases.application;

import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.entity.ProductEntity.ProductStatus;
import com.ucacue.bar.erp.accounting.application.AccountingPostingService;
import com.ucacue.bar.erp.accounting.application.TaxComputationService;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.AccountCatalogRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.AccountingDocumentTypeRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.DocumentSequenceRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.PayableRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.SriConfigurationRepository;
import com.ucacue.bar.erp.approval.application.ApprovalService;
import com.ucacue.bar.erp.approval.application.ApprovalStatusChangedEvent;
import com.ucacue.bar.erp.approval.domain.ApprovalTypes.ApprovalStatus;
import com.ucacue.bar.erp.approval.infrastructure.persistence.entity.ApprovalDefinitionEntity;
import com.ucacue.bar.erp.approval.infrastructure.persistence.entity.ApprovalInstanceEntity;
import com.ucacue.bar.erp.approval.infrastructure.persistence.repository.ApprovalDefinitionRepository;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantEntity;
import com.ucacue.bar.erp.platform.application.SequenceService;
import com.ucacue.bar.erp.purchases.application.PurchaseService.CreatePurchaseCommand;
import com.ucacue.bar.erp.purchases.application.PurchaseService.CreatePurchaseItem;
import com.ucacue.bar.erp.purchases.application.PurchaseService.PurchaseSummary;
import com.ucacue.bar.erp.purchases.domain.PurchaseTypes.PurchaseStatus;
import com.ucacue.bar.erp.purchases.infrastructure.persistence.entity.PurchaseEntity;
import com.ucacue.bar.erp.purchases.infrastructure.persistence.repository.PurchaseRepository;
import com.ucacue.bar.erp.shared.application.TabularExportService;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.repository.ProductRepository;
import com.ucacue.bar.repository.StockMovementRepository;
import com.ucacue.bar.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseApprovalIntegrationTest {

    private static final Long TENANT_ID = 1L;
    private static final Long PRODUCT_ID = 10L;

    @Mock private PurchaseRepository purchaseRepository;
    @Mock private ProductRepository productRepository;
    @Mock private StockMovementRepository stockMovementRepository;
    @Mock private UserRepository userRepository;
    @Mock private TenantContextResolver tenantContextResolver;
    @Mock private TaxComputationService taxComputationService;
    @Mock private AccountingPostingService accountingPostingService;
    @Mock private AccountCatalogRepository accountCatalogRepository;
    @Mock private PayableRepository payableRepository;
    @Mock private AccountingDocumentTypeRepository accountingDocumentTypeRepository;
    @Mock private DocumentSequenceRepository documentSequenceRepository;
    @Mock private SriConfigurationRepository sriConfigurationRepository;
    @Mock private TabularExportService tabularExportService;
    @Mock private SequenceService sequenceService;
    @Mock private ApprovalDefinitionRepository approvalDefinitionRepository;
    @Mock private ApprovalService approvalService;

    @InjectMocks private PurchaseService purchaseService;

    private PurchaseApprovalListener listener;

    @BeforeEach
    void setUp() {
        listener = new PurchaseApprovalListener(purchaseService);

        TenantEntity tenant = new TenantEntity();
        tenant.setId(TENANT_ID);
        tenant.setTenantCode("default");
        lenient().when(tenantContextResolver.resolveCurrent()).thenReturn(tenant);
        lenient().when(tenantContextResolver.resolve(anyString())).thenReturn(tenant);

        ProductEntity product = new ProductEntity();
        product.setId(PRODUCT_ID);
        product.setName("Café");
        product.setStock(0);
        product.setStatus(ProductStatus.AVAILABLE);
        lenient().when(productRepository.findAllByTenantIdAndIdInOrderByIdForUpdate(eq(TENANT_ID), any()))
                .thenReturn(List.of(product));

        TaxComputationService.TaxLineResult taxLine = new TaxComputationService.TaxLineResult(
                PRODUCT_ID, null, BigDecimal.valueOf(1000), BigDecimal.ZERO, BigDecimal.valueOf(1000));
        TaxComputationService.TaxSummary taxSummary = new TaxComputationService.TaxSummary(
                BigDecimal.valueOf(1000), BigDecimal.ZERO, BigDecimal.valueOf(1000), List.of(taxLine));
        lenient().when(taxComputationService.calculate(eq(TENANT_ID), any())).thenReturn(taxSummary);

        // Mirror save() returning the same instance (no JPA, so just echo)
        lenient().when(purchaseRepository.save(any(PurchaseEntity.class)))
                .thenAnswer(inv -> {
                    PurchaseEntity p = inv.getArgument(0);
                    if (p.getId() == null) p.setId(99L);
                    return p;
                });
    }

    private CreatePurchaseCommand sampleCommand() {
        return new CreatePurchaseCommand(
                "Distribuidora ACME",
                "0102030405001",
                "ventas@acme.ec",
                "001-001-000000123",
                null,
                null,
                null,
                List.of(new CreatePurchaseItem(PRODUCT_ID, 10, BigDecimal.valueOf(100))));
    }

    @Test
    void create_withoutApprovalDefinition_setsDraftAndUsesSequence() {
        when(approvalDefinitionRepository.findActiveByEntityType(TENANT_ID, "PURCHASE_ORDER"))
                .thenReturn(List.of());
        when(sequenceService.nextNumber(eq("PURCHASE_ORDER"), any())).thenReturn("PO-0000001");

        PurchaseSummary summary = purchaseService.create("default", sampleCommand());

        assertThat(summary.status()).isEqualTo(PurchaseStatus.DRAFT.name());
        assertThat(summary.internalNumber()).isEqualTo("PO-0000001");
        verify(approvalService, never()).requestApproval(anyString(), anyLong(), any());
    }

    @Test
    void create_overThreshold_marksPendingAndRequestsApproval() {
        ApprovalDefinitionEntity def = new ApprovalDefinitionEntity();
        def.setId(5L);
        def.setTenantId(TENANT_ID);
        def.setEntityType("PURCHASE_ORDER");
        def.setConditionExpr("#amount > 500");
        when(approvalDefinitionRepository.findActiveByEntityType(TENANT_ID, "PURCHASE_ORDER"))
                .thenReturn(List.of(def));
        when(sequenceService.nextNumber(eq("PURCHASE_ORDER"), any())).thenReturn("PO-0000002");

        ApprovalInstanceEntity instance = new ApprovalInstanceEntity();
        instance.setId(77L);
        instance.setTenantId(TENANT_ID);
        instance.setEntityType("PURCHASE_ORDER");
        instance.setEntityId(99L);
        instance.setStatus(ApprovalStatus.PENDING);
        when(approvalService.requestApproval(eq("PURCHASE_ORDER"), eq(99L), any())).thenReturn(instance);

        PurchaseSummary summary = purchaseService.create("default", sampleCommand());

        assertThat(summary.status()).isEqualTo(PurchaseStatus.PENDING_APPROVAL.name());
        assertThat(summary.approvalInstanceId()).isEqualTo(77L);
        verify(approvalService, times(1)).requestApproval(eq("PURCHASE_ORDER"), eq(99L), any());
    }

    @Test
    void create_belowThreshold_marksDraftDespiteDefinition() {
        ApprovalDefinitionEntity def = new ApprovalDefinitionEntity();
        def.setTenantId(TENANT_ID);
        def.setEntityType("PURCHASE_ORDER");
        def.setConditionExpr("#amount > 5000"); // 1000 < 5000 → no approval required
        when(approvalDefinitionRepository.findActiveByEntityType(TENANT_ID, "PURCHASE_ORDER"))
                .thenReturn(List.of(def));
        when(sequenceService.nextNumber(eq("PURCHASE_ORDER"), any())).thenReturn("PO-0000003");

        PurchaseSummary summary = purchaseService.create("default", sampleCommand());

        assertThat(summary.status()).isEqualTo(PurchaseStatus.DRAFT.name());
        verify(approvalService, never()).requestApproval(anyString(), anyLong(), any());
    }

    @Test
    void create_whenSequenceNotConfigured_proceedsWithNullInternalNumber() {
        when(approvalDefinitionRepository.findActiveByEntityType(TENANT_ID, "PURCHASE_ORDER"))
                .thenReturn(List.of());
        when(sequenceService.nextNumber(eq("PURCHASE_ORDER"), any()))
                .thenThrow(new NotFoundException("No hay secuencia"));

        PurchaseSummary summary = purchaseService.create("default", sampleCommand());

        assertThat(summary.status()).isEqualTo(PurchaseStatus.DRAFT.name());
        assertThat(summary.internalNumber()).isNull();
    }

    @Test
    void approvalListener_onApproved_movesPurchaseToDraft() {
        PurchaseEntity pending = new PurchaseEntity();
        pending.setId(99L);
        pending.setTenantId(TENANT_ID);
        pending.setStatus(PurchaseStatus.PENDING_APPROVAL);
        when(purchaseRepository.findById(99L)).thenReturn(Optional.of(pending));

        listener.onApprovalStatusChanged(new ApprovalStatusChangedEvent(
                TENANT_ID, 77L, "PURCHASE_ORDER", 99L, ApprovalStatus.APPROVED));

        ArgumentCaptor<PurchaseEntity> captor = ArgumentCaptor.forClass(PurchaseEntity.class);
        verify(purchaseRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PurchaseStatus.DRAFT);
    }

    @Test
    void approvalListener_onRejected_movesPurchaseToCancelled() {
        PurchaseEntity pending = new PurchaseEntity();
        pending.setId(99L);
        pending.setTenantId(TENANT_ID);
        pending.setStatus(PurchaseStatus.PENDING_APPROVAL);
        when(purchaseRepository.findById(99L)).thenReturn(Optional.of(pending));

        listener.onApprovalStatusChanged(new ApprovalStatusChangedEvent(
                TENANT_ID, 77L, "PURCHASE_ORDER", 99L, ApprovalStatus.REJECTED));

        ArgumentCaptor<PurchaseEntity> captor = ArgumentCaptor.forClass(PurchaseEntity.class);
        verify(purchaseRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PurchaseStatus.CANCELLED);
    }

    @Test
    void approvalListener_ignoresNonPurchaseEntityType() {
        listener.onApprovalStatusChanged(new ApprovalStatusChangedEvent(
                TENANT_ID, 77L, "OTHER_ENTITY", 99L, ApprovalStatus.APPROVED));

        verify(purchaseRepository, never()).findById(anyLong());
    }
}
