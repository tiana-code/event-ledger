package com.eventledger.domain.entity;

import com.eventledger.exception.StaleOwnerException;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "batch_locks",
    indexes = {
        @Index(name = "idx_batch_locks_batch_id", columnList = "batch_id", unique = true),
        @Index(name = "idx_batch_locks_expires_at", columnList = "expires_at")
    }
)
public class BatchLock {

    @Id
    @Column(name = "lock_id", nullable = false, updatable = false)
    private UUID lockId;

    @Column(name = "batch_id", nullable = false, updatable = false, unique = true)
    private UUID batchId;

    @Column(name = "owner_id", nullable = false, length = 255)
    private String ownerId;

    @Column(name = "fencing_token", nullable = false)
    private long fencingToken;

    @Column(name = "acquired_at", nullable = false, updatable = false)
    private Instant acquiredAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_ping_at", nullable = false)
    private Instant lastPingAt;

    protected BatchLock() {
    }

    public BatchLock(UUID lockId, UUID batchId, String ownerId, long fencingToken, Instant acquiredAt, Instant expiresAt) {
        Objects.requireNonNull(lockId, "lockId must not be null");
        Objects.requireNonNull(batchId, "batchId must not be null");
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("ownerId must not be blank");
        }
        Objects.requireNonNull(acquiredAt, "acquiredAt must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        if (!expiresAt.isAfter(acquiredAt)) {
            throw new IllegalArgumentException("expiresAt must be after acquiredAt");
        }
        this.lockId = lockId;
        this.batchId = batchId;
        this.ownerId = ownerId;
        this.fencingToken = fencingToken;
        this.acquiredAt = acquiredAt;
        this.expiresAt = expiresAt;
        this.lastPingAt = acquiredAt;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isOwnedBy(String candidateOwnerId) {
        return this.ownerId.equals(candidateOwnerId);
    }

    public void ping(String candidateOwnerId, Instant now, Instant newExpiresAt) {
        if (!isOwnedBy(candidateOwnerId)) {
            throw new IllegalStateException(
                "Cannot renew lock owned by %s from owner %s".formatted(this.ownerId, candidateOwnerId)
            );
        }
        if (isExpired(now)) {
            throw new StaleOwnerException(batchId, candidateOwnerId);
        }
        if (!newExpiresAt.isAfter(now)) {
            throw new IllegalArgumentException("newExpiresAt must be in the future");
        }
        if (newExpiresAt.isBefore(this.expiresAt)) {
            throw new IllegalArgumentException("Cannot shorten lock expiry");
        }
        this.lastPingAt = now;
        this.expiresAt = newExpiresAt;
    }

    public UUID getLockId() {
        return lockId;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public long getFencingToken() {
        return fencingToken;
    }

    public Instant getAcquiredAt() {
        return acquiredAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getLastPingAt() {
        return lastPingAt;
    }
}
