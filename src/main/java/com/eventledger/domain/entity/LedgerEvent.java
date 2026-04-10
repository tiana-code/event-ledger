package com.eventledger.domain.entity;

import com.eventledger.domain.enums.EventType;
import com.eventledger.domain.valueobject.Money;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "ledger_events",
        indexes = {
                @Index(name = "idx_ledger_events_account_id", columnList = "account_id"),
                @Index(name = "idx_ledger_events_transaction_id", columnList = "transaction_id"),
                @Index(name = "idx_ledger_events_created_at", columnList = "created_at"),
                @Index(name = "idx_ledger_events_journal_entry_id", columnList = "journal_entry_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_ledger_events_idempotency_key_event_type", columnNames = {"idempotency_key", "event_type"})
        }
)
public class LedgerEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    @Column(name = "journal_entry_id", updatable = false)
    private UUID journalEntryId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30, updatable = false)
    private EventType eventType;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "metadata", columnDefinition = "TEXT", updatable = false)
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerEvent() {
    }

    public LedgerEvent(
            UUID eventId,
            UUID accountId,
            UUID transactionId,
            Money money,
            EventType eventType,
            String idempotencyKey,
            UUID journalEntryId,
            String metadata
    ) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        Objects.requireNonNull(money, "money must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        if (!money.isPositive()) {
            throw new IllegalArgumentException("Ledger event amount must be positive: " + money.getAmount());
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        this.eventId = eventId;
        this.accountId = accountId;
        this.transactionId = transactionId;
        this.journalEntryId = journalEntryId;
        this.amount = money.getAmount();
        this.eventType = eventType;
        this.idempotencyKey = idempotencyKey;
        this.currency = money.getCurrency().getCurrencyCode();
        this.metadata = metadata;
        this.createdAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getJournalEntryId() {
        return journalEntryId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getCurrency() {
        return currency;
    }

    public String getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
