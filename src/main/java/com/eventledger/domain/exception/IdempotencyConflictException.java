package com.eventledger.domain.exception;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String idempotencyKey) {
        super("Idempotency conflict for key: " + idempotencyKey);
    }
}
