package com.ucacue.bar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "push_tokens", indexes = {
    @Index(name = "idx_push_tokens_user", columnList = "user_id"),
    @Index(name = "idx_push_tokens_token", columnList = "fcm_token")
})
@Getter
@Setter
@NoArgsConstructor
public class PushTokenEntity implements TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "fcm_token", nullable = false, unique = true, length = 255)
    private String fcmToken;

    @Column(length = 20)
    private String platform;

    @Column(name = "device_name", length = 255)
    private String deviceName;

    @Column(name = "last_used")
    private LocalDateTime lastUsed;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
