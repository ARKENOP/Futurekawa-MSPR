package com.futurekawa.lib.dto.response;

import java.math.BigDecimal;

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
