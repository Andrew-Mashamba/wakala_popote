package com.quickcash.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgentRegisterRequest {
    @NotBlank
    private String selcomAccountId;
    private String selcomAccountName;
}
