package com.ucacue.bar.erp.party.infrastructure.persistence.entity;

import com.ucacue.bar.entity.TenantScoped;
import com.ucacue.bar.erp.party.domain.PartyTypes.PartyType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "erp_parties", indexes = {
        @Index(name = "idx_erp_parties_tenant", columnList = "tenant_id"),
        @Index(name = "idx_erp_parties_type", columnList = "tenant_id, party_type")
})
@Getter
@Setter
@NoArgsConstructor
public class PartyEntity implements TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "party_type", nullable = false, length = 20)
    private PartyType partyType = PartyType.PERSON;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "legal_name", length = 200)
    private String legalName;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 30)
    private String mobile;

    @Column(length = 200)
    private String website;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartyIdentifierEntity> identifiers = new ArrayList<>();

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartyAddressEntity> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartyRoleEntity> roles = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
