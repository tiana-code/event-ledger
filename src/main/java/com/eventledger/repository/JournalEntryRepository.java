package com.eventledger.repository;

import com.eventledger.domain.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    Optional<JournalEntry> findByIdempotencyKey(String idempotencyKey);

    List<JournalEntry> findByTransactionId(UUID transactionId);
}
