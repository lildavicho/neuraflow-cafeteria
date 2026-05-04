package com.ucacue.bar.erp.party.infrastructure.persistence.repository;

import com.ucacue.bar.erp.party.infrastructure.persistence.entity.PartyEntity;
import com.ucacue.bar.erp.party.domain.PartyTypes.PartyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartyRepository extends JpaRepository<PartyEntity, Long> {

    Page<PartyEntity> findByTenantIdAndActiveTrue(Long tenantId, Pageable pageable);

    Page<PartyEntity> findByTenantId(Long tenantId, Pageable pageable);

    Optional<PartyEntity> findByIdAndTenantId(Long id, Long tenantId);

    @Query("""
        SELECT p FROM PartyEntity p
        WHERE p.tenantId = :tenantId AND p.active = true
        AND (LOWER(p.displayName) LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(p.email) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<PartyEntity> search(@Param("tenantId") Long tenantId,
                              @Param("q") String query,
                              Pageable pageable);

    @Query("""
        SELECT DISTINCT p FROM PartyEntity p
        JOIN p.roles r
        WHERE p.tenantId = :tenantId AND p.active = true AND r.roleType = :roleType AND r.active = true
        """)
    Page<PartyEntity> findByTenantIdAndRole(@Param("tenantId") Long tenantId,
                                             @Param("roleType") com.ucacue.bar.erp.party.domain.PartyTypes.RoleType roleType,
                                             Pageable pageable);

    @Query("""
        SELECT DISTINCT p FROM PartyEntity p
        JOIN p.roles r
        WHERE p.tenantId = :tenantId AND p.active = true AND r.roleType = :roleType AND r.active = true
        AND (LOWER(p.displayName) LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(p.email) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<PartyEntity> searchByRole(@Param("tenantId") Long tenantId,
                                    @Param("roleType") com.ucacue.bar.erp.party.domain.PartyTypes.RoleType roleType,
                                    @Param("q") String query,
                                    Pageable pageable);

    @Query("""
        SELECT p FROM PartyEntity p
        JOIN p.identifiers i
        WHERE p.tenantId = :tenantId
        AND i.identifierType = :idType
        AND LOWER(i.identifierValue) = LOWER(:idValue)
        """)
    Optional<PartyEntity> findByIdentifier(@Param("tenantId") Long tenantId,
                                            @Param("idType") com.ucacue.bar.erp.party.domain.PartyTypes.IdentifierType idType,
                                            @Param("idValue") String idValue);
}
