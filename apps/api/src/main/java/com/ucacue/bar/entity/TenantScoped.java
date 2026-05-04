package com.ucacue.bar.entity;

/**
 * Marker interface for entities that are scoped to a tenant.
 * Every tenant-scoped entity must store and expose its tenant_id.
 */
public interface TenantScoped {
    Long getTenantId();
    void setTenantId(Long tenantId);
}
