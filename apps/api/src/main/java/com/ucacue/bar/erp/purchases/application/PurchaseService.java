package com.ucacue.bar.erp.purchases.application;

import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.entity.ProductEntity.ProductStatus;
import com.ucacue.bar.entity.StockMovementEntity;
import com.ucacue.bar.entity.StockMovementEntity.MovementType;
import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.erp.accounting.application.AccountingPostingService;
import com.ucacue.bar.erp.accounting.application.TaxComputationService;
import com.ucacue.bar.erp.approval.application.ApprovalService;
import com.ucacue.bar.erp.approval.infrastructure.persistence.entity.ApprovalDefinitionEntity;
import com.ucacue.bar.erp.approval.infrastructure.persistence.entity.ApprovalInstanceEntity;
import com.ucacue.bar.erp.approval.infrastructure.persistence.repository.ApprovalDefinitionRepository;
import com.ucacue.bar.erp.platform.application.SequenceService;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AccountCatalogEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AccountingDocumentTypeEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.DocumentSequenceEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.PayableEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.SriConfigurationEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.AccountCatalogRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.AccountingDocumentTypeRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.DocumentSequenceRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.PayableRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.SriConfigurationRepository;
import com.ucacue.bar.erp.purchases.domain.PurchaseTypes.PurchaseStatus;
import com.ucacue.bar.erp.purchases.infrastructure.persistence.entity.PurchaseEntity;
import com.ucacue.bar.erp.purchases.infrastructure.persistence.entity.PurchaseItemEntity;
import com.ucacue.bar.erp.purchases.infrastructure.persistence.repository.PurchaseRepository;
import com.ucacue.bar.erp.shared.application.TabularExportService;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.exception.UnauthorizedException;
import com.ucacue.bar.repository.ProductRepository;
import com.ucacue.bar.repository.StockMovementRepository;
import com.ucacue.bar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.ucacue.bar.erp.accounting.domain.AccountingTypes.AccountingSourceModule.PURCHASES;

@Service
@Slf4j
@RequiredArgsConstructor
public class PurchaseService {

    private static final String SEQUENCE_CODE_PURCHASE_ORDER = "PURCHASE_ORDER";
    private static final String APPROVAL_ENTITY_TYPE_PURCHASE_ORDER = "PURCHASE_ORDER";
    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();

    private static final String ACCOUNT_INVENTORY = "1.1.03.01";
    private static final String ACCOUNT_TAX_RECEIVABLE = "1.1.04.01";
    private static final String ACCOUNT_PAYABLE = "2.1.01.01";
    private static final String ACCOUNT_CASH = "1.1.01.01";
    private static final String ACCOUNT_BANK = "1.1.01.02";

    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;
    private final TenantContextResolver tenantContextResolver;
    private final TaxComputationService taxComputationService;
    private final AccountingPostingService accountingPostingService;
    private final AccountCatalogRepository accountCatalogRepository;
    private final PayableRepository payableRepository;
    private final AccountingDocumentTypeRepository accountingDocumentTypeRepository;
    private final DocumentSequenceRepository documentSequenceRepository;
    private final SriConfigurationRepository sriConfigurationRepository;
    private final TabularExportService tabularExportService;
    private final SequenceService sequenceService;
    private final ApprovalDefinitionRepository approvalDefinitionRepository;
    private final ApprovalService approvalService;

    @Transactional(readOnly = true)
    public List<PurchaseSummary> list(String tenantCode) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        return purchaseRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public TabularExportService.ExportDocument export(String tenantCode, String format,
                                                      String status, String search,
                                                      LocalDate from, LocalDate to) {
        List<List<?>> rows = new ArrayList<>();
        for (PurchaseSummary purchase : filterSummaries(list(tenantCode), status, search, from, to)) {
            rows.add(List.of(
                    purchase.id(),
                    purchase.supplierName(),
                    purchase.supplierIdentification(),
                    purchase.externalDocumentNumber(),
                    purchase.status(),
                    purchase.subtotal(),
                    purchase.tax(),
                    purchase.total(),
                    purchase.dueDate(),
                    purchase.receivedAt(),
                    purchase.paidAt(),
                    purchase.notes()));
        }

        return tabularExportService.export(
                "Compras ERP",
                List.of("ID", "Proveedor", "Identificacion", "Documento", "Estado", "Subtotal", "IVA", "Total", "Vence", "Recibida", "Pagada", "Notas"),
                rows,
                format);
    }

