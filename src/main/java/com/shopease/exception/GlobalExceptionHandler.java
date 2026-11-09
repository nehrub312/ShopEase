package com.shopease.exception;

import com.shopease.dto.ApiError;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Converts exceptions thrown by the REST controllers into friendly JSON (no stack traces). */
@RestControllerAdvice(annotations = org.springframework.web.bind.annotation.RestController.class)
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), status.getReasonPhrase(), message));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> notFound(ResourceNotFoundException ex) { return build(HttpStatus.NOT_FOUND, ex.getMessage()); }

    @ExceptionHandler({InvalidQuantityException.class, EmptyCartException.class})
    public ResponseEntity<ApiError> badRequest(RuntimeException ex) { return build(HttpStatus.BAD_REQUEST, ex.getMessage()); }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiError> stock(InsufficientStockException ex) { return build(HttpStatus.CONFLICT, ex.getMessage()); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getDefaultMessage()).collect(Collectors.joining(" "));
        return build(HttpStatus.BAD_REQUEST, msg.isBlank() ? "Invalid request." : msg);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> unreadable(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "Invalid request. Please check the values you entered.");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> database(DataAccessException ex) {
        log.error("Database error", ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, "The database is temporarily unavailable. Please try again shortly.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> other(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse er) {
            HttpStatus st = HttpStatus.valueOf(er.getStatusCode().value());
            return build(st, "The request could not be processed.");
        }
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side. Please try again.");
    }
}
