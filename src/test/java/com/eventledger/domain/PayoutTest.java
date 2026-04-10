package com.eventledger.domain;

import com.eventledger.domain.entity.Payout;
import com.eventledger.domain.enums.PayoutStatus;
import com.eventledger.domain.exception.InvalidStateTransitionException;
import com.eventledger.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PayoutTest {

    private Payout newPayout() {
        return new Payout(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Money.of("100.00", "USD"),
            UUID.randomUUID().toString()
        );
    }

    @Test
    void newPayoutStartsAsPending() {
        Payout payout = newPayout();

        assertThat(payout.getStatus()).isEqualTo(PayoutStatus.PENDING);
        assertThat(payout.getCreatedAt()).isEqualTo(payout.getUpdatedAt());
    }

    @Test
    void markProcessingSetsTimestamp() {
        Payout payout = newPayout();

        payout.markProcessing();

        assertThat(payout.getStatus()).isEqualTo(PayoutStatus.PROCESSING);
        assertThat(payout.getProcessingStartedAt()).isNotNull();
    }

    @Test
    void markSentSetsTimestamp() {
        Payout payout = newPayout();
        payout.markProcessing();

        payout.markSent();

        assertThat(payout.getStatus()).isEqualTo(PayoutStatus.SENT);
        assertThat(payout.getSentAt()).isNotNull();
    }

    @Test
    void markConfirmedSetsTimestamp() {
        Payout payout = newPayout();
        payout.markProcessing();
        payout.markSent();

        payout.markConfirmed();

        assertThat(payout.getStatus()).isEqualTo(PayoutStatus.CONFIRMED);
        assertThat(payout.getConfirmedAt()).isNotNull();
    }

    @Test
    void failSetsCodeReasonAndTimestamp() {
        Payout payout = newPayout();
        payout.markProcessing();

        payout.fail("PROVIDER_REJECT", "Insufficient funds at provider");

        assertThat(payout.getStatus()).isEqualTo(PayoutStatus.FAILED);
        assertThat(payout.getFailureCode()).isEqualTo("PROVIDER_REJECT");
        assertThat(payout.getFailureReason()).isEqualTo("Insufficient funds at provider");
        assertThat(payout.getFailedAt()).isNotNull();
    }

    @Test
    void failWithBlankCodeThrows() {
        Payout payout = newPayout();

        assertThatThrownBy(() -> payout.fail("", "some reason"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("failureCode must not be blank");
    }

    @Test
    void failWithNullCodeThrows() {
        Payout payout = newPayout();

        assertThatThrownBy(() -> payout.fail(null, "some reason"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("failureCode must not be blank");
    }

    @Test
    void failFromPendingIsAllowed() {
        Payout payout = newPayout();

        payout.fail("VALIDATION_ERROR", "Invalid account");

        assertThat(payout.getStatus()).isEqualTo(PayoutStatus.FAILED);
    }

    @Test
    void invalidTransitionConfirmedToProcessingThrows() {
        Payout payout = newPayout();
        payout.markProcessing();
        payout.markSent();
        payout.markConfirmed();

        assertThatThrownBy(payout::markProcessing)
            .isInstanceOf(InvalidStateTransitionException.class)
            .hasMessageContaining("CONFIRMED")
            .hasMessageContaining("PROCESSING");
    }

    @Test
    void invalidTransitionFailedToSentThrows() {
        Payout payout = newPayout();
        payout.fail("TEST", "test");

        assertThatThrownBy(payout::markSent)
            .isInstanceOf(InvalidStateTransitionException.class)
            .hasMessageContaining("FAILED");
    }

    @Test
    void invalidTransitionPendingToConfirmedThrows() {
        Payout payout = newPayout();

        assertThatThrownBy(payout::markConfirmed)
            .isInstanceOf(InvalidStateTransitionException.class)
            .hasMessageContaining("PENDING");
    }

    @Test
    void failFromConfirmedStateThrows() {
        Payout payout = newPayout();
        payout.markProcessing();
        payout.markSent();
        payout.markConfirmed();

        assertThatThrownBy(() -> payout.fail("LATE", "late failure"))
            .isInstanceOf(InvalidStateTransitionException.class)
            .hasMessageContaining("CONFIRMED");
    }

    @Test
    void failFromAlreadyFailedThrows() {
        Payout payout = newPayout();
        payout.fail("FIRST", "first failure");

        assertThatThrownBy(() -> payout.fail("SECOND", "double fail"))
            .isInstanceOf(InvalidStateTransitionException.class)
            .hasMessageContaining("FAILED");
    }

    @Test
    void constructorWithZeroAmountThrows() {
        assertThatThrownBy(() -> new Payout(
            UUID.randomUUID(), UUID.randomUUID(), Money.of("0.00", "USD"), "key-1"
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Payout amount must be positive");
    }

    @Test
    void constructorWithNegativeAmountThrows() {
        assertThatThrownBy(() -> new Payout(
            UUID.randomUUID(), UUID.randomUUID(), Money.of("-1.00", "USD"), "key-2"
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Payout amount must be positive");
    }

    @Test
    void constructorRejectsNullPayoutId() {
        assertThatThrownBy(() -> new Payout(
            null, UUID.randomUUID(), Money.of("100.00", "USD"), "key"
        )).isInstanceOf(NullPointerException.class)
            .hasMessageContaining("payoutId");
    }

    @Test
    void constructorRejectsNullAccountId() {
        assertThatThrownBy(() -> new Payout(
            UUID.randomUUID(), null, Money.of("100.00", "USD"), "key"
        )).isInstanceOf(NullPointerException.class)
            .hasMessageContaining("accountId");
    }

    @Test
    void constructorRejectsNullMoney() {
        assertThatThrownBy(() -> new Payout(
            UUID.randomUUID(), UUID.randomUUID(), null, "key"
        )).isInstanceOf(NullPointerException.class)
            .hasMessageContaining("money");
    }

    @Test
    void constructorRejectsBlankIdempotencyKey() {
        assertThatThrownBy(() -> new Payout(
            UUID.randomUUID(), UUID.randomUUID(), Money.of("100.00", "USD"), " "
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("idempotencyKey");
    }

    @Test
    void constructorRejectsNullIdempotencyKey() {
        assertThatThrownBy(() -> new Payout(
            UUID.randomUUID(), UUID.randomUUID(), Money.of("100.00", "USD"), null
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("idempotencyKey");
    }

    @Test
    void fullLifecycleTracksAllTimestamps() {
        Payout payout = newPayout();

        payout.markProcessing();
        assertThat(payout.getProcessingStartedAt()).isNotNull();

        payout.markSent();
        assertThat(payout.getSentAt()).isNotNull();
        assertThat(payout.getSentAt()).isAfterOrEqualTo(payout.getProcessingStartedAt());

        payout.markConfirmed();
        assertThat(payout.getConfirmedAt()).isNotNull();
        assertThat(payout.getConfirmedAt()).isAfterOrEqualTo(payout.getSentAt());
    }

    @Test
    void jpyPayoutPreservesZeroScale() {
        Payout payout = new Payout(
            UUID.randomUUID(), UUID.randomUUID(),
            Money.of("1000", "JPY"), "jpy-key"
        );

        assertThat(payout.getAmount().scale()).isEqualTo(0);
        assertThat(payout.getCurrency()).isEqualTo("JPY");
    }
}
