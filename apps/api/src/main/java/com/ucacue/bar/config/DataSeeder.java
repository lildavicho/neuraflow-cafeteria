package com.ucacue.bar.config;

import com.ucacue.bar.entity.CategoryEntity;
import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.AccountNature;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.AccountType;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.DocumentDirection;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.FiscalDomain;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AccountCatalogEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AccountingDocumentTypeEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.DocumentSequenceEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.AccountCatalogRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.AccountingDocumentTypeRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.DocumentSequenceRepository;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantEntity;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantPlanSubscriptionEntity;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.repository.TenantPlanSubscriptionRepository;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.repository.TenantRepository;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.CommercialPlanCode;
import com.ucacue.bar.repository.CategoryRepository;
import com.ucacue.bar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final TenantRepository tenantRepository;
    private final TenantPlanSubscriptionRepository tenantPlanSubscriptionRepository;
    private final AccountCatalogRepository accountCatalogRepository;
    private final AccountingDocumentTypeRepository accountingDocumentTypeRepository;
    private final DocumentSequenceRepository documentSequenceRepository;

    @Value("${ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Value("${BUYER_EMAIL:}")
    private String buyerEmail;

    @Value("${BUYER_PASSWORD:}")
    private String buyerPassword;

    @Bean
    @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
    public CommandLineRunner seedDefaultUsers() {
        return args -> {
            log.info("Starting data seeding...");

            seedUser(
                    adminEmail,
                    adminPassword,
                    "Administrador UCACUE",
                    UserEntity.UserRole.ADMIN,
                    "0000000000",
                    true);

            seedUser(
                    buyerEmail,
                    buyerPassword,
                    "Comprador Demo",
                    UserEntity.UserRole.BUYER,
                    "0000000001",
                    false);

            TenantEntity tenant = seedDefaultTenant();
            seedDefaultCategories();
            seedAccountingFoundation(tenant);

            log.info("Data seeding completed successfully");
        };
    }

    private void seedUser(String email, String password, String fullName,
                          UserEntity.UserRole role, String identification, boolean required) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            if (required) {
                log.warn("{} seed omitido: configure ADMIN_EMAIL y ADMIN_PASSWORD", role);
            }
            return;
        }

        String normalizedEmail = email.trim();
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            log.info("User {} already exists, skipping...", normalizedEmail);
            return;
        }

        try {
            UserEntity user = new UserEntity();
            user.setEmail(normalizedEmail);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setName(fullName);
            user.setRole(role);
            user.setIdentification(identification);
            user.setActive(true);
            user.setProvider("email");

            userRepository.save(user);
            log.info("Created {} user: {}", role, normalizedEmail);
        } catch (Exception e) {
            log.error("Failed to create user {}: {}", normalizedEmail, e.getMessage());
            if (required) {
                throw new IllegalStateException("No se pudo crear el usuario semilla requerido: " + normalizedEmail, e);
            }
        }
    }

    private void seedDefaultCategories() {
        String[] names = {"Desayunos", "Almuerzos", "Snacks", "Bebidas", "Postres", "Ingredientes"};
        for (String name : names) {
            try {
                var existing = categoryRepository.findByName(name);
                if (existing.isPresent()) {
                    var category = existing.get();
                    if (Boolean.FALSE.equals(category.getActive())) {
                        category.setActive(true);
                        categoryRepository.save(category);
                        log.info("Reactivated category: {}", name);
                    }
                } else {
                    CategoryEntity category = new CategoryEntity();
                    category.setName(name);
                    category.setActive(true);
                    categoryRepository.save(category);
                    log.info("Created category: {}", name);
                }
            } catch (Exception e) {
                log.error("Failed to create category {}: {}", name, e.getMessage());
                throw new IllegalStateException("No se pudo crear la categoria semilla: " + name, e);
            }
        }
    }

    private TenantEntity seedDefaultTenant() {
        TenantEntity tenant = tenantRepository.findByTenantCodeIgnoreCase("default")
                .orElseGet(() -> {
                    TenantEntity entity = new TenantEntity();
                    entity.setTenantCode("default");
                    entity.setLegalName("UCACUE Demo Tenant");
                    entity.setTradeName("UCACUE Demo");
                    entity.setCountryCode("EC");
                    entity.setTimeZone("America/Guayaquil");
                    entity.setCurrencyCode("USD");
                    entity.setActive(Boolean.TRUE);
                    return tenantRepository.save(entity);
                });

        tenantPlanSubscriptionRepository.findFirstByTenantIdAndStatusOrderByStartsAtDesc(
                        tenant.getId(),
                        TenantPlanSubscriptionEntity.SubscriptionStatus.ACTIVE)
                .ifPresentOrElse(subscription -> {
                    if (subscription.getCommercialPlan() != CommercialPlanCode.VISION_AI) {
                        subscription.setStatus(TenantPlanSubscriptionEntity.SubscriptionStatus.CANCELLED);
                        subscription.setEndsAt(LocalDateTime.now());
                        tenantPlanSubscriptionRepository.save(subscription);
                        createTenantSubscription(tenant, CommercialPlanCode.VISION_AI);
                    }
                }, () -> createTenantSubscription(tenant, CommercialPlanCode.VISION_AI));

        return tenant;
    }

    private void createTenantSubscription(TenantEntity tenant, CommercialPlanCode planCode) {
        TenantPlanSubscriptionEntity subscription = new TenantPlanSubscriptionEntity();
        subscription.setTenant(tenant);
        subscription.setCommercialPlan(planCode);
        subscription.setStatus(TenantPlanSubscriptionEntity.SubscriptionStatus.ACTIVE);
        subscription.setStartsAt(LocalDateTime.now());
        tenantPlanSubscriptionRepository.save(subscription);
        log.info("Created tenant {} with commercial plan {}", tenant.getTenantCode(), planCode.name());
    }

    private void seedAccountingFoundation(TenantEntity tenant) {
        ensureAccount(tenant, "1.1.01.01", "Caja general", AccountType.ASSET, AccountNature.DEBIT);
        ensureAccount(tenant, "1.1.01.02", "Bancos", AccountType.ASSET, AccountNature.DEBIT);
        ensureAccount(tenant, "1.1.02.01", "Cuentas por cobrar clientes", AccountType.ASSET, AccountNature.DEBIT);
        ensureAccount(tenant, "1.1.03.01", "Inventario mercaderia", AccountType.ASSET, AccountNature.DEBIT);
        ensureAccount(tenant, "1.1.04.01", "IVA credito tributario", AccountType.ASSET, AccountNature.DEBIT);
        ensureAccount(tenant, "2.1.01.01", "Cuentas por pagar proveedores", AccountType.LIABILITY, AccountNature.CREDIT);
        ensureAccount(tenant, "2.1.02.01", "IVA debito fiscal", AccountType.LIABILITY, AccountNature.CREDIT);
        ensureAccount(tenant, "4.1.01.01", "Ventas mostrador", AccountType.REVENUE, AccountNature.CREDIT);
        ensureAccount(tenant, "5.1.01.01", "Costo de ventas", AccountType.EXPENSE, AccountNature.DEBIT);

        ensureDocumentType(tenant, "SALES_INVOICE", "Factura de venta", FiscalDomain.ACCOUNTING, DocumentDirection.OUTBOUND, false, true);
        ensureDocumentType(tenant, "PURCHASE_INVOICE", "Factura de compra", FiscalDomain.ACCOUNTING, DocumentDirection.INBOUND, false, true);
        AccountingDocumentTypeEntity sriInvoice = ensureDocumentType(
                tenant,
                "SRI_INVOICE",
                "Comprobante electronico factura",
                FiscalDomain.SRI,
                DocumentDirection.OUTBOUND,
                true,
                true);
        AccountingDocumentTypeEntity sriCreditNote = ensureDocumentType(
                tenant,
                "SRI_CREDIT_NOTE",
                "Comprobante electronico nota de credito",
                FiscalDomain.SRI,
                DocumentDirection.OUTBOUND,
                true,
                true);

        ensureSequence(tenant, sriInvoice, "001", "001");
        ensureSequence(tenant, sriCreditNote, "001", "001");
    }

    private void ensureAccount(TenantEntity tenant,
                               String accountCode,
                               String accountName,
                               AccountType accountType,
                               AccountNature accountNature) {
        accountCatalogRepository.findByTenantIdAndAccountCode(tenant.getId(), accountCode)
                .orElseGet(() -> {
                    AccountCatalogEntity account = new AccountCatalogEntity();
                    account.setTenantId(tenant.getId());
                    account.setAccountCode(accountCode);
                    account.setAccountName(accountName);
                    account.setAccountType(accountType);
                    account.setAccountNature(accountNature);
                    account.setTreeLevel(4);
                    account.setAllowPosting(Boolean.TRUE);
                    account.setActive(Boolean.TRUE);
                    return accountCatalogRepository.save(account);
                });
    }

    private AccountingDocumentTypeEntity ensureDocumentType(TenantEntity tenant,
                                                            String documentCode,
                                                            String documentName,
                                                            FiscalDomain fiscalDomain,
                                                            DocumentDirection documentDirection,
                                                            boolean requiresSequence,
                                                            boolean requiresCounterparty) {
        return accountingDocumentTypeRepository.findByTenantIdAndDocumentCode(tenant.getId(), documentCode)
                .orElseGet(() -> {
                    AccountingDocumentTypeEntity documentType = new AccountingDocumentTypeEntity();
                    documentType.setTenantId(tenant.getId());
                    documentType.setDocumentCode(documentCode);
                    documentType.setDocumentName(documentName);
                    documentType.setFiscalDomain(fiscalDomain);
                    documentType.setDocumentDirection(documentDirection);
                    documentType.setRequiresSequence(requiresSequence);
                    documentType.setRequiresCounterparty(requiresCounterparty);
                    documentType.setActive(Boolean.TRUE);
                    return accountingDocumentTypeRepository.save(documentType);
                });
    }

    private void ensureSequence(TenantEntity tenant,
                                AccountingDocumentTypeEntity documentType,
                                String establishmentCode,
                                String emissionPointCode) {
        documentSequenceRepository
                .findByTenantIdAndDocumentTypeDocumentCodeAndActiveTrue(tenant.getId(), documentType.getDocumentCode())
                .orElseGet(() -> {
                    DocumentSequenceEntity sequence = new DocumentSequenceEntity();
                    sequence.setTenantId(tenant.getId());
                    sequence.setDocumentType(documentType);
                    sequence.setEstablishmentCode(establishmentCode);
                    sequence.setEmissionPointCode(emissionPointCode);
                    sequence.setCurrentNumber(0L);
                    sequence.setIncrementStep(1);
                    sequence.setActive(Boolean.TRUE);
                    return documentSequenceRepository.save(sequence);
                });
    }
}
