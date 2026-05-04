package com.ucacue.bar.erp.party.application;

import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantEntity;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.repository.TenantRepository;
import com.ucacue.bar.erp.party.application.PartyService.AddressCommand;
import com.ucacue.bar.erp.party.application.PartyService.CreatePartyCommand;
import com.ucacue.bar.erp.party.application.PartyService.IdentifierCommand;
import com.ucacue.bar.erp.party.domain.PartyTypes.AddressType;
import com.ucacue.bar.erp.party.domain.PartyTypes.IdentifierType;
import com.ucacue.bar.erp.party.domain.PartyTypes.PartyType;
import com.ucacue.bar.erp.party.domain.PartyTypes.RoleType;
import com.ucacue.bar.erp.party.infrastructure.persistence.entity.PartyAddressEntity;
import com.ucacue.bar.erp.party.infrastructure.persistence.entity.PartyEntity;
import com.ucacue.bar.erp.party.infrastructure.persistence.entity.PartyIdentifierEntity;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
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
@Import({ PartyService.class, TenantContextResolver.class })
class PartyServiceTest {

    @Autowired private PartyService partyService;
    @Autowired private TenantRepository tenantRepository;

    @MockitoBean private AuditService auditService;

    @BeforeEach
    void setUp() {
        TenantEntity tenant = new TenantEntity();
        tenant.setTenantCode("default");
        tenant.setLegalName("Tenant Default");
        tenant.setTradeName("Tenant Default");
        tenantRepository.save(tenant);
    }

    private PartyEntity createBaseParty(String name, String identifierValue) {
        return partyService.create(new CreatePartyCommand(
                PartyType.PERSON, name, null, name, "Apellido",
                "test@example.com", null, null, null,
                identifierValue != null ? IdentifierType.CEDULA : null,
                identifierValue,
                null, null, null,
                List.of(RoleType.CUSTOMER)
        ));
    }

    // ── identifiers ────────────────────────────────────────────────────

    @Test
    void addIdentifier_marksFirstAsPrimary() {
        PartyEntity p = createBaseParty("Sin ID", null);
        PartyIdentifierEntity added = partyService.addIdentifier(p.getId(),
                new IdentifierCommand(IdentifierType.CEDULA, "0102030405", false));

        assertThat(added.getIsPrimary()).isTrue();
        assertThat(partyService.getById(p.getId()).getIdentifiers()).hasSize(1);
    }

    @Test
    void addIdentifier_secondNonPrimaryStaysSecondary() {
        PartyEntity p = createBaseParty("Con ID", "0102030405");
        partyService.addIdentifier(p.getId(),
                new IdentifierCommand(IdentifierType.RUC, "0102030405001", false));

        List<PartyIdentifierEntity> identifiers = partyService.getById(p.getId()).getIdentifiers();
        assertThat(identifiers).hasSize(2);
        assertThat(identifiers).filteredOn(i -> Boolean.TRUE.equals(i.getIsPrimary())).hasSize(1);
        assertThat(identifiers).filteredOn(i -> i.getIdentifierValue().equals("0102030405001"))
                .first().extracting(PartyIdentifierEntity::getIsPrimary).isEqualTo(false);
    }

    @Test
    void addIdentifier_rejectsDuplicateValue() {
        createBaseParty("Original", "0102030405");
        PartyEntity other = createBaseParty("Otra", null);

        assertThatThrownBy(() -> partyService.addIdentifier(other.getId(),
                new IdentifierCommand(IdentifierType.CEDULA, "0102030405", false)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void setPrimaryIdentifier_swapsPrimaryFlag() {
        PartyEntity p = createBaseParty("Toggle", "0102030405");
        PartyIdentifierEntity second = partyService.addIdentifier(p.getId(),
                new IdentifierCommand(IdentifierType.RUC, "0102030405001", false));

        partyService.setPrimaryIdentifier(p.getId(), second.getId());

        List<PartyIdentifierEntity> all = partyService.getById(p.getId()).getIdentifiers();
        assertThat(all).filteredOn(i -> Boolean.TRUE.equals(i.getIsPrimary()))
                .extracting(PartyIdentifierEntity::getId)
                .containsExactly(second.getId());
    }

    @Test
    void updateIdentifier_setPrimaryDemotesOthers() {
        PartyEntity p = createBaseParty("Promo", "0102030405");
        PartyIdentifierEntity second = partyService.addIdentifier(p.getId(),
                new IdentifierCommand(IdentifierType.RUC, "0102030405001", false));

        partyService.updateIdentifier(p.getId(), second.getId(),
                new IdentifierCommand(IdentifierType.RUC, "0102030405001", true));

        List<PartyIdentifierEntity> all = partyService.getById(p.getId()).getIdentifiers();
        assertThat(all).filteredOn(i -> Boolean.TRUE.equals(i.getIsPrimary())).hasSize(1);
        assertThat(all).filteredOn(i -> i.getId().equals(second.getId()))
                .first().extracting(PartyIdentifierEntity::getIsPrimary).isEqualTo(true);
    }

    @Test
    void removeIdentifier_promotesAnotherWhenPrimaryRemoved() {
        PartyEntity p = createBaseParty("Drop", "0102030405");
        PartyIdentifierEntity second = partyService.addIdentifier(p.getId(),
                new IdentifierCommand(IdentifierType.RUC, "0102030405001", false));

        Long primaryId = partyService.getById(p.getId()).getIdentifiers().stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsPrimary()))
                .findFirst().orElseThrow().getId();

        partyService.removeIdentifier(p.getId(), primaryId);

        List<PartyIdentifierEntity> remaining = partyService.getById(p.getId()).getIdentifiers();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getId()).isEqualTo(second.getId());
        assertThat(remaining.get(0).getIsPrimary()).isTrue();
    }

