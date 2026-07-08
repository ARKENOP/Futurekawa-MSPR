package com.futurekawa.backendcentral.dto.response;

import java.math.BigDecimal;

/** Miroir de PaysResponse (backend-local). */
public record Pays(
        Long id,
        String codePays,
        String nomPays,
        BigDecimal temperatureIdealeC,
        BigDecimal humiditeIdealePourcent,
        BigDecimal toleranceTemperatureC,
        BigDecimal toleranceHumiditePourcent,
        Boolean estActif
) {
}
