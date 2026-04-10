package com.eventledger.repository;

import com.eventledger.domain.entity.LedgerEvent;
import com.eventledger.domain.enums.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LedgerEventRepository extends JpaRepository<LedgerEvent, UUID> {

    List<LedgerEvent> findByAccountIdOrderByCreatedAtAsc(UUID accountId);

    Page<LedgerEvent> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);

    List<LedgerEvent> findByTransactionId(UUID transactionId);

    Optional<LedgerEvent> findByIdempotencyKey(String idempotencyKey);

    @Query("""
        SELECT COALESCE(SUM(
            CASE WHEN e.eventType = 'CREDIT' THEN e.amount
                 WHEN e.eventType = 'DEBIT' THEN -e.amount
            END
        ), 0)
        FROM LedgerEvent e
        WHERE e.accountId = :accountId
        AND e.eventType IN ('CREDIT', 'DEBIT')
    """)
    BigDecimal calculateBalanceFromEvents(@Param("accountId") UUID accountId);

    List<LedgerEvent> findByAccountIdAndEventTypeOrderByCreatedAtDesc(UUID accountId, EventType eventType);
}
