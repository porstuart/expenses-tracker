package com.budget.model.authentication;

public final class AuthenticationResult {

    private final boolean success;
    private final String errorMessage;

    private AuthenticationResult(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static AuthenticationResult successAuthenticated() {
        return new AuthenticationResult(true, null);
    }

    public static AuthenticationResult failure(String errorMessage) {
        return new AuthenticationResult(false, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
