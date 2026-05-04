package com.ucacue.bar.erp.purchases.infrastructure.persistence.repository;

import com.ucacue.bar.erp.purchases.infrastructure.persistence.entity.PurchaseItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItemEntity, Long> {
}
