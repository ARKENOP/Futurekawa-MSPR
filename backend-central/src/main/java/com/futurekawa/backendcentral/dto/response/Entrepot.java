package com.futurekawa.backendcentral.dto.response;

/** Miroir de EntrepotResponse (backend-local). */
public record Entrepot(
        Long id,
        String nomEntrepot,
        String localisation,
        Integer capaciteMax,
        String statutEntrepot,
        Long exploitationId,
        Long paysId
) {
}
