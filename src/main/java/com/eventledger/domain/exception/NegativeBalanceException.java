package com.eventledger.domain.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class NegativeBalanceException extends RuntimeException {

    public NegativeBalanceException(UUID accountId, BigDecimal currentBalance, BigDecimal debitAmount) {
        super("Account %s cannot go negative: balance=%s, debit=%s"
                .formatted(accountId, currentBalance, debitAmount));
    }
}
