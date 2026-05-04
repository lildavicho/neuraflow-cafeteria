package com.ucacue.bar.erp.party.application;

import com.ucacue.bar.erp.party.domain.PartyTypes.*;
import com.ucacue.bar.erp.party.infrastructure.persistence.entity.*;
import com.ucacue.bar.erp.party.infrastructure.persistence.repository.PartyAddressRepository;
import com.ucacue.bar.erp.party.infrastructure.persistence.repository.PartyIdentifierRepository;
import com.ucacue.bar.erp.party.infrastructure.persistence.repository.PartyRepository;
import com.ucacue.bar.erp.party.infrastructure.persistence.repository.PartyRoleRepository;
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
public class PartyService {

    private final PartyRepository partyRepository;
    private final PartyRoleRepository partyRoleRepository;
    private final PartyIdentifierRepository partyIdentifierRepository;
    private final PartyAddressRepository partyAddressRepository;
    private final TenantContextResolver tenantContextResolver;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<PartyEntity> list(String search, RoleType roleFilter, Pageable pageable) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasRole = roleFilter != null;

        if (hasRole && hasSearch) {
            return partyRepository.searchByRole(tenantId, roleFilter, search.trim(), pageable);
        }
        if (hasRole) {
            return partyRepository.findByTenantIdAndRole(tenantId, roleFilter, pageable);
        }
        if (hasSearch) {
            return partyRepository.search(tenantId, search.trim(), pageable);
        }
        return partyRepository.findByTenantIdAndActiveTrue(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public PartyEntity getById(Long id) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return partyRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Parte no encontrada: " + id));
    }

    @Transactional
    public PartyEntity create(CreatePartyCommand cmd) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();

        PartyEntity party = new PartyEntity();
        party.setTenantId(tenantId);
        party.setPartyType(cmd.partyType() != null ? cmd.partyType() : PartyType.PERSON);
        party.setDisplayName(cmd.displayName());
        party.setLegalName(cmd.legalName());
        party.setFirstName(cmd.firstName());
        party.setLastName(cmd.lastName());
        party.setEmail(cmd.email());
        party.setPhone(cmd.phone());
        party.setMobile(cmd.mobile());
        party.setNotes(cmd.notes());

        // Primary identifier (cedula/RUC)
        if (cmd.identifierType() != null && cmd.identifierValue() != null && !cmd.identifierValue().isBlank()) {
            partyRepository.findByIdentifier(tenantId, cmd.identifierType(), cmd.identifierValue().trim())
                    .ifPresent(existing -> {
                        throw new BadRequestException("Ya existe un registro con " +
                                cmd.identifierType() + ": " + cmd.identifierValue());
                    });

            PartyIdentifierEntity identifier = new PartyIdentifierEntity();
            identifier.setTenantId(tenantId);
            identifier.setParty(party);
            identifier.setIdentifierType(cmd.identifierType());
            identifier.setIdentifierValue(cmd.identifierValue().trim());
            identifier.setIsPrimary(true);
            party.getIdentifiers().add(identifier);
        }

        // Primary address
        if (cmd.street() != null && !cmd.street().isBlank()) {
            PartyAddressEntity address = new PartyAddressEntity();
            address.setTenantId(tenantId);
            address.setParty(party);
            address.setAddressType(AddressType.MAIN);
            address.setStreet(cmd.street());
            address.setCity(cmd.city());
            address.setProvince(cmd.province());
            address.setIsPrimary(true);
            party.getAddresses().add(address);
        }

        // Roles
        if (cmd.roles() != null) {
            for (RoleType roleType : cmd.roles()) {
                PartyRoleEntity role = new PartyRoleEntity();
                role.setTenantId(tenantId);
                role.setParty(party);
                role.setRoleType(roleType);
                party.getRoles().add(role);
            }
        }

