package com.ucacue.bar.repository;

import com.ucacue.bar.entity.IdempotentRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IdempotentRequestRepository extends JpaRepository<IdempotentRequestEntity, Long> {
    
    @Query("SELECT i FROM IdempotentRequestEntity i WHERE i.key = :key")
    Optional<IdempotentRequestEntity> findByKey(@Param("key") String key);
    
    @Modifying
    @Query("DELETE FROM IdempotentRequestEntity i WHERE i.expiresAt < :now")
    int deleteExpiredEntries(@Param("now") LocalDateTime now);
    
    boolean existsByKey(String key);
}
