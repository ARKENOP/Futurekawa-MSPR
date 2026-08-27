package com.futurekawa.lib.dto.request;

import jakarta.validation.constraints.NotNull;

import com.futurekawa.lib.enums.StatutAlerte;

public record UpdateAlerteRequest(
        @NotNull StatutAlerte statutAlerte
) {}
