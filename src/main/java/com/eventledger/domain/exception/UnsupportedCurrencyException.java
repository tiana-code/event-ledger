package com.eventledger.domain.exception;

public class UnsupportedCurrencyException extends RuntimeException {

    public UnsupportedCurrencyException(String currencyCode) {
        super("Unsupported currency: " + currencyCode);
    }
}
