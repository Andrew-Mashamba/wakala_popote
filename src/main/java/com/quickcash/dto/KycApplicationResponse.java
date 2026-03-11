package com.quickcash.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class KycApplicationResponse {

    private UUID id;
    private String status;
    private String fullName;
    private LocalDate dateOfBirth;
    private String nidaNumber;
    private String phoneNumber;
    private Boolean phoneVerified;
    private Boolean nidaVerified;
    private Boolean faceMatchPassed;
    private Boolean livenessCheckPassed;
    private Instant createdAt;
    private Instant submittedAt;
    private String rejectionReason;
    private String selcomAccountId;
}
