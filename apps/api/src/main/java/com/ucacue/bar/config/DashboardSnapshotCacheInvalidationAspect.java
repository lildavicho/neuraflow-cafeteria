package com.ucacue.bar.config;

import com.ucacue.bar.service.DashboardSnapshotCacheStore;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
public class DashboardSnapshotCacheInvalidationAspect {

    private static final String DASHBOARD_CACHE = "dashboard-snapshots";

    private final DashboardSnapshotCacheStore snapshotCacheStore;

    @AfterReturning("@annotation(cacheEvict)")
    public void invalidatePersistentDashboardSnapshot(CacheEvict cacheEvict) {
        if (containsDashboardCache(cacheEvict.cacheNames()) || containsDashboardCache(cacheEvict.value())) {
            snapshotCacheStore.invalidateAll();
        }
    }

    private boolean containsDashboardCache(String[] cacheNames) {
        return Arrays.asList(cacheNames).contains(DASHBOARD_CACHE);
    }
}
