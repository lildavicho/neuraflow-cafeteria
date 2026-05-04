package com.ucacue.bar.erp.purchases.application;

import com.ucacue.bar.erp.approval.application.ApprovalStatusChangedEvent;
import com.ucacue.bar.erp.approval.domain.ApprovalTypes.ApprovalStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PurchaseApprovalListener {

    private static final String ENTITY_TYPE = "PURCHASE_ORDER";

    private final PurchaseService purchaseService;

    @EventListener
    public void onApprovalStatusChanged(ApprovalStatusChangedEvent event) {
        if (!ENTITY_TYPE.equals(event.entityType())) {
            return;
        }
        if (event.status() == ApprovalStatus.APPROVED) {
            purchaseService.applyApprovalDecision(event.entityId(), true);
        } else if (event.status() == ApprovalStatus.REJECTED || event.status() == ApprovalStatus.CANCELLED) {
            purchaseService.applyApprovalDecision(event.entityId(), false);
        }
    }
}
