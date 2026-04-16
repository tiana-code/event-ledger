package com.eventledger.domain;

import com.eventledger.exception.UnsupportedCurrencyException;
import com.eventledger.domain.valueobject.SupportedCurrency;
import org.junit.jupiter.api.Test;

import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupportedCurrencyTest {

    @Test
    void fromCodeReturnsEnumConstantForKnownCode() {
        assertThat(SupportedCurrency.fromCode("USD")).isEqualTo(SupportedCurrency.USD);
    }

    @Test
    void fromCodeIsCaseInsensitive() {
        assertThat(SupportedCurrency.fromCode("usd"))
                .isEqualTo(SupportedCurrency.USD);
    }

    @Test
    void fromCodeThrowsForUnknownCode() {
        assertThatThrownBy(() -> SupportedCurrency.fromCode("XYZ"))
                .isInstanceOf(UnsupportedCurrencyException.class)
                .hasMessageContaining("XYZ");
    }

    @Test
    void fromCodeThrowsForNull() {
        assertThatThrownBy(() -> SupportedCurrency.fromCode(null))
                .isInstanceOf(UnsupportedCurrencyException.class);
    }

    @Test
    void validateDoesNotThrowForSupportedCode() {
        org.assertj.core.api.Assertions.assertThatNoException()
                .isThrownBy(() -> SupportedCurrency.validate("USD"));
    }

    @Test
    void validateThrowsForUnsupportedCode() {
        assertThatThrownBy(() -> SupportedCurrency.validate("XYZ"))
                .isInstanceOf(UnsupportedCurrencyException.class)
                .hasMessageContaining("XYZ");
    }

    @Test
    void fractionDigitsForUsd() {
        assertThat(SupportedCurrency.USD.fractionDigits()).isEqualTo(2);
    }

    @Test
    void fractionDigitsForJpy() {
        assertThat(SupportedCurrency.JPY.fractionDigits()).isEqualTo(0);
    }

    @Test
    void fractionDigitsForKwd() {
        assertThat(SupportedCurrency.KWD.fractionDigits()).isEqualTo(3);
    }

    @Test
    void toJavaCurrencyReturnsCorrectInstance() {
        assertThat(SupportedCurrency.USD.toJavaCurrency()).isEqualTo(Currency.getInstance("USD"));
        assertThat(SupportedCurrency.EUR.toJavaCurrency()).isEqualTo(Currency.getInstance("EUR"));
        assertThat(SupportedCurrency.JPY.toJavaCurrency()).isEqualTo(Currency.getInstance("JPY"));
    }

    @Test
    void codeReturnsUppercaseString() {
        assertThat(SupportedCurrency.USD.code()).isEqualTo("USD");
        assertThat(SupportedCurrency.EUR.code()).isEqualTo("EUR");
        assertThat(SupportedCurrency.JPY.code()).isEqualTo("JPY");
    }
}
