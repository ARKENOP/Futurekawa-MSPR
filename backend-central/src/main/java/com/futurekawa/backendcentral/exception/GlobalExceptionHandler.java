package com.futurekawa.backendcentral.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(LocalBackendUnavailableException.class)
    public ProblemDetail handleUnavailable(LocalBackendUnavailableException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problemDetail.setTitle("Service Unavailable");
        problemDetail.setType(URI.create("https://api.futurekawa.com/errors/local-backend-unavailable"));
        problemDetail.setProperty("codePays", ex.getCodePays());
        return problemDetail;
    }

    @ExceptionHandler(UnknownCountryException.class)
    public ProblemDetail handleUnknownCountry(UnknownCountryException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Not Found");
        problemDetail.setType(URI.create("https://api.futurekawa.com/errors/unknown-country"));
        problemDetail.setProperty("codePays", ex.getCodePays());
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Validation failed for request");
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://api.futurekawa.com/errors/validation-failed"));

        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        problemDetail.setProperty("invalid_fields", fieldErrors);

        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://api.futurekawa.com/errors/internal-error"));
        return problemDetail;
    }
}
