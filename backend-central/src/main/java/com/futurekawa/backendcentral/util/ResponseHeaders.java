package com.futurekawa.backendcentral.util;

import java.util.List;

import org.springframework.http.ResponseEntity;

public final class ResponseHeaders {

    private static final String UNAVAILABLE_HEADER = "X-Unavailable-Countries";

    private ResponseHeaders() {
    }

    public static ResponseEntity.BodyBuilder withUnavailable(List<String> unavailable) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (!unavailable.isEmpty()) {
            builder = builder.header(UNAVAILABLE_HEADER, String.join(",", unavailable));
        }
        return builder;
    }
}
