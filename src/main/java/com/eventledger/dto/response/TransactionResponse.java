package com.eventledger.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID transactionId,
        UUID debitAccountId,
        UUID creditAccountId,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        Instant createdAt
) {
}
