package com.eventledger.domain;

import com.eventledger.domain.entity.LedgerEvent;
import com.eventledger.domain.enums.EventType;
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

        LedgerEvent debit = new LedgerEvent(
                UUID.randomUUID(), accountId, transactionId,
                new BigDecimal("100.00"), EventType.DEBIT, sharedKey, "USD", null
        );

        LedgerEvent credit = new LedgerEvent(
                UUID.randomUUID(), accountId, transactionId,
                new BigDecimal("100.00"), EventType.CREDIT, sharedKey, "USD", null
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
        String idempotencyKey = "idem-key-123";

        LedgerEvent event = new LedgerEvent(
                eventId, accountId, transactionId,
                new BigDecimal("250.00"), EventType.CREDIT, idempotencyKey, "EUR", "{\"ref\":\"INV-42\"}"
        );

        assertThat(event.getEventId()).isEqualTo(eventId);
        assertThat(event.getAccountId()).isEqualTo(accountId);
        assertThat(event.getTransactionId()).isEqualTo(transactionId);
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
        BigDecimal amount = new BigDecimal("500.00");

        LedgerEvent debit = new LedgerEvent(
                UUID.randomUUID(), UUID.randomUUID(), transactionId,
                amount, EventType.DEBIT, idempotencyKey, "USD", null
        );

        LedgerEvent credit = new LedgerEvent(
                UUID.randomUUID(), UUID.randomUUID(), transactionId,
                amount, EventType.CREDIT, idempotencyKey, "USD", null
        );

        BigDecimal netBalance = credit.getAmount().subtract(debit.getAmount());
        assertThat(netBalance).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void constructorRejectsZeroAmount() {
        assertThatThrownBy(() -> new LedgerEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.ZERO, EventType.CREDIT, "key", "USD", null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void constructorRejectsNegativeAmount() {
        assertThatThrownBy(() -> new LedgerEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("-10.00"), EventType.DEBIT, "key", "USD", null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void constructorRejectsBlankIdempotencyKey() {
        assertThatThrownBy(() -> new LedgerEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("10.00"), EventType.CREDIT, " ", "USD", null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idempotencyKey");
    }

    @Test
    void constructorRejectsBlankCurrency() {
        assertThatThrownBy(() -> new LedgerEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("10.00"), EventType.CREDIT, "key-1", "", null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");
    }

    @Test
    void constructorRejectsNullAccountId() {
        assertThatThrownBy(() -> new LedgerEvent(
                UUID.randomUUID(), null, UUID.randomUUID(),
                new BigDecimal("10.00"), EventType.CREDIT, "key-1", "USD", null
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructorRejectsNullTransactionId() {
        assertThatThrownBy(() -> new LedgerEvent(
                UUID.randomUUID(), UUID.randomUUID(), null,
                new BigDecimal("10.00"), EventType.CREDIT, "key-1", "USD", null
        )).isInstanceOf(NullPointerException.class);
    }
}
