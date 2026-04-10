package com.eventledger.dto.response;

import com.eventledger.domain.enums.PayoutStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PayoutResponse(
    UUID payoutId,
    UUID accountId,
    BigDecimal amount,
    String currency,
    PayoutStatus status,
    String idempotencyKey,
    String failureCode,
    String failureReason,
    Instant createdAt,
    Instant updatedAt,
    Instant processingStartedAt,
    Instant sentAt,
    Instant confirmedAt,
    Instant failedAt
) {
}
