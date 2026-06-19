package com.futurekawa.backendlocal.dto.response;

import java.math.BigDecimal;

/**
 * DTO for returning Pays details to the REST API.
 */
public record PaysResponse(
        Long id,
        String codePays,
        String nomPays,
        BigDecimal temperatureIdealeC,
        BigDecimal humiditeIdealePourcent,
        BigDecimal toleranceTemperatureC,
        BigDecimal toleranceHumiditePourcent,
        Boolean estActif
) {}
