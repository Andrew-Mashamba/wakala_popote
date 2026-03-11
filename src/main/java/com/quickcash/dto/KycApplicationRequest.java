package com.quickcash.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class KycApplicationRequest {

    @NotBlank
    private String fullName;

    @NotNull
    private LocalDate dateOfBirth;

    private String gender; // MALE, FEMALE

    private String nationality;

    @NotBlank
    private String nidaNumber;

    @NotBlank
    private String phoneNumber;

    private String email;
}
