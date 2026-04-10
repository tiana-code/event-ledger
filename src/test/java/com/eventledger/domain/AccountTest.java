package com.eventledger.domain;

import com.eventledger.domain.entity.Account;
import com.eventledger.domain.enums.AccountType;
import com.eventledger.domain.exception.CurrencyMismatchException;
import com.eventledger.domain.exception.InsufficientBalanceException;
import com.eventledger.domain.exception.UnsupportedCurrencyException;
import com.eventledger.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private Account newMerchantAccount() {
        return new Account(UUID.randomUUID(), UUID.randomUUID(), AccountType.MERCHANT, "USD");
    }

    private Account newPlatformAccount() {
        return new Account(UUID.randomUUID(), UUID.randomUUID(), AccountType.PLATFORM, "USD");
    }

    @Test
    void creditIncreasesBalance() {
        Account account = newMerchantAccount();

        account.credit(Money.of("100.00", "USD"));

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void debitDecreasesBalance() {
        Account account = newMerchantAccount();
        account.credit(Money.of("200.00", "USD"));

        account.debit(Money.of("75.00", "USD"));

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("125.00"));
    }

    @Test
    void creditWithZeroAmountThrows() {
        Account account = newMerchantAccount();

        assertThatThrownBy(() -> account.credit(Money.of("0.00", "USD")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be positive");
    }

    @Test
    void creditWithNegativeAmountThrows() {
        Account account = newMerchantAccount();

        assertThatThrownBy(() -> account.credit(Money.of("-50.00", "USD")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be positive");
    }

    @Test
    void debitWithZeroAmountThrows() {
        Account account = newMerchantAccount();

        assertThatThrownBy(() -> account.debit(Money.of("0.00", "USD")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be positive");
    }

    @Test
    void multipleCreditsAndDebitsAccumulateCorrectly() {
        Account account = newMerchantAccount();

        account.credit(Money.of("500.00", "USD"));
        account.credit(Money.of("300.00", "USD"));
        account.debit(Money.of("200.00", "USD"));

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("600.00"));
    }

    @Test
    void currencyMismatchOnCreditThrowsDomainException() {
        Account account = newMerchantAccount();

        assertThatThrownBy(() -> account.credit(Money.of("100.00", "EUR")))
                .isInstanceOf(CurrencyMismatchException.class)
                .hasMessageContaining("USD")
                .hasMessageContaining("EUR");
    }

    @Test
    void currencyMismatchOnDebitThrowsDomainException() {
        Account account = newMerchantAccount();
        account.credit(Money.of("100.00", "USD"));

        assertThatThrownBy(() -> account.debit(Money.of("50.00", "EUR")))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void merchantAccountCannotGoNegative() {
        Account account = newMerchantAccount();
        account.credit(Money.of("50.00", "USD"));

        assertThatThrownBy(() -> account.debit(Money.of("100.00", "USD")))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    void escrowAccountCannotGoNegative() {
        Account account = new Account(UUID.randomUUID(), UUID.randomUUID(), AccountType.ESCROW, "USD");

        assertThatThrownBy(() -> account.debit(Money.of("100.00", "USD")))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void payoutAccountCannotGoNegative() {
        Account account = new Account(UUID.randomUUID(), UUID.randomUUID(), AccountType.PAYOUT, "USD");

        assertThatThrownBy(() -> account.debit(Money.of("100.00", "USD")))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void platformAccountCanGoNegative() {
        Account account = newPlatformAccount();

        account.debit(Money.of("100.00", "USD"));

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("-100.00"));
    }

    @Test
    void creditWithNullAmountThrows() {
        Account account = newMerchantAccount();

        assertThatThrownBy(() -> account.credit(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void newAccountStartsWithZeroBalance() {
        Account account = newMerchantAccount();

        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.getCurrency()).isEqualTo("USD");
        assertThat(account.getAccountType()).isEqualTo(AccountType.MERCHANT);
        assertThat(account.getCreatedAt()).isNotNull();
        assertThat(account.getCreatedAt()).isEqualTo(account.getUpdatedAt());
    }

    @Test
    void constructorRejectsNullAccountId() {
        assertThatThrownBy(() -> new Account(null, UUID.randomUUID(), AccountType.MERCHANT, "USD"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("accountId");
    }

    @Test
    void constructorRejectsNullOwnerId() {
        assertThatThrownBy(() -> new Account(UUID.randomUUID(), null, AccountType.MERCHANT, "USD"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ownerId");
    }

    @Test
    void constructorRejectsNullAccountType() {
        assertThatThrownBy(() -> new Account(UUID.randomUUID(), UUID.randomUUID(), null, "USD"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("accountType");
    }

    @Test
    void constructorRejectsNullCurrency() {
        assertThatThrownBy(() -> new Account(UUID.randomUUID(), UUID.randomUUID(), AccountType.MERCHANT, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("currency");
    }

    @Test
    void constructorRejectsUnsupportedCurrency() {
        assertThatThrownBy(() -> new Account(UUID.randomUUID(), UUID.randomUUID(), AccountType.MERCHANT, "XYZ"))
                .isInstanceOf(UnsupportedCurrencyException.class);
    }

    @Test
    void jpyAccountStartsWithZeroScaleBalance() {
        Account account = new Account(UUID.randomUUID(), UUID.randomUUID(), AccountType.MERCHANT, "JPY");

        assertThat(account.getBalance().scale()).isEqualTo(0);
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void kwdAccountStartsWithThreeScaleBalance() {
        Account account = new Account(UUID.randomUUID(), UUID.randomUUID(), AccountType.MERCHANT, "KWD");

        assertThat(account.getBalance().scale()).isEqualTo(3);
    }
}
