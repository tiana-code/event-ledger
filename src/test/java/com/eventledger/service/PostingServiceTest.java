package com.eventledger.service;

import com.eventledger.domain.entity.Account;
import com.eventledger.domain.entity.JournalEntry;
import com.eventledger.domain.enums.AccountType;
import com.eventledger.exception.AccountNotFoundException;
import com.eventledger.exception.CurrencyMismatchException;
import com.eventledger.exception.InsufficientBalanceException;
import com.eventledger.domain.valueobject.Money;
import com.eventledger.repository.AccountRepository;
import com.eventledger.repository.JournalEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostingServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @InjectMocks
    private PostingServiceImpl postingService;

    private UUID debitAccountId;
    private UUID creditAccountId;
    private Money amount;
    private String idempotencyKey;
    private String description;

    @BeforeEach
    void setUp() {
        debitAccountId = UUID.randomUUID();
        creditAccountId = UUID.randomUUID();
        amount = Money.of("100.00", "USD");
        idempotencyKey = "test-idempotency-key-" + UUID.randomUUID();
        description = "Test transaction";
    }

    private void stubFindByIdForUpdate(Map<UUID, Account> accountsById) {
        when(accountRepository.findByIdForUpdate(any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID id = invocation.getArgument(0);
                    return Optional.ofNullable(accountsById.get(id));
                });
    }

    @Test
    void happyPath_debitAndCreditAccountsFoundBalancesUpdatedJournalEntrySaved() {
        Account debitAccount = new Account(debitAccountId, UUID.randomUUID(), AccountType.MERCHANT, "USD");
        debitAccount.credit(Money.of("1000.00", "USD"));

        Account creditAccount = new Account(creditAccountId, UUID.randomUUID(), AccountType.MERCHANT, "USD");

        stubFindByIdForUpdate(Map.of(debitAccountId, debitAccount, creditAccountId, creditAccount));

        when(journalEntryRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(journalEntryRepository.save(any(JournalEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        JournalEntry result = postingService.postTransaction(
                debitAccountId, creditAccountId, amount, idempotencyKey, description);

        assertThat(result).isNotNull();
        assertThat(result.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getDescription()).isEqualTo(description);
        assertThat(result.getPostings()).hasSize(2);

        assertThat(debitAccount.getBalance()).isEqualByComparingTo("900.00");
        assertThat(creditAccount.getBalance()).isEqualByComparingTo("100.00");

        verify(journalEntryRepository).save(any(JournalEntry.class));
    }

    @Test
    void idempotencyReturnsExisting_whenJournalEntryAlreadyExistsNoMutationsOccur() {
        JournalEntry existingEntry = mock(JournalEntry.class);
        when(journalEntryRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingEntry));

        JournalEntry result = postingService.postTransaction(
                debitAccountId, creditAccountId, amount, idempotencyKey, description);

        assertThat(result).isSameAs(existingEntry);

        verifyNoInteractions(accountRepository);
        verify(journalEntryRepository, never()).save(any());
    }

    @Test
    void debitAccountNotFound_throwsAccountNotFoundException() {
        when(journalEntryRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(accountRepository.findByIdForUpdate(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postingService.postTransaction(
                debitAccountId, creditAccountId, amount, idempotencyKey, description))
                .isInstanceOf(AccountNotFoundException.class);

        verify(journalEntryRepository, never()).save(any());
    }

    @Test
    void creditAccountNotFound_throwsAccountNotFoundException() {
        Account debitAccount = new Account(debitAccountId, UUID.randomUUID(), AccountType.MERCHANT, "USD");
        debitAccount.credit(Money.of("1000.00", "USD"));

        when(journalEntryRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(accountRepository.findByIdForUpdate(any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID id = invocation.getArgument(0);
                    if (id.equals(debitAccountId)) {
                        return Optional.of(debitAccount);
                    }
                    return Optional.empty();
                });

        assertThatThrownBy(() -> postingService.postTransaction(
                debitAccountId, creditAccountId, amount, idempotencyKey, description))
                .isInstanceOf(AccountNotFoundException.class);

        verify(journalEntryRepository, never()).save(any());
    }

    @Test
    void currencyMismatchOnDebitAccount_throwsCurrencyMismatchException() {
        Account debitAccount = new Account(debitAccountId, UUID.randomUUID(), AccountType.MERCHANT, "EUR");
        debitAccount.credit(Money.of("1000.00", "EUR"));

        Account creditAccount = new Account(creditAccountId, UUID.randomUUID(), AccountType.MERCHANT, "USD");

        stubFindByIdForUpdate(Map.of(debitAccountId, debitAccount, creditAccountId, creditAccount));
        when(journalEntryRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postingService.postTransaction(
                debitAccountId, creditAccountId, amount, idempotencyKey, description))
                .isInstanceOf(CurrencyMismatchException.class);

        verify(journalEntryRepository, never()).save(any());
    }

    @Test
    void currencyMismatchOnCreditAccount_throwsCurrencyMismatchException() {
        Account debitAccount = new Account(debitAccountId, UUID.randomUUID(), AccountType.MERCHANT, "USD");
        debitAccount.credit(Money.of("1000.00", "USD"));

        Account creditAccount = new Account(creditAccountId, UUID.randomUUID(), AccountType.MERCHANT, "EUR");

        stubFindByIdForUpdate(Map.of(debitAccountId, debitAccount, creditAccountId, creditAccount));
        when(journalEntryRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postingService.postTransaction(
                debitAccountId, creditAccountId, amount, idempotencyKey, description))
                .isInstanceOf(CurrencyMismatchException.class);

        verify(journalEntryRepository, never()).save(any());
    }

    @Test
    void insufficientBalance_debitOnMerchantAccountWithNoFundsThrowsInsufficientBalanceException() {
        Account debitAccount = new Account(debitAccountId, UUID.randomUUID(), AccountType.MERCHANT, "USD");
        Account creditAccount = new Account(creditAccountId, UUID.randomUUID(), AccountType.MERCHANT, "USD");

        stubFindByIdForUpdate(Map.of(debitAccountId, debitAccount, creditAccountId, creditAccount));
        when(journalEntryRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postingService.postTransaction(
                debitAccountId, creditAccountId, amount, idempotencyKey, description))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining(debitAccountId.toString());

        verify(journalEntryRepository, never()).save(any());
    }
}
