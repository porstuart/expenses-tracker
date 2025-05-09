package com.budget.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    
    private HttpStatus status;

    public ApiException(HttpStatus status, String message ) {
        super(message);
        this.status = status;
    }

    public ApiException(String message) {
        super(message);
    }

    public ApiException(String message, Throwable exception) {
        super(message, exception);
    }

    public ApiException(HttpStatus status, Throwable exception) {
        super(exception);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

}
