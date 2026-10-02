package dev.ledgerflow;

import org.springframework.http.HttpStatus;

public class DomainException extends RuntimeException {
    final String code;
    final HttpStatus status;
    DomainException(String code, String message, HttpStatus status) {
        super(message); this.code = code; this.status = status;
    }
}
