package com.futurekawa.backendlocal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for returning MesureStockage details to the REST API.
 */
public record MesureStockageResponse(
        Long id,
        String idCapteur,
        LocalDateTime dateHeureMesure,
        BigDecimal temperatureC,
        BigDecimal humiditePourcent,
        Long entrepotId,
        Long lotId
) {}
