package com.quickcash.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PhonePinLoginRequest {

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "PIN is required")
    @Size(min = 4, max = 4)
    @Pattern(regexp = "\\d{4}", message = "PIN must be 4 digits")
    private String pin;
}
