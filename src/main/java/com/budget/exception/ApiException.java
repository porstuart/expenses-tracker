package com.budget.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        if (status == null) {
            throw new IllegalArgumentException("HttpStatus cannot be null");
        }
        this.status = status;
    }

    public ApiException(HttpStatus status, String message, Throwable exception) {
        super(message, exception);
        if (status == null) {
            throw new IllegalArgumentException("HttpStatus cannot be null");
        }
        this.status = status;
    }

    public ApiException(HttpStatus status, Throwable exception) {
        super(exception);
        if (status == null) {
            throw new IllegalArgumentException("HttpStatus cannot be null");
        }
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

}
