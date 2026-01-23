package com.ucacue.bar.repository;

import com.ucacue.bar.entity.MlPredictionEntity;
import com.ucacue.bar.entity.MlPredictionEntity.PredictionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MlPredictionRepository extends JpaRepository<MlPredictionEntity, Long> {

    @Query("select p from MlPredictionEntity p where p.order.id = :orderId and p.predictionType = :type order by p.createdAt desc")
    Optional<MlPredictionEntity> findLatestByOrderAndType(@Param("orderId") Long orderId,
                                                          @Param("type") PredictionType type);
}
