package com.eventledger.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "event-ledger")
public record EventLedgerConfig(
        @DefaultValue BatchLock batchLock,
        @DefaultValue ManifestProcessing manifestProcessing,
        @DefaultValue Cleanup cleanup,
        @DefaultValue Idempotency idempotency,
        @DefaultValue Transaction transaction
) {
    public record BatchLock(
            @DefaultValue("30") @Positive
            long ttlSeconds,

            @DefaultValue("5000") @Min(100)
            long acquireTimeoutMillis
    ) {
    }

    public record ManifestProcessing(
            @DefaultValue("3") @Positive
            int maxRetryAttempts,

            @DefaultValue("500") @Min(100)
            long retryDelayMillis
    ) {
    }

    public record Cleanup(
            @DefaultValue("300") @Positive
            long expiredLockCleanupIntervalSeconds
    ) {
    }

    public record Idempotency(
            @DefaultValue("30") @Positive
            int retentionDays
    ) {
    }

    public record Transaction(
            @DefaultValue("30") @Positive
            int timeoutSeconds
    ) {
    }
}
