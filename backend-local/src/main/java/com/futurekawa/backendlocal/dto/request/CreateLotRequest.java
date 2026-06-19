package com.futurekawa.backendlocal.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLotRequest(
        @NotBlank(message = "Reference lot is required")
        @Size(max = 50)
        String referenceLot,

        @NotNull(message = "Storage entry date is required")
        LocalDate dateEntreeStockage,

        LocalDate dateRecolte,

        @Size(max = 255)
        String qualiteLot,

        @NotNull(message = "Exploitation ID is required")
        Long exploitationId,

        @NotNull(message = "Entrepôt ID is required")
        Long entrepotId
) {
}
