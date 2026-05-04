package com.ucacue.bar.erp.accounting.application;

import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.AccountNature;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.AccountType;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.AccountingSourceModule;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.DocumentDirection;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.FiscalDomain;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.PayableStatus;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.ReceivableStatus;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxComputationType;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxScope;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AccountCatalogEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AccountingDocumentTypeEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AccountingEntryEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AccountingEntryLineEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.DocumentSequenceEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.PayableEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.ProductTaxRuleEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.ReceivableEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxRuleEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.AccountCatalogRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.AccountingDocumentTypeRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.AccountingEntryLineRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.AccountingEntryRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.DocumentSequenceRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.PayableRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.ProductTaxRuleRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.ReceivableRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxRuleRepository;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountingManagementService {

    private static final String ACCOUNT_CASH = "1.1.01.01";
    private static final String ACCOUNT_BANK = "1.1.01.02";
    private static final String ACCOUNT_RECEIVABLE = "1.1.02.01";
    private static final String ACCOUNT_PAYABLE = "2.1.01.01";
    private static final String ACCOUNT_SALES = "4.1.01.01";
    private static final int JOURNAL_ENTRY_LIMIT = 100;
    private static final int ACCOUNTING_ITEMS_LIMIT = 200;

    private final TenantContextResolver tenantContextResolver;
    private final AccountCatalogRepository accountCatalogRepository;
    private final TaxRuleRepository taxRuleRepository;
    private final AccountingDocumentTypeRepository accountingDocumentTypeRepository;
    private final DocumentSequenceRepository documentSequenceRepository;
    private final AccountingEntryRepository accountingEntryRepository;
    private final AccountingEntryLineRepository accountingEntryLineRepository;
    private final ReceivableRepository receivableRepository;
    private final PayableRepository payableRepository;
    private final ProductTaxRuleRepository productTaxRuleRepository;
    private final ProductRepository productRepository;
    private final AccountingPostingService accountingPostingService;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "accounting-reference", key = "'accounts:' + #tenantCode")
    public List<AccountView> listAccounts(String tenantCode) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        return accountCatalogRepository.findByTenantIdOrderByAccountCodeAsc(tenantId).stream()
                .map(this::toAccountView)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = "accounting-reference", allEntries = true)
    public AccountView upsertAccount(String tenantCode, UpsertAccountCommand command) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        validateAccountCommand(command);
        AccountCatalogEntity entity = command.id() != null
                ? accountCatalogRepository.findByTenantIdAndId(tenantId, command.id())
                .orElseThrow(() -> new NotFoundException("Cuenta contable no encontrada"))
                : new AccountCatalogEntity();
        entity.setTenantId(tenantId);
        entity.setAccountCode(command.accountCode());
        entity.setAccountName(command.accountName());
        entity.setParentAccountId(command.parentAccountId());
        entity.setAccountType(command.accountType());
        entity.setAccountNature(command.accountNature());
        entity.setTreeLevel(command.treeLevel() != null ? command.treeLevel() : 1);
        entity.setAllowPosting(command.allowPosting() == null || command.allowPosting());
        entity.setActive(command.active() == null || command.active());
        return toAccountView(accountCatalogRepository.save(entity));
    }

    private void validateAccountCommand(UpsertAccountCommand command) {
        if (command == null) {
            throw new BadRequestException("La cuenta contable es obligatoria");
        }
        if (command.accountCode() == null || command.accountCode().isBlank()) {
            throw new BadRequestException("El codigo de cuenta es obligatorio");
        }
        if (command.accountName() == null || command.accountName().isBlank()) {
            throw new BadRequestException("El nombre de cuenta es obligatorio");
        }
        if (command.accountType() == null) {
            throw new BadRequestException("El tipo de cuenta es obligatorio");
        }
        if (command.accountNature() == null) {
            throw new BadRequestException("La naturaleza de cuenta es obligatoria");
        }
        if (command.treeLevel() != null && command.treeLevel() < 1) {
            throw new BadRequestException("El nivel de la cuenta debe ser mayor a cero");
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "accounting-reference", key = "'tax-rules:' + #tenantCode")
    public List<TaxRuleView> listTaxRules(String tenantCode) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        return taxRuleRepository.findByTenantIdOrderByTaxCodeAsc(tenantId).stream()
                .map(this::toTaxRuleView)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = "accounting-reference", allEntries = true)
    public TaxRuleView upsertTaxRule(String tenantCode, UpsertTaxRuleCommand command) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        validateTaxRuleCommand(command);
        TaxRuleEntity entity = command.id() != null
                ? taxRuleRepository.findByTenantIdAndId(tenantId, command.id())
                .orElseThrow(() -> new NotFoundException("Regla tributaria no encontrada"))
                : new TaxRuleEntity();
        entity.setTenantId(tenantId);
        entity.setTaxCode(command.taxCode());
        entity.setTaxName(command.taxName());
        entity.setTaxScope(command.taxScope());
        entity.setComputationType(command.computationType());
        entity.setRate(command.rate() != null ? command.rate() : BigDecimal.ZERO);
        entity.setTaxAuthorityCode(command.taxAuthorityCode());
        entity.setTaxableObjectCode(command.taxableObjectCode());
        entity.setIncludedInPrice(command.includedInPrice() != null && command.includedInPrice());
        entity.setActive(command.active() == null || command.active());
        entity.setValidFrom(command.validFrom());
        entity.setValidUntil(command.validUntil());
        return toTaxRuleView(taxRuleRepository.save(entity));
    }

    private void validateTaxRuleCommand(UpsertTaxRuleCommand command) {
        if (command == null) {
            throw new BadRequestException("La regla tributaria es obligatoria");
        }
        if (command.taxCode() == null || command.taxCode().isBlank()) {
            throw new BadRequestException("El codigo tributario es obligatorio");
        }
        if (command.taxName() == null || command.taxName().isBlank()) {
            throw new BadRequestException("El nombre tributario es obligatorio");
        }
        if (command.taxScope() == null) {
            throw new BadRequestException("El alcance tributario es obligatorio");
        }
        if (command.computationType() == null) {
            throw new BadRequestException("El tipo de calculo tributario es obligatorio");
        }
        if (command.rate() == null || command.rate().signum() < 0) {
            throw new BadRequestException("La tasa tributaria no puede ser negativa");
        }
        if (command.validFrom() != null && command.validUntil() != null && command.validUntil().isBefore(command.validFrom())) {
            throw new BadRequestException("La vigencia tributaria es invalida");
        }
    }

    @Transactional
    @CacheEvict(cacheNames = "accounting-reference", allEntries = true)
    public ProductTaxRuleView assignProductTaxRule(String tenantCode, Long productId, Long taxRuleId) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        ProductEntity product = productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        TaxRuleEntity taxRule = taxRuleRepository.findByTenantIdAndId(tenantId, taxRuleId)
                .orElseThrow(() -> new NotFoundException("Regla tributaria no encontrada"));

        ProductTaxRuleEntity mapping = productTaxRuleRepository.findByTenantIdAndProductIdAndActiveTrue(tenantId, productId)
                .orElseGet(ProductTaxRuleEntity::new);
        mapping.setTenantId(tenantId);
        mapping.setProduct(product);
        mapping.setTaxRule(taxRule);
        mapping.setActive(Boolean.TRUE);
        ProductTaxRuleEntity saved = productTaxRuleRepository.save(mapping);
        return new ProductTaxRuleView(saved.getId(), saved.getProduct().getId(), saved.getProduct().getName(),
                saved.getTaxRule().getId(), saved.getTaxRule().getTaxCode(), saved.getTaxRule().getTaxName());
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "accounting-reference", key = "'document-types:' + #tenantCode")
    public List<DocumentTypeView> listDocumentTypes(String tenantCode) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        return accountingDocumentTypeRepository.findByTenantIdOrderByDocumentCodeAsc(tenantId).stream()
                .map(this::toDocumentTypeView)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = "accounting-reference", allEntries = true)
    public DocumentTypeView upsertDocumentType(String tenantCode, UpsertDocumentTypeCommand command) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        validateDocumentTypeCommand(command);
        AccountingDocumentTypeEntity entity = command.id() != null
                ? accountingDocumentTypeRepository.findByTenantIdAndId(tenantId, command.id())
                .orElseThrow(() -> new NotFoundException("Tipo de documento no encontrado"))
                : new AccountingDocumentTypeEntity();
        entity.setTenantId(tenantId);
        entity.setDocumentCode(command.documentCode());
        entity.setDocumentName(command.documentName());
        entity.setFiscalDomain(command.fiscalDomain());
        entity.setDocumentDirection(command.documentDirection());
        entity.setRequiresSequence(command.requiresSequence() == null || command.requiresSequence());
        entity.setRequiresCounterparty(command.requiresCounterparty() != null && command.requiresCounterparty());
        entity.setActive(command.active() == null || command.active());
        return toDocumentTypeView(accountingDocumentTypeRepository.save(entity));
    }

    private void validateDocumentTypeCommand(UpsertDocumentTypeCommand command) {
        if (command == null) {
            throw new BadRequestException("El tipo documental es obligatorio");
        }
        if (command.documentCode() == null || command.documentCode().isBlank()) {
            throw new BadRequestException("El codigo documental es obligatorio");
        }
        if (command.documentName() == null || command.documentName().isBlank()) {
            throw new BadRequestException("El nombre documental es obligatorio");
        }
        if (command.fiscalDomain() == null) {
            throw new BadRequestException("El dominio fiscal es obligatorio");
        }
        if (command.documentDirection() == null) {
            throw new BadRequestException("La direccion documental es obligatoria");
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "accounting-reference", key = "'sequences:' + #tenantCode")
    public List<SequenceView> listSequences(String tenantCode) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        return documentSequenceRepository.findByTenantIdOrderByDocumentTypeDocumentCodeAscEstablishmentCodeAscEmissionPointCodeAsc(tenantId)
                .stream()
                .map(this::toSequenceView)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = "accounting-reference", allEntries = true)
    public SequenceView upsertSequence(String tenantCode, UpsertSequenceCommand command) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        validateSequenceCommand(command);
        DocumentSequenceEntity entity = command.id() != null
                ? documentSequenceRepository.findByTenantIdAndId(tenantId, command.id())
                .orElseThrow(() -> new NotFoundException("Secuencia no encontrada"))
                : new DocumentSequenceEntity();
        AccountingDocumentTypeEntity documentType = accountingDocumentTypeRepository.findByTenantIdAndId(tenantId, command.documentTypeId())
                .orElseThrow(() -> new NotFoundException("Tipo documental no encontrado"));
        entity.setTenantId(tenantId);
        entity.setDocumentType(documentType);
        entity.setEstablishmentCode(command.establishmentCode());
        entity.setEmissionPointCode(command.emissionPointCode());
        entity.setCurrentNumber(command.currentNumber() != null ? command.currentNumber() : 0L);
        entity.setIncrementStep(command.incrementStep() != null ? command.incrementStep() : 1);
        entity.setActive(command.active() == null || command.active());
        return toSequenceView(documentSequenceRepository.save(entity));
    }

    private void validateSequenceCommand(UpsertSequenceCommand command) {
        if (command == null) {
            throw new BadRequestException("La secuencia tributaria es obligatoria");
        }
        if (command.documentTypeId() == null) {
            throw new BadRequestException("El tipo documental es obligatorio");
        }
        if (command.establishmentCode() == null || command.establishmentCode().isBlank()) {
            throw new BadRequestException("El codigo de establecimiento es obligatorio");
        }
        if (command.emissionPointCode() == null || command.emissionPointCode().isBlank()) {
            throw new BadRequestException("El codigo del punto de emision es obligatorio");
        }
        if (command.currentNumber() != null && command.currentNumber() < 0) {
            throw new BadRequestException("El numero secuencial no puede ser negativo");
        }
        if (command.incrementStep() != null && command.incrementStep() <= 0) {
            throw new BadRequestException("El incremento secuencial debe ser mayor a cero");
        }
    }

    @Transactional(readOnly = true)
    public List<JournalEntryView> listJournalEntries(String tenantCode) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        List<AccountingEntryEntity> entries = accountingEntryRepository.findByTenantIdOrderByCreatedAtDesc(
                tenantId,
                PageRequest.of(0, JOURNAL_ENTRY_LIMIT));
        List<Long> entryIds = entries.stream()
                .map(AccountingEntryEntity::getId)
                .toList();
        Map<Long, List<AccountingEntryLineEntity>> linesByEntryId = new HashMap<>();
        if (!entryIds.isEmpty()) {
            for (AccountingEntryLineEntity line : accountingEntryLineRepository.findByEntryIdInOrderByEntryIdAscLineNumberAsc(entryIds)) {
                linesByEntryId.computeIfAbsent(line.getEntry().getId(), ignored -> new java.util.ArrayList<>()).add(line);
            }
        }
        return entries.stream()
                .map(entry -> new JournalEntryView(
                        entry.getId(),
                        entry.getEntryNumber(),
                        entry.getEntryDate(),
                        entry.getStatus().name(),
                        entry.getSourceModule().name(),
                        entry.getSourceType(),
                        entry.getSourceId(),
                        entry.getDescription(),
                        entry.getExternalReference(),
                        entry.getPostedAt(),
                        linesByEntryId.getOrDefault(entry.getId(), List.of()).stream()
                                .map(this::toJournalLineView)
                                .toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReceivableView> listReceivables(String tenantCode) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        return receivableRepository.findByTenantIdOrderByIssueDateDescCreatedAtDesc(
                        tenantId,
                        PageRequest.of(0, ACCOUNTING_ITEMS_LIMIT))
                .stream()
                .map(this::toReceivableView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PayableView> listPayables(String tenantCode) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        return payableRepository.findByTenantIdOrderByIssueDateDescCreatedAtDesc(
                        tenantId,
                        PageRequest.of(0, ACCOUNTING_ITEMS_LIMIT))
                .stream()
                .map(this::toPayableView)
                .toList();
    }

    @Transactional
    public ReceivableView createManualReceivable(String tenantCode, CreateManualReceivableCommand command) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        BigDecimal amount = amount(command.totalAmount());
        if (amount.signum() <= 0) {
            throw new BadRequestException("El monto de la cuenta por cobrar debe ser mayor a cero");
        }
        long sourceId = System.currentTimeMillis();

        AccountingPostingService.AccountingPostingResult result = accountingPostingService.post(
                new AccountingPostingService.PostAccountingEntryCommand(
                        tenantId,
                        AccountingSourceModule.ACCOUNTING,
                        "MANUAL_RECEIVABLE",
                        sourceId,
                        command.issueDate() != null ? command.issueDate() : LocalDate.now(),
                        command.description(),
                        command.externalReference(),
                        List.of(
                                new AccountingPostingService.PostAccountingEntryLineCommand(
                                        resolveAccountId(tenantId, ACCOUNT_RECEIVABLE), null, amount, BigDecimal.ZERO,
                                        "Registro CxC", command.externalReference()),
                                new AccountingPostingService.PostAccountingEntryLineCommand(
                                        resolveAccountId(tenantId, command.revenueAccountCode() != null ? command.revenueAccountCode() : ACCOUNT_SALES),
                                        null, BigDecimal.ZERO, amount, "Contrapartida CxC", command.externalReference())),
                        new AccountingPostingService.CreateReceivableCommand(
                                command.customerId(),
                                "MANUAL_RECEIVABLE",
                                sourceId,
                                command.issueDate() != null ? command.issueDate() : LocalDate.now(),
                                command.dueDate(),
                                amount),
                        null,
                        null));

        ReceivableEntity receivable = receivableRepository.findByTenantIdAndId(tenantId, result.receivableId())
                .orElseThrow(() -> new NotFoundException("No se pudo crear la cuenta por cobrar"));
        return toReceivableView(receivable);
    }

    @Transactional
    public ReceivableView applyReceivablePayment(String tenantCode, Long receivableId, ApplyPaymentCommand command) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        ReceivableEntity receivable = receivableRepository.findByTenantIdAndId(tenantId, receivableId)
                .orElseThrow(() -> new NotFoundException("Cuenta por cobrar no encontrada"));
        BigDecimal paymentAmount = amount(command.amount());
        if (paymentAmount.signum() <= 0 || paymentAmount.compareTo(amount(receivable.getBalance())) > 0) {
            throw new BadRequestException("Monto de cobro invalido");
        }

        accountingPostingService.post(new AccountingPostingService.PostAccountingEntryCommand(
                tenantId,
                AccountingSourceModule.CASH,
                "RECEIVABLE_PAYMENT",
                receivable.getId(),
                command.paymentDate() != null ? command.paymentDate() : LocalDate.now(),
                "Cobro de cuenta por cobrar " + receivable.getId(),
                command.reference(),
                List.of(
                        new AccountingPostingService.PostAccountingEntryLineCommand(
                                resolveAccountId(tenantId, command.accountCode() != null ? command.accountCode() : ACCOUNT_BANK),
                                null, paymentAmount, BigDecimal.ZERO, "Ingreso por cobro", command.reference()),
                        new AccountingPostingService.PostAccountingEntryLineCommand(
                                resolveAccountId(tenantId, ACCOUNT_RECEIVABLE),
                                null, BigDecimal.ZERO, paymentAmount, "Aplicacion de cobro", command.reference())),
                null,
                null,
                null));

        receivable.setPaidAmount(amount(receivable.getPaidAmount()).add(paymentAmount));
        receivable.setBalance(amount(receivable.getTotalAmount()).subtract(receivable.getPaidAmount()));
        receivable.setStatus(receivable.getBalance().signum() == 0 ? ReceivableStatus.PAID : ReceivableStatus.PARTIALLY_PAID);
        return toReceivableView(receivableRepository.save(receivable));
    }

    @Transactional
    public PayableView applyPayablePayment(String tenantCode, Long payableId, ApplyPaymentCommand command) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        PayableEntity payable = payableRepository.findByTenantIdAndId(tenantId, payableId)
                .orElseThrow(() -> new NotFoundException("Cuenta por pagar no encontrada"));
        BigDecimal paymentAmount = amount(command.amount());
        if (paymentAmount.signum() <= 0 || paymentAmount.compareTo(amount(payable.getBalance())) > 0) {
            throw new BadRequestException("Monto de pago invalido");
        }

        accountingPostingService.post(new AccountingPostingService.PostAccountingEntryCommand(
                tenantId,
                AccountingSourceModule.CASH,
                "PAYABLE_PAYMENT",
                payable.getId(),
                command.paymentDate() != null ? command.paymentDate() : LocalDate.now(),
                "Pago de cuenta por pagar " + payable.getId(),
                command.reference(),
                List.of(
                        new AccountingPostingService.PostAccountingEntryLineCommand(
                                resolveAccountId(tenantId, ACCOUNT_PAYABLE),
                                null, paymentAmount, BigDecimal.ZERO, "Aplicacion de pago", command.reference()),
                        new AccountingPostingService.PostAccountingEntryLineCommand(
                                resolveAccountId(tenantId, command.accountCode() != null ? command.accountCode() : ACCOUNT_BANK),
                                null, BigDecimal.ZERO, paymentAmount, "Salida por pago", command.reference())),
                null,
                null,
                null));

        payable.setPaidAmount(amount(payable.getPaidAmount()).add(paymentAmount));
        payable.setBalance(amount(payable.getTotalAmount()).subtract(payable.getPaidAmount()));
        payable.setStatus(payable.getBalance().signum() == 0 ? PayableStatus.PAID : PayableStatus.PARTIALLY_PAID);
        return toPayableView(payableRepository.save(payable));
    }

    private AccountView toAccountView(AccountCatalogEntity entity) {
        return new AccountView(entity.getId(), entity.getAccountCode(), entity.getAccountName(),
                entity.getParentAccountId(), entity.getAccountType(), entity.getAccountNature(),
                entity.getTreeLevel(), entity.getAllowPosting(), entity.getActive());
    }

    private TaxRuleView toTaxRuleView(TaxRuleEntity entity) {
        return new TaxRuleView(entity.getId(), entity.getTaxCode(), entity.getTaxName(), entity.getTaxScope(),
                entity.getComputationType(), entity.getRate(), entity.getTaxAuthorityCode(),
                entity.getTaxableObjectCode(), entity.getIncludedInPrice(), entity.getActive(),
                entity.getValidFrom(), entity.getValidUntil());
    }

    private DocumentTypeView toDocumentTypeView(AccountingDocumentTypeEntity entity) {
        return new DocumentTypeView(entity.getId(), entity.getDocumentCode(), entity.getDocumentName(),
                entity.getFiscalDomain(), entity.getDocumentDirection(), entity.getRequiresSequence(),
                entity.getRequiresCounterparty(), entity.getActive());
    }

    private SequenceView toSequenceView(DocumentSequenceEntity entity) {
        return new SequenceView(entity.getId(), entity.getDocumentType().getId(), entity.getDocumentType().getDocumentCode(),
                entity.getEstablishmentCode(), entity.getEmissionPointCode(), entity.getCurrentNumber(),
                entity.getIncrementStep(), entity.getActive());
    }

    private JournalEntryLineView toJournalLineView(AccountingEntryLineEntity entity) {
        return new JournalEntryLineView(entity.getLineNumber(), entity.getAccountId(), entity.getTaxRuleId(),
                entity.getDebit(), entity.getCredit(), entity.getDescription(), entity.getReferenceCode());
    }

    private ReceivableView toReceivableView(ReceivableEntity entity) {
        return new ReceivableView(entity.getId(), entity.getCustomerId(), entity.getSourceDocumentType(),
                entity.getSourceDocumentId(), entity.getIssueDate(), entity.getDueDate(), entity.getTotalAmount(),
                entity.getPaidAmount(), entity.getBalance(), entity.getStatus().name(), entity.getAccountingEntryId(),
                entity.getCreatedAt());
    }

    private PayableView toPayableView(PayableEntity entity) {
        return new PayableView(entity.getId(), entity.getSupplierId(), entity.getSourceDocumentType(),
                entity.getSourceDocumentId(), entity.getIssueDate(), entity.getDueDate(), entity.getTotalAmount(),
                entity.getPaidAmount(), entity.getBalance(), entity.getStatus().name(), entity.getAccountingEntryId(),
                entity.getCreatedAt());
    }

    private Long resolveAccountId(Long tenantId, String accountCode) {
        return accountCatalogRepository.findByTenantIdAndAccountCode(tenantId, accountCode)
                .orElseThrow(() -> new NotFoundException("No existe la cuenta " + accountCode))
                .getId();
    }

    private BigDecimal amount(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public record AccountView(
            Long id,
            String accountCode,
            String accountName,
            Long parentAccountId,
            AccountType accountType,
            AccountNature accountNature,
            Integer treeLevel,
            Boolean allowPosting,
            Boolean active) {
    }

    public record UpsertAccountCommand(
            Long id,
            String accountCode,
            String accountName,
            Long parentAccountId,
            AccountType accountType,
            AccountNature accountNature,
            Integer treeLevel,
            Boolean allowPosting,
            Boolean active) {
    }

    public record TaxRuleView(
            Long id,
            String taxCode,
            String taxName,
            TaxScope taxScope,
            TaxComputationType computationType,
            BigDecimal rate,
            String taxAuthorityCode,
            String taxableObjectCode,
            Boolean includedInPrice,
            Boolean active,
            LocalDate validFrom,
            LocalDate validUntil) {
    }

    public record UpsertTaxRuleCommand(
            Long id,
            String taxCode,
            String taxName,
            TaxScope taxScope,
            TaxComputationType computationType,
            BigDecimal rate,
            String taxAuthorityCode,
            String taxableObjectCode,
            Boolean includedInPrice,
            Boolean active,
            LocalDate validFrom,
            LocalDate validUntil) {
    }

    public record ProductTaxRuleView(
            Long id,
            Long productId,
            String productName,
            Long taxRuleId,
            String taxCode,
            String taxName) {
    }

    public record DocumentTypeView(
            Long id,
            String documentCode,
            String documentName,
            FiscalDomain fiscalDomain,
            DocumentDirection documentDirection,
            Boolean requiresSequence,
            Boolean requiresCounterparty,
            Boolean active) {
    }

    public record UpsertDocumentTypeCommand(
            Long id,
            String documentCode,
            String documentName,
            FiscalDomain fiscalDomain,
            DocumentDirection documentDirection,
            Boolean requiresSequence,
            Boolean requiresCounterparty,
            Boolean active) {
    }

    public record SequenceView(
            Long id,
            Long documentTypeId,
            String documentCode,
            String establishmentCode,
            String emissionPointCode,
            Long currentNumber,
            Integer incrementStep,
            Boolean active) {
    }

    public record UpsertSequenceCommand(
            Long id,
            Long documentTypeId,
            String establishmentCode,
            String emissionPointCode,
            Long currentNumber,
            Integer incrementStep,
            Boolean active) {
    }

    public record JournalEntryView(
            Long id,
            String entryNumber,
            LocalDate entryDate,
            String status,
            String sourceModule,
            String sourceType,
            Long sourceId,
            String description,
            String externalReference,
            java.time.LocalDateTime postedAt,
            List<JournalEntryLineView> lines) {
    }

    public record JournalEntryLineView(
            Integer lineNumber,
            Long accountId,
            Long taxRuleId,
            BigDecimal debit,
            BigDecimal credit,
            String description,
            String referenceCode) {
    }

    public record ReceivableView(
            Long id,
            Long customerId,
            String sourceDocumentType,
            Long sourceDocumentId,
            LocalDate issueDate,
            LocalDate dueDate,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            BigDecimal balance,
            String status,
            Long accountingEntryId,
            java.time.LocalDateTime createdAt) {
    }

    public record PayableView(
            Long id,
            Long supplierId,
            String sourceDocumentType,
            Long sourceDocumentId,
            LocalDate issueDate,
            LocalDate dueDate,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            BigDecimal balance,
            String status,
            Long accountingEntryId,
            java.time.LocalDateTime createdAt) {
    }

    public record CreateManualReceivableCommand(
            Long customerId,
            String description,
            String externalReference,
            LocalDate issueDate,
            LocalDate dueDate,
            BigDecimal totalAmount,
            String revenueAccountCode) {
    }

    public record ApplyPaymentCommand(
            BigDecimal amount,
            LocalDate paymentDate,
            String reference,
            String accountCode) {
    }
}
