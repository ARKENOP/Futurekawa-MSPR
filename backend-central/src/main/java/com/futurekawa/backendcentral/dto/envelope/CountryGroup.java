package com.futurekawa.backendcentral.dto.envelope;

import java.util.List;

/** Groupe une liste simple (non paginée) par pays d'origine (§4.2/§4.3 du contrat). */
public record CountryGroup<T>(
        String codePays,
        String nomPays,
        List<T> data
) {
}
