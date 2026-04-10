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
        SELECT CAST(:lockId AS uuid), CAST(:batchId AS uuid), :ownerId, :fencingToken,
               CAST(:acquiredAt AS timestamptz), CAST(:expiresAt AS timestamptz), CAST(:acquiredAt AS timestamptz)
        WHERE NOT EXISTS (
            SELECT 1 FROM batch_locks WHERE batch_id = CAST(:batchId AS uuid) AND expires_at > NOW()
        )
    """, nativeQuery = true)
    int tryAcquire(
        @Param("lockId") UUID lockId,
        @Param("batchId") UUID batchId,
        @Param("ownerId") String ownerId,
        @Param("fencingToken") long fencingToken,
        @Param("acquiredAt") Instant acquiredAt,
        @Param("expiresAt") Instant expiresAt
    );
}
