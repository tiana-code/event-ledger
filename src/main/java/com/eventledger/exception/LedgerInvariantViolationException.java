package com.eventledger.exception;

public class LedgerInvariantViolationException extends RuntimeException {

    public LedgerInvariantViolationException(String invariant) {
        super("Ledger invariant violated: " + invariant);
    }
}
