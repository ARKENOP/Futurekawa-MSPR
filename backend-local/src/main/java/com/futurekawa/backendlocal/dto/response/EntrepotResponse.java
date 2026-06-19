package com.futurekawa.backendlocal.dto.response;

/**
 * DTO for returning Entrepot details to the REST API.
 */
public record EntrepotResponse(
        Long id,
        String nomEntrepot,
        String localisation,
        Integer capaciteMax,
        String statutEntrepot,
        Long exploitationId,
        Long paysId
) {}
