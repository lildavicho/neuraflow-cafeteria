package com.ucacue.bar.repository;

import com.ucacue.bar.entity.PeopleCountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PeopleCountRepository extends JpaRepository<PeopleCountEntity, Long> {

    @Query("select p from PeopleCountEntity p where p.timestamp between :from and :to order by p.timestamp asc")
    List<PeopleCountEntity> findBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    void deleteByTimestampBefore(LocalDateTime threshold);
}
