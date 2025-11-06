package com.ucacue.bar.service;

import com.ucacue.bar.entity.PeopleCountEntity;
import com.ucacue.bar.entity.PeopleCountHourlyEntity;
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
    private final RealtimeGateway realtimeGateway;

    @Transactional
    public void recordPeopleCount(LocalDateTime timestamp, int count) {
        PeopleCountEntity entity = new PeopleCountEntity();
        entity.setTimestamp(timestamp);
        entity.setCount(count);
        peopleCountRepository.save(entity);
        realtimeGateway.people(java.util.Map.of(
            "timestamp", timestamp,
            "count", count
        ));
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void aggregateLastHour() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusHours(1);
        List<PeopleCountEntity> lastHour = peopleCountRepository.findBetween(from, now);
        if (lastHour.isEmpty()) {
            return;
        }

        double avg = lastHour.stream()
            .mapToInt(PeopleCountEntity::getCount)
            .average()
            .orElse(0.0);

        PeopleCountHourlyEntity hourly = new PeopleCountHourlyEntity();
        hourly.setHourStart(now.truncatedTo(ChronoUnit.HOURS));
        hourly.setAverageCount(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        peopleCountHourlyRepository.save(hourly);

        realtimeGateway.people(java.util.Map.of(
            "type", "hourly",
            "hourStart", hourly.getHourStart(),
            "avg", hourly.getAverageCount()
        ));

        // Trim raw events older than a day to prevent unbounded growth
        peopleCountRepository.deleteByTimestampBefore(now.minusDays(2));
    }
}
