package com.eventledger.repository;

import com.eventledger.domain.entity.BatchLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface BatchLockRepository extends JpaRepository<BatchLock, UUID> {

    Optional<BatchLock> findByBatchId(UUID batchId);

    boolean existsByBatchIdAndExpiresAtAfter(UUID batchId, Instant now);

    @Modifying
    @Query("DELETE FROM BatchLock lock WHERE lock.expiresAt < :now")
    int deleteExpiredLocks(@Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM BatchLock lock WHERE lock.batchId = :batchId AND lock.ownerId = :ownerId")
    int releaseByOwner(@Param("batchId") UUID batchId, @Param("ownerId") String ownerId);

    @Modifying
    @Query(value = """
        INSERT INTO batch_locks (lock_id, batch_id, owner_id, fencing_token, acquired_at, expires_at, last_ping_at)
        VALUES (CAST(:lockId AS uuid), CAST(:batchId AS uuid), :ownerId,
                nextval('batch_lock_fencing_seq'),
                CAST(:acquiredAt AS timestamptz), CAST(:expiresAt AS timestamptz), CAST(:acquiredAt AS timestamptz))
        ON CONFLICT (batch_id) DO UPDATE
        SET lock_id = CAST(:lockId AS uuid),
            owner_id = :ownerId,
            fencing_token = nextval('batch_lock_fencing_seq'),
            acquired_at = CAST(:acquiredAt AS timestamptz),
            expires_at = CAST(:expiresAt AS timestamptz),
            last_ping_at = CAST(:acquiredAt AS timestamptz)
        WHERE batch_locks.expires_at <= NOW()
    """, nativeQuery = true)
    int tryAcquire(
        @Param("lockId") UUID lockId,
        @Param("batchId") UUID batchId,
        @Param("ownerId") String ownerId,
        @Param("acquiredAt") Instant acquiredAt,
        @Param("expiresAt") Instant expiresAt
    );
}
