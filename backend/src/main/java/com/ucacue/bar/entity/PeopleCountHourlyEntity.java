package com.ucacue.bar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "people_counts_hourly", indexes = {
    @Index(name = "idx_people_counts_hourly_hour_start", columnList = "hour_start")
})
@Getter
@Setter
@NoArgsConstructor
public class PeopleCountHourlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hour_start", nullable = false)
    private LocalDateTime hourStart;

    @Column(name = "avg_count", nullable = false, precision = 10, scale = 2)
    private BigDecimal averageCount;
}
