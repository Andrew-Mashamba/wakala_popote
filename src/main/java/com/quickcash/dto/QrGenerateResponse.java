package com.quickcash.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class QrGenerateResponse {

    private UUID requestId;
    private String qrToken;
    private Instant expiresAt;
    private int expiresInSeconds;
}
