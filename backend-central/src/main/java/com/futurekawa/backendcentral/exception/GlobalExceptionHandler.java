package com.futurekawa.backendcentral.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LocalBackendUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleUnavailable(LocalBackendUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(LocalDateTime.now(), HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "Service Unavailable", ex.getMessage()));
    }

    @ExceptionHandler(UnknownCountryException.class)
    public ResponseEntity<ErrorResponse> handleUnknownCountry(UnknownCountryException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(LocalDateTime.now(), HttpStatus.NOT_FOUND.value(),
                        "Not Found", ex.getMessage()));
    }
}
