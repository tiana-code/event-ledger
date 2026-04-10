package com.eventledger.dto.request;

import com.eventledger.domain.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateAccountRequest(
    @NotNull UUID ownerId,
    @NotNull AccountType accountType,
    @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter uppercase ISO currency code")
    String currency
) {
}
