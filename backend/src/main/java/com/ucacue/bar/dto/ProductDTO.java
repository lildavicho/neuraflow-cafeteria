package com.ucacue.bar.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDTO {
    
    private Long id;
    
    @NotNull(message = "La categoría es obligatoria")
    private Long categoryId;
    
    private String categoryName;
    
    @NotBlank(message = "El código es obligatorio")
    @Pattern(regexp = "^[A-Z]{3}[0-9]{3}$", message = "El código debe tener formato XXX000")
    private String code;
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String name;
    
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String description;
    
    private String imageUrl;
    
    @NotBlank(message = "La unidad es obligatoria")
    private String unit;
    
    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    @DecimalMax(value = "9999.99", message = "El precio no puede exceder 9999.99")
    private BigDecimal price;
    
    @DecimalMin(value = "0.00", message = "El precio de compra no puede ser negativo")
    private BigDecimal purchasePrice;
    
    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;
    
    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    @Builder.Default
    private Integer minStock = 5;
    
    private String status;
    
    private LocalDateTime created;
    
    private LocalDateTime updated;
    
    // Computed fields
    private Boolean lowStock;
    private BigDecimal profitMargin;
}
