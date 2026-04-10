package com.eventledger.domain.enums;

public enum AccountType {

    MERCHANT(false),
    PLATFORM(true),
    ESCROW(false),
    PAYOUT(false);

    private final boolean allowsNegativeBalance;

    AccountType(boolean allowsNegativeBalance) {
        this.allowsNegativeBalance = allowsNegativeBalance;
    }

    public boolean allowsNegativeBalance() {
        return allowsNegativeBalance;
    }
}
