package com.eventledger.domain.entity;

import com.eventledger.domain.enums.AccountType;
import com.eventledger.domain.exception.CurrencyMismatchException;
import com.eventledger.domain.exception.NegativeBalanceException;
import com.eventledger.domain.valueobject.Money;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Account() {
    }

    public Account(UUID accountId, UUID ownerId, AccountType accountType, String currency) {
        this.accountId = accountId;
        this.ownerId = ownerId;
        this.accountType = accountType;
        this.currency = currency;
        this.balance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void credit(Money amount) {
        requirePositiveAndMatchingCurrency(amount);
        this.balance = this.balance.add(amount.getAmount());
        this.updatedAt = Instant.now();
    }

    public void debit(Money amount) {
        requirePositiveAndMatchingCurrency(amount);
        BigDecimal newBalance = this.balance.subtract(amount.getAmount());
        if (this.accountType == AccountType.MERCHANT && newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeBalanceException(accountId, balance, amount.getAmount());
        }
        this.balance = newBalance;
        this.updatedAt = Instant.now();
    }

    private void requirePositiveAndMatchingCurrency(Money amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (!amount.getCurrency().getCurrencyCode().equals(this.currency)) {
            throw new CurrencyMismatchException(this.currency, amount.getCurrency().getCurrencyCode());
        }
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
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
}
