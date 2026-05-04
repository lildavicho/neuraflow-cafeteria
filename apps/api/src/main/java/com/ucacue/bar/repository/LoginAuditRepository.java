package com.ucacue.bar.repository;

import com.ucacue.bar.entity.LoginAuditEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginAuditRepository extends JpaRepository<LoginAuditEntity, Long> {
    Page<LoginAuditEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<LoginAuditEntity> findByEmailOrderByCreatedAtDesc(String email, Pageable pageable);

    List<LoginAuditEntity> findByEmailAndSuccessFalseAndCreatedAtAfter(
            String email, LocalDateTime after);

    long countByEmailAndSuccessFalseAndCreatedAtAfter(String email, LocalDateTime after);

    // ── Tenant-filtered queries ──

    Page<LoginAuditEntity> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    Page<LoginAuditEntity> findByTenantIdAndEmailOrderByCreatedAtDesc(Long tenantId, String email, Pageable pageable);
}
