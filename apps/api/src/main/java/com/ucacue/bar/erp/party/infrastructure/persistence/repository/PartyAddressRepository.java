package com.ucacue.bar.erp.party.infrastructure.persistence.repository;

import com.ucacue.bar.erp.party.infrastructure.persistence.entity.PartyAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartyAddressRepository extends JpaRepository<PartyAddressEntity, Long> {
}
