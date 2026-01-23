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
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "model_metrics", indexes = {
    @Index(name = "idx_model_metrics_name", columnList = "model_name"),
    @Index(name = "idx_model_metrics_training", columnList = "training_date")
})
@Getter
@Setter
@NoArgsConstructor
public class ModelMetricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_name", length = 255, nullable = false)
    private String modelName;

    @Column(name = "metric_type", length = 20, nullable = false)
    private String metricType;

    @Column(name = "metric_value", precision = 10, scale = 4, nullable = false)
    private BigDecimal metricValue;

    @CreationTimestamp
    @Column(name = "training_date", nullable = false, updatable = false)
    private LocalDateTime trainingDate;

    @Column(name = "data_points_used")
    private Integer dataPointsUsed;
}
