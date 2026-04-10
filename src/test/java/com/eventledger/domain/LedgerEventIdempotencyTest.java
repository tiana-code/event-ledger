package com.eventledger.domain;

import com.eventledger.domain.entity.LedgerEvent;
import com.eventledger.domain.enums.EventType;
import com.eventledger.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerEventIdempotencyTest {

    @Test
    void twoEventsWithSameIdempotencyKeyButDifferentTypesAreDistinct() {
        String sharedKey = UUID.randomUUID().toString();
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID journalEntryId = UUID.randomUUID();
        Money amount = Money.of("100.00", "USD");

        LedgerEvent debit = new LedgerEvent(
                UUID.randomUUID(), accountId, transactionId,
                amount, EventType.DEBIT, sharedKey, journalEntryId, null
        );

        LedgerEvent credit = new LedgerEvent(
                UUID.randomUUID(), accountId, transactionId,
                amount, EventType.CREDIT, sharedKey, journalEntryId, null
        );

        assertThat(debit.getIdempotencyKey()).isEqualTo(credit.getIdempotencyKey());
        assertThat(debit.getEventType()).isNotEqualTo(credit.getEventType());
        assertThat(debit.getEventId()).isNotEqualTo(credit.getEventId());
    }

    @Test
    void ledgerEventStoresAllFieldsCorrectly() {
        UUID eventId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID journalEntryId = UUID.randomUUID();
        String idempotencyKey = "idem-key-123";
        Money amount = Money.of("250.00", "EUR");

        LedgerEvent event = new LedgerEvent(
                eventId, accountId, transactionId,
                amount, EventType.CREDIT, idempotencyKey, journalEntryId, "{\"ref\":\"INV-42\"}"
        );

        assertThat(event.getEventId()).isEqualTo(eventId);
        assertThat(event.getAccountId()).isEqualTo(accountId);
        assertThat(event.getTransactionId()).isEqualTo(transactionId);
        assertThat(event.getJournalEntryId()).isEqualTo(journalEntryId);
        assertThat(event.getAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(event.getEventType()).isEqualTo(EventType.CREDIT);
        assertThat(event.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(event.getCurrency()).isEqualTo("EUR");
        assertThat(event.getMetadata()).contains("INV-42");
        assertThat(event.getCreatedAt()).isNotNull();
    }

    @Test
    void doubleEntryDebitAndCreditSumToZero() {
        UUID transactionId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        Money amount = Money.of("500.00", "USD");

        LedgerEvent debit = new LedgerEvent(
                UUID.randomUUID(), UUID.randomUUID(), transactionId,
                amount, EventType.DEBIT, idempotencyKey, null, null
        );

        LedgerEvent credit = new LedgerEvent(
                UUID.randomUUID(), UUID.randomUUID(), transactionId,
                amount, EventType.CREDIT, idempotencyKey, null, null
        );

        BigDecimal netBalance = credit.getAmount().subtract(debit.getAmount());
        assertThat(netBalance).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void constructorRejectsZeroAmount() {
        assertThatThrownBy(() -> new LedgerEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Money.of("0.00", "USD"), EventType.CREDIT, "key", null, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void constructorRejectsNegativeAmount() {
        assertThatThrownBy(() -> new LedgerEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Money.of("-10.00", "USD"), EventType.DEBIT, "key", null, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void constructorRejectsBlankIdempotencyKey() {
        assertThatThrownBy(() -> new LedgerEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Money.of("10.00", "USD"), EventType.CREDIT, " ", null, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idempotencyKey");
    }

    @Test
    void constructorRejectsNullAccountId() {
        assertThatThrownBy(() -> new LedgerEvent(
                UUID.randomUUID(), null, UUID.randomUUID(),
                Money.of("10.00", "USD"), EventType.CREDIT, "key-1", null, null
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructorRejectsNullTransactionId() {
        assertThatThrownBy(() -> new LedgerEvent(
                UUID.randomUUID(), UUID.randomUUID(), null,
                Money.of("10.00", "USD"), EventType.CREDIT, "key-1", null, null
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructorRejectsNullMoney() {
        assertThatThrownBy(() -> new LedgerEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                null, EventType.CREDIT, "key-1", null, null
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("money");
    }

    @Test
    void jpyCurrencyPreservesZeroScale() {
        Money jpyAmount = Money.of("1000", "JPY");

        LedgerEvent event = new LedgerEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                jpyAmount, EventType.CREDIT, "jpy-key", null, null
        );

        assertThat(event.getAmount().scale()).isEqualTo(0);
        assertThat(event.getCurrency()).isEqualTo("JPY");
    }

    @Test
    void kwdCurrencyPreservesThreeScale() {
        Money kwdAmount = Money.of("1.500", "KWD");

        LedgerEvent event = new LedgerEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                kwdAmount, EventType.DEBIT, "kwd-key", null, null
        );

        assertThat(event.getAmount().scale()).isEqualTo(3);
        assertThat(event.getCurrency()).isEqualTo("KWD");
    }

    @Test
    void journalEntryIdIsOptional() {
        LedgerEvent event = new LedgerEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Money.of("10.00", "USD"), EventType.CREDIT, "key", null, null
        );

        assertThat(event.getJournalEntryId()).isNull();
    }
}
