package com.eventledger.domain;

import com.eventledger.domain.entity.JournalEntry;
import com.eventledger.domain.entity.LedgerEvent;
import com.eventledger.domain.enums.EventType;
import com.eventledger.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JournalEntryTest {

    private static final UUID TRANSACTION_ID = UUID.randomUUID();
    private static final UUID DEBIT_ACCOUNT_ID = UUID.randomUUID();
    private static final UUID CREDIT_ACCOUNT_ID = UUID.randomUUID();
    private static final String IDEMPOTENCY_KEY = "txn-abc-001";
    private static final String DESCRIPTION = "Payment from merchant to reserve";

    private JournalEntry createEntry(Money amount) {
        return JournalEntry.create(
                TRANSACTION_ID,
                IDEMPOTENCY_KEY,
                amount,
                DEBIT_ACCOUNT_ID,
                CREDIT_ACCOUNT_ID,
                DESCRIPTION
        );
    }

    @Test
    void createProducesOneDebitAndOneCreditPosting() {
        Money amount = Money.of("100.00", "USD");

        JournalEntry entry = createEntry(amount);

        List<LedgerEvent> postings = entry.getPostings();
        assertThat(postings).hasSize(2);
        assertThat(postings).anyMatch(e -> e.getEventType() == EventType.DEBIT);
        assertThat(postings).anyMatch(e -> e.getEventType() == EventType.CREDIT);
    }

    @Test
    void createDebitAndCreditHaveMatchingAmounts() {
        Money amount = Money.of("250.75", "USD");

        JournalEntry entry = createEntry(amount);

        LedgerEvent debit = entry.getDebitEvent();
        LedgerEvent credit = entry.getCreditEvent();
        assertThat(debit.getAmount()).isEqualByComparingTo(new BigDecimal("250.75"));
        assertThat(credit.getAmount()).isEqualByComparingTo(new BigDecimal("250.75"));
    }

    @Test
    void createDebitPostingIsAssignedToDebitAccount() {
        JournalEntry entry = createEntry(Money.of("50.00", "USD"));

        assertThat(entry.getDebitEvent().getAccountId()).isEqualTo(DEBIT_ACCOUNT_ID);
    }

    @Test
    void createCreditPostingIsAssignedToCreditAccount() {
        JournalEntry entry = createEntry(Money.of("50.00", "USD"));

        assertThat(entry.getCreditEvent().getAccountId()).isEqualTo(CREDIT_ACCOUNT_ID);
    }

    @Test
    void createWithSameDebitAndCreditAccountThrows() {
        UUID sameAccount = UUID.randomUUID();

        assertThatThrownBy(() ->
                JournalEntry.create(TRANSACTION_ID, IDEMPOTENCY_KEY, Money.of("100.00", "USD"),
                        sameAccount, sameAccount, DESCRIPTION)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("debit and credit accounts must be different");
    }

    @Test
    void idempotencyKeyPropagatedToBothPostings() {
        JournalEntry entry = createEntry(Money.of("100.00", "USD"));

        assertThat(entry.getDebitEvent().getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
        assertThat(entry.getCreditEvent().getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
    }

    @Test
    void allPostingsShareTheSameTransactionId() {
        JournalEntry entry = createEntry(Money.of("100.00", "USD"));

        UUID expectedTransactionId = entry.getTransactionId();
        assertThat(entry.getDebitEvent().getTransactionId()).isEqualTo(expectedTransactionId);
        assertThat(entry.getCreditEvent().getTransactionId()).isEqualTo(expectedTransactionId);
    }

    @Test
    void allPostingsShareTheSameJournalEntryId() {
        JournalEntry entry = createEntry(Money.of("100.00", "USD"));

        UUID expectedJournalEntryId = entry.getJournalEntryId();
        assertThat(entry.getDebitEvent().getJournalEntryId()).isEqualTo(expectedJournalEntryId);
        assertThat(entry.getCreditEvent().getJournalEntryId()).isEqualTo(expectedJournalEntryId);
    }

    @Test
    void transactionIdMatchesProvidedTransactionId() {
        JournalEntry entry = createEntry(Money.of("100.00", "USD"));

        assertThat(entry.getTransactionId()).isEqualTo(TRANSACTION_ID);
    }

    @Test
    void getDebitEventReturnsDebitTypedPosting() {
        JournalEntry entry = createEntry(Money.of("100.00", "USD"));

        assertThat(entry.getDebitEvent().getEventType()).isEqualTo(EventType.DEBIT);
    }

    @Test
    void getCreditEventReturnsCreditTypedPosting() {
        JournalEntry entry = createEntry(Money.of("100.00", "USD"));

        assertThat(entry.getCreditEvent().getEventType()).isEqualTo(EventType.CREDIT);
    }

    @Test
    void blankIdempotencyKeyThrows() {
        assertThatThrownBy(() ->
                JournalEntry.create(TRANSACTION_ID, "   ", Money.of("100.00", "USD"),
                        DEBIT_ACCOUNT_ID, CREDIT_ACCOUNT_ID, DESCRIPTION)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idempotencyKey must not be blank");
    }

    @Test
    void emptyIdempotencyKeyThrows() {
        assertThatThrownBy(() ->
                JournalEntry.create(TRANSACTION_ID, "", Money.of("100.00", "USD"),
                        DEBIT_ACCOUNT_ID, CREDIT_ACCOUNT_ID, DESCRIPTION)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idempotencyKey must not be blank");
    }

    @Test
    void nullIdempotencyKeyThrows() {
        assertThatThrownBy(() ->
                JournalEntry.create(TRANSACTION_ID, null, Money.of("100.00", "USD"),
                        DEBIT_ACCOUNT_ID, CREDIT_ACCOUNT_ID, DESCRIPTION)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idempotencyKey must not be blank");
    }

    @Test
    void nullAmountThrows() {
        assertThatThrownBy(() ->
                JournalEntry.create(TRANSACTION_ID, IDEMPOTENCY_KEY, null,
                        DEBIT_ACCOUNT_ID, CREDIT_ACCOUNT_ID, DESCRIPTION)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount must not be null");
    }

    @Test
    void zeroAmountThrows() {
        Money zero = Money.of("0.00", "USD");

        assertThatThrownBy(() -> createEntry(zero))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must be positive");
    }

    @Test
    void negativeAmountThrows() {
        Money negative = Money.of("-1.00", "USD");

        assertThatThrownBy(() -> createEntry(negative))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must be positive");
    }

    @Test
    void createWithJpyAmountRecordsCorrectCurrencyAndScale() {
        Money jpyAmount = Money.of("1500", "JPY");

        JournalEntry entry = JournalEntry.create(
                TRANSACTION_ID, IDEMPOTENCY_KEY, jpyAmount,
                DEBIT_ACCOUNT_ID, CREDIT_ACCOUNT_ID, DESCRIPTION
        );

        assertThat(entry.getCurrency()).isEqualTo("JPY");
        assertThat(entry.getDebitEvent().getCurrency()).isEqualTo("JPY");
        assertThat(entry.getCreditEvent().getCurrency()).isEqualTo("JPY");
    }

    @Test
    void createWithJpyAmountStoresWholeUnitAmount() {
        Money jpyAmount = Money.of("5000", "JPY");

        JournalEntry entry = JournalEntry.create(
                TRANSACTION_ID, IDEMPOTENCY_KEY, jpyAmount,
                DEBIT_ACCOUNT_ID, CREDIT_ACCOUNT_ID, DESCRIPTION
        );

        assertThat(entry.getDebitEvent().getAmount()).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(entry.getCreditEvent().getAmount()).isEqualByComparingTo(new BigDecimal("5000"));
    }

    @Test
    void getPostingsReturnsUnmodifiableCopy() {
        JournalEntry entry = createEntry(Money.of("100.00", "USD"));

        List<LedgerEvent> postings = entry.getPostings();

        assertThatThrownBy(() -> postings.add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getPostingsReturnsCopyNotLiveReference() {
        JournalEntry entry = createEntry(Money.of("100.00", "USD"));

        List<LedgerEvent> firstCall = entry.getPostings();
        List<LedgerEvent> secondCall = entry.getPostings();

        assertThat(firstCall).isNotSameAs(secondCall);
        assertThat(firstCall).containsExactlyElementsOf(secondCall);
    }

    @Test
    void descriptionIsStoredOnEntry() {
        JournalEntry entry = createEntry(Money.of("100.00", "USD"));

        assertThat(entry.getDescription()).isEqualTo(DESCRIPTION);
    }

    @Test
    void nullDescriptionIsPermitted() {
        JournalEntry entry = JournalEntry.create(
                TRANSACTION_ID, IDEMPOTENCY_KEY, Money.of("100.00", "USD"),
                DEBIT_ACCOUNT_ID, CREDIT_ACCOUNT_ID, null
        );

        assertThat(entry.getDescription()).isNull();
    }

    @Test
    void createdAtIsSetOnCreate() {
        JournalEntry entry = createEntry(Money.of("100.00", "USD"));

        assertThat(entry.getCreatedAt()).isNotNull();
    }

    @Test
    void journalEntryIdIsGeneratedAndNotNull() {
        JournalEntry entry = createEntry(Money.of("100.00", "USD"));

        assertThat(entry.getJournalEntryId()).isNotNull();
    }
}
