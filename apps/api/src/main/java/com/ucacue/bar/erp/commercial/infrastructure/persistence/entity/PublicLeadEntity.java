package com.ucacue.bar.erp.commercial.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.OffsetDateTime;

@Entity
@Table(name = "public_leads", indexes = {
        @Index(name = "idx_public_leads_status_created", columnList = "status,created_at"),
        @Index(name = "idx_public_leads_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
public class PublicLeadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 180)
    private String fullName;

    @Column(name = "company_name", length = 180)
    private String companyName;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(length = 120)
    private String city;

    @Column(name = "business_type", length = 120)
    private String businessType;

    @Column(name = "branch_count")
    private Integer branchCount;

    @Column(length = 120)
    private String interest;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 40)
    private String status = "NEW";

    @Column(nullable = false, length = 80)
    private String source = "LANDING";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
