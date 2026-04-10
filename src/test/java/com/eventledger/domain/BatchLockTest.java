package com.eventledger.domain;

import com.eventledger.domain.entity.BatchLock;
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
        Instant past = Instant.now().minusSeconds(60);
        BatchLock lock = newLock(past);

        assertThat(lock.isExpired(Instant.now())).isTrue();
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
    void fencingTokenIsStored() {
        Instant now = Instant.now();
        BatchLock lock = new BatchLock(UUID.randomUUID(), UUID.randomUUID(), "owner", 42L, now, now.plusSeconds(30));

        assertThat(lock.getFencingToken()).isEqualTo(42L);
    }
}