    @Transactional
    public PurchaseSummary create(String tenantCode, CreatePurchaseCommand command) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        validateCreateCommand(command);
        if (command.items() == null || command.items().isEmpty()) {
            throw new BadRequestException("La compra debe incluir items");
        }
        for (CreatePurchaseItem item : command.items()) {
            if (item.productId() == null) {
                throw new BadRequestException("Cada item de compra debe incluir un producto");
            }
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new BadRequestException("La cantidad de compra debe ser mayor a cero");
            }
            if (item.unitCost() == null || item.unitCost().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("El costo unitario debe ser mayor a cero");
            }
        }

        Map<Long, ProductEntity> products = lockProducts(tenantId, command.items().stream().map(CreatePurchaseItem::productId).toList());
        TaxComputationService.TaxSummary taxSummary = taxComputationService.calculate(
                tenantId,
                command.items().stream()
                        .map(item -> new TaxComputationService.TaxableLine(item.productId(), item.quantity(), item.unitCost()))
                        .toList());

        Map<Long, TaxComputationService.TaxLineResult> taxLinesByProduct = new LinkedHashMap<>();
        for (TaxComputationService.TaxLineResult line : taxSummary.lines()) {
            taxLinesByProduct.put(line.productId(), line);
        }

        PurchaseEntity purchase = new PurchaseEntity();
        purchase.setTenantId(tenantId);
        purchase.setBranchId(command.branchId());
        purchase.setSupplierName(command.supplierName());
        purchase.setSupplierIdentification(command.supplierIdentification());
        purchase.setSupplierEmail(command.supplierEmail());
        purchase.setExternalDocumentNumber(command.externalDocumentNumber());
        purchase.setDueDate(command.dueDate());
        purchase.setNotes(command.notes());
        purchase.setSubtotal(taxSummary.subtotal());
        purchase.setTax(taxSummary.taxAmount());
        purchase.setTotal(taxSummary.total());
        purchase.setInternalNumber(generateInternalNumber(command.branchId()));
        purchase.setStatus(resolveInitialStatus(taxSummary.total()));

        for (CreatePurchaseItem item : command.items()) {
            ProductEntity product = products.get(item.productId());
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new BadRequestException("Cantidad invalida para " + product.getName());
            }
            if (item.unitCost() == null || item.unitCost().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Costo invalido para " + product.getName());
            }

            TaxComputationService.TaxLineResult taxLine = taxLinesByProduct.get(item.productId());
            PurchaseItemEntity purchaseItem = new PurchaseItemEntity();
            purchaseItem.setProduct(product);
            purchaseItem.setQuantity(item.quantity());
            purchaseItem.setUnitCost(item.unitCost().setScale(2, RoundingMode.HALF_UP));
            purchaseItem.setLineSubtotal(taxLine != null ? taxLine.subtotalAmount() : item.unitCost().multiply(BigDecimal.valueOf(item.quantity())));
            purchaseItem.setTaxRuleId(taxLine != null ? taxLine.taxRuleId() : null);
            purchaseItem.setTaxAmount(taxLine != null ? taxLine.taxAmount() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            purchaseItem.setLineTotal(taxLine != null ? taxLine.totalAmount() : purchaseItem.getLineSubtotal());
            purchase.addItem(purchaseItem);
        }

        PurchaseEntity saved = purchaseRepository.save(purchase);
        if (saved.getStatus() == PurchaseStatus.PENDING_APPROVAL) {
            try {
                ApprovalInstanceEntity instance = approvalService.requestApproval(
                        APPROVAL_ENTITY_TYPE_PURCHASE_ORDER, saved.getId(), currentUserIdOrNull());
                saved.setApprovalInstanceId(instance.getId());
                saved = purchaseRepository.save(saved);
            } catch (RuntimeException ex) {
                log.warn("Could not request approval for purchase {}: {}", saved.getId(), ex.getMessage());
                saved.setStatus(PurchaseStatus.DRAFT);
                saved = purchaseRepository.save(saved);
            }
        }
        return toSummary(saved);
    }

    /**
     * Invoked by the approval listener when an approval instance for a purchase transitions to a
     * terminal state. APPROVED transitions the PO to DRAFT (ready to receive); REJECTED/CANCELLED
     * move it to CANCELLED.
     */
    @Transactional
    public void applyApprovalDecision(Long purchaseId, boolean approved) {
        PurchaseEntity purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new NotFoundException("Compra no encontrada: " + purchaseId));
        if (purchase.getStatus() != PurchaseStatus.PENDING_APPROVAL) {
            return;
        }
        purchase.setStatus(approved ? PurchaseStatus.DRAFT : PurchaseStatus.CANCELLED);
        purchaseRepository.save(purchase);
    }

    private String generateInternalNumber(Long branchId) {
        try {
            return sequenceService.nextNumber(SEQUENCE_CODE_PURCHASE_ORDER, branchId);
        } catch (NotFoundException ex) {
            log.debug("No active PURCHASE_ORDER sequence configured; internalNumber left null");
            return null;
        }
    }

    private PurchaseStatus resolveInitialStatus(BigDecimal total) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        List<ApprovalDefinitionEntity> definitions =
                approvalDefinitionRepository.findActiveByEntityType(tenantId, APPROVAL_ENTITY_TYPE_PURCHASE_ORDER);
        BigDecimal amount = total != null ? total : BigDecimal.ZERO;
        for (ApprovalDefinitionEntity def : definitions) {
            if (evaluateApprovalCondition(def.getConditionExpr(), amount)) {
                return PurchaseStatus.PENDING_APPROVAL;
            }
        }
        return PurchaseStatus.DRAFT;
    }

    private boolean evaluateApprovalCondition(String expr, BigDecimal amount) {
        if (expr == null || expr.isBlank()) {
            return true;
        }
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("amount", amount);
            Object result = SPEL_PARSER.parseExpression(expr).getValue(context);
            return Boolean.TRUE.equals(result);
        } catch (RuntimeException ex) {
            log.warn("Invalid approval conditionExpr '{}': {}", expr, ex.getMessage());
            return false;
        }
    }

    private Long currentUserIdOrNull() {
        try {
            return currentUser().getId();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void validateCreateCommand(CreatePurchaseCommand command) {
        if (command == null) {
            throw new BadRequestException("La compra es obligatoria");
        }
        if (command.supplierName() == null || command.supplierName().isBlank()) {
            throw new BadRequestException("El nombre del proveedor es obligatorio");
        }
        if (command.supplierIdentification() == null || command.supplierIdentification().isBlank()) {
            throw new BadRequestException("La identificacion del proveedor es obligatoria");
        }
        if (command.externalDocumentNumber() == null || command.externalDocumentNumber().isBlank()) {
            throw new BadRequestException("El numero de documento externo es obligatorio");
        }
    }

    @Transactional
    public PurchaseSummary receive(String tenantCode, Long purchaseId) {
        PurchaseEntity purchase = requirePurchase(tenantCode, purchaseId);
        if (purchase.getStatus() != PurchaseStatus.DRAFT) {
            throw new BadRequestException("Solo se pueden recibir compras en borrador");
        }

        Map<Long, Integer> quantities = aggregateQuantities(purchase.getItems());
        Map<Long, ProductEntity> products = lockProducts(purchase.getTenantId(), new ArrayList<>(quantities.keySet()));
        UserEntity actor = currentUser();

        for (PurchaseItemEntity item : purchase.getItems()) {
            ProductEntity product = products.get(item.getProduct().getId());
            int stockBefore = product.getStock();
            int stockAfter = stockBefore + item.getQuantity();
            product.setStock(stockAfter);
            product.setPurchasePrice(item.getUnitCost());
            if (stockAfter > 0 && product.getStatus() == ProductStatus.SOLD_OUT) {
                product.setStatus(ProductStatus.AVAILABLE);
            }

            StockMovementEntity movement = new StockMovementEntity();
            movement.setProduct(product);
            movement.setType(MovementType.IN);
            movement.setQuantity(item.getQuantity());
            movement.setStockBefore(stockBefore);
            movement.setStockAfter(stockAfter);
            movement.setReason("Ingreso por compra " + purchase.getId());
            movement.setReferenceType("PURCHASE");
            movement.setReferenceId(purchase.getId());
            movement.setUser(actor);
            stockMovementRepository.save(movement);
        }
        productRepository.saveAll(products.values());

        AccountingPostingService.AccountingPostingResult postingResult = accountingPostingService.post(
                new AccountingPostingService.PostAccountingEntryCommand(
                        purchase.getTenantId(),
                        PURCHASES,
                        "PURCHASE_RECEIPT",
                        purchase.getId(),
                        LocalDate.now(),
                        "Recepcion de compra " + purchase.getId(),
                        purchase.getExternalDocumentNumber(),
                        buildReceiptLines(purchase),
                        null,
                        new AccountingPostingService.CreatePayableCommand(
                                null,
                                "PURCHASE",
                                purchase.getId(),
                                LocalDate.now(),
                                purchase.getDueDate(),
                                amount(purchase.getTotal())),
                        buildPurchaseTaxDocument(purchase)));

        purchase.setStatus(PurchaseStatus.RECEIVED);
        purchase.setReceivedAt(LocalDateTime.now());
        purchase.setAccountingEntryId(postingResult.accountingEntryId());
        purchase.setPayableId(postingResult.payableId());
        return toSummary(purchaseRepository.save(purchase));
    }

    @Transactional
    public PurchaseSummary pay(String tenantCode, Long purchaseId, RegisterPurchasePaymentCommand command) {
        PurchaseEntity purchase = requirePurchase(tenantCode, purchaseId);
        if (purchase.getStatus() != PurchaseStatus.RECEIVED) {
            throw new BadRequestException("Solo se pueden pagar compras recibidas");
        }
        if (purchase.getPayableId() == null) {
            throw new BadRequestException("La compra no tiene una cuenta por pagar asociada");
        }

        PayableEntity payable = payableRepository.findByTenantIdAndId(purchase.getTenantId(), purchase.getPayableId())
                .orElseThrow(() -> new NotFoundException("Cuenta por pagar no encontrada"));
        if (payable.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("La compra ya no tiene saldo pendiente");
        }

        BigDecimal paymentAmount = amount(command.amount() != null ? command.amount() : payable.getBalance());
        if (paymentAmount.signum() <= 0 || paymentAmount.compareTo(amount(payable.getBalance())) > 0) {
            throw new BadRequestException("Monto de pago invalido");
        }

        AccountingPostingService.AccountingPostingResult postingResult = accountingPostingService.post(
                new AccountingPostingService.PostAccountingEntryCommand(
                        purchase.getTenantId(),
                        PURCHASES,
                        "PURCHASE_PAYMENT",
                        purchase.getId(),
                        command.paymentDate() != null ? command.paymentDate() : LocalDate.now(),
                        "Pago de compra " + purchase.getId(),
                        command.reference(),
                        List.of(
                                new AccountingPostingService.PostAccountingEntryLineCommand(
                                        resolveAccountId(purchase.getTenantId(), ACCOUNT_PAYABLE),
                                        null, paymentAmount, BigDecimal.ZERO, "Cancelacion de compra", command.reference()),
                                new AccountingPostingService.PostAccountingEntryLineCommand(
                                        resolveAccountId(purchase.getTenantId(), resolvePaymentAccount(command.accountCode())),
                                        null, BigDecimal.ZERO, paymentAmount, "Salida por pago de compra", command.reference())),
                        null,
                        null,
                        null));

        payable.setPaidAmount(amount(payable.getPaidAmount()).add(paymentAmount));
        payable.setBalance(amount(payable.getTotalAmount()).subtract(payable.getPaidAmount()));
        if (payable.getBalance().signum() == 0) {
            payable.setStatus(com.ucacue.bar.erp.accounting.domain.AccountingTypes.PayableStatus.PAID);
            purchase.setStatus(PurchaseStatus.PAID);
            purchase.setPaidAt(LocalDateTime.now());
        } else {
            payable.setStatus(com.ucacue.bar.erp.accounting.domain.AccountingTypes.PayableStatus.PARTIALLY_PAID);
        }
        payableRepository.save(payable);

        purchase.setPaymentAccountingEntryId(postingResult.accountingEntryId());
        return toSummary(purchaseRepository.save(purchase));
    }

    private List<AccountingPostingService.PostAccountingEntryLineCommand> buildReceiptLines(PurchaseEntity purchase) {
        List<AccountingPostingService.PostAccountingEntryLineCommand> lines = new ArrayList<>();
        lines.add(new AccountingPostingService.PostAccountingEntryLineCommand(
                resolveAccountId(purchase.getTenantId(), ACCOUNT_INVENTORY),
                null,
                amount(purchase.getSubtotal()),
                BigDecimal.ZERO,
                "Ingreso inventario compra " + purchase.getId(),
                purchase.getExternalDocumentNumber()));
        if (amount(purchase.getTax()).signum() > 0) {
            lines.add(new AccountingPostingService.PostAccountingEntryLineCommand(
                    resolveAccountId(purchase.getTenantId(), ACCOUNT_TAX_RECEIVABLE),
                    null,
                    amount(purchase.getTax()),
                    BigDecimal.ZERO,
                    "IVA credito tributario compra " + purchase.getId(),
                    purchase.getExternalDocumentNumber()));
        }
        lines.add(new AccountingPostingService.PostAccountingEntryLineCommand(
                resolveAccountId(purchase.getTenantId(), ACCOUNT_PAYABLE),
                null,
                BigDecimal.ZERO,
                amount(purchase.getTotal()),
                "Registro CxP compra " + purchase.getId(),
                purchase.getExternalDocumentNumber()));
        return lines;
    }

    private AccountingPostingService.CreateTaxDocumentCommand buildPurchaseTaxDocument(PurchaseEntity purchase) {
        AccountingDocumentTypeEntity documentType = accountingDocumentTypeRepository
                .findByTenantIdAndDocumentCode(purchase.getTenantId(), "PURCHASE_INVOICE")
                .orElseThrow(() -> new NotFoundException("No existe tipo documental de factura de compra configurado"));

        Long sequenceId = Boolean.TRUE.equals(documentType.getRequiresSequence())
                ? documentSequenceRepository.findByTenantIdAndDocumentTypeDocumentCodeAndActiveTrue(purchase.getTenantId(), documentType.getDocumentCode())
                .map(DocumentSequenceEntity::getId)
                .orElseThrow(() -> new NotFoundException("No existe una secuencia tributaria activa para la factura de compra"))
                : null;

        return new AccountingPostingService.CreateTaxDocumentCommand(
                documentType,
                sequenceId,
                resolveEnvironmentCode(purchase.getTenantId()),
                LocalDate.now(),
                purchase.getSupplierIdentification(),
                purchase.getSupplierName(),
                amount(purchase.getSubtotal()),
                amount(purchase.getTax()),
                amount(purchase.getTotal()),
                null,
                null,
                purchase.getItems().stream()
                        .filter(item -> amount(item.getTaxAmount()).signum() > 0)
                        .map(item -> new AccountingPostingService.CreateTaxDocumentTaxLineCommand(
                                item.getTaxRuleId(),
                                amount(item.getLineSubtotal()),
                                amount(item.getTaxAmount()),
                                percentage(item.getLineSubtotal(), item.getTaxAmount())))
                        .toList());
    }

    private PurchaseEntity requirePurchase(String tenantCode, Long purchaseId) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        return purchaseRepository.findByTenantIdAndId(tenantId, purchaseId)
                .orElseThrow(() -> new NotFoundException("Compra no encontrada"));
    }

    private List<PurchaseSummary> filterSummaries(List<PurchaseSummary> purchases, String status, String search,
                                                  LocalDate from, LocalDate to) {
        String normalizedStatus = status != null ? status.trim() : "";
        String normalizedSearch = search != null ? search.trim().toLowerCase() : "";
        return purchases.stream()
                .filter(purchase -> normalizedStatus.isBlank() || purchase.status().equalsIgnoreCase(normalizedStatus))
                .filter(purchase -> normalizedSearch.isBlank()
                        || containsIgnoreCase(purchase.supplierName(), normalizedSearch)
                        || containsIgnoreCase(purchase.supplierIdentification(), normalizedSearch)
                        || containsIgnoreCase(purchase.externalDocumentNumber(), normalizedSearch))
                .filter(purchase -> from == null || (purchase.createdAt() != null && !purchase.createdAt().toLocalDate().isBefore(from)))
                .filter(purchase -> to == null || (purchase.createdAt() != null && !purchase.createdAt().toLocalDate().isAfter(to)))
                .toList();
    }

    private String resolveEnvironmentCode(Long tenantId) {
        SriConfigurationEntity configuration = sriConfigurationRepository.findByTenantId(tenantId)
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .orElseThrow(() -> new NotFoundException("No existe configuracion SRI activa para el tenant"));
        return configuration.getEnvironmentCode();
    }

    private Map<Long, ProductEntity> lockProducts(Long tenantId, List<Long> productIds) {
        List<Long> sortedIds = productIds.stream().distinct().sorted(Comparator.naturalOrder()).toList();
        List<ProductEntity> products = productRepository.findAllByTenantIdAndIdInOrderByIdForUpdate(tenantId, sortedIds);
        if (products.size() != sortedIds.size()) {
            throw new NotFoundException("Uno o mas productos no existen");
        }
        Map<Long, ProductEntity> map = new LinkedHashMap<>();
        for (ProductEntity product : products) {
            map.put(product.getId(), product);
        }
        return map;
    }

    private Map<Long, Integer> aggregateQuantities(List<PurchaseItemEntity> items) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (PurchaseItemEntity item : items) {
            quantities.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
        }
        return quantities;
    }

    private PurchaseSummary toSummary(PurchaseEntity purchase) {
        return new PurchaseSummary(
                purchase.getId(),
                purchase.getSupplierName(),
                purchase.getSupplierIdentification(),
                purchase.getSupplierEmail(),
                purchase.getExternalDocumentNumber(),
                purchase.getInternalNumber(),
                purchase.getStatus().name(),
                purchase.getSubtotal(),
                purchase.getTax(),
                purchase.getTotal(),
                purchase.getDueDate(),
                purchase.getReceivedAt(),
                purchase.getPaidAt(),
                purchase.getNotes(),
                purchase.getBranchId(),
                purchase.getApprovalInstanceId(),
                purchase.getAccountingEntryId(),
                purchase.getPaymentAccountingEntryId(),
                purchase.getPayableId(),
                purchase.getCreatedAt(),
                purchase.getItems().stream()
                        .map(item -> new PurchaseItemSummary(
                                item.getId(),
                                item.getProduct().getId(),
                                item.getProduct().getName(),
                                item.getQuantity(),
                                item.getUnitCost(),
                                item.getLineSubtotal(),
                                item.getTaxRuleId(),
                                item.getTaxAmount(),
                                item.getLineTotal()))
                        .toList());
    }

    private UserEntity currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new UnauthorizedException("Usuario autenticado requerido");
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .orElseThrow(() -> new UnauthorizedException("Usuario autenticado no encontrado"));
    }

    private Long resolveAccountId(Long tenantId, String accountCode) {
        AccountCatalogEntity account = accountCatalogRepository.findByTenantIdAndAccountCode(tenantId, accountCode)
                .orElseThrow(() -> new NotFoundException("Cuenta contable no encontrada: " + accountCode));
        return account.getId();
    }

    private String resolvePaymentAccount(String accountCode) {
        if (accountCode == null || accountCode.isBlank()) {
            return ACCOUNT_BANK;
        }
        return switch (accountCode.trim().toUpperCase()) {
            case "CASH" -> ACCOUNT_CASH;
            case "BANK" -> ACCOUNT_BANK;
            default -> accountCode;
        };
    }

    private BigDecimal percentage(BigDecimal subtotal, BigDecimal taxAmount) {
        if (amount(subtotal).compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return amount(taxAmount)
                .multiply(BigDecimal.valueOf(100))
                .divide(amount(subtotal), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal amount(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    public record CreatePurchaseCommand(
            String supplierName,
            String supplierIdentification,
            String supplierEmail,
            String externalDocumentNumber,
            LocalDate dueDate,
            String notes,
            Long branchId,
            List<CreatePurchaseItem> items) {
    }

    public record CreatePurchaseItem(
            Long productId,
            Integer quantity,
            BigDecimal unitCost) {
    }

    public record RegisterPurchasePaymentCommand(
            BigDecimal amount,
            LocalDate paymentDate,
            String reference,
            String accountCode) {
    }

    public record PurchaseSummary(
            Long id,
            String supplierName,
            String supplierIdentification,
            String supplierEmail,
            String externalDocumentNumber,
            String internalNumber,
            String status,
            BigDecimal subtotal,
            BigDecimal tax,
            BigDecimal total,
            LocalDate dueDate,
            LocalDateTime receivedAt,
            LocalDateTime paidAt,
            String notes,
            Long branchId,
            Long approvalInstanceId,
            Long accountingEntryId,
            Long paymentAccountingEntryId,
            Long payableId,
            LocalDateTime createdAt,
            List<PurchaseItemSummary> items) {
    }

    public record PurchaseItemSummary(
            Long id,
            Long productId,
            String productName,
            Integer quantity,
            BigDecimal unitCost,
            BigDecimal lineSubtotal,
            Long taxRuleId,
            BigDecimal taxAmount,
            BigDecimal lineTotal) {
    }
}
