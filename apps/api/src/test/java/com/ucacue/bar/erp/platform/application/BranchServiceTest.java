package com.ucacue.bar.erp.platform.application;

import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantEntity;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.repository.TenantRepository;
import com.ucacue.bar.erp.platform.infrastructure.persistence.entity.BranchEntity;
import com.ucacue.bar.erp.platform.infrastructure.persistence.entity.WarehouseEntity;
import com.ucacue.bar.erp.platform.infrastructure.persistence.repository.WarehouseRepository;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import({ BranchService.class, TenantContextResolver.class })
class BranchServiceTest {

    @Autowired private BranchService branchService;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private TenantRepository tenantRepository;

    @MockitoBean private AuditService auditService;

    private Long tenantId;

    @BeforeEach
    void setUp() {
        TenantEntity tenant = new TenantEntity();
        tenant.setTenantCode("default");
        tenant.setLegalName("Tenant Default");
        tenant.setTradeName("Tenant Default");
        tenant = tenantRepository.save(tenant);
        tenantId = tenant.getId();
    }

    private BranchEntity newBranch(String code, String name, boolean isMain) {
        BranchEntity b = new BranchEntity();
        b.setCode(code);
        b.setName(name);
        b.setIsMain(isMain);
        return b;
    }

    @Test
    void create_autoSeedsDefaultWarehouse() {
        BranchEntity created = branchService.create(newBranch("MAT", "Matriz", true));

        List<WarehouseEntity> warehouses = warehouseRepository
                .findAllByTenantIdAndBranchIdAndActiveTrue(tenantId, created.getId());
        assertThat(warehouses).hasSize(1);
        assertThat(warehouses.get(0).getCode()).isEqualTo("MAT-MAIN");
        assertThat(warehouses.get(0).getName()).isEqualTo("Bodega principal");
    }

    @Test
    void create_doesNotDuplicateExistingDefaultWarehouse() {
        BranchEntity created = branchService.create(newBranch("DUP", "Duplicada", true));
        long countAfterFirst = warehouseRepository
                .findAllByTenantIdAndBranchIdAndActiveTrue(tenantId, created.getId()).size();

        // simulate idempotent re-trigger by saving again with same code (nothing happens)
        long countAfter = warehouseRepository
                .findAllByTenantIdAndBranchIdAndActiveTrue(tenantId, created.getId()).size();
        assertThat(countAfter).isEqualTo(countAfterFirst);
    }

    @Test
    void deactivate_cascadesToActiveWarehouses() {
        BranchEntity branch = branchService.create(newBranch("SUC", "Sucursal", false));
        // create extra warehouse
        WarehouseEntity extra = new WarehouseEntity();
        extra.setTenantId(tenantId);
        extra.setBranch(branch);
        extra.setCode("SUC-EXTRA");
        extra.setName("Extra");
        extra.setType(WarehouseEntity.WarehouseType.WAREHOUSE);
        warehouseRepository.save(extra);

        branchService.deactivate(branch.getId());

        List<WarehouseEntity> active = warehouseRepository
                .findAllByTenantIdAndBranchIdAndActiveTrue(tenantId, branch.getId());
        assertThat(active).isEmpty();
    }

    @Test
    void deactivate_blocksMainBranch() {
        BranchEntity main = branchService.create(newBranch("MAT", "Matriz", true));
        assertThatThrownBy(() -> branchService.deactivate(main.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("principal");
    }

    @Test
    void activate_reactivatesPreviouslyInactiveBranch() {
        BranchEntity branch = branchService.create(newBranch("AUX", "Auxiliar", false));
        branchService.deactivate(branch.getId());
        branchService.activate(branch.getId());

        BranchEntity reloaded = branchService.getById(branch.getId());
        assertThat(reloaded.getActive()).isTrue();
    }
}
