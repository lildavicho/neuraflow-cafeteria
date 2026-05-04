package com.ucacue.bar.erp.platform.application;

import com.ucacue.bar.erp.platform.infrastructure.persistence.entity.BranchEntity;
import com.ucacue.bar.erp.platform.infrastructure.persistence.entity.WarehouseEntity;
import com.ucacue.bar.erp.platform.infrastructure.persistence.repository.WarehouseRepository;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final BranchService branchService;
    private final TenantContextResolver tenantContextResolver;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<WarehouseEntity> list(Long branchId, Pageable pageable) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        if (branchId != null) {
            return warehouseRepository.findByTenantIdAndBranchId(tenantId, branchId, pageable);
        }
        return warehouseRepository.findByTenantIdAndActiveTrue(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public List<WarehouseEntity> listAll(Long branchId) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        if (branchId != null) {
            return warehouseRepository.findAllByTenantIdAndBranchIdAndActiveTrue(tenantId, branchId);
        }
        return warehouseRepository.findAllByTenantIdAndActiveTrue(tenantId);
    }

    @Transactional(readOnly = true)
    public WarehouseEntity getById(Long id) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return warehouseRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Bodega no encontrada: " + id));
    }

    @Transactional
    public WarehouseEntity create(Long branchId, WarehouseEntity warehouse) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        BranchEntity branch = branchService.getById(branchId);

        warehouse.setTenantId(tenantId);
        warehouse.setBranch(branch);

        validateCodeUnique(tenantId, warehouse.getCode(), null);

        WarehouseEntity saved = warehouseRepository.save(warehouse);
        auditService.logAction(null, "CREATE", "WAREHOUSE", saved.getId(),
                "Bodega creada: " + saved.getName() + " en sucursal " + branch.getName());
        return saved;
    }

    @Transactional
    public WarehouseEntity update(Long id, WarehouseEntity updates) {
        WarehouseEntity existing = getById(id);

        if (updates.getCode() != null && !updates.getCode().equalsIgnoreCase(existing.getCode())) {
            validateCodeUnique(existing.getTenantId(), updates.getCode(), id);
            existing.setCode(updates.getCode());
        }
        if (updates.getName() != null) existing.setName(updates.getName());
        if (updates.getType() != null) existing.setType(updates.getType());
        if (updates.getAddress() != null) existing.setAddress(updates.getAddress());
        if (updates.getActive() != null) existing.setActive(updates.getActive());

        WarehouseEntity saved = warehouseRepository.save(existing);
        auditService.logAction(null, "UPDATE", "WAREHOUSE", saved.getId(), "Bodega actualizada: " + saved.getName());
        return saved;
    }

    @Transactional
    public void deactivate(Long id) {
        WarehouseEntity warehouse = getById(id);
        warehouse.setActive(false);
        warehouseRepository.save(warehouse);
        auditService.logAction(null, "DEACTIVATE", "WAREHOUSE", warehouse.getId(),
                "Bodega desactivada: " + warehouse.getName());
    }

    private void validateCodeUnique(Long tenantId, String code, Long excludeId) {
        long count = warehouseRepository.countByTenantIdAndCodeExcluding(tenantId, code, excludeId);
        if (count > 0) {
            throw new BadRequestException("Ya existe una bodega con codigo: " + code);
        }
    }
}
