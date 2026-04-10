package com.eventledger.domain.valueobject;

import com.eventledger.domain.exception.UnsupportedCurrencyException;

import java.util.Currency;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum SupportedCurrency {

    USD("USD", 2),
    EUR("EUR", 2),
    GBP("GBP", 2),
    JPY("JPY", 0),
    KWD("KWD", 3),
    BHD("BHD", 3),
    OMR("OMR", 3);

    private static final Map<String, SupportedCurrency> BY_CODE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(SupportedCurrency::code, c -> c));

    private final Currency javaCurrency;
    private final int fractionDigits;

    SupportedCurrency(String code, int fractionDigits) {
        this.javaCurrency = Currency.getInstance(code);
        this.fractionDigits = fractionDigits;
    }

    public static SupportedCurrency fromCode(String code) {
        if (code == null) {
            throw new UnsupportedCurrencyException("null");
        }
        SupportedCurrency result = BY_CODE.get(code.toUpperCase());
        if (result == null) {
            throw new UnsupportedCurrencyException(code);
        }
        return result;
    }

    public static void validate(String currencyCode) {
        fromCode(currencyCode);
    }

    public String code() {
        return javaCurrency.getCurrencyCode();
    }

    public Currency toJavaCurrency() {
        return javaCurrency;
    }

    public int fractionDigits() {
        return fractionDigits;
    }
}
