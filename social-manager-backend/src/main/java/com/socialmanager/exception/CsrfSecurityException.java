package com.socialmanager.exception;

public class CsrfSecurityException extends RuntimeException {
    public CsrfSecurityException(String message) {
        super(message);
    }
}
