package com.eventledger.domain.entity;

import com.eventledger.domain.enums.EventType;
import com.eventledger.domain.exception.UnbalancedPostingException;
import com.eventledger.domain.valueobject.Money;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "journal_entries",
        indexes = {
                @Index(name = "idx_journal_entries_transaction_id", columnList = "transaction_id")
        }
)
public class JournalEntry {

    @Id
    @Column(name = "journal_entry_id", nullable = false, updatable = false)
    private UUID journalEntryId;

    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "description", length = 1000, updatable = false)
    private String description;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "journal_entry_id")
    private List<LedgerEvent> postings = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected JournalEntry() {
    }

    private JournalEntry(UUID journalEntryId, UUID transactionId, String idempotencyKey,
                         String currency, String description, List<LedgerEvent> postings) {
        this.journalEntryId = journalEntryId;
        this.transactionId = transactionId;
        this.idempotencyKey = idempotencyKey;
        this.currency = currency;
        this.description = description;
        this.postings = new ArrayList<>(postings);
        this.createdAt = Instant.now();
    }

    public static JournalEntry create(UUID transactionId, String idempotencyKey,
                                      Money amount, UUID debitAccountId,
                                      UUID creditAccountId, String description) {
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(debitAccountId, "debitAccountId must not be null");
        Objects.requireNonNull(creditAccountId, "creditAccountId must not be null");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (debitAccountId.equals(creditAccountId)) {
            throw new IllegalArgumentException("debit and credit accounts must be different");
        }

        UUID journalEntryId = UUID.randomUUID();
        String currencyCode = amount.getCurrency().getCurrencyCode();

        LedgerEvent debitEvent = new LedgerEvent(
                UUID.randomUUID(), debitAccountId, transactionId,
                amount, EventType.DEBIT, idempotencyKey,
                journalEntryId, null
        );

        LedgerEvent creditEvent = new LedgerEvent(
                UUID.randomUUID(), creditAccountId, transactionId,
                amount, EventType.CREDIT, idempotencyKey,
                journalEntryId, null
        );

        JournalEntry entry = new JournalEntry(
                journalEntryId, transactionId, idempotencyKey,
                currencyCode, description, List.of(debitEvent, creditEvent)
        );
        entry.validate();
        return entry;
    }

    private void validate() {
        if (postings.size() < 2) {
            throw new IllegalStateException("Journal entry must have at least 2 postings");
        }

        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;

        for (LedgerEvent event : postings) {
            if (!event.getCurrency().equals(this.currency)) {
                throw new IllegalStateException(
                        "Posting currency %s does not match journal currency %s"
                                .formatted(event.getCurrency(), this.currency));
            }
            if (event.getEventType() == EventType.DEBIT) {
                debitTotal = debitTotal.add(event.getAmount());
            } else if (event.getEventType() == EventType.CREDIT) {
                creditTotal = creditTotal.add(event.getAmount());
            }
        }

        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new UnbalancedPostingException(debitTotal, creditTotal);
        }
    }

    public LedgerEvent getDebitEvent() {
        return postings.stream()
                .filter(e -> e.getEventType() == EventType.DEBIT)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No debit event in journal entry"));
    }

    public LedgerEvent getCreditEvent() {
        return postings.stream()
                .filter(e -> e.getEventType() == EventType.CREDIT)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No credit event in journal entry"));
    }

    public UUID getJournalEntryId() {
        return journalEntryId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public List<LedgerEvent> getPostings() {
        return List.copyOf(postings);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
