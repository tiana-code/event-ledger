package com.eventledger.domain.entity;

import com.eventledger.exception.InvalidStateTransitionException;
import com.eventledger.domain.enums.PayoutStatus;
import com.eventledger.domain.valueobject.Money;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "payouts",
        indexes = {
                @Index(name = "idx_payouts_account_id", columnList = "account_id"),
                @Index(name = "idx_payouts_status_created_at", columnList = "status, created_at")
        }
)
public class Payout {

    private static final Map<PayoutStatus, Set<PayoutStatus>> ALLOWED_TRANSITIONS;

    static {
        ALLOWED_TRANSITIONS = new EnumMap<>(PayoutStatus.class);
        ALLOWED_TRANSITIONS.put(PayoutStatus.PENDING, EnumSet.of(PayoutStatus.PROCESSING, PayoutStatus.FAILED));
        ALLOWED_TRANSITIONS.put(PayoutStatus.PROCESSING, EnumSet.of(PayoutStatus.SENT, PayoutStatus.FAILED));
        ALLOWED_TRANSITIONS.put(PayoutStatus.SENT, EnumSet.of(PayoutStatus.CONFIRMED, PayoutStatus.FAILED));
        ALLOWED_TRANSITIONS.put(PayoutStatus.CONFIRMED, EnumSet.noneOf(PayoutStatus.class));
        ALLOWED_TRANSITIONS.put(PayoutStatus.FAILED, EnumSet.noneOf(PayoutStatus.class));
    }

    @Id
    @Column(name = "payout_id", nullable = false, updatable = false)
    private UUID payoutId;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayoutStatus status;

    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    protected Payout() {
    }

    public Payout(UUID payoutId, UUID accountId, Money money, String idempotencyKey) {
        Objects.requireNonNull(payoutId, "payoutId must not be null");
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(money, "money must not be null");
        if (!money.isPositive()) {
            throw new IllegalArgumentException("Payout amount must be positive");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        this.payoutId = payoutId;
        this.accountId = accountId;
        this.amount = money.getAmount();
        this.currency = money.getCurrency().getCurrencyCode();
        this.idempotencyKey = idempotencyKey;
        this.status = PayoutStatus.PENDING;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void markProcessing() {
        transition(PayoutStatus.PROCESSING);
        this.processingStartedAt = Instant.now();
    }

    public void markSent() {
        transition(PayoutStatus.SENT);
        this.sentAt = Instant.now();
    }

    public void markConfirmed() {
        transition(PayoutStatus.CONFIRMED);
        this.confirmedAt = Instant.now();
    }

    public void fail(String failureCode, String reason) {
        if (failureCode == null || failureCode.isBlank()) {
            throw new IllegalArgumentException("failureCode must not be blank");
        }
        transition(PayoutStatus.FAILED);
        this.failureCode = failureCode;
        this.failureReason = reason;
        this.failedAt = Instant.now();
    }

    private void transition(PayoutStatus newStatus) {
        Set<PayoutStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(this.status, EnumSet.noneOf(PayoutStatus.class));
        if (!allowed.contains(newStatus)) {
            throw new InvalidStateTransitionException(this.status.name(), newStatus.name());
        }
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public UUID getPayoutId() {
        return payoutId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PayoutStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getProcessingStartedAt() {
        return processingStartedAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }
}
