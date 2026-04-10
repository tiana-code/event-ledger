package com.eventledger.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePayoutRequest(
        @NotNull
        UUID accountId,

        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 15, fraction = 4)
        BigDecimal amount,

        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter uppercase ISO currency code")
        String currency,

        @NotBlank @Size(max = 255)
        String idempotencyKey
) {
}
