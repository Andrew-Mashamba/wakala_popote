package com.quickcash.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class DepositTrackResponse {

    private UUID depositId;
    private String status;
    private Double agentLat;
    private Double agentLng;
    private String agentDisplayName;
}
