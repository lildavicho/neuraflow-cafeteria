package com.ucacue.bar.repository;

import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.entity.UserEntity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.tenant WHERE LOWER(u.email) = LOWER(:email)")
    Optional<UserEntity> findByEmailIgnoreCaseWithTenant(@Param("email") String email);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    Optional<UserEntity> findByIdentification(String identification);

    Optional<UserEntity> findByFirebaseUid(String firebaseUid);
    Optional<UserEntity> findByTenantIdAndEmailIgnoreCase(Long tenantId, String email);
    
    boolean existsByIdentification(String identification);
    
    Page<UserEntity> findByRole(UserRole role, Pageable pageable);
    
    Page<UserEntity> findByActiveTrue(Pageable pageable);

    long countByTenantId(Long tenantId);

    Page<UserEntity> findByTenantId(Long tenantId, Pageable pageable);

    Optional<UserEntity> findByTenantIdAndId(Long tenantId, Long id);
    
    @Query("SELECT u FROM UserEntity u WHERE " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "u.identification LIKE CONCAT('%', :search, '%'))")
    Page<UserEntity> searchUsers(@Param("search") String search, Pageable pageable);
}
