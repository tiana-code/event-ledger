package com.eventledger.service;

import com.eventledger.domain.entity.Account;
import com.eventledger.domain.entity.JournalEntry;
import com.eventledger.exception.AccountNotFoundException;
import com.eventledger.exception.CurrencyMismatchException;
import com.eventledger.domain.valueobject.Money;
import com.eventledger.repository.AccountRepository;
import com.eventledger.repository.JournalEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class PostingServiceImpl implements PostingService {

    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;

    public PostingServiceImpl(AccountRepository accountRepository,
                              JournalEntryRepository journalEntryRepository) {
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    @Override
    @Transactional
    public JournalEntry postTransaction(UUID debitAccountId, UUID creditAccountId,
                                        Money amount, String idempotencyKey, String description) {
        Optional<JournalEntry> existing = journalEntryRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        boolean debitFirst = debitAccountId.compareTo(creditAccountId) < 0;
        UUID firstId = debitFirst ? debitAccountId : creditAccountId;
        UUID secondId = debitFirst ? creditAccountId : debitAccountId;

        Account first = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new AccountNotFoundException(firstId));
        Account second = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new AccountNotFoundException(secondId));

        Account debitAccount = debitAccountId.equals(firstId) ? first : second;
        Account creditAccount = creditAccountId.equals(firstId) ? first : second;

        String currencyCode = amount.getCurrency().getCurrencyCode();
        if (!debitAccount.getCurrency().equals(currencyCode)) {
            throw new CurrencyMismatchException(debitAccount.getCurrency(), currencyCode);
        }
        if (!creditAccount.getCurrency().equals(currencyCode)) {
            throw new CurrencyMismatchException(creditAccount.getCurrency(), currencyCode);
        }

        UUID transactionId = UUID.randomUUID();
        JournalEntry entry = JournalEntry.create(
                transactionId, idempotencyKey, amount,
                debitAccountId, creditAccountId, description
        );

        debitAccount.debit(amount);
        creditAccount.credit(amount);

        return journalEntryRepository.save(entry);
    }
}
