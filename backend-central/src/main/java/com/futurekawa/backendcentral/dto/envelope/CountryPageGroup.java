package com.futurekawa.backendcentral.dto.envelope;

public record CountryPageGroup<T>(
        String codePays,
        String nomPays,
        PageDto<T> page
) {
}
