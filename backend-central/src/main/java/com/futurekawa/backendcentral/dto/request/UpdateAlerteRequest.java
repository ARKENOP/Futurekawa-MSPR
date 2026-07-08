package com.futurekawa.backendcentral.dto.request;

import jakarta.validation.constraints.NotNull;

import com.futurekawa.backendcentral.dto.enums.StatutAlerte;

public record UpdateAlerteRequest(
        @NotNull StatutAlerte statutAlerte
) {
}
