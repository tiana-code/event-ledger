package com.eventledger.domain.exception;

public class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException(String expected, String actual) {
        super("Currency mismatch: expected=%s, actual=%s".formatted(expected, actual));
    }
}
