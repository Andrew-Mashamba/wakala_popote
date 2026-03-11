package com.quickcash.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    private UUID id;
    private String uid;
    private String displayName;
    private String email;
    private String phoneNumber;
    private String photoUrl;
    private Double latitude;
    private Double longitude;
    private String address;
    private String userType;
}
