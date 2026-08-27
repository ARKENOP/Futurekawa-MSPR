package com.futurekawa.lib.dto.response;

public record EntrepotResponse(
        Long id,
        String nomEntrepot,
        String localisation,
        Integer capaciteMax,
        String statutEntrepot,
        Long exploitationId,
        Long paysId
) {}
