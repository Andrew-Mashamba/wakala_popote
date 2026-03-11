package com.quickcash.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class B2bDisbursementRequest {

    @NotNull
    private java.util.UUID businessUserId;

    @Valid
    @NotNull
    @Size(min = 1, max = 500)
    private List<B2bDisbursementItem> items;
}
