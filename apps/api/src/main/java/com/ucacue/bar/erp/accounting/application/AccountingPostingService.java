package com.ucacue.bar.erp.accounting.application;

import com.ucacue.bar.erp.accounting.domain.AccountingTypes.AccountingEntryStatus;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.AccountingSourceModule;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.PayableStatus;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.ReceivableStatus;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxDocumentStatus;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AccountingDocumentTypeEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AccountingEntryEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AccountingEntryLineEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.DocumentSequenceEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.PayableEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.ReceivableEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentTaxLineEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.AccountingEntryLineRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.AccountingEntryRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.DocumentSequenceRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.PayableRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.ReceivableRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentTaxLineRepository;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class AccountingPostingService {

    private static final DateTimeFormatter ENTRY_NUMBER_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final AtomicLong ENTRY_SEQUENCE = new AtomicLong();

    private final AccountingEntryRepository accountingEntryRepository;
    private final AccountingEntryLineRepository accountingEntryLineRepository;
    private final DocumentSequenceRepository documentSequenceRepository;
    private final ReceivableRepository receivableRepository;
    private final PayableRepository payableRepository;
    private final TaxDocumentRepository taxDocumentRepository;
    private final TaxDocumentTaxLineRepository taxDocumentTaxLineRepository;

    @Transactional
    public AccountingPostingResult post(PostAccountingEntryCommand command) {
        validate(command);

        AccountingEntryEntity entry = new AccountingEntryEntity();
        entry.setTenantId(command.tenantId());
        entry.setEntryNumber(nextEntryNumber(command.sourceModule()));
        entry.setEntryDate(command.entryDate() != null ? command.entryDate() : LocalDate.now());
        entry.setStatus(AccountingEntryStatus.POSTED);
        entry.setSourceModule(command.sourceModule());
        entry.setSourceType(command.sourceType());
        entry.setSourceId(command.sourceId());
        entry.setDescription(command.description());
        entry.setExternalReference(command.externalReference());
        entry.setPostedAt(LocalDateTime.now());
        AccountingEntryEntity savedEntry = accountingEntryRepository.save(entry);

        List<AccountingEntryLineEntity> lines = new ArrayList<>();
        int lineNumber = 1;
        for (PostAccountingEntryLineCommand item : command.lines()) {
            AccountingEntryLineEntity line = new AccountingEntryLineEntity();
            line.setEntry(savedEntry);
            line.setLineNumber(lineNumber++);
            line.setAccountId(item.accountId());
            line.setTaxRuleId(item.taxRuleId());
            line.setDebit(amount(item.debit()));
            line.setCredit(amount(item.credit()));
            line.setDescription(item.description());
            line.setReferenceCode(item.referenceCode());
            lines.add(line);
        }
        accountingEntryLineRepository.saveAll(lines);

        ReceivableEntity receivable = buildReceivable(command, savedEntry);
        if (receivable != null) {
            receivableRepository.save(receivable);
        }

        PayableEntity payable = buildPayable(command, savedEntry);
        if (payable != null) {
            payableRepository.save(payable);
        }

        TaxDocumentEntity taxDocument = buildTaxDocument(command, savedEntry);
        if (taxDocument != null) {
            TaxDocumentEntity savedTaxDocument = taxDocumentRepository.save(taxDocument);
            saveTaxLines(savedTaxDocument, command.taxDocument());
            taxDocument = savedTaxDocument;
        }

        return new AccountingPostingResult(
                savedEntry.getId(),
                savedEntry.getEntryNumber(),
                receivable != null ? receivable.getId() : null,
                payable != null ? payable.getId() : null,
                taxDocument != null ? taxDocument.getId() : null
        );
    }

    private ReceivableEntity buildReceivable(PostAccountingEntryCommand command, AccountingEntryEntity entry) {
        if (command.receivable() == null) {
            return null;
        }
        ReceivableEntity receivable = new ReceivableEntity();
        receivable.setTenantId(command.tenantId());
        receivable.setCustomerId(command.receivable().customerId());
        receivable.setSourceDocumentType(command.receivable().sourceDocumentType());
        receivable.setSourceDocumentId(command.receivable().sourceDocumentId());
        receivable.setIssueDate(command.receivable().issueDate() != null ? command.receivable().issueDate() : entry.getEntryDate());
        receivable.setDueDate(command.receivable().dueDate());
        receivable.setTotalAmount(amount(command.receivable().amount()));
        receivable.setPaidAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        receivable.setBalance(amount(command.receivable().amount()));
        receivable.setStatus(ReceivableStatus.OPEN);
        receivable.setAccountingEntryId(entry.getId());
        return receivable;
    }

    private PayableEntity buildPayable(PostAccountingEntryCommand command, AccountingEntryEntity entry) {
        if (command.payable() == null) {
            return null;
        }
        PayableEntity payable = new PayableEntity();
        payable.setTenantId(command.tenantId());
        payable.setSupplierId(command.payable().supplierId());
        payable.setSourceDocumentType(command.payable().sourceDocumentType());
        payable.setSourceDocumentId(command.payable().sourceDocumentId());
        payable.setIssueDate(command.payable().issueDate() != null ? command.payable().issueDate() : entry.getEntryDate());
        payable.setDueDate(command.payable().dueDate());
        payable.setTotalAmount(amount(command.payable().amount()));
        payable.setPaidAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        payable.setBalance(amount(command.payable().amount()));
        payable.setStatus(PayableStatus.OPEN);
        payable.setAccountingEntryId(entry.getId());
        return payable;
    }

    private TaxDocumentEntity buildTaxDocument(PostAccountingEntryCommand command, AccountingEntryEntity entry) {
        if (command.taxDocument() == null) {
            return null;
        }
        CreateTaxDocumentCommand draft = command.taxDocument();
        TaxDocumentEntity taxDocument = new TaxDocumentEntity();
        taxDocument.setTenantId(command.tenantId());
        taxDocument.setSourceModule(command.sourceModule());
        taxDocument.setSourceType(command.sourceType());
        taxDocument.setSourceId(command.sourceId());
        taxDocument.setDocumentType(draft.documentType());
        if (Boolean.TRUE.equals(draft.documentType().getRequiresSequence()) && draft.sequenceId() == null) {
            throw new NotFoundException("No existe una secuencia tributaria activa para el documento solicitado");
        }
        if (draft.sequenceId() != null) {
            taxDocument.setSequence(reserveNextSequence(command.tenantId(), draft.sequenceId(), taxDocument));
        }
        taxDocument.setEnvironmentCode(draft.environmentCode());
        taxDocument.setStatus(TaxDocumentStatus.PENDING_VALIDATION);
        taxDocument.setIssueDate(draft.issueDate() != null ? draft.issueDate() : entry.getEntryDate());
        taxDocument.setBuyerIdentification(draft.buyerIdentification());
        taxDocument.setBuyerName(draft.buyerName());
        taxDocument.setSubtotalAmount(amount(draft.subtotalAmount()));
        taxDocument.setTaxAmount(amount(draft.taxAmount()));
        taxDocument.setTotalAmount(amount(draft.totalAmount()));
        taxDocument.setXmlPayload(draft.xmlPayload());
        taxDocument.setValidationErrors(draft.validationErrors());
        return taxDocument;
    }

    private void saveTaxLines(TaxDocumentEntity taxDocument, CreateTaxDocumentCommand taxDocumentCommand) {
        if (taxDocumentCommand == null || taxDocumentCommand.taxLines() == null || taxDocumentCommand.taxLines().isEmpty()) {
            return;
        }
        List<TaxDocumentTaxLineEntity> taxLines = new ArrayList<>();
        for (CreateTaxDocumentTaxLineCommand item : taxDocumentCommand.taxLines()) {
            TaxDocumentTaxLineEntity taxLine = new TaxDocumentTaxLineEntity();
            taxLine.setTaxDocument(taxDocument);
            taxLine.setTaxRuleId(item.taxRuleId());
            taxLine.setTaxableBase(amount(item.taxableBase()));
            taxLine.setTaxAmount(amount(item.taxAmount()));
            taxLine.setPercentage(item.percentage() != null ? item.percentage() : BigDecimal.ZERO);
            taxLines.add(taxLine);
        }
        taxDocumentTaxLineRepository.saveAll(taxLines);
    }

    private DocumentSequenceEntity reserveNextSequence(Long tenantId, Long sequenceId, TaxDocumentEntity taxDocument) {
        DocumentSequenceEntity sequence = documentSequenceRepository.findWithLockByTenantIdAndId(tenantId, sequenceId)
                .orElseThrow(() -> new NotFoundException("No existe la secuencia tributaria solicitada"));
        if (sequence.getIncrementStep() == null || sequence.getIncrementStep() <= 0) {
            throw new BadRequestException("La secuencia tributaria tiene un incremento invalido");
        }
        if (sequence.getCurrentNumber() == null || sequence.getCurrentNumber() < 0) {
            throw new BadRequestException("La secuencia tributaria tiene un numero actual invalido");
        }
        long nextNumber = sequence.getCurrentNumber() + sequence.getIncrementStep();
        sequence.setCurrentNumber(nextNumber);
        documentSequenceRepository.save(sequence);
        taxDocument.setEstablishmentCode(sequence.getEstablishmentCode());
        taxDocument.setEmissionPointCode(sequence.getEmissionPointCode());
        taxDocument.setSequentialNumber(String.format("%09d", nextNumber));
        return sequence;
    }

    private void validate(PostAccountingEntryCommand command) {
        if (command.tenantId() == null) {
            throw new BadRequestException("tenantId es obligatorio");
        }
        if (command.sourceModule() == null) {
            throw new BadRequestException("sourceModule es obligatorio");
        }
        if (command.sourceType() == null || command.sourceType().isBlank()) {
            throw new BadRequestException("sourceType es obligatorio");
        }
        if (command.sourceId() == null) {
            throw new BadRequestException("sourceId es obligatorio");
        }
        if (command.description() == null || command.description().isBlank()) {
            throw new BadRequestException("La descripcion del asiento es obligatoria");
        }
        if (command.lines() == null || command.lines().size() < 2) {
            throw new BadRequestException("El asiento requiere al menos dos lineas");
        }

        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        for (PostAccountingEntryLineCommand line : command.lines()) {
            if (line.accountId() == null) {
                throw new BadRequestException("Cada linea debe tener una cuenta contable");
            }
            if (amount(line.debit()).signum() < 0 || amount(line.credit()).signum() < 0) {
                throw new BadRequestException("Los valores de debito y credito no pueden ser negativos");
            }
            debit = debit.add(amount(line.debit()));
            credit = credit.add(amount(line.credit()));
            if (amount(line.debit()).signum() == 0 && amount(line.credit()).signum() == 0) {
                throw new BadRequestException("Cada linea debe tener debito o credito");
            }
        }

        if (debit.compareTo(credit) != 0) {
            throw new BadRequestException("El asiento contable no esta balanceado");
        }
    }

    private BigDecimal amount(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private String nextEntryNumber(AccountingSourceModule sourceModule) {
        long sequence = ENTRY_SEQUENCE.updateAndGet(current -> current >= 999_999 ? 1 : current + 1);
        return sourceModule.name() + "-" + LocalDateTime.now().format(ENTRY_NUMBER_FORMAT) + "-" + String.format("%06d", sequence);
    }

    public record PostAccountingEntryCommand(
            Long tenantId,
            AccountingSourceModule sourceModule,
            String sourceType,
            Long sourceId,
            LocalDate entryDate,
            String description,
            String externalReference,
            List<PostAccountingEntryLineCommand> lines,
            CreateReceivableCommand receivable,
            CreatePayableCommand payable,
            CreateTaxDocumentCommand taxDocument) {
    }

    public record PostAccountingEntryLineCommand(
            Long accountId,
            Long taxRuleId,
            BigDecimal debit,
            BigDecimal credit,
            String description,
            String referenceCode) {
    }

    public record CreateReceivableCommand(
            Long customerId,
            String sourceDocumentType,
            Long sourceDocumentId,
            LocalDate issueDate,
            LocalDate dueDate,
            BigDecimal amount) {
    }

    public record CreatePayableCommand(
            Long supplierId,
            String sourceDocumentType,
            Long sourceDocumentId,
            LocalDate issueDate,
            LocalDate dueDate,
            BigDecimal amount) {
    }

    public record CreateTaxDocumentCommand(
            AccountingDocumentTypeEntity documentType,
            Long sequenceId,
            String environmentCode,
            LocalDate issueDate,
            String buyerIdentification,
            String buyerName,
            BigDecimal subtotalAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String xmlPayload,
            String validationErrors,
            List<CreateTaxDocumentTaxLineCommand> taxLines) {
    }

    public record CreateTaxDocumentTaxLineCommand(
            Long taxRuleId,
            BigDecimal taxableBase,
            BigDecimal taxAmount,
            BigDecimal percentage) {
    }

    public record AccountingPostingResult(
            Long accountingEntryId,
            String entryNumber,
            Long receivableId,
            Long payableId,
            Long taxDocumentId) {
    }
}
