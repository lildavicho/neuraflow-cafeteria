package com.ucacue.bar.erp.party.infrastructure.persistence.entity;

import com.ucacue.bar.entity.TenantScoped;
import com.ucacue.bar.erp.party.domain.PartyTypes.IdentifierType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "erp_party_identifiers", indexes = {
        @Index(name = "idx_erp_party_identifiers_party", columnList = "party_id")
})
@Getter
@Setter
@NoArgsConstructor
public class PartyIdentifierEntity implements TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private PartyEntity party;

    @Enumerated(EnumType.STRING)
    @Column(name = "identifier_type", nullable = false, length = 30)
    private IdentifierType identifierType;

    @Column(name = "identifier_value", nullable = false, length = 50)
    private String identifierValue;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = Boolean.FALSE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
