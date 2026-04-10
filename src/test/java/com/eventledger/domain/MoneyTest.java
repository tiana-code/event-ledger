package com.eventledger.domain;

import com.eventledger.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void createWithValidAmountAndCurrency() {
        Money money = Money.of("100.00", "USD");

        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(money.getCurrency()).isEqualTo(Currency.getInstance("USD"));
    }

    @Test
    void scaleMatchesCurrencyFractionDigits() {
        Money usd = Money.of("10", "USD");
        assertThat(usd.getAmount().scale()).isEqualTo(2);

        Money jpy = Money.of("1000", "JPY");
        assertThat(jpy.getAmount().scale()).isEqualTo(0);
    }

    @Test
    void rejectsScaleExceedingCurrencyFractionDigits() {
        assertThatThrownBy(() -> Money.of("10.999", "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scale")
                .hasMessageContaining("USD");
    }

    @Test
    void jpyRejectsDecimalPlaces() {
        assertThatThrownBy(() -> Money.of("100.5", "JPY"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JPY");
    }

    @Test
    void zeroCreation() {
        Money zero = Money.zero("USD");

        assertThat(zero.isZero()).isTrue();
        assertThat(zero.isPositive()).isFalse();
        assertThat(zero.isNegative()).isFalse();
    }

    @Test
    void addSameCurrency() {
        Money first = Money.of("10.50", "USD");
        Money second = Money.of("20.25", "USD");

        Money sum = first.add(second);

        assertThat(sum.getAmount()).isEqualByComparingTo(new BigDecimal("30.75"));
    }

    @Test
    void addDifferentCurrencyThrows() {
        Money usd = Money.of("10.00", "USD");
        Money eur = Money.of("10.00", "EUR");

        assertThatThrownBy(() -> usd.add(eur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void subtractSameCurrency() {
        Money minuend = Money.of("30.00", "USD");
        Money subtrahend = Money.of("10.50", "USD");

        Money difference = minuend.subtract(subtrahend);

        assertThat(difference.getAmount()).isEqualByComparingTo(new BigDecimal("19.50"));
    }

    @Test
    void negate() {
        Money positive = Money.of("25.00", "USD");

        Money negated = positive.negate();

        assertThat(negated.isNegative()).isTrue();
        assertThat(negated.getAmount()).isEqualByComparingTo(new BigDecimal("-25.00"));
    }

    @Test
    void comparisonMethods() {
        Money small = Money.of("10.00", "USD");
        Money large = Money.of("50.00", "USD");

        assertThat(large.isGreaterThan(small)).isTrue();
        assertThat(small.isLessThan(large)).isTrue();
        assertThat(small.isGreaterThan(large)).isFalse();
    }

    @Test
    void compareTo() {
        Money tenDollars = Money.of("10.00", "USD");
        Money twentyDollars = Money.of("20.00", "USD");
        Money anotherTen = Money.of("10.00", "USD");

        assertThat(tenDollars.compareTo(twentyDollars)).isNegative();
        assertThat(twentyDollars.compareTo(tenDollars)).isPositive();
        assertThat(tenDollars.compareTo(anotherTen)).isZero();
    }

    @Test
    void equalityByValueNotReference() {
        Money firstInstance = Money.of("100.00", "USD");
        Money secondInstance = Money.of("100.00", "USD");

        assertThat(firstInstance).isEqualTo(secondInstance);
        assertThat(firstInstance.hashCode()).isEqualTo(secondInstance.hashCode());
    }

    @Test
    void differentCurrenciesNotEqual() {
        Money usd = Money.of("100.00", "USD");
        Money eur = Money.of("100.00", "EUR");

        assertThat(usd).isNotEqualTo(eur);
    }

    @Test
    void nullAmountThrows() {
        assertThatThrownBy(() -> new Money(null, Currency.getInstance("USD")))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullCurrencyThrows() {
        assertThatThrownBy(() -> new Money(BigDecimal.TEN, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void toStringFormat() {
        Money money = Money.of("42.50", "USD");

        assertThat(money.toString()).isEqualTo("42.50 USD");
    }

    @Test
    void ofMinorUnitsUsd() {
        Money money = Money.ofMinorUnits(1050, "USD");

        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("10.50"));
        assertThat(money.getCurrency()).isEqualTo(Currency.getInstance("USD"));
    }

    @Test
    void ofMinorUnitsJpy() {
        Money money = Money.ofMinorUnits(1000, "JPY");

        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("1000"));
        assertThat(money.getAmount().scale()).isEqualTo(0);
    }

    @Test
    void ofMinorUnitsKwd() {
        Money money = Money.ofMinorUnits(1500, "KWD");

        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("1.500"));
        assertThat(money.getAmount().scale()).isEqualTo(3);
    }

    @Test
    void toMinorUnitsUsd() {
        Money money = Money.of("10.50", "USD");

        assertThat(money.toMinorUnits()).isEqualTo(1050L);
    }

    @Test
    void toMinorUnitsJpy() {
        Money money = Money.of("1000", "JPY");

        assertThat(money.toMinorUnits()).isEqualTo(1000L);
    }

    @Test
    void toMinorUnitsKwd() {
        Money money = Money.of("1.500", "KWD");

        assertThat(money.toMinorUnits()).isEqualTo(1500L);
    }

    @Test
    void minorUnitsRoundTrip() {
        Money original = Money.of("99.99", "USD");
        long minorUnits = original.toMinorUnits();
        Money restored = Money.ofMinorUnits(minorUnits, "USD");

        assertThat(restored).isEqualTo(original);
    }

    @Test
    void ofWithCurrencyObject() {
        Currency eur = Currency.getInstance("EUR");
        Money money = Money.of(new BigDecimal("25.50"), eur);

        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("25.50"));
        assertThat(money.getCurrency()).isEqualTo(eur);
    }

    @Test
    void kwdThreeDecimalPlaces() {
        Money money = Money.of("1.234", "KWD");

        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("1.234"));
        assertThat(money.getAmount().scale()).isEqualTo(3);
    }

    @Test
    void kwdRejectsFourDecimalPlaces() {
        assertThatThrownBy(() -> Money.of("1.2345", "KWD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("KWD");
    }
}
