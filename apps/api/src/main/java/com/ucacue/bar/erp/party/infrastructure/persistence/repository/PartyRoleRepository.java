package com.ucacue.bar.erp.party.infrastructure.persistence.repository;

import com.ucacue.bar.erp.party.domain.PartyTypes.RoleType;
import com.ucacue.bar.erp.party.infrastructure.persistence.entity.PartyRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartyRoleRepository extends JpaRepository<PartyRoleEntity, Long> {

    List<PartyRoleEntity> findByPartyIdAndActiveTrue(Long partyId);

    Optional<PartyRoleEntity> findByTenantIdAndPartyIdAndRoleType(Long tenantId, Long partyId, RoleType roleType);

    long countByTenantIdAndRoleTypeAndActiveTrue(Long tenantId, RoleType roleType);
}
