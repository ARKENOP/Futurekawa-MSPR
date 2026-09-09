package com.futurekawa.lib.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MesureStockageResponse(
        Long id,
        String idCapteur,
        LocalDateTime dateHeureMesure,
        BigDecimal temperatureC,
        BigDecimal humiditePourcent,
        Long entrepotId,
        Long lotId
) {}
