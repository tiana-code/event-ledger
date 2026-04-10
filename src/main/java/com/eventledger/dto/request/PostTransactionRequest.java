package com.eventledger.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record PostTransactionRequest(
        @NotNull
        UUID debitAccountId,

        @NotNull
        UUID creditAccountId,

        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2)
        BigDecimal amount,

        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter uppercase ISO currency code")
        String currency,

        @NotBlank @Size(max = 255)
        String idempotencyKey,

        @Size(max = 1000)
        String description
) {
}
