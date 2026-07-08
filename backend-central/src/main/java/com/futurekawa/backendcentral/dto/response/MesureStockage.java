package com.futurekawa.backendcentral.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Miroir de MesureStockageResponse (backend-local). */
public record MesureStockage(
        Long id,
        String idCapteur,
        LocalDateTime dateHeureMesure,
        BigDecimal temperatureC,
        BigDecimal humiditePourcent,
        Long entrepotId,
        Long lotId
) {
}
