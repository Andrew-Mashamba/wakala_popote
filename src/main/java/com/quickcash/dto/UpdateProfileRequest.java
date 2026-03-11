package com.quickcash.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String displayName;
    private String email;
    private String phoneNumber;
    private String photoUrl;
}
