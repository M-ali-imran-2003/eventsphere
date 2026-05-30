package com.example.eventsphere.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DiscountCodeRequest {

    @NotBlank(message = "Promo code string is required (e.g., SUMMER20)")
    private String code;

    @NotNull(message = "Discount percentage is required")
    @DecimalMin(value = "1.0", message = "Discount must be at least 1%")
    @DecimalMax(value = "100.0", message = "Discount cannot exceed 100%")
    private BigDecimal discountValue;

    @NotNull(message = "Maximum uses limit is required")
    @Min(value = 1, message = "Code must be usable at least once")
    private Integer maxUses;

    @NotNull(message = "Expiration date is required")
    @Future(message = "The expiration date must be in the future")
    private LocalDateTime validUntil;

}