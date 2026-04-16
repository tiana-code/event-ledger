package com.eventledger.exception;

public class LockAcquisitionException extends RuntimeException {

    public LockAcquisitionException(String lockKey) {
        super("Failed to acquire lock: " + lockKey);
    }
}
