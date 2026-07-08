package com.futurekawa.backendcentral.dto.request;

import jakarta.validation.constraints.NotNull;

import com.futurekawa.backendcentral.dto.enums.StatutLot;

public record UpdateLotRequest(
        @NotNull StatutLot statutLot
) {
}
