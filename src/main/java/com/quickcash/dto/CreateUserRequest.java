package com.quickcash.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank(message = "uid is required")
    private String uid;  // Firebase UID

    private String phoneNumber;
    private String displayName;
    private String email;
    private Boolean emailVerified;
    private Boolean isAnonymous;
    private String photoURL;
    private String token; // FCM token
}