        PartyEntity saved = partyRepository.save(party);
        auditService.logAction(null, "CREATE", "PARTY", saved.getId(),
                "Parte creada: " + saved.getDisplayName());
        return saved;
    }

    @Transactional
    public PartyEntity update(Long id, UpdatePartyCommand cmd) {
        PartyEntity existing = getById(id);

        if (cmd.displayName() != null) existing.setDisplayName(cmd.displayName());
        if (cmd.legalName() != null) existing.setLegalName(cmd.legalName());
        if (cmd.firstName() != null) existing.setFirstName(cmd.firstName());
        if (cmd.lastName() != null) existing.setLastName(cmd.lastName());
        if (cmd.email() != null) existing.setEmail(cmd.email());
        if (cmd.phone() != null) existing.setPhone(cmd.phone());
        if (cmd.mobile() != null) existing.setMobile(cmd.mobile());
        if (cmd.notes() != null) existing.setNotes(cmd.notes());
        if (cmd.active() != null) existing.setActive(cmd.active());

        PartyEntity saved = partyRepository.save(existing);
        auditService.logAction(null, "UPDATE", "PARTY", saved.getId(),
                "Parte actualizada: " + saved.getDisplayName());
        return saved;
    }

    @Transactional
    public void addRole(Long partyId, RoleType roleType) {
        PartyEntity party = getById(partyId);
        Long tenantId = party.getTenantId();

        partyRoleRepository.findByTenantIdAndPartyIdAndRoleType(tenantId, partyId, roleType)
                .ifPresent(existing -> {
                    if (Boolean.TRUE.equals(existing.getActive())) {
                        throw new BadRequestException("La parte ya tiene el rol: " + roleType);
                    }
                    existing.setActive(true);
                    partyRoleRepository.save(existing);
                    return;
                });

        PartyRoleEntity role = new PartyRoleEntity();
        role.setTenantId(tenantId);
        role.setParty(party);
        role.setRoleType(roleType);
        partyRoleRepository.save(role);

        auditService.logAction(null, "ADD_ROLE", "PARTY", partyId,
                "Rol " + roleType + " agregado a " + party.getDisplayName());
    }

    @Transactional
    public void removeRole(Long partyId, RoleType roleType) {
        PartyEntity party = getById(partyId);
        partyRoleRepository.findByTenantIdAndPartyIdAndRoleType(party.getTenantId(), partyId, roleType)
                .ifPresent(role -> {
                    role.setActive(false);
                    partyRoleRepository.save(role);
                });
    }

    @Transactional(readOnly = true)
    public long countByRole(RoleType roleType) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return partyRoleRepository.countByTenantIdAndRoleTypeAndActiveTrue(tenantId, roleType);
    }

    @Transactional(readOnly = true)
    public PartyEntity findByIdentifier(IdentifierType type, String value) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        if (type == null || value == null || value.isBlank()) {
            throw new BadRequestException("Tipo y valor de identificador son obligatorios.");
        }
        return partyRepository.findByIdentifier(tenantId, type, value.trim())
                .orElseThrow(() -> new NotFoundException("No existe parte con " + type + ": " + value));
    }

    // ── Identifier CRUD ───────────────────────────────────────────────

    @Transactional
    public PartyIdentifierEntity addIdentifier(Long partyId, IdentifierCommand cmd) {
        PartyEntity party = getById(partyId);
        Long tenantId = party.getTenantId();
        String value = requireValue(cmd.value(), "El valor del identificador es obligatorio.");

        partyRepository.findByIdentifier(tenantId, cmd.type(), value).ifPresent(existing -> {
            throw new BadRequestException("Ya existe un registro con " + cmd.type() + ": " + value);
        });

        boolean makePrimary = Boolean.TRUE.equals(cmd.isPrimary()) || party.getIdentifiers().isEmpty();
        if (makePrimary) {
            party.getIdentifiers().forEach(i -> i.setIsPrimary(false));
        }

        PartyIdentifierEntity identifier = new PartyIdentifierEntity();
        identifier.setTenantId(tenantId);
        identifier.setParty(party);
        identifier.setIdentifierType(cmd.type());
        identifier.setIdentifierValue(value);
        identifier.setIsPrimary(makePrimary);

        PartyIdentifierEntity saved = partyIdentifierRepository.saveAndFlush(identifier);
        party.getIdentifiers().add(saved);
        partyRepository.save(party);

        auditService.logAction(null, "ADD_IDENTIFIER", "PARTY", partyId,
                "Identificador " + cmd.type() + " " + value + " agregado a " + party.getDisplayName());
        return saved;
    }

    @Transactional
    public PartyIdentifierEntity updateIdentifier(Long partyId, Long identifierId, IdentifierCommand cmd) {
        PartyEntity party = getById(partyId);
        PartyIdentifierEntity identifier = findIdentifier(party, identifierId);

        String value = requireValue(cmd.value(), "El valor del identificador es obligatorio.");
        IdentifierType nextType = cmd.type() != null ? cmd.type() : identifier.getIdentifierType();

        boolean valueOrTypeChanged = !value.equalsIgnoreCase(identifier.getIdentifierValue())
                || nextType != identifier.getIdentifierType();
        if (valueOrTypeChanged) {
            partyRepository.findByIdentifier(party.getTenantId(), nextType, value).ifPresent(existing -> {
                if (!existing.getId().equals(party.getId())) {
                    throw new BadRequestException("Ya existe un registro con " + nextType + ": " + value);
                }
            });
        }

        identifier.setIdentifierType(nextType);
        identifier.setIdentifierValue(value);

        if (Boolean.TRUE.equals(cmd.isPrimary()) && !Boolean.TRUE.equals(identifier.getIsPrimary())) {
            party.getIdentifiers().forEach(i -> i.setIsPrimary(false));
            identifier.setIsPrimary(true);
        }

        partyRepository.save(party);
        auditService.logAction(null, "UPDATE_IDENTIFIER", "PARTY", partyId,
                "Identificador #" + identifierId + " actualizado");
        return identifier;
    }

    @Transactional
    public void removeIdentifier(Long partyId, Long identifierId) {
        PartyEntity party = getById(partyId);
        PartyIdentifierEntity identifier = findIdentifier(party, identifierId);
        boolean wasPrimary = Boolean.TRUE.equals(identifier.getIsPrimary());
        party.getIdentifiers().remove(identifier);
        if (wasPrimary && !party.getIdentifiers().isEmpty()) {
            party.getIdentifiers().get(0).setIsPrimary(true);
        }
        partyRepository.save(party);
        auditService.logAction(null, "REMOVE_IDENTIFIER", "PARTY", partyId,
                "Identificador #" + identifierId + " eliminado");
    }

    @Transactional
    public void setPrimaryIdentifier(Long partyId, Long identifierId) {
        PartyEntity party = getById(partyId);
        PartyIdentifierEntity target = findIdentifier(party, identifierId);
        party.getIdentifiers().forEach(i -> i.setIsPrimary(false));
        target.setIsPrimary(true);
        partyRepository.save(party);
        auditService.logAction(null, "SET_PRIMARY_IDENTIFIER", "PARTY", partyId,
                "Identificador #" + identifierId + " marcado como principal");
    }

    // ── Address CRUD ──────────────────────────────────────────────────

    @Transactional
    public PartyAddressEntity addAddress(Long partyId, AddressCommand cmd) {
        PartyEntity party = getById(partyId);
        Long tenantId = party.getTenantId();
        String street = requireValue(cmd.street(), "La calle es obligatoria.");

        boolean makePrimary = Boolean.TRUE.equals(cmd.isPrimary()) || party.getAddresses().isEmpty();
        if (makePrimary) {
            party.getAddresses().forEach(a -> a.setIsPrimary(false));
        }

        PartyAddressEntity address = new PartyAddressEntity();
        address.setTenantId(tenantId);
        address.setParty(party);
        address.setAddressType(cmd.type() != null ? cmd.type() : AddressType.MAIN);
        address.setStreet(street);
        address.setCity(cmd.city());
        address.setProvince(cmd.province());
        address.setPostalCode(cmd.postalCode());
        if (cmd.country() != null && !cmd.country().isBlank()) {
            address.setCountry(cmd.country().trim().toUpperCase());
        }
        address.setIsPrimary(makePrimary);

        PartyAddressEntity saved = partyAddressRepository.saveAndFlush(address);
        party.getAddresses().add(saved);
        partyRepository.save(party);

        auditService.logAction(null, "ADD_ADDRESS", "PARTY", partyId,
                "Direccion agregada a " + party.getDisplayName());
        return saved;
    }

    @Transactional
    public PartyAddressEntity updateAddress(Long partyId, Long addressId, AddressCommand cmd) {
        PartyEntity party = getById(partyId);
        PartyAddressEntity address = findAddress(party, addressId);

        if (cmd.type() != null) address.setAddressType(cmd.type());
        if (cmd.street() != null) address.setStreet(requireValue(cmd.street(), "La calle es obligatoria."));
        if (cmd.city() != null) address.setCity(cmd.city());
        if (cmd.province() != null) address.setProvince(cmd.province());
        if (cmd.postalCode() != null) address.setPostalCode(cmd.postalCode());
        if (cmd.country() != null && !cmd.country().isBlank()) {
            address.setCountry(cmd.country().trim().toUpperCase());
        }

        if (Boolean.TRUE.equals(cmd.isPrimary()) && !Boolean.TRUE.equals(address.getIsPrimary())) {
            party.getAddresses().forEach(a -> a.setIsPrimary(false));
            address.setIsPrimary(true);
        }

        partyRepository.save(party);
        auditService.logAction(null, "UPDATE_ADDRESS", "PARTY", partyId,
                "Direccion #" + addressId + " actualizada");
        return address;
    }

    @Transactional
    public void removeAddress(Long partyId, Long addressId) {
        PartyEntity party = getById(partyId);
        PartyAddressEntity address = findAddress(party, addressId);
        boolean wasPrimary = Boolean.TRUE.equals(address.getIsPrimary());
        party.getAddresses().remove(address);
        if (wasPrimary && !party.getAddresses().isEmpty()) {
            party.getAddresses().get(0).setIsPrimary(true);
        }
        partyRepository.save(party);
        auditService.logAction(null, "REMOVE_ADDRESS", "PARTY", partyId,
                "Direccion #" + addressId + " eliminada");
    }

    @Transactional
    public void setPrimaryAddress(Long partyId, Long addressId) {
        PartyEntity party = getById(partyId);
        PartyAddressEntity target = findAddress(party, addressId);
        party.getAddresses().forEach(a -> a.setIsPrimary(false));
        target.setIsPrimary(true);
        partyRepository.save(party);
        auditService.logAction(null, "SET_PRIMARY_ADDRESS", "PARTY", partyId,
                "Direccion #" + addressId + " marcada como principal");
    }

    // ── helpers ───────────────────────────────────────────────────────

    private PartyIdentifierEntity findIdentifier(PartyEntity party, Long identifierId) {
        return party.getIdentifiers().stream()
                .filter(i -> i.getId().equals(identifierId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Identificador " + identifierId + " no pertenece a la parte " + party.getId()));
    }

    private PartyAddressEntity findAddress(PartyEntity party, Long addressId) {
        return party.getAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Direccion " + addressId + " no pertenece a la parte " + party.getId()));
    }

    private String requireValue(String raw, String message) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new BadRequestException(message);
        }
        return raw.trim();
    }

    // ── Commands ──────────────────────────────────────────────────────

    public record CreatePartyCommand(
            PartyType partyType,
            String displayName,
            String legalName,
            String firstName,
            String lastName,
            String email,
            String phone,
            String mobile,
            String notes,
            IdentifierType identifierType,
            String identifierValue,
            String street,
            String city,
            String province,
            List<RoleType> roles
    ) {}

    public record UpdatePartyCommand(
            String displayName,
            String legalName,
            String firstName,
            String lastName,
            String email,
            String phone,
            String mobile,
            String notes,
            Boolean active
    ) {}

    public record IdentifierCommand(
            IdentifierType type,
            String value,
            Boolean isPrimary
    ) {}

    public record AddressCommand(
            AddressType type,
            String street,
            String city,
            String province,
            String postalCode,
            String country,
            Boolean isPrimary
    ) {}
}
