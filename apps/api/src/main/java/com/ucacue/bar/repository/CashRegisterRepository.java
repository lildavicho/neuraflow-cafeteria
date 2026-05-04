package com.ucacue.bar.repository;

import com.ucacue.bar.entity.CashRegisterEntity;
import com.ucacue.bar.entity.CashRegisterEntity.RegisterStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CashRegisterRepository extends JpaRepository<CashRegisterEntity, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<CashRegisterEntity> findTopByStatusOrderByOpenedAtDesc(RegisterStatus status);

    @EntityGraph(attributePaths = "user")
    Optional<CashRegisterEntity> findTopByUserIdAndStatusOrderByOpenedAtDesc(Long userId, RegisterStatus status);

    // ── Tenant-filtered queries ──

    @EntityGraph(attributePaths = "user")
    List<CashRegisterEntity> findByTenantId(Long tenantId);

    @EntityGraph(attributePaths = "user")
    Optional<CashRegisterEntity> findTopByTenantIdAndStatusOrderByOpenedAtDesc(Long tenantId, RegisterStatus status);

    @EntityGraph(attributePaths = "user")
    Optional<CashRegisterEntity> findTopByTenantIdAndUserIdAndStatusOrderByOpenedAtDesc(Long tenantId, Long userId, RegisterStatus status);

    @EntityGraph(attributePaths = "user")
    Optional<CashRegisterEntity> findByTenantIdAndId(Long tenantId, Long id);
}
