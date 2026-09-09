package com.futurekawa.lib.dto.request;

import jakarta.validation.constraints.NotNull;

import com.futurekawa.lib.enums.StatutLot;

public record UpdateLotRequest(
        @NotNull StatutLot statutLot
) {}
