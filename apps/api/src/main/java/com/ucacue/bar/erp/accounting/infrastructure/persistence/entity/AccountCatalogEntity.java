package com.ucacue.bar.erp.accounting.infrastructure.persistence.entity;

import com.ucacue.bar.erp.accounting.domain.AccountingTypes.AccountNature;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.AccountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "acc_account_catalog", indexes = {
        @Index(name = "idx_acc_account_catalog_tenant_code", columnList = "tenant_id,account_code", unique = true),
        @Index(name = "idx_acc_account_catalog_parent", columnList = "parent_account_id")
})
@Getter
@Setter
@NoArgsConstructor
public class AccountCatalogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "account_code", nullable = false, length = 30)
    private String accountCode;

    @Column(name = "account_name", nullable = false, length = 150)
    private String accountName;

    @Column(name = "parent_account_id")
    private Long parentAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_nature", nullable = false, length = 20)
    private AccountNature accountNature;

    @Column(name = "tree_level", nullable = false)
    private Integer treeLevel = 1;

    @Column(name = "allow_posting", nullable = false)
    private Boolean allowPosting = Boolean.TRUE;

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
