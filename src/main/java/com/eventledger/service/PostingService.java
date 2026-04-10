package com.eventledger.service;

import com.eventledger.domain.entity.JournalEntry;
import com.eventledger.domain.valueobject.Money;

import java.util.UUID;

public interface PostingService {

    JournalEntry postTransaction(UUID debitAccountId, UUID creditAccountId,
                                  Money amount, String idempotencyKey, String description);
}
