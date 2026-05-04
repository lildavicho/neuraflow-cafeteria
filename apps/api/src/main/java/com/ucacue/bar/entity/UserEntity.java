package com.ucacue.bar.entity;

import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email", unique = true),
    @Index(name = "idx_users_firebase_uid", columnList = "firebase_uid"),
    @Index(name = "idx_users_created_at", columnList = "created_at"),
    @Index(name = "idx_users_tenant", columnList = "tenant_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(length = 15)
    private String phone;

    @Column(length = 20, unique = true)
    private String identification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private TenantEntity tenant;

    @Column(name = "platform_admin", nullable = false)
    private Boolean platformAdmin = false;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(length = 100, unique = true)
    private String firebaseUid;

    @Column(length = 50)
    private String provider;

    @Column(length = 255)
    private String avatarUrl;

    @Column(length = 64)
    private String totpSecret;

    @Column(nullable = false)
    private Boolean totpEnabled = false;

    @Column(nullable = false)
    private Boolean totpVerified = false;

    @Column(nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column
    private LocalDateTime lockedUntil;

    @Column
    private LocalDateTime lastLoginAt;

    @Column(length = 45)
    private String lastLoginIp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum UserRole {
        ADMIN,
        BUYER,
        CASHIER,
        CUSTOMER
    }
}
