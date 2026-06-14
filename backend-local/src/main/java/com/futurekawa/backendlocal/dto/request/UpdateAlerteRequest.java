package com.futurekawa.backendlocal.dto.request;

import jakarta.validation.constraints.NotNull;
import com.futurekawa.backendlocal.model.enums.StatutAlerte;

/**
 * DTO for updating an Alerte's status via REST API (e.g., closing).
 */
public record UpdateAlerteRequest(
        @NotNull StatutAlerte statutAlerte
) {}
