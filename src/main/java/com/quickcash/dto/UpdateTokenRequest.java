package com.quickcash.dto;

import lombok.Data;

@Data
public class UpdateTokenRequest {

    private String userId;
    private String firebaseId;
    private String firebaseToken;
}
