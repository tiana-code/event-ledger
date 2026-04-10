package com.eventledger.domain.exception;

import java.util.UUID;

public class PayoutNotFoundException extends RuntimeException {

    public PayoutNotFoundException(UUID payoutId) {
        super("Payout not found: " + payoutId);
    }
}
