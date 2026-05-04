package com.ucacue.bar.erp.platform.application;

import com.ucacue.bar.erp.platform.infrastructure.persistence.entity.BranchEntity;
import com.ucacue.bar.erp.platform.infrastructure.persistence.entity.WarehouseEntity;
import com.ucacue.bar.erp.platform.infrastructure.persistence.entity.WarehouseEntity.WarehouseType;
import com.ucacue.bar.erp.platform.infrastructure.persistence.repository.BranchRepository;
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
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final WarehouseRepository warehouseRepository;
    private final TenantContextResolver tenantContextResolver;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<BranchEntity> list(Pageable pageable, boolean activeOnly) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        if (activeOnly) {
            return branchRepository.findByTenantIdAndActiveTrue(tenantId, pageable);
        }
        return branchRepository.findByTenantId(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public List<BranchEntity> listAll() {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return branchRepository.findAllByTenantIdAndActiveTrue(tenantId);
    }

    @Transactional(readOnly = true)
    public BranchEntity getById(Long id) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return branchRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Sucursal no encontrada: " + id));
    }

    @Transactional
    public BranchEntity create(BranchEntity branch) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        branch.setTenantId(tenantId);

        validateCodeUnique(tenantId, branch.getCode(), null);

        if (Boolean.TRUE.equals(branch.getIsMain())) {
            branchRepository.findByTenantIdAndIsMainTrue(tenantId)
                    .ifPresent(existing -> {
                        existing.setIsMain(false);
                        branchRepository.save(existing);
                    });
        }

        BranchEntity saved = branchRepository.save(branch);
        auditService.logAction(null, "CREATE", "BRANCH", saved.getId(), "Sucursal creada: " + saved.getName());
        ensureDefaultWarehouse(saved);
        return saved;
    }

    private void ensureDefaultWarehouse(BranchEntity branch) {
        String defaultCode = buildDefaultWarehouseCode(branch);
        if (warehouseRepository.findByTenantIdAndCodeIgnoreCase(branch.getTenantId(), defaultCode).isPresent()) {
            return;
        }
        WarehouseEntity warehouse = new WarehouseEntity();
        warehouse.setTenantId(branch.getTenantId());
        warehouse.setBranch(branch);
        warehouse.setCode(defaultCode);
        warehouse.setName("Bodega principal");
        warehouse.setType(WarehouseType.STORE);
        warehouseRepository.save(warehouse);
        auditService.logAction(null, "CREATE", "WAREHOUSE", warehouse.getId(),
                "Bodega principal creada para sucursal " + branch.getName());
    }

    private String buildDefaultWarehouseCode(BranchEntity branch) {
        String base = branch.getCode() != null ? branch.getCode().toUpperCase(Locale.ROOT) : "BR" + branch.getId();
        return base + "-MAIN";
    }

    @Transactional
    public BranchEntity update(Long id, BranchEntity updates) {
        BranchEntity existing = getById(id);

        if (updates.getCode() != null && !updates.getCode().equalsIgnoreCase(existing.getCode())) {
            validateCodeUnique(existing.getTenantId(), updates.getCode(), id);
            existing.setCode(updates.getCode());
        }
        if (updates.getName() != null) existing.setName(updates.getName());
        if (updates.getAddress() != null) existing.setAddress(updates.getAddress());
        if (updates.getCity() != null) existing.setCity(updates.getCity());
        if (updates.getProvince() != null) existing.setProvince(updates.getProvince());
        if (updates.getPhone() != null) existing.setPhone(updates.getPhone());
        if (updates.getEmail() != null) existing.setEmail(updates.getEmail());
        if (updates.getSriEstablishmentCode() != null) existing.setSriEstablishmentCode(updates.getSriEstablishmentCode());
        if (updates.getActive() != null) existing.setActive(updates.getActive());

        if (Boolean.TRUE.equals(updates.getIsMain()) && !Boolean.TRUE.equals(existing.getIsMain())) {
            branchRepository.findByTenantIdAndIsMainTrue(existing.getTenantId())
                    .ifPresent(main -> {
                        main.setIsMain(false);
                        branchRepository.save(main);
                    });
            existing.setIsMain(true);
        }

        BranchEntity saved = branchRepository.save(existing);
        auditService.logAction(null, "UPDATE", "BRANCH", saved.getId(), "Sucursal actualizada: " + saved.getName());
        return saved;
    }

    @Transactional
    public void deactivate(Long id) {
        BranchEntity branch = getById(id);
        if (Boolean.TRUE.equals(branch.getIsMain())) {
            throw new BadRequestException("No se puede desactivar la sucursal principal. Marca otra como principal primero.");
        }
        branch.setActive(false);
        branchRepository.save(branch);
        auditService.logAction(null, "DEACTIVATE", "BRANCH", branch.getId(), "Sucursal desactivada: " + branch.getName());

        List<WarehouseEntity> warehouses = warehouseRepository
                .findAllByTenantIdAndBranchIdAndActiveTrue(branch.getTenantId(), branch.getId());
        for (WarehouseEntity warehouse : warehouses) {
            warehouse.setActive(false);
            warehouseRepository.save(warehouse);
            auditService.logAction(null, "DEACTIVATE", "WAREHOUSE", warehouse.getId(),
                    "Bodega desactivada en cascada por sucursal " + branch.getName());
        }
    }

    @Transactional
    public void activate(Long id) {
        BranchEntity branch = getById(id);
        if (Boolean.TRUE.equals(branch.getActive())) {
            return;
        }
        branch.setActive(true);
        branchRepository.save(branch);
        auditService.logAction(null, "ACTIVATE", "BRANCH", branch.getId(), "Sucursal activada: " + branch.getName());
    }

    private void validateCodeUnique(Long tenantId, String code, Long excludeId) {
        long count = branchRepository.countByTenantIdAndCodeExcluding(tenantId, code, excludeId);
        if (count > 0) {
            throw new BadRequestException("Ya existe una sucursal con codigo: " + code);
        }
    }
}
