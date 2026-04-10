package com.eventledger.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public record ProcessManifestRequest(
        @NotNull
        UUID manifestId,

        @NotNull
        UUID sourceAccountId,

        @NotNull
        UUID destinationAccountId,

        @NotBlank @Size(max = 255)
        String idempotencyKey,

        @Size(max = 20)
        Map<String, String> metadata
) {
}
