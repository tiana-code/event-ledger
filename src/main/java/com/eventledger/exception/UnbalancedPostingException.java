package com.eventledger.exception;

import java.math.BigDecimal;

public class UnbalancedPostingException extends RuntimeException {

    public UnbalancedPostingException(BigDecimal debitTotal, BigDecimal creditTotal) {
        super("Postings do not balance: debits=%s, credits=%s".formatted(debitTotal, creditTotal));
    }
}
