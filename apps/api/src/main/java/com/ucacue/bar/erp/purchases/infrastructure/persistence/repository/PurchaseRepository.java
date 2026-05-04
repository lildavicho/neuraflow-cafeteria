package com.ucacue.bar.erp.purchases.infrastructure.persistence.repository;

import com.ucacue.bar.erp.purchases.infrastructure.persistence.entity.PurchaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PurchaseRepository extends JpaRepository<PurchaseEntity, Long> {
    java.util.Optional<PurchaseEntity> findByTenantIdAndId(Long tenantId, Long id);

    List<PurchaseEntity> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    @Query("select p from PurchaseEntity p where p.tenantId = :tenantId " +
           "and p.receivedAt is not null and p.receivedAt between :from and :to " +
           "order by p.receivedAt asc")
    List<PurchaseEntity> findReceivedBetweenByTenantId(@Param("tenantId") Long tenantId,
                                                        @Param("from") LocalDateTime from,
                                                        @Param("to") LocalDateTime to);
}
