package com.eventledger.domain.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(UUID accountId, BigDecimal available, BigDecimal required) {
        super("Insufficient balance on account %s: available=%s, required=%s"
                .formatted(accountId, available, required));
    }
}
