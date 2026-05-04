package com.ucacue.bar.erp.accounting.infrastructure.persistence.repository;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.EmailTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplateEntity, Long> {

    Optional<EmailTemplateEntity> findFirstByTenantIdAndTemplateCodeAndActiveTrueOrderByVersionDesc(Long tenantId,
                                                                                                    String templateCode);

    Optional<EmailTemplateEntity> findFirstByTenantIdIsNullAndTemplateCodeAndActiveTrueOrderByVersionDesc(String templateCode);
}
