package com.futurekawa.backendlocal.exception;

import java.net.URI;

import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.futurekawa.backendlocal.exception.ResourceNotFoundException;

/**
 * Global exception handler providing RFC 7807 ProblemDetail responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(URI.create("https://api.futurekawa.com/errors/not-found"));
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for request");
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://api.futurekawa.com/errors/validation-failed"));

        // Add detailed field errors
        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        problemDetail.setProperty("invalid_fields", fieldErrors);

        return problemDetail;
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handleInvalidSortProperty(PropertyReferenceException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Invalid 'sort' property: '" + ex.getPropertyName() + "'. Use an existing field, e.g. 'dateHeureMesure,desc'.");
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://api.futurekawa.com/errors/invalid-sort"));
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://api.futurekawa.com/errors/internal-error"));
        // Don't expose internal exception messages to the client for security, but log them ideally.
        return problemDetail;
    }
}
