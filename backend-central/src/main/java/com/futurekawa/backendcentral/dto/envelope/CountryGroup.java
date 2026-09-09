package com.futurekawa.backendcentral.dto.envelope;

import java.util.List;

public record CountryGroup<T>(
        String codePays,
        String nomPays,
        List<T> data
) {
}
