package com.eventledger.domain;

import com.eventledger.domain.entity.BatchLock;
import com.eventledger.exception.StaleOwnerException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BatchLockTest {

    private BatchLock newLock(Instant expiresAt) {
        Instant now = Instant.now();
        return new BatchLock(UUID.randomUUID(), UUID.randomUUID(), "worker-1", 1L, now, expiresAt);
    }

    @Test
    void notExpiredBeforeDeadline() {
        Instant future = Instant.now().plusSeconds(60);
        BatchLock lock = newLock(future);

        assertThat(lock.isExpired(Instant.now())).isFalse();
    }

    @Test
    void expiredAfterDeadline() {
        Instant acquiredAt = Instant.now().minusSeconds(120);
        Instant expiresAt = Instant.now().minusSeconds(60);
        BatchLock lock = new BatchLock(UUID.randomUUID(), UUID.randomUUID(), "worker-1", 1L, acquiredAt, expiresAt);

        assertThat(lock.isExpired(Instant.now())).isTrue();
    }

    @Test
    void expiredAtExactExpiryMoment() {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(30);
        BatchLock lock = new BatchLock(UUID.randomUUID(), UUID.randomUUID(), "worker-1", 1L, now, expiresAt);

        assertThat(lock.isExpired(expiresAt)).isTrue();
    }

    @Test
    void ownershipCheck() {
        BatchLock lock = newLock(Instant.now().plusSeconds(60));

        assertThat(lock.isOwnedBy("worker-1")).isTrue();
        assertThat(lock.isOwnedBy("worker-2")).isFalse();
    }

    @Test
    void pingByOwnerUpdatesExpiry() {
        BatchLock lock = newLock(Instant.now().plusSeconds(30));
        Instant now = Instant.now();
        Instant newExpiry = now.plusSeconds(60);

        lock.ping("worker-1", now, newExpiry);

        assertThat(lock.getExpiresAt()).isEqualTo(newExpiry);
        assertThat(lock.getLastPingAt()).isEqualTo(now);
    }

    @Test
    void pingByNonOwnerThrows() {
        BatchLock lock = newLock(Instant.now().plusSeconds(30));

        assertThatThrownBy(() -> lock.ping("worker-2", Instant.now(), Instant.now().plusSeconds(60)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("worker-1")
                .hasMessageContaining("worker-2");
    }

    @Test
    void pingExpiredLockThrows() {
        Instant now = Instant.now();
        BatchLock lock = new BatchLock(UUID.randomUUID(), UUID.randomUUID(), "worker-1", 1L,
                now.minusSeconds(60), now.minusSeconds(30));

        assertThatThrownBy(() -> lock.ping("worker-1", now, now.plusSeconds(30)))
                .isInstanceOf(StaleOwnerException.class);
    }

    @Test
    void pingWithPastNewExpiresAtThrows() {
        Instant now = Instant.now();
        BatchLock lock = new BatchLock(UUID.randomUUID(), UUID.randomUUID(), "worker-1", 1L,
                now.minusSeconds(10), now.plusSeconds(30));

        assertThatThrownBy(() -> lock.ping("worker-1", now, now.minusSeconds(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    @Test
    void pingCannotShortenExpiry() {
        Instant now = Instant.now();
        Instant originalExpiry = now.plusSeconds(60);
        BatchLock lock = new BatchLock(UUID.randomUUID(), UUID.randomUUID(), "worker-1", 1L,
                now.minusSeconds(10), originalExpiry);

        Instant shorterExpiry = now.plusSeconds(30);
        assertThatThrownBy(() -> lock.ping("worker-1", now, shorterExpiry))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("shorten");
    }

    @Test
    void fencingTokenIsStored() {
        Instant now = Instant.now();
        BatchLock lock = new BatchLock(UUID.randomUUID(), UUID.randomUUID(), "owner", 42L, now, now.plusSeconds(30));

        assertThat(lock.getFencingToken()).isEqualTo(42L);
    }

    @Test
    void constructorRejectsNullLockId() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> new BatchLock(null, UUID.randomUUID(), "owner", 1L, now, now.plusSeconds(30)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("lockId");
    }

    @Test
    void constructorRejectsNullBatchId() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> new BatchLock(UUID.randomUUID(), null, "owner", 1L, now, now.plusSeconds(30)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("batchId");
    }

    @Test
    void constructorRejectsBlankOwnerId() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> new BatchLock(UUID.randomUUID(), UUID.randomUUID(), " ", 1L, now, now.plusSeconds(30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ownerId");
    }

    @Test
    void constructorRejectsExpiresAtBeforeAcquiredAt() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> new BatchLock(UUID.randomUUID(), UUID.randomUUID(), "owner", 1L,
                now, now.minusSeconds(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiresAt must be after acquiredAt");
    }

    @Test
    void constructorRejectsExpiresAtEqualToAcquiredAt() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> new BatchLock(UUID.randomUUID(), UUID.randomUUID(), "owner", 1L, now, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiresAt must be after acquiredAt");
    }
}
