package com.ucacue.bar.service;

import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantEntity;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardSnapshotWarmupService {

    private final DashboardService dashboardService;
    private final TenantRepository tenantRepository;

    @Value("${app.dashboard.snapshot.precompute-enabled:true}")
    private boolean precomputeEnabled;

    @Scheduled(
            initialDelayString = "${app.dashboard.snapshot.initial-delay-ms:20000}",
            fixedDelayString = "${app.dashboard.snapshot.refresh-delay-ms:45000}")
    public void refreshActiveTenantSnapshots() {
        if (!precomputeEnabled) {
            return;
        }

        for (TenantEntity tenant : tenantRepository.findByActiveTrue()) {
            try {
                dashboardService.refreshSnapshotCache(tenant.getId(), tenant.getTenantCode());
            } catch (Exception ex) {
                log.debug("Dashboard snapshot warmup skipped for tenant {}: {}",
                        tenant.getTenantCode(), ex.getMessage());
            }
        }
    }
}
