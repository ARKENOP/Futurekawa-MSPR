package com.futurekawa.backendcentral.dto.envelope;

/** Groupe une page (paginée) par pays d'origine (§4.4/§4.5 du contrat). */
public record CountryPageGroup<T>(
        String codePays,
        String nomPays,
        PageDto<T> page
) {
}
