package com.ucacue.bar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotent_requests", indexes = {
    @Index(name = "idx_idempotent_key", columnList = "idempotency_key")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdempotentRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String key;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String response;

    @Column(nullable = false)
    private Integer statusCode;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
}
