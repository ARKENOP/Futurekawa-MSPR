package com.futurekawa.backendlocal.dto.request;

import jakarta.validation.constraints.NotNull;
import com.futurekawa.backendlocal.model.enums.StatutLot;

/**
 * DTO for updating a Lot's status via REST API.
 */
public record UpdateLotRequest(
        @NotNull StatutLot statutLot
) {}
