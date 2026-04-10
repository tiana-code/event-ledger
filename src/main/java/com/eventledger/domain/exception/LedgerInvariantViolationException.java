package com.eventledger.domain.exception;

public class LedgerInvariantViolationException extends RuntimeException {

    public LedgerInvariantViolationException(String invariant) {
        super("Ledger invariant violated: " + invariant);
    }
}
