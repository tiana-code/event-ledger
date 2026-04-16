package com.eventledger.exception;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String idempotencyKey) {
        super("Idempotency conflict for key: " + idempotencyKey);
    }
}
