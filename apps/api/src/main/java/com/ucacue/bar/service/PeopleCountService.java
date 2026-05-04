package com.ucacue.bar.service;

import com.ucacue.bar.entity.PeopleCountEntity;
import com.ucacue.bar.entity.PeopleCountHourlyEntity;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.repository.TenantRepository;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.repository.PeopleCountHourlyRepository;
import com.ucacue.bar.repository.PeopleCountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PeopleCountService {

    private final PeopleCountRepository peopleCountRepository;
    private final PeopleCountHourlyRepository peopleCountHourlyRepository;
    private final TenantRepository tenantRepository;
    private final RealtimeGateway realtimeGateway;
    private final TenantContextResolver tenantContextResolver;

    @Transactional
    public void recordPeopleCount(LocalDateTime timestamp, int count) {
        if (timestamp == null) {
            throw new BadRequestException("timestamp is required");
        }
        if (timestamp.isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new BadRequestException("timestamp cannot be in the future");
        }
        if (count < 0) {
            throw new BadRequestException("count must be non-negative");
        }
        PeopleCountEntity entity = new PeopleCountEntity();
        entity.setTenantId(resolveTenantIdSafe());
        entity.setTimestamp(timestamp);
        entity.setCount(count);
        peopleCountRepository.save(entity);
        try {
            Long tenantId = entity.getTenantId();
            realtimeGateway.people(java.util.Map.of(
                "tenantId", tenantId,
                "timestamp", timestamp,
                "count", count
            ));
        } catch (Exception ex) {
            log.warn("Unable to publish people count update", ex);
        }
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void aggregateLastHour() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusHours(1);
        tenantRepository.findAll().stream()
            .filter(tenant -> Boolean.TRUE.equals(tenant.getActive()))
            .forEach(tenant -> aggregateTenantWindow(tenant.getId(), from, now));
    }

    private void aggregateTenantWindow(Long tenantId, LocalDateTime from, LocalDateTime now) {
        List<PeopleCountEntity> lastHour = peopleCountRepository.findBetweenByTenantId(tenantId, from, now);
        if (lastHour.isEmpty()) {
            peopleCountRepository.deleteByTenantIdAndTimestampBefore(tenantId, now.minusDays(2));
            return;
        }

        double avg = lastHour.stream()
            .mapToInt(PeopleCountEntity::getCount)
            .average()
            .orElse(0.0);

        PeopleCountHourlyEntity hourly = new PeopleCountHourlyEntity();
        LocalDateTime hourStart = now.truncatedTo(ChronoUnit.HOURS);
        hourly.setTenantId(tenantId);
        hourly.setHourStart(hourStart);
        hourly.setAverageCount(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        PeopleCountHourlyEntity existing = peopleCountHourlyRepository.findByTenantIdAndHourStart(tenantId, hourStart).orElse(null);
        if (existing != null) {
            existing.setAverageCount(hourly.getAverageCount());
            peopleCountHourlyRepository.save(existing);
        } else {
            peopleCountHourlyRepository.save(hourly);
        }

        try {
            realtimeGateway.people(java.util.Map.of(
                "type", "hourly",
                "tenantId", tenantId,
                "hourStart", hourly.getHourStart(),
                "avg", hourly.getAverageCount()
            ));
        } catch (Exception ex) {
            log.warn("Unable to publish hourly people count update", ex);
        }

        // Trim raw events older than two days to prevent unbounded growth
        peopleCountRepository.deleteByTenantIdAndTimestampBefore(tenantId, now.minusDays(2));
    }

    private Long resolveTenantIdSafe() {
        return tenantContextResolver.resolveCurrent().getId();
    }
}
