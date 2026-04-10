package com.eventledger.dto.request;

import com.eventledger.domain.enums.PayoutStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransitionPayoutRequest(
        @NotNull
        PayoutStatus targetStatus,

        @Size(max = 100)
        String failureCode,

        @Size(max = 1000)
        String failureReason
) {
}
