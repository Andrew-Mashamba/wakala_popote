package com.quickcash.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateLocationRequestV1 {
    @NotNull
    private Double latitude;
    @NotNull
    private Double longitude;
    private String address;
    private String subLocality;
    private String locality;
    private String administrativeArea;
    private String postalCode;
    private String country;
}
