package com.eventledger.dto.response;

import com.eventledger.domain.enums.AccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountBalanceResponse(
        UUID accountId,
        UUID ownerId,
        AccountType accountType,
        BigDecimal balance,
        String currency,
        Long version,
        Instant updatedAt
) {
}
