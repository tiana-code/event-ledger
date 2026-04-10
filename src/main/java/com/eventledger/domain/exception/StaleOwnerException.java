package com.eventledger.domain.exception;

import java.util.UUID;

public class StaleOwnerException extends RuntimeException {

    public StaleOwnerException(UUID batchId, String ownerId) {
        super("Lock on batch %s is expired, cannot be operated by owner %s".formatted(batchId, ownerId));
    }
}
