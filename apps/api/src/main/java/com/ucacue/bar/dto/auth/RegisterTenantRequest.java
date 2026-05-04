package com.ucacue.bar.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Alta pública de un nuevo negocio + usuario administrador.
 * Crea el tenant, le asigna plan START y retorna tokens para el admin.
 */
@Data
public class RegisterTenantRequest {

    @NotBlank(message = "El código del negocio es obligatorio")
    @Size(min = 3, max = 64)
    @Pattern(regexp = "[a-z0-9][a-z0-9_-]*[a-z0-9]",
            message = "Usa letras minúsculas, números o guiones")
    private String tenantCode;

    @NotBlank(message = "La razón social es obligatoria")
    @Size(min = 3, max = 255)
    private String legalName;

    @Size(max = 255)
    private String tradeName;

    @NotBlank(message = "El nombre del administrador es obligatorio")
    @Size(min = 3, max = 100)
    private String fullName;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @Pattern(regexp = "\\d{10,15}|", message = "Teléfono inválido")
    private String phone;
}
