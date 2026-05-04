package com.ucacue.bar.erp.party.interfaces.http;

import com.ucacue.bar.erp.party.application.PartyService;
import com.ucacue.bar.erp.party.application.PartyService.AddressCommand;
import com.ucacue.bar.erp.party.application.PartyService.CreatePartyCommand;
import com.ucacue.bar.erp.party.application.PartyService.IdentifierCommand;
import com.ucacue.bar.erp.party.application.PartyService.UpdatePartyCommand;
import com.ucacue.bar.erp.party.domain.PartyTypes.*;
import com.ucacue.bar.erp.party.infrastructure.persistence.entity.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/erp/parties")
@RequiredArgsConstructor
@Tag(name = "ERP Parties", description = "Gestion unificada de personas y organizaciones")
public class PartyController {

    private final PartyService partyService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('parties:read')")
    @Operation(summary = "Listar partes con busqueda y filtro por rol")
    public ResponseEntity<Page<PartyResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        RoleType roleFilter = role != null && !role.isBlank() ? RoleType.valueOf(role.toUpperCase()) : null;
        Page<PartyResponse> result = partyService
                .list(search, roleFilter, PageRequest.of(page, size, Sort.by("displayName")))
                .map(PartyResponse::from);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('parties:read')")
    @Operation(summary = "Obtener parte por ID con detalles")
    public ResponseEntity<PartyDetailResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(PartyDetailResponse.from(partyService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('parties:create')")
    @Operation(summary = "Crear nueva parte (persona u organizacion)")
    public ResponseEntity<PartyDetailResponse> create(@Valid @RequestBody CreatePartyRequest request) {
        CreatePartyCommand cmd = new CreatePartyCommand(
                request.partyType(),
                request.displayName(),
                request.legalName(),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.mobile(),
                request.notes(),
                request.identifierType(),
                request.identifierValue(),
                request.street(),
                request.city(),
                request.province(),
                request.roles()
        );
        PartyEntity created = partyService.create(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(PartyDetailResponse.from(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('parties:update')")
    @Operation(summary = "Actualizar parte")
    public ResponseEntity<PartyDetailResponse> update(@PathVariable Long id,
                                                       @Valid @RequestBody UpdatePartyRequest request) {
        UpdatePartyCommand cmd = new UpdatePartyCommand(
                request.displayName(),
                request.legalName(),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.mobile(),
                request.notes(),
                request.active()
        );
        PartyEntity updated = partyService.update(id, cmd);
        return ResponseEntity.ok(PartyDetailResponse.from(updated));
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('parties:roles')")
    @Operation(summary = "Agregar rol a una parte")
    public ResponseEntity<Void> addRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        RoleType roleType = RoleType.valueOf(body.get("role").toUpperCase());
        partyService.addRole(id, roleType);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/roles/{role}")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('parties:roles')")
    @Operation(summary = "Remover rol de una parte")
    public ResponseEntity<Void> removeRole(@PathVariable Long id, @PathVariable String role) {
        partyService.removeRole(id, RoleType.valueOf(role.toUpperCase()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/counts")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('parties:read')")
    @Operation(summary = "Conteos por rol")
    public ResponseEntity<Map<String, Long>> counts() {
        return ResponseEntity.ok(Map.of(
                "customers", partyService.countByRole(RoleType.CUSTOMER),
                "suppliers", partyService.countByRole(RoleType.SUPPLIER),
                "employees", partyService.countByRole(RoleType.EMPLOYEE),
                "students", partyService.countByRole(RoleType.STUDENT)
        ));
    }

    @GetMapping("/by-identifier")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('parties:read')")
    @Operation(summary = "Buscar parte por identificador (cedula/RUC/pasaporte)")
    public ResponseEntity<PartyDetailResponse> findByIdentifier(
            @RequestParam("type") IdentifierType type,
            @RequestParam("value") String value) {
        return ResponseEntity.ok(PartyDetailResponse.from(partyService.findByIdentifier(type, value)));
    }

    // ── Identifier endpoints ──────────────────────────────────────────

    @PostMapping("/{id}/identifiers")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('parties:update')")
    @Operation(summary = "Agregar identificador a una parte")
    public ResponseEntity<IdentifierDto> addIdentifier(
            @PathVariable Long id,
            @Valid @RequestBody IdentifierRequest request) {
        PartyIdentifierEntity created = partyService.addIdentifier(id,
                new IdentifierCommand(request.type(), request.value(), request.isPrimary()));
        return ResponseEntity.status(HttpStatus.CREATED).body(IdentifierDto.from(created));
    }

    @PutMapping("/{id}/identifiers/{identifierId}")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('parties:update')")
    @Operation(summary = "Actualizar identificador de una parte")
    public ResponseEntity<IdentifierDto> updateIdentifier(
            @PathVariable Long id,
            @PathVariable Long identifierId,
            @Valid @RequestBody IdentifierRequest request) {
        PartyIdentifierEntity updated = partyService.updateIdentifier(id, identifierId,
                new IdentifierCommand(request.type(), request.value(), request.isPrimary()));
        return ResponseEntity.ok(IdentifierDto.from(updated));
    }

    @DeleteMapping("/{id}/identifiers/{identifierId}")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('parties:update')")
    @Operation(summary = "Eliminar identificador")
    public ResponseEntity<Void> removeIdentifier(@PathVariable Long id, @PathVariable Long identifierId) {
        partyService.removeIdentifier(id, identifierId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/identifiers/{identifierId}/primary")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('parties:update')")
    @Operation(summary = "Marcar identificador como principal")
    public ResponseEntity<Void> setPrimaryIdentifier(@PathVariable Long id, @PathVariable Long identifierId) {
        partyService.setPrimaryIdentifier(id, identifierId);
        return ResponseEntity.ok().build();
    }

    // ── Address endpoints ─────────────────────────────────────────────

    @PostMapping("/{id}/addresses")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('parties:update')")
    @Operation(summary = "Agregar direccion a una parte")
    public ResponseEntity<AddressDto> addAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {
        PartyAddressEntity created = partyService.addAddress(id, new AddressCommand(
                request.type(), request.street(), request.city(), request.province(),
                request.postalCode(), request.country(), request.isPrimary()));
        return ResponseEntity.status(HttpStatus.CREATED).body(AddressDto.from(created));
    }

    @PutMapping("/{id}/addresses/{addressId}")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('parties:update')")
    @Operation(summary = "Actualizar direccion de una parte")
    public ResponseEntity<AddressDto> updateAddress(
            @PathVariable Long id,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request) {
        PartyAddressEntity updated = partyService.updateAddress(id, addressId, new AddressCommand(
                request.type(), request.street(), request.city(), request.province(),
                request.postalCode(), request.country(), request.isPrimary()));
        return ResponseEntity.ok(AddressDto.from(updated));
    }

    @DeleteMapping("/{id}/addresses/{addressId}")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('parties:update')")
    @Operation(summary = "Eliminar direccion")
    public ResponseEntity<Void> removeAddress(@PathVariable Long id, @PathVariable Long addressId) {
        partyService.removeAddress(id, addressId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/addresses/{addressId}/primary")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('parties:update')")
    @Operation(summary = "Marcar direccion como principal")
    public ResponseEntity<Void> setPrimaryAddress(@PathVariable Long id, @PathVariable Long addressId) {
        partyService.setPrimaryAddress(id, addressId);
        return ResponseEntity.ok().build();
    }

    // ── Request DTOs ──────────────────────────────────────────────────

    public record CreatePartyRequest(
            PartyType partyType,
            @NotBlank @Size(max = 200) String displayName,
            @Size(max = 200) String legalName,
            @Size(max = 100) String firstName,
            @Size(max = 100) String lastName,
            @Size(max = 150) String email,
            @Size(max = 30) String phone,
            @Size(max = 30) String mobile,
            String notes,
            IdentifierType identifierType,
            @Size(max = 50) String identifierValue,
            @Size(max = 300) String street,
            @Size(max = 100) String city,
            @Size(max = 100) String province,
            List<RoleType> roles
    ) {}

    public record UpdatePartyRequest(
            @Size(max = 200) String displayName,
            @Size(max = 200) String legalName,
            @Size(max = 100) String firstName,
            @Size(max = 100) String lastName,
            @Size(max = 150) String email,
            @Size(max = 30) String phone,
            @Size(max = 30) String mobile,
            String notes,
            Boolean active
    ) {}

    public record IdentifierRequest(
            IdentifierType type,
            @NotBlank @Size(max = 50) String value,
            Boolean isPrimary
    ) {}

    public record AddressRequest(
            AddressType type,
            @Size(max = 300) String street,
            @Size(max = 100) String city,
            @Size(max = 100) String province,
            @Size(max = 20) String postalCode,
            @Size(max = 5) String country,
            Boolean isPrimary
    ) {}

    // ── Response DTOs ─────────────────────────────────────────────────

    public record PartyResponse(
            Long id,
            String partyType,
            String displayName,
            String email,
            String phone,
            String primaryIdentifierType,
            String primaryIdentifierValue,
            List<String> roles,
            Boolean active,
            String createdAt
    ) {
        public static PartyResponse from(PartyEntity e) {
            String idType = null;
            String idValue = null;
            if (e.getIdentifiers() != null) {
                var primary = e.getIdentifiers().stream()
                        .filter(i -> Boolean.TRUE.equals(i.getIsPrimary()))
                        .findFirst()
                        .or(() -> e.getIdentifiers().stream().findFirst());
                if (primary.isPresent()) {
                    idType = primary.get().getIdentifierType().name();
                    idValue = primary.get().getIdentifierValue();
                }
            }
            List<String> roleList = e.getRoles() != null
                    ? e.getRoles().stream()
                        .filter(r -> Boolean.TRUE.equals(r.getActive()))
                        .map(r -> r.getRoleType().name())
                        .toList()
                    : List.of();

            return new PartyResponse(
                    e.getId(),
                    e.getPartyType() != null ? e.getPartyType().name() : null,
                    e.getDisplayName(),
                    e.getEmail(),
                    e.getPhone(),
                    idType, idValue,
                    roleList,
                    e.getActive(),
                    e.getCreatedAt() != null ? e.getCreatedAt().toString() : null
            );
        }
    }

    public record PartyDetailResponse(
            Long id,
            String partyType,
            String displayName,
            String legalName,
            String firstName,
            String lastName,
            String email,
            String phone,
            String mobile,
            String website,
            String notes,
            Boolean active,
            List<IdentifierDto> identifiers,
            List<AddressDto> addresses,
            List<String> roles,
            String createdAt,
            String updatedAt
    ) {
        public static PartyDetailResponse from(PartyEntity e) {
            List<IdentifierDto> ids = e.getIdentifiers() != null
                    ? e.getIdentifiers().stream().map(IdentifierDto::from).toList()
                    : List.of();

            List<AddressDto> addrs = e.getAddresses() != null
                    ? e.getAddresses().stream().map(AddressDto::from).toList()
                    : List.of();

            List<String> roleList = e.getRoles() != null
                    ? e.getRoles().stream()
                        .filter(r -> Boolean.TRUE.equals(r.getActive()))
                        .map(r -> r.getRoleType().name())
                        .toList()
                    : List.of();

            return new PartyDetailResponse(
                    e.getId(),
                    e.getPartyType() != null ? e.getPartyType().name() : null,
                    e.getDisplayName(), e.getLegalName(),
                    e.getFirstName(), e.getLastName(),
                    e.getEmail(), e.getPhone(), e.getMobile(),
                    e.getWebsite(), e.getNotes(), e.getActive(),
                    ids, addrs, roleList,
                    e.getCreatedAt() != null ? e.getCreatedAt().toString() : null,
                    e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null
            );
        }
    }

    public record IdentifierDto(Long id, String type, String value, Boolean isPrimary) {
        public static IdentifierDto from(PartyIdentifierEntity i) {
            return new IdentifierDto(i.getId(), i.getIdentifierType().name(),
                    i.getIdentifierValue(), i.getIsPrimary());
        }
    }

    public record AddressDto(Long id, String type, String street, String city,
                              String province, String postalCode, String country, Boolean isPrimary) {
        public static AddressDto from(PartyAddressEntity a) {
            return new AddressDto(a.getId(), a.getAddressType().name(), a.getStreet(),
                    a.getCity(), a.getProvince(), a.getPostalCode(), a.getCountry(), a.getIsPrimary());
        }
    }
}
