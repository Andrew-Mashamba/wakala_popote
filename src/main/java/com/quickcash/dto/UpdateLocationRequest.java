package com.quickcash.dto;

import lombok.Data;

@Data
public class UpdateLocationRequest {

    private String userId;
    private Double latitude;
    private Double longitude;
    private String address;
    private String subLocality;
    private String locality;
    private String administrativeArea;
    private String postalCode;
    private String country;
}
