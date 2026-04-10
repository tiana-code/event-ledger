package com.eventledger.repository;

import com.eventledger.domain.entity.Account;
import com.eventledger.domain.enums.AccountType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT account FROM Account account WHERE account.accountId = :accountId")
    Optional<Account> findByIdForUpdate(@Param("accountId") UUID accountId);

    List<Account> findByOwnerIdOrderByCreatedAtAsc(UUID ownerId);

    Page<Account> findByOwnerIdOrderByCreatedAtAsc(UUID ownerId, Pageable pageable);

    List<Account> findByAccountType(AccountType accountType);
}
