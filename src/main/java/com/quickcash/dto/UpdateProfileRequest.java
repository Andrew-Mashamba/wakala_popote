package com.quickcash.dto;

import lombok.Data;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
public class UpdateProfileRequest {
    private String displayName;
    private String email;
    private String phoneNumber;
    private String photoUrl;
    private String nidaNumber;
    /** 4-digit PIN (optional; hashed before storage). */
    @Size(min = 4, max = 4)
    @Pattern(regexp = "\\d{4}", message = "PIN must be 4 digits")
    private String pin;
}
