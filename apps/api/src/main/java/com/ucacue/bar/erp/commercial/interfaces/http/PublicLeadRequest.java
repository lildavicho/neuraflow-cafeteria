package com.ucacue.bar.erp.commercial.interfaces.http;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicLeadRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 180)
        String fullName,

        @Size(max = 180)
        String companyName,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es valido")
        @Size(max = 255)
        String email,

        @Size(max = 50)
        String phone,

        @Size(max = 120)
        String city,

        @Size(max = 120)
        String businessType,

        @Min(1)
        @Max(10000)
        Integer branchCount,

        @Size(max = 120)
        String interest,

        @Size(max = 2000)
        String message) {
}
