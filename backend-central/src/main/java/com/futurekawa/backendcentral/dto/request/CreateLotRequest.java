package com.futurekawa.backendcentral.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corps de POST /api/v1/lots. codePays route la création vers le bon backend local
 * (§4.8 du contrat) et n'est pas transmis tel quel au local (déjà implicite là-bas).
 */
public record CreateLotRequest(
        @NotBlank(message = "codePays is required")
        String codePays,

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
