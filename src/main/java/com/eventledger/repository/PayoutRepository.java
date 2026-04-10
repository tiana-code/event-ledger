package com.eventledger.repository;

import com.eventledger.domain.entity.Payout;
import com.eventledger.domain.enums.PayoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayoutRepository extends JpaRepository<Payout, UUID> {

    Optional<Payout> findByIdempotencyKey(String idempotencyKey);

    List<Payout> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    Page<Payout> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);

    List<Payout> findByStatus(PayoutStatus status);

    Page<Payout> findByStatusOrderByCreatedAtAsc(PayoutStatus status, Pageable pageable);
}
