package com.futurekawa.backendcentral.client;

import java.time.LocalDate;

/**
 * Corps réellement envoyé à POST {local}/api/v1/lots. Le backend local n'a pas la
 * notion de codePays (il est déjà mono-pays) : le CreateLotRequest du frontend porte
 * codePays uniquement pour le routage central, et ce champ est retiré ici avant relais.
 */
record LocalCreateLotPayload(
        String referenceLot,
        LocalDate dateEntreeStockage,
        LocalDate dateRecolte,
        String qualiteLot,
        Long exploitationId,
        Long entrepotId
) {
}
