package com.futurekawa.lib.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLotRequest(
        @NotBlank(message = "referenceLot is required")
        @Size(max = 50)
        String referenceLot,

        @NotNull(message = "dateEntreeStockage is required")
        LocalDate dateEntreeStockage,

        LocalDate dateRecolte,

        @Size(max = 255)
        String qualiteLot,

        @NotNull(message = "exploitationId is required")
        Long exploitationId,

        @NotNull(message = "entrepotId is required")
        Long entrepotId
) {}
