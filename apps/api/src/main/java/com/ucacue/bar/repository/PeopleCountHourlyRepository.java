package com.ucacue.bar.repository;

import com.ucacue.bar.entity.PeopleCountHourlyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PeopleCountHourlyRepository extends JpaRepository<PeopleCountHourlyEntity, Long> {
    List<PeopleCountHourlyEntity> findByHourStartBetweenOrderByHourStartAsc(LocalDateTime from, LocalDateTime to);
    java.util.Optional<PeopleCountHourlyEntity> findByTenantIdAndHourStart(Long tenantId, LocalDateTime hourStart);

    // ── Tenant-filtered queries ──

    List<PeopleCountHourlyEntity> findByTenantIdAndHourStartBetweenOrderByHourStartAsc(Long tenantId, LocalDateTime from, LocalDateTime to);
}
