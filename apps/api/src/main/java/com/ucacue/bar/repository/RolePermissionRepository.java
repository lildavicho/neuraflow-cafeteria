package com.ucacue.bar.repository;

import com.ucacue.bar.entity.RolePermissionEntity;
import com.ucacue.bar.entity.UserEntity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, Long> {

    /**
     * Resuelve los permisos efectivos para un rol y tenant:
     *  - si existe al menos una override para ese (role, tenantId), devuelve sólo esas;
     *  - en caso contrario, devuelve las asignaciones default (tenant_id IS NULL).
     */
    @Query("""
        SELECT rp FROM RolePermissionEntity rp
        JOIN FETCH rp.permission p
        WHERE rp.role = :role
          AND (
            rp.tenantId = :tenantId
            OR (
              rp.tenantId IS NULL
              AND NOT EXISTS (
                SELECT 1 FROM RolePermissionEntity rp2
                WHERE rp2.role = :role AND rp2.tenantId = :tenantId
              )
            )
          )
    """)
    List<RolePermissionEntity> findEffective(@Param("role") UserRole role,
                                             @Param("tenantId") Long tenantId);

    List<RolePermissionEntity> findByRoleAndTenantId(UserRole role, Long tenantId);

    @Query("SELECT rp FROM RolePermissionEntity rp JOIN FETCH rp.permission WHERE rp.role = :role AND rp.tenantId IS NULL")
    List<RolePermissionEntity> findDefaultsByRole(@Param("role") UserRole role);

    void deleteByRoleAndTenantId(UserRole role, Long tenantId);
}