    @Test
    void removeIdentifier_failsWhenNotOwned() {
        PartyEntity p = createBaseParty("Owner", "0102030405");
        PartyEntity stranger = createBaseParty("Stranger", null);
        PartyIdentifierEntity strangerId = partyService.addIdentifier(stranger.getId(),
                new IdentifierCommand(IdentifierType.CEDULA, "1112223334", false));

        assertThatThrownBy(() -> partyService.removeIdentifier(p.getId(), strangerId.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findByIdentifier_returnsParty() {
        PartyEntity p = createBaseParty("Lookup", "0102030405");
        PartyEntity found = partyService.findByIdentifier(IdentifierType.CEDULA, "0102030405");
        assertThat(found.getId()).isEqualTo(p.getId());
    }

    @Test
    void findByIdentifier_throwsWhenMissing() {
        assertThatThrownBy(() -> partyService.findByIdentifier(IdentifierType.CEDULA, "9999999999"))
                .isInstanceOf(NotFoundException.class);
    }

    // ── addresses ──────────────────────────────────────────────────────

    @Test
    void addAddress_marksFirstAsPrimaryWithDefaultCountry() {
        PartyEntity p = createBaseParty("Addr", "0102030405");
        PartyAddressEntity addr = partyService.addAddress(p.getId(),
                new AddressCommand(AddressType.MAIN, "Av. Solano 123", "Cuenca", "Azuay",
                        "010101", null, false));

        assertThat(addr.getIsPrimary()).isTrue();
        assertThat(addr.getCountry()).isEqualTo("EC");
    }

    @Test
    void addAddress_failsWithoutStreet() {
        PartyEntity p = createBaseParty("Addr", "0102030405");
        assertThatThrownBy(() -> partyService.addAddress(p.getId(),
                new AddressCommand(AddressType.MAIN, "  ", null, null, null, null, false)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void setPrimaryAddress_swapsCorrectly() {
        PartyEntity p = createBaseParty("Addr", "0102030405");
        partyService.addAddress(p.getId(),
                new AddressCommand(AddressType.MAIN, "Calle 1", null, null, null, null, false));
        PartyAddressEntity second = partyService.addAddress(p.getId(),
                new AddressCommand(AddressType.SHIPPING, "Calle 2", null, null, null, null, false));

        partyService.setPrimaryAddress(p.getId(), second.getId());

        List<PartyAddressEntity> all = partyService.getById(p.getId()).getAddresses();
        assertThat(all).filteredOn(a -> Boolean.TRUE.equals(a.getIsPrimary()))
                .extracting(PartyAddressEntity::getId)
                .containsExactly(second.getId());
    }

    @Test
    void updateAddress_changesFieldsAndPromotesPrimary() {
        PartyEntity p = createBaseParty("Addr", "0102030405");
        PartyAddressEntity first = partyService.addAddress(p.getId(),
                new AddressCommand(AddressType.MAIN, "Calle 1", null, null, null, null, false));
        PartyAddressEntity second = partyService.addAddress(p.getId(),
                new AddressCommand(AddressType.SHIPPING, "Calle 2", null, null, null, null, false));

        partyService.updateAddress(p.getId(), second.getId(),
                new AddressCommand(AddressType.SHIPPING, "Calle 2 actualizada", "Cuenca", "Azuay",
                        "010101", "ec", true));

        PartyEntity reloaded = partyService.getById(p.getId());
        PartyAddressEntity updated = reloaded.getAddresses().stream()
                .filter(a -> a.getId().equals(second.getId())).findFirst().orElseThrow();
        assertThat(updated.getStreet()).isEqualTo("Calle 2 actualizada");
        assertThat(updated.getCity()).isEqualTo("Cuenca");
        assertThat(updated.getCountry()).isEqualTo("EC");
        assertThat(updated.getIsPrimary()).isTrue();
        assertThat(reloaded.getAddresses()).filteredOn(a -> a.getId().equals(first.getId()))
                .first().extracting(PartyAddressEntity::getIsPrimary).isEqualTo(false);
    }

    @Test
    void removeAddress_promotesAnotherWhenPrimaryRemoved() {
        PartyEntity p = createBaseParty("Addr", "0102030405");
        PartyAddressEntity first = partyService.addAddress(p.getId(),
                new AddressCommand(AddressType.MAIN, "Calle 1", null, null, null, null, false));
        PartyAddressEntity second = partyService.addAddress(p.getId(),
                new AddressCommand(AddressType.SHIPPING, "Calle 2", null, null, null, null, false));

        partyService.removeAddress(p.getId(), first.getId());

        List<PartyAddressEntity> remaining = partyService.getById(p.getId()).getAddresses();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getId()).isEqualTo(second.getId());
        assertThat(remaining.get(0).getIsPrimary()).isTrue();
    }
}
