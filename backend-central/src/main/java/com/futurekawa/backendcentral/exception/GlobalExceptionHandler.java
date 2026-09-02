package com.futurekawa.backendcentral.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler(HttpClientErrorException.class)
    public ProblemDetail handleLocalClientError(HttpClientErrorException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                status != null ? status : HttpStatus.BAD_REQUEST,
                "The country backend rejected the request: " + ex.getStatusText());
        problemDetail.setTitle(status != null ? status.getReasonPhrase() : "Bad Request");
        problemDetail.setType(URI.create("https://api.futurekawa.com/errors/local-backend-rejected"));
        return problemDetail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.warn("Unreadable request body: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "The request body could not be read. Check the JSON syntax and that every "
                        + "status or type value is one the API accepts.");
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://api.futurekawa.com/errors/unreadable-body"));
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleParameterTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '" + ex.getName() + "'.");
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://api.futurekawa.com/errors/invalid-parameter"));
        problemDetail.setProperty("parameter", ex.getName());
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
