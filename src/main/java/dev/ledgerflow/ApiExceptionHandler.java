package dev.ledgerflow;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(DomainException.class)
    ResponseEntity<Api.ErrorResponse> domain(DomainException e) { return error(e.status, e.code, e.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Api.ErrorResponse> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream().map(f -> f.getField()+": "+f.getDefaultMessage()).sorted().reduce((a,b) -> a+"; "+b).orElse("Invalid request");
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
    }
    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    ResponseEntity<Api.ErrorResponse> malformed(Exception e) { return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Check fields, UUIDs, JSON and pagination limits"); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Api.ErrorResponse> conflict(DataIntegrityViolationException e) { return error(HttpStatus.CONFLICT, "WRITE_CONFLICT", "Transfer request conflicted with a stored record; retry with the same request ID to retrieve its result"); }
    @ExceptionHandler(PessimisticLockingFailureException.class)
    ResponseEntity<Api.ErrorResponse> lock(PessimisticLockingFailureException e) { return error(HttpStatus.CONFLICT, "RETRYABLE_LOCK_CONFLICT", "Wallet is busy; retry with the same request ID"); }
    private ResponseEntity<Api.ErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new Api.ErrorResponse(code, message, Instant.now()));
    }
}
