package com.eventledger.dto.response;

import com.eventledger.domain.enums.ManifestStage;

import java.util.UUID;

public record ManifestResponse(
        UUID manifestId,
        ManifestStage finalStage,
        boolean settled,
        String message
) {
}
